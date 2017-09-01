package remi.distributedFS.db.impl;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.datastruct.LoadErasedException;
import remi.distributedFS.util.Ref;

public class FsFileFromFile extends FsObjectImplFromFile implements FsFile {

	int currentChunkSize = -1;
	List<FsChunkFromFile> chunks;
	
	public FsFileFromFile(FsTableLocal master, long sectorId, FsDirectory parent) {
		super(master, sectorId, parent);
//		chunks = new ListeningArrayList<>(this.getDirty());
		chunks = new ArrayList<>();
	}
	

	public void load(ByteBuffer buffer){
		//check if it's a file
		byte type = buffer.get();
		if(type != FsTableLocal.FILE){
			System.err.println("Error, not a file at "+getId());
			throw new LoadErasedException("Error, not a file at "+getId());
		}
		
		super.load(buffer);
		//now, it should be at pos ~337
		
		//now read entries.
		//go to fixed pos (because it's simpler)
		buffer.position(360);
		//i have 656 bytes in this sector. 82 long
		currentChunkSize = buffer.getInt();
		buffer.getInt(); //not used
		long nbChunks = buffer.getLong();
		
		int canRead = ((FsTableLocal.FS_SECTOR_SIZE-360)/8)-3;
		ByteBuffer currentBuffer = buffer;
//		long currentSector = this.getId();
		//read folder
		for(int i=0;i<nbChunks;i++){
			chunks.add(new FsChunkFromFile(master, currentBuffer.getLong(), this,i));
			canRead--;
			if(canRead == 0){
				canRead = goToNext(currentBuffer);
			}
		}
	}
	
	public synchronized void save(ByteBuffer buffer){
		//set "erased" or d"directory"
		buffer.put(parentId<0?0:FsTableLocal.FILE);
		super.save(buffer);

		//now, it should be at pos ~337
		
		//now read entries.
		//go to fixed pos (because it's simpler)
		buffer.position(360);
		//i have 656 bytes in this sector. 82 long
		buffer.putInt(currentChunkSize);
		buffer.putInt(0); //not used
		buffer.putLong(chunks.size());
		
		int canRead = 80;
		ByteBuffer currentBuffer = buffer;
		Ref<Long> currentSector = new Ref<>(this.getId());
		//read folder
		for(int i=0;i<chunks.size();i++){
			currentBuffer.putLong((chunks.get(i)).getId());
			canRead--;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, currentSector);
			}
		}

		//write last sector
		currentBuffer.rewind();
		master.saveSector(currentBuffer, currentSector.get());
		

		
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
		System.out.println("chunks:"+chunks);
		return new ListeningArrayList<FsChunk>(chunks, this.getDirty());
	}

	@Override
	public int getChunkSize() {
		return currentChunkSize;
	}

	@Override
	public void rearangeChunks(int newSizeChunk, int newNbChunks) {
		currentChunkSize = newSizeChunk;
		//create chunks
		List<FsChunkFromFile> newchunks = new ArrayList<>();
		for(int i=0;i<newNbChunks-1;i++){
			newchunks.add(createchunk(i, newSizeChunk,newSizeChunk));
		}
		newchunks.add(createchunk(newNbChunks-1, 0,newSizeChunk));
		
		//TODO: copy data, in memory if possible, or use a different name and then rename
		
		this.chunks = newchunks;
	}
	
	protected FsChunkFromFile createchunk(int num, int currentSize, int maxSize){

		checkLoaded();
		long newId = master.requestNewSector();
		FsChunkFromFile newone = new FsChunkFromFile(master, newId, this, num);
		newone.loaded = true;
//		newone.data = new File(master.getRootRep()+getPath()+(num==0?"":num));
//		newone.currentSize = currentSize; //FIXME: should be set by the chunk itself when we put data into it.
//		newone.maxSize = maxSize;
		setDirty(true);
		return newone;
	}

	@Override
	public void accept(FsObjectVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public long getSize() {
		return FsFileMethods.getSize(this);
	}


	@Override
	public void truncate(long newSize) {
		
		long currentSize = getSize();
		
		if(newSize>=currentSize){
			System.err.println("useless truncate on "+getPath());
			return;
		}
		
		int idxChunk = getNbChunks();
		while(currentSize > newSize){
			idxChunk--;
			FsChunkFromFile chunk = chunks.get(idxChunk);
			System.out.println("trauncate chunk size "+chunk.currentSize+" > ? "+currentSize+" - "+newSize+" = "+(currentSize-newSize));
			if(chunk.currentSize < currentSize-newSize){
				if(idxChunk ==0){
					//just clean it
					chunk.truncate(0);
				}else{
					//delete this chunk
					chunks.remove(idxChunk);
					System.err.println("remove chunk "+idxChunk);
					chunk.unallocate();
					setDirty(true);
				}
			}else{
				System.err.println("reduce chunk from "+chunk.currentSize+" to "+(chunk.currentSize - currentSize + newSize));
				chunk.truncate(chunk.currentSize - currentSize + newSize);
			}
			currentSize = getSize();
		}
	}



}
