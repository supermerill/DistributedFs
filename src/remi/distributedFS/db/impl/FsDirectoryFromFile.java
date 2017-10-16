package remi.distributedFS.db.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.util.Ref;

public class FsDirectoryFromFile extends FsObjectImplFromFile  implements FsDirectory{
	ArrayList<FsDirectoryFromFile> dirs = null;
	ArrayList<FsFileFromFile> files = null;
//	Map<String,Long> delete = null;
	List<FsObjectImplFromFile> deleteObjs = null;
	
	long lastChange = 0;
	long lastChangeUid = 0;

	FsDirectoryFromFile(FsTableLocal master, long mysector, FsDirectory parent){
		super(master, mysector, parent);
		dirs = new ArrayList<FsDirectoryFromFile>();
		files = new ArrayList<FsFileFromFile>();
//		delete = new HashMap<>(0);
		deleteObjs = new ArrayList<>();
	}
	
	//only for the root
	FsDirectoryFromFile(FsTableLocal fsTableLocal, int sectorPos) {
		super(fsTableLocal, sectorPos);
		this.parent = this;
		id = 0;
		parentId = 0;
		dirs = new ArrayList<FsDirectoryFromFile>();
		files = new ArrayList<FsFileFromFile>();
//		delete = new HashMap<>(0);
		deleteObjs = new ArrayList<>();
	}

	public void setModifyDate(){

		// modification(s) ? -> set timestamp!
    	this.setModifyDate(System.currentTimeMillis());
		System.out.println("new modifydate for folder '"+this.getPath()+"' : "+this.getModifyDate());
		this.setModifyUID(master.getUserId());
	}
	
//	FsDirectoryFromFile(FsTableLocal master, ByteBuffer buffer, long mysector, FsDirectory parent){
//		this(master, mysector, parent);
//		
//		this.load(buffer);
//	}
	
	public void load(ByteBuffer buffer){
		//check if it's a dir
		byte type = buffer.get();
		if(type !=FsTableLocal.DIRECTORY){
			System.err.println("Error, "+type+" not a directory @"+getSector()+" "+buffer.position());
			super.load(buffer);
			System.err.println("Error, "+type+" not a directory with name "+getName());
			throw new WrongSectorTypeException("Error, "+type+" not a directory @"+getSector());
		}

		super.load(buffer);
		//now, it should be at pos ~337
		lastChange = buffer.getLong();
		lastChangeUid = buffer.getLong();
		
		//now read entries.
		//i have 656 bytes in this sector. 82 long
		long nbDir = buffer.getLong();
		long nbFile = buffer.getLong();
		long nbDel = buffer.getLong();
		
		//ensure long-distance from end
		buffer.position(buffer.position() + 8 - (buffer.position()%8));
		//get how many long read we can do
		int canRead =  FsTableLocal.FS_SECTOR_SIZE/8 - (buffer.position()/8 + 1);
		ByteBuffer currentBuffer = buffer;
//		long currentSector = this.getId();
		Ref<Integer> sectorNum = new Ref<>(0);
		//read folder
		for(int i=0;i<nbDir;i++){
			dirs.add(new FsDirectoryFromFile(master, currentBuffer.getLong(), this));
			canRead--;
			if(canRead == 0){
				canRead = goToNextAndLoad(currentBuffer, sectorNum);
			}
		}
		for(int i=0;i<nbFile;i++){
			files.add(new FsFileFromFile(master, currentBuffer.getLong(), this));
			canRead--;
			if(canRead == 0){
				canRead = goToNextAndLoad(currentBuffer, sectorNum);
			}
		}
		for(int i=0;i<nbDel;i++){
			deleteObjs.add(new FsUnknownObject(master, currentBuffer.getLong(), this));
			canRead--;
			if(canRead == 0){
				canRead = goToNextAndLoad(currentBuffer, sectorNum);
			}
		}
	}

	@Override
	@Deprecated
	public void print(ByteBuffer buffer) {
		super.print(buffer);

		buffer.position(360);
		//i have 656 bytes in this sector. 82 long
		long nbDir = buffer.getLong();
		long nbFile = buffer.getLong();
		long nbDel = buffer.getLong();
		System.out.println("nbDir = "+nbDir);

		int canRead = ((FsTableLocal.FS_SECTOR_SIZE-360)/8)-4;
		System.out.println("Canread = "+canRead);
		ByteBuffer currentBuffer = buffer;
//		long currentSector = this.getId();
		Ref<Integer> sectorNum = new Ref<>(0);
		//read folder
		for(int i=0;i<nbDir;i++){
			System.out.println("Dir : "+currentBuffer.getLong());
			canRead--;
			if(canRead == 0){
				canRead = goToNextAndLoad(currentBuffer, sectorNum);
			}
		}
		for(int i=0;i<nbFile;i++){
			canRead--;
			if(canRead == 0){
				canRead = goToNextAndLoad(currentBuffer, sectorNum);
			}
		}
	}
	

