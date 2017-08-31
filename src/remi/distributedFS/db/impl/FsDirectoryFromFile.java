package remi.distributedFS.db.impl;

import java.nio.ByteBuffer;
import java.util.List;


import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.util.Ref;

public class FsDirectoryFromFile extends FsObjectImplFromFile  implements FsDirectory{
	List<FsDirectory> dirs = null;
	List<FsFile> files = null;

	FsDirectoryFromFile(FsTableLocal master, long mysector, FsDirectory parent){
		super(master, mysector, parent);
		dirs = new ListeningArrayList<>(this.getDirty());
		files = new ListeningArrayList<>(this.getDirty());
	}
	
//	FsDirectoryFromFile(FsTableLocal master, ByteBuffer buffer, long mysector, FsDirectory parent){
//		this(master, mysector, parent);
//		
//		this.load(buffer);
//	}
	
	public void load(ByteBuffer buffer){
		//check if it's a dir
		byte type = buffer.get();
		if(type !=1){
			System.err.println("Error, not a directory at "+getId()+buffer.position());
			throw new RuntimeException("Error, not a directory at "+getId());
		}
		
		super.load(buffer);
		//now, it should be at pos ~337
		
		//now read entries.
		//go to fixed pos (because it's simpler)
		buffer.position(360);
		//i have 656 bytes in this sector. 82 long
		long nbDir = buffer.getLong();
		long nbFile = buffer.getLong();
		
		// 360 : position set behind
		// /8 -> long type
		// -3 -> remove nbdir read, nbfiel read and "next sector" read
		int canRead = ((FsTableLocal.FS_SECTOR_SIZE-360)/8)-3;
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
		buffer.put((byte)1);
		super.save(buffer);

		//now, it should be at pos ~337
		
		//now read entries.
		//go to fixed pos (because it's simpler)
		buffer.position(360);
		//i have 656 bytes in this sector. 82 long
		buffer.putLong(dirs.size());
		buffer.putLong(files.size());
		
		int canRead = 80;
		ByteBuffer currentBuffer = buffer;
		Ref<Long> currentSector = new Ref<>(this.getId());
		//read folder
		for(int i=0;i<dirs.size();i++){
			currentBuffer.putLong(dirs.get(i).getId());
			canRead--;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, currentSector);
			}
		}
		for(int i=0;i<files.size();i++){
			currentBuffer.putLong(files.get(i).getId());
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
		return files; 
	}
	
	@Override
	public List<FsDirectory> getDirs() { 
		checkLoaded();
		return dirs; 
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
