package remi.distributedFS.datastruct;

public interface FsObject {
	
	public long getDeleteDate();
	public void setDeleteDate(long deleteDate);
	public short getPUGA();
	public void setPUGA(short pugaData) ;
	public long getModifyDate() ;
	public long getCreationDate() ;
	public String getName() ;
	public long getId() ;
	public long getUserId() ;
	public long getGroupId() ;
	public long getParentId() ;
	public FsDirectory getParent() ;
	public void setParent(FsDirectory newDir);
	public String getPath();
	public void setName(String newName) ;

	public abstract void accept(FsObjectVisitor visitor);
	
	public long getModifyUID() ;
	
	public long getDeleteUID() ;
	public long getCreatorUID() ;
	public void setCreatorUID(long creatorUserId) ;
	public void setModifyUID(long modifyUserId) ;
	public void setDeleteUID(long deleteUserId) ;
	public long getComputerId() ;
	public void setComputerId(long computerId) ;
	public void setUserId(long uid) ;
	public void setParentId(long parentId) ;
	public void setCreationDate(long creationDate) ;
	public void setModifyDate(long modifyDate) ;
	public void setGroupId(long groupId) ;
	
	
	/**
	 * Notify that modifications on this object are finished and now should be saved / transmitted.
	 */
	public void flush();
	
}
