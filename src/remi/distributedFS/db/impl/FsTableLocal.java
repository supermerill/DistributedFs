package remi.distributedFS.db.impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.util.ByteBuff;

public class FsTableLocal implements StorageManager{

	FsDirectory root;
	
	static final int FS_SECTOR_SIZE = 512; // bytes, at least 384 to have enough place to allocate other sector in a linked chain way
	ByteBuffer buffer;
	
	FileChannel fsFile;
	
	List<Long> unusedSectors = new ArrayList<>();
	
	long fileSize = 0;
	
	//TODO: create a separate thread for flushing/updating
	
	
	public FsTableLocal(String filename) throws IOException{
		 buffer = ByteBuffer.allocate(FS_SECTOR_SIZE);
		
		 File fic = new File(filename);
		 if(!fic.exists()) fic.createNewFile();
		fsFile = FileChannel.open(fic.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
		//read root
		root = readOrCreate(0);
		//get each unused position inside
		fileSize = fsFile.size();
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
			 * 3: extended
			 * 4: in trasferts?
			 */
			byte state = buffer.get();
			if(state == 1){
				//ok, read!
				
				FsDirectoryFromFile dir = new FsDirectoryFromFile(this, sectorPos, null);
				buffer.rewind();
				dir.load(buffer);
				if(dir.getParentId()==0){
					dir.setParent(dir);
				}

				return dir;
			
			}else if(state == 0){
				//erased, return null;
				System.err.println("error, shouldn't happen: you try to read a sector that is erased.");
				return null;
			}else if(state == 2){
				//TODO: get/create file
			}else if(state == 3){
				//error
				System.err.println("error, shouldn't happen: you try to read a sector that is an extension of an other? entry.");
				return null;
			}else if(state == 4){
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

	//return a sector, ie 64 bytes
	void getContentAt(ByteBuff ret, long fsSector, int nbSector){
		
	}
	
	void setContentAt(ByteBuff dataToWrite, long fsSector, int nbSector){
		
	}
	
	void releaseFolder(FsDirectory folderToRemove){
		
	}
	
	void releaseFile(FsFile folderToRemove){
		
	}
	
	void releaseChunk(FsChunk toRemove){
		
	}
	
	@Override
	public FsDirectory getRoot() {
		return root;
	}

	public long getComputerId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getUserId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getGroupId() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
}
