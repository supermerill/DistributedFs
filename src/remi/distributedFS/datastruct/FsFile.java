package remi.distributedFS.datastruct;

import java.util.ArrayList;
import java.util.Collection;
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
	 * @return ordered used chunks. not null
	 */
	public abstract List<FsChunk> getChunks();
	
	/**
	 * get all chunks stored, even unused ones.
	 * @return all available chunks. not null
	 */
	public abstract Collection<FsChunk> getAllChunks();

//	/**
//	 * Get the chunk size of all chunks before the last one.
//	 * @return chunk size, can be 0 if nbCHunks < 2
//	 */
//	public abstract int getChunkSize();
	
//	/**
//	 * change the number of data (or how) stored by this file in chunks. 
//	 * @param newSizeChunk new size of chunks. 
//	 * Change this if there are enough data to warrant a reduction in chunks number.
//	 * @param newNbChunks new number of chunks. If the last chunks has (or will) 
//	 * a size > chunksSize, it's a good idea to increase this aprameter.
//	 */
//	public abstract void rearangeChunks(int newSizeChunk, int newNbChunks);
	

	/**
	 * Create a new chunk. It will not be added to the file content right now.
	 * @param id if>0, it will use this id. If <=0, it will create a new id.
	 * @return the newly added chunk.
	 */
	public abstract FsChunk createNewChunk(long id);
	
	/**
	 * Tell to the file that their new chunks order/content is this one now.
	 * @param newList the new list of chunks to use.
	 */
	public abstract void setChunks(List<FsChunk> newList);

	/**
	 * Get the size of this file in io.
	 * @return
	 */
	public abstract long getSize();

//	public void truncate(long size);

		
		/**
		 * read from buff.pos to buff.limit (so we are going to read limit-position bytes).
		 * <br>note: you must ensure there are enough ytes to read before calling this method.
		 * @param buff output buffer
		 * @param offset from where we read on this file.
		 */
		public static void read(FsFile file, ByteBuff buff, long offset) {
			System.out.println("READ FILE : "+file.getPath()+" read by "+offset+" -> "+(offset+buff.limit()-buff.position()));
			System.out.println("now pos = "+buff.position());
			//go to first chunk
			long currentPos = 0;
			int chunkIdx = 0;
			FsChunk chunk = file.getChunks().get(chunkIdx);
			while(currentPos+chunk.currentSize()<offset && chunkIdx+1<file.getChunks().size()){
				currentPos+=chunk.currentSize();
				chunkIdx++;
				chunk = file.getChunks().get(chunkIdx);
			}
			//choose first chunk
			int chunkOffset = (int) (offset - currentPos);
			//first read
			System.out.println("read first chunk");
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
			long size = 0;
			for(FsChunk chunk : file.getChunks()){
				size += chunk.currentSize();
			}
			return size;
		}
	
		public static void write(FsFile file, ByteBuff buff, long offset) {
			System.out.println("WRITE FILE : "+file.getPath()+" writen by "+offset+" -> "+(offset+buff.limit()-buff.position()));
			System.out.println("now pos = "+buff.position());
			
			//grow file if needed
			if(file.getSize() < offset + buff.limit()-buff.position()){
				growFile(file, offset + buff.limit()-buff.position());
			}
			
			//go to first chunk
			long currentPos = 0;
			int chunkIdx = 0;
			FsChunk chunk = file.getChunks().get(chunkIdx);
			while(currentPos+chunk.currentSize()<offset && chunkIdx+1<file.getChunks().size()){
				currentPos+=chunk.currentSize();
				chunkIdx++;
				chunk = file.getChunks().get(chunkIdx);
			}
			int chunkOffset = (int) (offset - currentPos);
			//first write
			System.out.println("write first chunk");
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
			file.changes();
		}
	