	public synchronized void save(ByteBuffer buffer){
		if(id<0){
			System.err.println("WARN, can't save a directory without id : "+getPath()+" : "+getId()+" "+getParentId());
			new Exception().printStackTrace();
		}
		System.out.println("save folder"+getPath()+" with id "+id+" and parentid "+parentId);
		//set "erased" or d"directory"
		if(getParentId()<0){
			System.err.println("WARN, can't save a directory without parent : "+getPath()+" : "+getId()+" "+getParentId());
			buffer.put(FsTableLocal.ERASED);
			//TODO: erase also "extended" chunks
//		}else if(deleteDate>0){
//			buffer.put(FsTableLocal.DELETED);
		}else{
			buffer.put(FsTableLocal.DIRECTORY);
		}
		super.save(buffer);

		//now, it should be at pos ~337
		buffer.putLong(lastChange);
		buffer.putLong(lastChangeUid);
		
		//now read entries.
		buffer.putLong(dirs.size());
		buffer.putLong(files.size());
		buffer.putLong(deleteObjs.size());

		// 360 : position set behind
		// /8 -> long type
		// -4 -> remove nbdir read, nbfile read, nbDel read,  and "next sector" read
//		int canRead = ((FsTableLocal.FS_SECTOR_SIZE-360)/8)-4;
		
		//ensure it's a long -distance from the end
		buffer.position(buffer.position() + 8 - (buffer.position()%8));
		//how many long before end?
		int canRead = FsTableLocal.FS_SECTOR_SIZE/8 - (buffer.position()/8 + 1);
		
		//write arrays
		ByteBuffer currentBuffer = buffer;
//		Ref<Long> currentSector = new Ref<>(this.getSector());
		currentBuffer.mark();
		currentBuffer.position(FsTableLocal.FS_SECTOR_SIZE-8);
		System.out.println("first sec end with "+currentBuffer.getLong());
		currentBuffer.reset();
		Ref<Integer> sectorNum = new Ref<>(0);
		//read folder
		for(int i=0;i<dirs.size();i++){
			currentBuffer.putLong(dirs.get(i).getSector());
			canRead--;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, sectorNum);
			}
		}
		for(int i=0;i<files.size();i++){
			currentBuffer.putLong(files.get(i).getSector());
			canRead--;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, sectorNum);
			}
		}
		for(int i=0;i<deleteObjs.size();i++){
			currentBuffer.putLong(deleteObjs.get(i).getSector());
			canRead--;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, sectorNum);
			}
		}
		

		int pos = currentBuffer.position();
		currentBuffer.position(FsTableLocal.FS_SECTOR_SIZE-8);
		System.out.println("last sec end with "+currentBuffer.getLong());
		currentBuffer.position(pos);
		
		//save last sector (previous are saved in goToNextOrCreate)
		//fill last sector with zeros
		flushLastSector(currentBuffer, sectorNum);
