package remi.distributedFS.db.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.log.Logs;
import remi.distributedFS.util.Ref;

public class FsFileFromFile extends FsObjectImplFromFile implements FsFile {

	int currentChunkSize = -1;
	List<FsChunk> chunks;
	List<FsChunkFromFile> allChunks;
	
	public FsFileFromFile(FsTableLocal master, long sectorId, FsDirectory parent) {
		super(master, sectorId, parent);
//		chunks = new ListeningArrayList<>(this.getDirty());
		chunks = new ArrayList<>();
		allChunks = new ArrayList<>();
	}

	public void load(ByteBuffer buffer){
		//check if it's a file
		byte type = buffer.get();
		if(type != FsTableLocal.FILE){
			Logs.logDb.warning("Error, not a file at "+getSector());
			throw new WrongSectorTypeException("Error, not a file at "+getSector());
		}
		
		super.load(buffer);
		//now, it should be at pos ~337
		
		//now read entries.
		//i have 656 bytes in this sector. 82 long
		currentChunkSize = buffer.getInt();
		buffer.getInt(); //not used
		int nbAllChunks = buffer.getInt();
		int nbChunks = buffer.getInt();

		//ensure long-distance from end
		buffer.position(buffer.position() + 8 - (buffer.position()%8));
		//get how many int read we can do
		//wrong int canRead =  FsTableLocal.FS_SECTOR_SIZE/4 - (buffer.position()/4 + 2);
		int canRead =  FsTableLocal.FS_SECTOR_SIZE/4 - (buffer.position()/4 + 2);
		
		ByteBuffer currentBuffer = buffer;
//		long currentSector = this.getId();
		Ref<Integer> sectorNum = new Ref<>(0);
		//read AllChunks
		for(int i=0;i<nbAllChunks;i++){
			allChunks.add(master.factory.createChunk(master, currentBuffer.getLong(), this,i));
			canRead-=2;
			if(canRead == 0){
				canRead = goToNextAndLoad(currentBuffer, sectorNum)*2;
			}
		}
		//note: it works because i write longs before ints

		for(int ii=0;ii<currentBuffer.limit();ii++){System.out.print(currentBuffer.get(ii)+" "); }
		//read chunk pos
		for(int i=0;i<nbChunks;i++){
			chunks.add(allChunks.get(currentBuffer.getInt()));
			canRead--;
			if(canRead == 0){
				canRead = goToNextAndLoad(currentBuffer, sectorNum)*2;
			}
		}
	}
	
	public synchronized void save(ByteBuffer buffer){
		//set "erased" or d"directory"
		if(parentId<0){
			buffer.put(FsTableLocal.ERASED);
			//TODO: erase also "extended" chunks
//		}else if(deleteDate>0){
//			buffer.put(FsTableLocal.DELETED);
		}else{
			buffer.put(FsTableLocal.FILE);
		}
		super.save(buffer);

		//now, it should be at pos ~337
		
		//now read entries.
		//i have 656 bytes in this sector. 82 long
		buffer.putInt(currentChunkSize);
		buffer.putInt(0); //not used (yet)
		buffer.putInt(allChunks.size());
		buffer.putInt(chunks.size());

		//ensure long-distance from end
		buffer.position(buffer.position() + 8 - (buffer.position()%8));
		//get how many int read we can do
		int canRead =  FsTableLocal.FS_SECTOR_SIZE/4 - (buffer.position()/4 + 2);
		
		ByteBuffer currentBuffer = buffer;
//		Ref<Long> currentSector = new Ref<>(this.getSector());
		Ref<Integer> sectorNum = new Ref<>(0);
		//write chunks
		for(int i=0;i<allChunks.size();i++){
			currentBuffer.putLong((allChunks.get(i)).getSector());
			canRead-=2; //1 long  = 2 ints
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, sectorNum)*2; // *2 to pass in int
			}
		}
		
		//note: it works because i write longs before ints
		
		//write chunks order
		for(int i=0;i<chunks.size();i++){
			currentBuffer.putInt(allChunks.indexOf(chunks.get(i)));
			canRead--; // 1 int
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, sectorNum)*2; // *2 to pass in int
			}
		}

//		//fill last sector with zeros
		for(int ii=0;ii<currentBuffer.limit();ii++){System.out.print(currentBuffer.get(ii)+" ");}
		flushLastSector(currentBuffer, sectorNum);
