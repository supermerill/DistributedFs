package remi.distributedFS.datastruct;

import remi.distributedFS.util.Ref;

public abstract class FsObjectImpl implements FsObject {
	//static data saved inside the directory descriptor.
	protected long id; // unique id for this file on the cluster, composed by computerId(2B, heavyside) and whatever is unique (6B)
	protected long parentId; //the way to find my parent in the db.
	protected long creationDate;
	protected long creatorUserId;
	protected long modifyDate;
	protected long modifyUserId;
	protected long deleteDate;
	protected long deleteUserId;
	protected long computerId;
	protected long groupId;
	protected short PUGA;
	protected String name; //limited to 255 (ie, it's 256 byte, with the 0 at the end)
	
	//useful, many times, but sometimes it's not loaded... to check!
	protected FsDirectory parent;
	
	// true if the content on disk is not == on memory
	private Ref<Boolean> dirty = new Ref<>(false);
	
	public long getDeleteDate() {
		return deleteDate;
	}
	public void setDeleteDate(long deleteDate) {
		this.deleteDate = deleteDate;
	}
	public short getPUGA() {
		return PUGA;
	}
	public void setPUGA(short pugaData) {
		PUGA = pugaData;
	}
	public long getModifyDate() {
		return modifyDate;
	}
	public long getCreationDate() {
		return creationDate;
	}
	public String getName() {
		return name;
	}
	public long getId() {
		return id;
	}
	public long getUserId() {
		return getCreatorUID();
	}
	public long getGroupId() {
		return groupId;
	}
	public long getParentId() {
		return parentId;
	}
	public FsDirectory getParent() {
		return parent;
	}
	public void setParent(FsDirectory newDir) {
		parent = newDir;
		if(newDir==null)parentId = -1;
		else parentId = newDir.getId();
	}
	public String getPath() {
		if(parent == this || parent == null){
			return getName();
		}
		return parent.getPath()+"/"+getName();
	}
	public void setName(String newName) {
		this.name = newName;
	}

	public abstract void accept(FsObjectVisitor visitor);
	
	public long getModifyUID() {
		return modifyUserId;
	}
	
	public long getDeleteUID() {
		return deleteUserId;
	}
	public long getCreatorUID() {
		return creatorUserId;
	}
	public void setCreatorUID(long creatorUserId) {
		this.creatorUserId = creatorUserId;
	}
	public void setModifyUID(long modifyUserId) {
		this.modifyUserId = modifyUserId;
	}
	public void setDeleteUID(long deleteUserId) {
		this.deleteUserId = deleteUserId;
	}
	public long getComputerId() {
		return computerId;
	}
	public void setComputerId(long computerId) {
		this.computerId = computerId;
	}
	public void setUserId(long uid) {
		setCreatorUID(uid);
	}
	public void setParentId(long parentId) {
		this.parentId = parentId;
	}
	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}
	public void setModifyDate(long modifyDate) {
		this.modifyDate = modifyDate;
	}
	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}
	public boolean isDirty() {
		return dirty.get();
	}
	public void setDirty(boolean dirty) {
		this.dirty.set(dirty);
	}
	public Ref<Boolean> getDirty() {
		return dirty;
	}
	
	
	public void delete(){
		
	}
	
}
