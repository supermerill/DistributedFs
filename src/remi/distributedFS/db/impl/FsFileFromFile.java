package remi.distributedFS.db.impl;

import java.nio.ByteBuffer;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.util.Ref;

public class FsFileFromFile extends FsObjectImplFromFile implements FsFile {

	int currentChunkSize = -1;
	List<FsChunk> chunks;
	
	public FsFileFromFile(FsTableLocal master, long sectorId, FsDirectory parent) {
		super(master, sectorId, parent);
		chunks = new ListeningArrayList<>(this.getDirty());
	}
	

	public void load(ByteBuffer buffer){
		//check if it's a file
		byte type = buffer.get();
		if(type !=2){
			System.err.println("Error, not a file at "+getId());
			throw new RuntimeException("Error, not a file at "+getId());
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
		
		int canRead = 80;
		ByteBuffer currentBuffer = buffer;
//		long currentSector = this.getId();
		//read folder
		for(int i=0;i<nbChunks;i++){
			chunks.add(new FsChunkFromFile(master, currentBuffer.getLong(), this));
			canRead--;
			if(canRead == 0){
				canRead = goToNext(currentBuffer);
			}
		}
	}
	
	public synchronized void save(ByteBuffer buffer){
		buffer.put((byte)2);
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
			currentBuffer.putLong(((FsChunkFromFile)chunks.get(i)).getId());
			canRead--;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, currentSector);
			}
		}
		setDirty(false);
	}

	@Override
	public int getNbChunks() {
		return chunks.size();
	}

	@Override
	public List<FsChunk> getChunks() {
		return chunks;
	}

	@Override
	public int getChunkSize() {
		return currentChunkSize;
	}

	@Override
	public void rearangeChunks(int newSizeChunk, int newNbChunks) {
		// TODO Auto-generated method stub

	}

	@Override
	public void accept(FsObjectVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public long getSize() {
		return FsFileMethods.getSize(this);
	}

}
