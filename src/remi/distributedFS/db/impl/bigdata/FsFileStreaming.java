package remi.distributedFS.db.impl.bigdata;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.db.impl.FsFileFromFile;
import remi.distributedFS.db.impl.FsTableLocal;
import remi.distributedFS.db.impl.WrongSectorTypeException;

public class FsFileStreaming extends FsFileFromFile implements FsFile {

	FsChunkStreaming streamer;
	
	public FsFileStreaming(FsTableLocal master, long sectorId, FsDirectory parent) {
		super(master, sectorId, parent);
		streamer = null;
	}
	

	public void load(ByteBuffer buffer){
		//check if it's a file
		byte type = buffer.get();
		if(type != FsTableLocal.FILE){
			System.err.println("Error, not a file at "+getSector());
			throw new WrongSectorTypeException("Error, not a file at "+getSector());
		}
		
		super.load(buffer);
		//now, it should be at pos ~337
		
		//now read entries.
		//i have 656 bytes in this sector. 82 long
		long chunkSector = buffer.getLong();
		long chunkId = buffer.getLong();

		if(streamer == null && chunkSector>0) {
			streamer = new FsChunkStreaming(master, chunkSector, this, chunkId);
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
		buffer.putLong(streamer==null?-1:streamer.getSector());
		buffer.putLong(streamer==null?-1:streamer.getId());
		
		setDirty(false);
	}

	@Override
	public int getNbChunks() {
		return streamer == null ? 0 : 1;
	}

	@Override
	public List<FsChunk> getChunks() {
		return streamer == null ? new ArrayList<>(0) : Arrays.asList(streamer);
	}
	
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
		return getChunks();
	}


	@Override
	public FsChunk createNewChunk(final long id) {
		System.out.println("Create new chunk from id : "+id);
		if(streamer==null) {
			long newSectorId = master.requestNewSector();
			long goodId = id;
			if(id<=0){
				goodId = (((long)master.getComputerId())<<48 | (newSectorId&0xFFFFFFFFFFFFL)); //assume only 2^48 sector will be ever needed
			}
			streamer = new FsChunkStreaming(master, newSectorId, this, goodId, true);
			setDirty(true);
			return streamer;
		}else {
			throw new RuntimeException("Error, can't create a second chunk for file "+getName());
		}
	}


	@Override
	public void setChunks(List<FsChunk> newList) {
		//nop
	}

	@Override
	public void delete(){
		//delete content
		streamer.delete();
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