//		Arrays.fill(currentBuffer.array(), currentBuffer.position(), currentBuffer.limit(), (byte)0);
//		currentBuffer.rewind();
//		master.saveSector(currentBuffer, currentSector.get());
		setDirty(false);
		System.out.println("end save folder"+getPath()+" with id "+id+" and parentid "+parentId);
	}

	@Override
	public List<FsFile> getFiles() { 
		checkLoaded();
		return (List)files; 
	}
	
	@Override
	public List<FsDirectory> getDirs() { 
		checkLoaded();
		return (List)dirs; 
	}

	@Override
	public List<FsObject> getDelete() {
		checkLoaded();
//		Map<String, Long> retMap = new HashMap<>();
//		for(FsObject obj : deleteObjs){
//			retMap.put(obj.getName(), obj.getDeleteDate());
//		}
		return (List)deleteObjs;
	}
	
	@Override
	public FsFile createSubFile(String name) {
		checkLoaded();
		long newSectorId = master.requestNewSector();
		FsFileFromFile newone = new FsFileFromFile(master, newSectorId, this);
		newone.loaded = true;
		newone.setName(name);
		setObjectContent(newone);
		files.add(newone);
		setDirty(true);
		newone.setDirty(true);
		return newone;
	}
	
	protected void setObjectContent(FsObjectImplFromFile obj){
		checkLoaded();
		obj.setPUGA(getPUGA());
		obj.setParent(this);
		obj.setParentId(this.getId());
		obj.setCreationDate(System.currentTimeMillis());
		obj.setModifyDate(System.currentTimeMillis());
		obj.setDeleteDate(0);
		obj.setComputerId(master.getComputerId());
		obj.setUserId(master.getUserId());
		obj.setGroupId(master.getGroupId());
		obj.setCreatorUID(master.getUserId());
		obj.setModifyUID(master.getUserId());
		obj.setDeleteUID(-1);
	}
	
	@Override
	public FsDirectory createSubDir(String name) {
		checkLoaded();
		long newId = master.requestNewSector();
		FsDirectoryFromFile newone = new FsDirectoryFromFile(master, newId, this);
		newone.loaded = true;
		newone.setName(name);
		setObjectContent(newone);
		dirs.add(newone);
		setDirty(true);
		newone.setDirty(true);
		return newone;
	}

	@Override
	public void accept(FsObjectVisitor visitor) {
		checkLoaded();
		visitor.visit(this);
	}

	@Override
	public void removeFile(FsFile fic) {
		fic.setDeleteDate(System.currentTimeMillis()); 
		fic.setDeleteUID(master.getUserId());
//		fic.delete();
//		FsErasedObject eobj = new FsErasedObject(master, e.getId(), FsDirectoryFromFile.this);
		files.remove(fic);
		if(!deleteObjs.contains(fic)){
			deleteObjs.add((FsObjectImplFromFile) fic);
		}else{
			System.err.println("Error: this file is already deleted!");
			new Exception().printStackTrace();
		}
		setModifyDate();
		setDirty(true);
		((FsObjectImplFromFile)fic).setDirty(true);
		fic.flush();
		flush();
	}

	@Override
	public void removeDir(FsDirectory dir) {
		dir.setDeleteDate(System.currentTimeMillis()); 
		dir.setDeleteUID(master.getUserId());
//		dir.delete();
//		FsErasedObject eobj = new FsErasedObject(master, e.getId(), FsDirectoryFromFile.this);
		dirs.remove(dir);
		if(!deleteObjs.contains(dir)){
			deleteObjs.add((FsObjectImplFromFile) dir);
		}else{
			System.err.println("Error: this dir is already deleted!");
			new Exception().printStackTrace();
		}
		setModifyDate();
		setDirty(true);
		((FsObjectImplFromFile)dir).setDirty(true);
		dir.flush();
		flush();
	}

	@Override
	public void removeCompletely(FsObject obj) {
		if(deleteObjs.contains(obj)){
			deleteObjs.remove(obj);
			obj.delete();
			setDirty(true);
			flush();
		}
	}

	@Override
	public void moveFile(FsFile obj, FsDirectory newDir) {
		files.remove(obj);
		obj.setModifyDate(System.currentTimeMillis()); 
		obj.setModifyUID(master.getUserId());
		obj.setParent(newDir);
		obj.setParentId(newDir.getId());
		setModifyDate();
		setModifyUID(master.getUserId());
		newDir.getFiles().add(obj);
		newDir.setModifyDate(System.currentTimeMillis()); 
		newDir.setModifyUID(master.getUserId());
		((FsObjectImplFromFile)obj).setDirty(true);
		setDirty(true);
		((FsObjectImplFromFile)newDir).setDirty(true);
		obj.flush();
		flush();
		newDir.flush();
	}

	@Override
	public void moveDir(FsDirectory obj, FsDirectory newDir) {
		dirs.remove(obj);
		long modDate = System.currentTimeMillis();
		System.out.println("MOVE DIR "+obj.getModifyDate()+" -> "+modDate);
		obj.setModifyDate(modDate); 
		obj.setModifyUID(master.getUserId());
		obj.setParent(newDir);
		obj.setParentId(newDir.getId());
		setModifyDate();
		setModifyUID(master.getUserId());
		newDir.getDirs().add(obj);
		newDir.setModifyDate(modDate); 
		newDir.setModifyUID(master.getUserId());
		newDir.setLastChangeDate(modDate); 
		newDir.setLastChangeUID(master.getUserId());
		((FsObjectImplFromFile)obj).setDirty(true);
		setDirty(true);
		((FsObjectImplFromFile)newDir).setDirty(true);
		//ned to flush me because obj isn't my child anymore -> his flush doesn't flush me
    	flush();
	}

	@Override
	public FsDirectory asDirectory() {
		return this;
	}
	
	@Override
	public FsFile asFile() {
		return null;
	}
	
	protected class Deleter<E extends FsObjectImplFromFile> implements Consumer<E>{

		@Override
		public void accept(E e) {
		}
		
	}
	
	protected class Adder<E extends FsObjectImplFromFile> implements Consumer<E>{

		@Override
		public void accept(E e) {
			setModifyDate();
			e.setDirty(true);
			setDirty(true);
		}
		
	}

	@Override
	public long getLastChangeDate() {
		checkLoaded();
		return lastChange;
	}

	@Override
	public void setLastChangeDate(long timestamp) {
		checkLoaded();
		this.lastChange = timestamp;
		FsDirectory parent = getParent();
		if(parent != this && parent != null){
			parent.setLastChangeDate(timestamp);
		}
	}

	@Override
	public long getLastChangeUID() {
		checkLoaded();
		return lastChangeUid;
	}

	@Override
	public void setLastChangeUID(long uid) {
		checkLoaded();
		this.lastChangeUid = uid;
		FsDirectory parent = getParent();
		if(parent != this && parent != null){
			parent.setLastChangeUID(lastChangeUid);
		}
	}

	
	@Override
	public void delete(){
		//delete content
		for(int i=0;i<dirs.size();i++){
			dirs.get(i).delete();
		}
		for(int i=0;i<files.size();i++){
			files.get(i).delete();
		}
		for(int i=0;i<deleteObjs.size();i++){
			deleteObjs.get(i).delete();
		}
		dirs.clear();
		files.clear();
		deleteObjs.clear();
		
		//delete self
		super.delete();
	}
	
}