//		Arrays.fill(currentBuffer.array(), currentBuffer.position(), currentBuffer.limit(), (byte)0);
//		//write last sector
//		currentBuffer.rewind();
//		master.saveSector(currentBuffer, currentSector.get());
		

		
		//also propagate to chunks descriptors.
		for(FsChunk chunk : chunks){
			currentBuffer.rewind();
			((FsChunkFromFile)chunk).save(currentBuffer);
		}
		
		setDirty(false);
	}

	@Override
	public int getNbChunks() {
		return chunks.size();
	}

	@Override
	public List<FsChunk> getChunks() {
//		return new ListeningArrayList<FsChunk>(chunks, e->{setDirty(true);},e->{setDirty(true);});
		return chunks;
	}
	
//	protected FsChunkFromFile createchunk(int num, int currentSize, int maxSize){
//
//		checkLoaded();
//		long newId = master.requestNewSector();
//		FsChunkFromFile newone = new FsChunkFromFile(master, newId, this, num);
//		newone.loaded = true;
//		newone.isValid = true;
//		newone.lastChange = System.currentTimeMillis();
////		newone.data = new File(master.getRootRep()+getPath()+(num==0?"":num));
////		newone.currentSize = currentSize; //FIXME: should be set by the chunk itself when we put data into it.
////		newone.maxSize = maxSize;
//		setDirty(true);
//		return newone;
//	}

	@Override
	public void accept(FsObjectVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public long getSize() {
		return FsFile.getSize(this);
	}


	@Override
	public Collection<FsChunk> getAllChunks() {
		return (List)allChunks;
	}


	@Override
	public FsChunk createNewChunk(long id) {
		Logs.logDb.info("Create new chunk from id : "+id);
		long newSectorId = master.requestNewSector();
		long goodId = id;
		if(id<=0){
//			newChunk.id = ((long)master.getComputerId())<<48 | (newSectorId&0xFFFFFFFFFFFFL);
			goodId = (((long)master.getComputerId())<<48 | (newSectorId&0xFFFFFFFFFFFFL)); //assume only 2^48 sector will be ever needed
//			Logs.logDb.info("Create new id : "+master.getComputerId()+" => "+(((long)master.getComputerId())<<48)+", sector = "+newSectorId +" => "+(newSectorId&0xFFFFFFFFFFFFL)
//					+" , so | ="+(((long)master.getComputerId())<<48 | (newSectorId&0xFFFFFFFFFFFFL))+", + = "+((((long)master.getComputerId())<<48) + (newSectorId&0xFFFFFFFFFFFFL)));
		}
		FsChunkFromFile newChunk = master.factory.createChunk(master, newSectorId, this, goodId);
		if(id<=0){
			newChunk.lastChange = System.currentTimeMillis();
			newChunk.isValid = true;
		}
		newChunk.loaded = true;
		allChunks.add(newChunk);
		setDirty(true);
		Logs.logDb.info("Create new chunk : sector="+newSectorId+", id = "+newChunk.getId());
//		new Exception().printStackTrace();
		return newChunk;
	}


	@Override
	public void setChunks(List<FsChunk> newList) {
		for(int i=0;i<newList.size();i++){
			((FsChunkFromFile)newList.get(i)).idx = i;
		}
		chunks = newList;
		setDirty(true);
	}


//	@Override
//	public void truncate(long newSize) {
//		
//		long currentSize = getSize();
//		
//		if(newSize>=currentSize){
//			Logs.logDb.warning("useless truncate on "+getPath());
//			return;
//		}
//		
//		int idxChunk = getNbChunks();
//		while(currentSize > newSize){
//			idxChunk--;
//			FsChunkFromFile chunk = chunks.get(idxChunk);
//			Logs.logDb.info("trauncate chunk size "+chunk.currentSize+" > ? "+currentSize+" - "+newSize+" = "+(currentSize-newSize));
//			if(chunk.currentSize < currentSize-newSize){
//				if(idxChunk ==0){
//					//just clean it
//					chunk.setCurrentSize(0);
//				}else{
//					//delete this chunk
//					chunks.remove(idxChunk);
//					Logs.logDb.warning("remove chunk "+idxChunk);
//					chunk.unallocate();
//					setDirty(true);
//				}
//			}else{
//				Logs.logDb.warning("reduce chunk from "+chunk.currentSize+" to "+(chunk.currentSize - currentSize + newSize));
//				chunk.truncate(chunk.currentSize - currentSize + newSize);
//			}
//			currentSize = getSize();
//		}
//	}

	@Override
	public void delete(){
		//delete content
		for(int i=0;i<chunks.size();i++){
			chunks.get(i).delete();
		}
		//delete self
		super.delete();
	}



	@Override
	public FsDirectory asDirectory() {
		return null;
	}
	
	@Override
	public FsFile asFile() {
		return this;
	}
	
}
