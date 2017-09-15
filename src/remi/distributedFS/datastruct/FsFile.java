package remi.distributedFS.datastruct;

import java.util.List;

import remi.distributedFS.util.ByteBuff;

public interface FsFile extends FsObject {

//	protected int chunkSize;
//	protected List<FsChunk> currentChunks = new ArrayList<>();
//	protected List<FsChunk> oldChunks = new ArrayList<>();

	public void accept(FsObjectVisitor visitor);
	
	/**
	 * get the number of chunks to store this file
	 * @return int, >=0
	 */
	public abstract int getNbChunks();
	
	/**
	 * get the current chunks for this file.
	 * @return all chunks. not null
	 */
	public abstract List<FsChunk> getChunks();

	/**
	 * Get the chunk size of all chunks before the last one.
	 * @return chunk size, can be 0 if nbCHunks < 2
	 */
	public abstract int getChunkSize();
	
	/**
	 * change the number of data (or how) stored by this file in chunks. 
	 * @param newSizeChunk new size of chunks. 
	 * Change this if there are enough data to warrant a reduction in chunks number.
	 * @param newNbChunks new number of chunks. If the last chunks has (or will) 
	 * a size > chunksSize, it's a good idea to increase this aprameter.
	 */
	public abstract void rearangeChunks(int newSizeChunk, int newNbChunks);
	

	public abstract long getSize();

	public void truncate(long size);

	public static class FsFileMethods{
		/**
		 * read from buff.pos to buff.limit (so we are going to read limit-position bytes).
		 * <br>note: you must ensure there are enough ytes to read before calling this method.
		 * @param buff output buffer
		 * @param offset from where we read on this file.
		 */
		public static void read(FsFile file, ByteBuff buff, long offset) {
			System.out.println("READ FILE : "+file.getPath()+" read by "+offset+" -> "+(offset+buff.limit()-buff.position()));
			System.out.println("now pos = "+buff.position());
			//choose first chunk
			int chunkIdx = (int) (offset/file.getChunkSize());
			if(file.getNbChunks()==1) chunkIdx = 0;
			int chunkOffset = (int) (offset - file.getChunkSize() * chunkIdx);
			//first read
			System.out.println("read first chunk");
			FsChunk chunk = file.getChunks().get(chunkIdx);
			if(chunk.currentSize()-chunkOffset>buff.limit()-buff.position()){
				//read some part
				System.out.println("read inside : "+(buff.limit()-buff.position()));
				chunk.read(buff, chunkOffset, buff.limit()-buff.position());
			}else{
				//full read
				System.out.println("read a part : "+chunk.currentSize()+" / "+(buff.limit()-buff.position()));
				chunk.read(buff, chunkOffset, chunk.currentSize());
			}
			System.out.println("now pos = "+buff.position());
			chunkIdx++;
			//other reads
			while(buff.position()<buff.limit()){
				System.out.println("read chunk n°"+chunkIdx+", now i need "+(buff.limit()-buff.position())+"more");
				chunk = file.getChunks().get(chunkIdx);
				if(chunk.currentSize()>buff.limit()-buff.position()){
					//read some part
					chunk.read(buff, 0, buff.limit()-buff.position());
				}else{
					//full read
					chunk.read(buff, 0, chunk.currentSize());
				}
				System.out.println("now pos = "+buff.position());
				chunkIdx++;
			}
		}
		
		public static long getSize(FsFile file){
			if(file.getChunks().size()==0){
				return 0;
			}
			long sizeOct = file.getChunkSize() * (file.getChunks().size() - 1);
			sizeOct += file.getChunks().get(file.getChunks().size()-1).currentSize();
			return sizeOct;
		}
	
		public static void write(FsFile file, ByteBuff buff, long offset) {
			System.out.println("WRITE FILE : "+file.getPath()+" writen by "+offset+" -> "+(offset+buff.limit()-buff.position()));
			System.out.println("now pos = "+buff.position());
			//choose first chunk
			int chunkIdx = (int) (offset/file.getChunkSize());
			if(file.getNbChunks()==1) chunkIdx = 0;
			int chunkOffset = (int) (offset - file.getChunkSize() * chunkIdx);
			//first write
			System.out.println("write first chunk");
			FsChunk chunk = file.getChunks().get(chunkIdx);
			if(chunk.currentSize()-chunkOffset>buff.limit()-buff.position() ||  file.getNbChunks()==chunkIdx+1){
				System.out.println("write inside : "+(buff.limit()-buff.position()));
				//write some part
				chunk.write(buff, chunkOffset, buff.limit()-buff.position());
			}else{
				//full write
				System.out.println("write a part : "+chunk.currentSize()+" / "+(buff.limit()-buff.position()));
				chunk.write(buff, chunkOffset, chunk.currentSize());
			}
			System.out.println("now pos = "+buff.position());
			chunkIdx++;
			//other writes
			while(buff.position()<buff.limit()){
				System.out.println("write chunk n°"+chunkIdx+", now i need "+(buff.limit()-buff.position())+"more");
				chunk = file.getChunks().get(chunkIdx);
				if(chunk.currentSize()>buff.limit()-buff.position() || file.getNbChunks()==chunkIdx+1){
					//write some part
					chunk.write(buff, 0, buff.limit()-buff.position());
				}else{
					//full write
					chunk.write(buff, 0, chunk.currentSize());
				}
				System.out.println("now pos = "+buff.position());
				chunkIdx++;
			}
		}
	
		public static FsChunk getChunk(FsFile file, int i) {
			return file.getChunks().get(i);
		}
	}

}