//		public static FsChunk getChunk(FsFile file, int idx) {
//			return file.getChunks().get(idx);
//		}
	
		public static FsChunk getChunk(FsFile file, long id) {
			for(FsChunk ch : file.getChunks()){
				if(ch.getId() == id){
					return ch;
				}
			}
			return null;
		}
	
		public static FsChunk getFromAllChunk(FsFile file, long id) {
			for(FsChunk ch : file.getAllChunks()){
				if(ch.getId() == id){
					return ch;
				}
			}
			return null;
		}

		public static void truncate(FsFile file, long size) {

			System.out.println("truncate FILE : "+file.getSize()+" -> "+size);
			List<FsChunk> newLst = new ArrayList<>();
			//go to first chunk
			long currentPos = 0;
			int chunkIdx = 0;
			FsChunk chunk = file.getChunks().get(chunkIdx);
			newLst.add(chunk);
			while(currentPos+chunk.currentSize()<size && chunkIdx+1<file.getChunks().size()){
				currentPos+=chunk.currentSize();
				chunkIdx++;
				chunk = file.getChunks().get(chunkIdx);
				newLst.add(chunk);
			}
			//truncate the last chunk
			chunk.setCurrentSize((int) (size - currentPos));
			//remove all others chunks
			file.setChunks(newLst);
		}
	

	public static void growFile(FsFile file, long newSize) {
		System.out.println("WRITE FILE : growFile "+newSize);
		synchronized (file) { //TODO : think more about sync 
			if(file.getChunks().size()==0){
				System.out.println("WRITE FILE : createFirstChunk ");
				int newMaxSizeChunk = 1024*4; //min 4kio
				FsChunk newChunk = file.createNewChunk(-1);
				System.out.println("WRITE FILE : flush1 ");
				newChunk.setMaxSize(newMaxSizeChunk);
				newChunk.setCurrentSize(0);
				System.out.println("WRITE FILE : flush2 ");
				newChunk.flush();
				System.out.println("WRITE FILE : setLst ");
				List<FsChunk> lst = new ArrayList<>(file.getChunks());
				lst.add(newChunk);
				file.setChunks(lst);
				System.out.println("WRITE FILE : firstChunk created ");
			}
			long needNewSize = newSize - file.getSize();
			System.out.println("WRITE FILE : file size :  "+file.getSize()+" < "+newSize);
			//grow last chunk
			FsChunk lastChunk = file.getChunks().get(file.getChunks().size()-1);
			if(lastChunk.getMaxSize() - lastChunk.currentSize() >= needNewSize){
				lastChunk.setCurrentSize((int) (lastChunk.currentSize() + needNewSize));
				lastChunk.changes();
				System.out.println("WRITE FILE : setFistChunk "+lastChunk.currentSize());
				return;
			}
			//else
			needNewSize -= (lastChunk.getMaxSize() - lastChunk.currentSize());
			lastChunk.setCurrentSize(lastChunk.getMaxSize());
			while(needNewSize>0){
				//create new chunk (bigger than last one, as it seems too small)
				int newMaxSizeChunk = Math.min(Math.max(1024*4, lastChunk.getMaxSize()*2), 1073741824); //max 1gio per chunk
				FsChunk newChunk = file.createNewChunk(-1);
				newChunk.setCurrentSize(0);
				newChunk.setMaxSize(newMaxSizeChunk);
				System.out.println("WRITE FILE : lastChunk.getMaxSize() "+newChunk.getMaxSize());
				List<FsChunk> lst = new ArrayList<>(file.getChunks());
				lst.add(newChunk);
				file.setChunks(lst);
				//grow it
				lastChunk = newChunk;
				if(lastChunk.getMaxSize() - lastChunk.currentSize() >= needNewSize){
					lastChunk.setCurrentSize((int) (lastChunk.currentSize() + needNewSize));
					return;
				}
				needNewSize -= (lastChunk.getMaxSize() - lastChunk.currentSize());
				System.out.println("WRITE FILE : lastChunk.getMaxSize() "+lastChunk.getMaxSize()+" : lastChunk.currentSize() "+lastChunk.currentSize());
				lastChunk.setCurrentSize(lastChunk.getMaxSize());
				System.out.println("WRITE FILE : now grow to "+file.getSize()+" : reste "+needNewSize);
			}

		}
	}

}
