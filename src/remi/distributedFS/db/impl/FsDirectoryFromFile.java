package remi.distributedFS.db.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.datastruct.LoadErasedException;
import remi.distributedFS.util.Ref;

public class FsDirectoryFromFile extends FsObjectImplFromFile  implements FsDirectory{
	List<FsDirectoryFromFile> dirs = null;
	List<FsFileFromFile> files = null;
//	Map<String,Long> delete = null;
	List<FsObjectImplFromFile> deleteObjs = null;
	
	protected class Deleter<E extends FsObjectImplFromFile> implements Consumer<E>{

		@Override
		public void accept(E e) {
			e.setDeleteDate(System.currentTimeMillis()); 
			e.setDeleteUID(master.getUserId());
			e.delete();
			e.flush();
//			FsErasedObject eobj = new FsErasedObject(master, e.getId(), FsDirectoryFromFile.this);
			deleteObjs.add(e);
			setModifyDate();
			setDirty(true);
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

	FsDirectoryFromFile(FsTableLocal master, long mysector, FsDirectory parent){
		super(master, mysector, parent);
		Deleter del = new Deleter();
		Adder add = new Adder();
		dirs = new ListeningArrayList<FsDirectoryFromFile>(add, del);
		files = new ListeningArrayList<FsFileFromFile>(add, del);
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
			System.err.println("Error, "+type+" not a directory at "+getSector()+" "+buffer.position());
			throw new LoadErasedException("Error, "+type+" not a directory at "+getSector());
		}
		
		super.load(buffer);
		//now, it should be at pos ~337
		
		//now read entries.
		//go to fixed pos (because it's simpler)
		buffer.position(360);
		//i have 656 bytes in this sector. 82 long
		long nbDir = buffer.getLong();
		long nbFile = buffer.getLong();
		long nbDel = buffer.getLong();
		
		// 360 : position set behind
		// /8 -> long type
		// -3 -> remove nbdir read, nbfiel read, nbDel read,  and "next sector" read
		int canRead = ((FsTableLocal.FS_SECTOR_SIZE-360)/8)-4;
		ByteBuffer currentBuffer = buffer;
//		long currentSector = this.getId();
		//read folder
		for(int i=0;i<nbDir;i++){
			dirs.add(new FsDirectoryFromFile(master, currentBuffer.getLong(), this));
			canRead--;
			if(canRead == 0){
				canRead = goToNext(currentBuffer);
			}
		}
		for(int i=0;i<nbFile;i++){
			files.add(new FsFileFromFile(master, currentBuffer.getLong(), this));
			canRead--;
			if(canRead == 0){
				canRead = goToNext(currentBuffer);
			}
		}
		for(int i=0;i<nbDel;i++){
			deleteObjs.add(new FsUnknownObject(master, currentBuffer.getLong(), this));
			canRead--;
			if(canRead == 0){
				canRead = goToNext(currentBuffer);
			}
		}
	}

	@Override
	public void print(ByteBuffer buffer) {
		super.print(buffer);

		buffer.position(360);
		//i have 656 bytes in this sector. 82 long
		long nbDir = buffer.getLong();
		long nbFile = buffer.getLong();
		System.out.println("nbDir = "+nbDir);
		
		int canRead = 80;
		ByteBuffer currentBuffer = buffer;
//		long currentSector = this.getId();
		//read folder
		for(int i=0;i<nbDir;i++){
			System.out.println("Dir : "+currentBuffer.getLong());
			canRead--;
			if(canRead == 0){
				canRead = goToNext(currentBuffer);
			}
		}
		for(int i=0;i<nbFile;i++){
			canRead--;
			if(canRead == 0){
				canRead = goToNext(currentBuffer);
			}
		}
	}
	

	public synchronized void save(ByteBuffer buffer){
		//set "erased" or d"directory"
		if(parentId<0){
			buffer.put(FsTableLocal.ERASED);
			//TODO: erase also "extended" chunks
//		}else if(deleteDate>0){
//			buffer.put(FsTableLocal.DELETED);
		}else{
			buffer.put(FsTableLocal.DIRECTORY);
		}
		super.save(buffer);

		//now, it should be at pos ~337
		
		//now read entries.
		//go to fixed pos (because it's simpler)
		buffer.position(360);
		//i have 656 bytes in this sector. 82 long
		buffer.putLong(dirs.size());
		buffer.putLong(files.size());
		buffer.putLong(deleteObjs.size());
		
		int canRead = 80;
		ByteBuffer currentBuffer = buffer;
		Ref<Long> currentSector = new Ref<>(this.getSector());
		//read folder
		for(int i=0;i<dirs.size();i++){
			currentBuffer.putLong(dirs.get(i).getSector());
			canRead--;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, currentSector);
			}
		}
		for(int i=0;i<files.size();i++){
			currentBuffer.putLong(files.get(i).getSector());
			canRead--;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, currentSector);
			}
		}
		for(int i=0;i<deleteObjs.size();i++){
			currentBuffer.putLong(deleteObjs.get(i).getSector());
			canRead--;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, currentSector);
			}
		}
		//save last sector (previous are saved in goToNextOrCreate)
		currentBuffer.rewind();
		master.saveSector(currentBuffer, currentSector.get());
		setDirty(false);
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
		long newId = master.requestNewSector();
		FsFileFromFile newone = new FsFileFromFile(master, newId, this);
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

}
