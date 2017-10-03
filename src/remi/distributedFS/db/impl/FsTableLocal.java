package remi.distributedFS.db.impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.fs.FileSystemManager;

public class FsTableLocal implements StorageManager{

	public static final byte ERASED = 0;
	public static final byte DIRECTORY = 1;
	public static final byte FILE = 2;
	public static final byte CHUNK = 3;
	public static final byte EXTENSION = 4;
	public static final byte MOVING = 5;

	FileSystemManager manager;
	
	FsDirectory root;
	
	static final int FS_SECTOR_SIZE = 512; // bytes, at least 384 to have enough place to allocate other sector in a linked chain way
	ByteBuffer buffer;
	
	FileChannel fsFile;
	
	List<Long> unusedSectors = new ArrayList<>();
	
	long fileSize = 0;
	private String rootRep;
	
	//TODO: create a separate thread for flushing/updating
	
	
	public FsTableLocal(String rootRep, String filename, FileSystemManager manager) throws IOException{
		this.manager = manager;
		this.rootRep = rootRep;
		 buffer = ByteBuffer.allocate(FS_SECTOR_SIZE);
		
		 File fic = new File(filename);
		 if(!fic.exists()) fic.createNewFile();
		fsFile = FileChannel.open(fic.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
		//read root
		root = readOrCreate(0);
		//get each unused position inside
		fileSize = fsFile.size();
	}
	
	

	public FileSystemManager getManager() {
		return manager;
	}



	protected FsDirectory readOrCreate(int sectorPos) {
		
		try {
			fsFile.position(FS_SECTOR_SIZE*sectorPos);
			
			buffer.rewind();
			
			if(fsFile.size()<FS_SECTOR_SIZE){
				//create!
				FsDirectoryFromFile obj = new FsDirectoryFromFile(this, 0, null);
				obj.loaded = true;
				obj.setName("");
				obj.setPUGA((short)0x1FF);
				obj.setParent(obj);
				obj.setParentId(obj.getId());
				obj.setCreationDate(System.currentTimeMillis());
				obj.setModifyDate(System.currentTimeMillis());
				obj.setDeleteDate(0);
				obj.setComputerId(getComputerId());
				obj.setUserId(getUserId());
				obj.setGroupId(getGroupId());
				obj.setCreatorUID(getUserId());
				obj.setModifyUID(getUserId());
				obj.setDeleteUID(-1);
				
				obj.save(buffer);
				return obj;
			}
			
			int nbread = fsFile.read(buffer);
			buffer.rewind();
			if(nbread != FS_SECTOR_SIZE){
				System.err.println("error, can't read enough bytes from file!!");
				throw new RuntimeException("error, can't read enough bytes from file!! : "+nbread);
			}
		
			///first byte: state
			//0: erased, 1:used , 2: moved to an other position (todo : check if it's finished or a crash has occured).
			//3: used by the rprevious one (because it's a directory with has many many file, or a file with many many chunks)
			/*
			 * 0: erased
			 * 1: directory
			 * 2: file
			 * 2: chunk
			 * 3: extended
			 * 4: in trasferts?
			 */
			byte state = buffer.get();
			if(state == DIRECTORY){
				//ok, read!
				
				FsDirectoryFromFile dir = new FsDirectoryFromFile(this, sectorPos, null);
				buffer.rewind();
				dir.load(buffer);
				if(dir.getParentId()==0){
					dir.setParent(dir);
				}

				return dir;
			
			}else if(state == ERASED){
				//erased, return null;
				System.err.println("error, shouldn't happen: you try to read a sector that is erased.");
				return null;
			}else if(state == FILE){
				//TODO: get/create file
			}else if(state == CHUNK){
				//TODO: get/create chunk
			}else if(state == EXTENSION){
				//error
				System.err.println("error, shouldn't happen: you try to read a sector that is an extension of an other? entry.");
				return null;
			}else if(state == MOVING){
				//this entry is moved
				//TODO: check te new sector
			}
		
		

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void loadSector(ByteBuffer currentBuffer, long currentSector) {
		try {
			fsFile.read(currentBuffer, currentSector*FS_SECTOR_SIZE);
			currentBuffer.rewind();
		} catch (IOException e) {
			throw new RuntimeException(e); 
		}
	}

	public long requestNewSector() {
		//check if we have an unused one
		long idRet = -1;
		synchronized (unusedSectors) {
			if(!unusedSectors.isEmpty()){
				idRet = unusedSectors.remove(unusedSectors.size()-1);
			}
		}
	
		System.out.println("idRet="+idRet);
		//create new
		if(idRet<0){
			idRet = fileSize/FS_SECTOR_SIZE;
			fileSize += FS_SECTOR_SIZE;
		}

		System.out.println("new sector="+idRet);
		return idRet;
		
	}

	public void saveSector(ByteBuffer buff, long sectorId) {
//		System.out.println("Save buffer : "+buff.limit()+" @"+sectorId);
		if(buff.limit()-buff.position()!=FS_SECTOR_SIZE){
			System.err.println("Error, someone want me to write an incomplete sector!");
			throw new RuntimeException("Error, someone want me to write an incomplete sector!: ");
		}
		try {
			fsFile.write(buff, sectorId*FS_SECTOR_SIZE);
		} catch (IOException e) {
			throw new RuntimeException(e); 
		}
	}
	


	public void releaseSector(long id) {
		unusedSectors.add(id);
	}
	
	@Override
	public FsDirectory getRoot() {
		return root;
	}

	public long getComputerId() {
		return manager.getComputerId();
	}

	public long getUserId() {
		return manager.getUserId();
	}

	public long getGroupId() {
		return manager.getGroupId();
	}
	
	public String getRootRep(){
		return rootRep;
	}
	
	
}
