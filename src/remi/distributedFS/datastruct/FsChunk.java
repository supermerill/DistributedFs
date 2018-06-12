package remi.distributedFS.datastruct;


import it.unimi.dsi.fastutil.shorts.ShortList;
import remi.distributedFS.util.ByteBuff;

public abstract interface FsChunk extends FsId{

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
	/**
	 * Get the size of the chunk content. It can be >0 even if it's not stored locally.
	 * @return size in bytes.
	 */
	public int currentSize();
	public void setCurrentSize(int newSize);
	/**
	 * Get the max size this chunk can grow. It's almost not used.
	 * @return the max size attribute
	 */
	public int getMaxSize();
	public void setMaxSize(int newMaxSize);
	/**
	 * Ask if the chunk is stored locally or not.
	 * @return true if it can be grab from the local storage.
	 */
	public boolean isPresent();
	/**
	 * Set if the chunk is present localay or not. Used to remove it from the local storage.
	 * @param isPresentLocally if false, delete the local storage of this chunk
	 */
	public void setPresent(boolean isPresentLocally);
	/**
	 * Get the list of computerId where this chunk can be grabbed, useful to get it if it's not stored locally
	 * and to know in how many computer it's stored.
	 * @return
	 */
	public ShortList serverIdPresent();
	/**
	 * @return in ms
	 */
	public long getModifyDate();
	public long getModifyUID();
	/**
	 * 
	 * @return in ms
	 */
	public long getLastAccessDate();
	
	/**
	 * unique id for this element in the filesystem. It should contain the computerid who created it and a unique id from this computer.
	 */
	public long getId();
	
	@Override
	public void flush();
	@Override
	public void changes();
	@Override
	public void delete();


}
