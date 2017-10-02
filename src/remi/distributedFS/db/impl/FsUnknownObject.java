package remi.distributedFS.db.impl;

import java.nio.ByteBuffer;

import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.util.Ref;

public class FsUnknownObject extends FsObjectImplFromFile{
	FsObjectImplFromFile me;
	
	public FsUnknownObject(FsTableLocal master, long sectorId, FsDirectory parent) {
		super(master, sectorId, parent);
		me = null;
	}

	public void load(ByteBuffer buffer){
		//check if it's a file
		byte type = buffer.get();
		//if it contain a "file" or a directory, then create them instead of me
		if(type == FsTableLocal.DIRECTORY){
			me = new FsDirectoryFromFile(master, getSector(), parent);
		}else if(type == FsTableLocal.FILE){
			me = new FsFileFromFile(master, getSector(), parent);
		}else if(type == FsTableLocal.DELETED){
			me = new FsDeletedObject(master, getSector(), parent);
		}else{
			System.err.println("Error, not an object at "+getSector());
			throw new WrongSectorTypeException("Error, not an object at "+getSector());
		}
	}
	
	@Override
	public FsDirectory asDirectory() {
		checkLoaded();
		return me instanceof FsDirectory ? (FsDirectory) me : null;
	}
	
	@Override
	public FsFile asFile() {
		checkLoaded();
		return me instanceof FsFile ? (FsFile) me : null;
	}

	public synchronized void save(ByteBuffer buffer){
		checkLoaded();
		me.save(buffer);
	}

	@Override
	public void accept(FsObjectVisitor visitor) {
		checkLoaded();
		me.accept(visitor);
	}

	@Override
	public void print() {
		checkLoaded();
		me.print();
	}

	@Override
	public void print(ByteBuffer buffer) {
		checkLoaded();
		me.print(buffer);
	}

	@Override
	public void setDirty(boolean dirty) {
		checkLoaded();
		me.setDirty(dirty);
	}

	@Override
	public int goToNextOrCreate(ByteBuffer buff, Ref<Long> currentSector) {
		checkLoaded();
		return me.goToNextOrCreate(buff, currentSector);
	}

	@Override
	public int goToNext(ByteBuffer buff) {
		checkLoaded();
		return me.goToNext(buff);
	}

	@Override
	public void flush() {
		checkLoaded();
		me.flush();
	}

	@Override
	public void checkLoaded() {
		if(me == null){
			super.checkLoaded();
		}else{
			me.checkLoaded();
		}
	}

	@Override
	public long getDeleteDate() {
		checkLoaded();
		return me.getDeleteDate();
	}

	@Override
	public void setDeleteDate(long deleteDate) {
		checkLoaded();
		me.setDeleteDate(deleteDate);
	}

	@Override
	public short getPUGA() {
		checkLoaded();
		return me.getPUGA();
	}

	@Override
	public void setPUGA(short pugaData) {
		checkLoaded();
		me.setPUGA(pugaData);
	}

	@Override
	public long getModifyDate() {
		checkLoaded();
		return me.getModifyDate();
	}

	@Override
	public long getCreationDate() {
		checkLoaded();
		return me.getCreationDate();
	}

	@Override
	public String getName() {
		checkLoaded();
		return me.getName();
	}

	@Override
	public long getGroupId() {
		checkLoaded();
		return me.getGroupId();
	}

	@Override
	public long getParentId() {
		checkLoaded();
		return me==null?super.parent.getId():me.getParentId();
	}

	@Override
	public FsDirectory getParent() {
		checkLoaded();
		return me==null?super.parent:me.getParent();
	}

	@Override
	public void setParent(FsDirectory newDir) {
		checkLoaded();
		me.setParent(newDir);
	}

	@Override
	public String getPath() {
		checkLoaded();
		return me.getPath();
	}

	@Override
	public void setName(String newName) {
		checkLoaded();
		me.setName(newName);
	}

	@Override
	public long getModifyUID() {
		checkLoaded();
		return me.getModifyUID();
	}

	@Override
	public long getDeleteUID() {
		checkLoaded();
		return me.getDeleteUID();
	}

	@Override
	public long getCreatorUID() {
		checkLoaded();
		return me.getCreatorUID();
	}

	@Override
	public void setCreatorUID(long creatorUserId) {
		checkLoaded();
		me.setCreatorUID(creatorUserId);
	}

	@Override
	public void setModifyUID(long modifyUserId) {
		checkLoaded();
		me.setModifyUID(modifyUserId);
	}

	@Override
	public void setDeleteUID(long deleteUserId) {
		checkLoaded();
		me.setDeleteUID(deleteUserId);
	}

	@Override
	public long getComputerId() {
		checkLoaded();
		return me.getComputerId();
	}

	@Override
	public void setComputerId(long computerId) {
		checkLoaded();
		me.setComputerId(computerId);
	}

	@Override
	public void setParentId(long parentId) {
		checkLoaded();
		me.setParentId(parentId);
	}

	@Override
	public void setCreationDate(long creationDate) {
		checkLoaded();
		me.setCreationDate(creationDate);
	}

	@Override
	public void setModifyDate(long modifyDate) {
		checkLoaded();
		me.setModifyDate(modifyDate);
	}

	@Override
	public void setGroupId(long groupId) {
		checkLoaded();
		me.setGroupId(groupId);
	}

	@Override
	public long getUserId() {
		checkLoaded();
		return me.getUserId();
	}

	@Override
	public void setUserId(long uid) {
		checkLoaded();
		me.setUserId(uid);
	}

	@Override
	public boolean isDirty() {
		checkLoaded();
		return me.isDirty();
	}

	@Override
	public Ref<Boolean> getDirty() {
		checkLoaded();
		return me.getDirty();
	}

	@Override
	public void delete() {
		checkLoaded();
		me.delete();
	}

	@Override
	public long getSector() {
		return super.sector;
	}
	
	
}
