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
		setDirty(this.deleteDate != deleteDate);
		this.deleteDate = deleteDate;
	}
	public short getPUGA() {
		return PUGA;
	}
	public void setPUGA(short pugaData) {
		setDirty(PUGA != pugaData);
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
	public void setName(String newName) {
		this.name = newName;
		setDirty(true);
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
		setDirty(parent != newDir);
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
		setDirty(this.creatorUserId != creatorUserId);
		this.creatorUserId = creatorUserId;
	}
	public void setModifyUID(long modifyUserId) {
		setDirty(this.modifyUserId != modifyUserId);
		this.modifyUserId = modifyUserId;
	}
	public void setDeleteUID(long deleteUserId) {
		setDirty(this.deleteUserId != deleteUserId);
		this.deleteUserId = deleteUserId;
	}
	public long getComputerId() {
		return computerId;
	}
	public void setComputerId(long computerId) {
		setDirty(this.computerId != computerId);
		this.computerId = computerId;
	}
	public void setUserId(long uid) {
		setCreatorUID(uid);
	}
	public void setParentId(long parentId) {
		setDirty(this.parentId != parentId);
		this.parentId = parentId;
	}
	public void setCreationDate(long creationDate) {
		setDirty(this.creationDate != creationDate);
		this.creationDate = creationDate;
	}
	public void setModifyDate(long modifyDate) {
		setDirty(this.modifyDate != modifyDate);
		this.modifyDate = modifyDate;
		if(parent != this && parent != null) parent.setModifyDate(modifyDate);
	}
	public void setGroupId(long groupId) {
		this.groupId = groupId;
		setDirty(true);
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
	
	
	@Override
	public FsDirectory asDirectory() {
		return this instanceof FsDirectory ? (FsDirectory) this : null;
	}
	
	@Override
	public FsFile asFile() {
		return this instanceof FsFile ? (FsFile) this : null;
	}
	
}
