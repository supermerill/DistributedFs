package remi.distributedFS.datastruct;


import java.util.List;

import remi.distributedFS.util.ByteBuff;

public abstract interface FsChunk {

	//data
	/**
	 * Read some bytes from this chunk.
	 * @param toAppend buffer where data are put (if the return value is true).
	 * @param offset from where to read.
	 * @param size size of things readed.
	 * @return true if possible. false if offset and size put us out of bounds.
	 */
	public boolean read(ByteBuff toAppend, int offset, int size); 
	
	/**
	 * Write some bytes to this chunk. 
	 * <br>If this chunk is a "last chunk", we can write more data than his size (up to a max size limit).
	 * @param toAppend buffer where data are read (if the return value is true).
	 * @param offset from where to write.
	 * @param size size of things to write.
	 * @return true if success. false if offset and size put us out of bounds.
	 */
	public boolean write(ByteBuff toWrite, int offset, int size); 
	
	
	//metadata
	public int currentSize();
	public void setCurrentSize(int newSize);
	public int getMaxSize();
	public void setMaxSize(int newMaxSize);
	public boolean isPresent();
	public void setPresent(boolean isPresentLocally);
	public List<Long> serverIdPresent();
	public long lastModificationTimestamp();
	public long lastModificationUID();

	public long getId();

	public void flush();

	public void changes();

	public void delete();

}
