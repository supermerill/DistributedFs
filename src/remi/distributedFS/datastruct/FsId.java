package remi.distributedFS.datastruct;

public interface FsId {
	/**
	 * 
	 * @return in ms
	 */
	public long getModifyDate();
	public long getModifyUID() ;
	public long getId();

	/**
	 * Visitor function.
	 * @param visitor the visitor who visit this element.
	 */
	public abstract void accept(FsObjectVisitor visitor);
	
	/**
	 * Perform deletion operation : delete sub-thing, delete content, delete entry in fs.
	 * It's final and irrecoverable. If you want just mark this as "deleted", use removeDir/removeFile/removeChunk instead.
	 */
	public void delete(); 

	/**
	 * Call this when a changes occur on this element. It updates the ModifyDate.
	 */
	public void changes();
	
	/**
	 * Notify that modifications on this object are finished and now should be saved / transmitted.
	 */
	public void flush();
	
}
