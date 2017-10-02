package remi.distributedFS.db.impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.omg.CORBA.VisibilityHelper;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.fs.FileSystemManager;

public class FsTableLocal implements StorageManager{

	public static final byte ERASED = -1; // no data
	public static final byte DIRECTORY = 1;
	public static final byte FILE = 2;
	public static final byte CHUNK = 3;
	public static final byte EXTENSION = 4;
	public static final byte DELETED = 5; //deleted object (but it's kept here)
	public static final byte MOVING = 6;

	FileSystemManager manager;
	
	FsDirectory root;

	static final int FS_SECTOR_SIZE = 512; // bytes, at least 384 to have enough place to allocate other sector in a linked chain way
	ByteBuffer buffer;
	
	FileChannel fsFile;
	
	List<Long> unusedSectors = new ArrayList<>();
	
	long fileSize = 0;
	private String rootRep;
	
	//TODO: create a separate thread for flushing/updating

	//this is a stub to help me tracking pos of objects.
	Long2ObjectMap<FsObject> objectId2LocalCluster = new Long2ObjectOpenHashMap<>();
	
	public FsTableLocal(String rootRep, String filename, FileSystemManager manager) throws IOException{
		this.manager = manager;
		this.rootRep = rootRep;
		//check root rep 
		File rootF = new File(rootRep);
		if(!rootF.exists()){
			rootF.mkdirs();
		}
		
		
		 buffer = ByteBuffer.allocate(FS_SECTOR_SIZE);
		
		 File fic = new File(filename);
		 if(!fic.exists()) fic.createNewFile();
		fsFile = FileChannel.open(fic.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
		//read root
		root = readOrCreateRoot(0);
		//get each unused position inside
		fileSize = fsFile.size();
		

		//load all content from fs
		//it's a stub, this content should be stored in a 'small' separate file.
//		getRoot().accept(new Visu());
		getRoot().accept(new Loader());
	}
	class Visu implements FsObjectVisitor{
		
		int nbEspace = 0;

		@Override
		public void visit(FsDirectory dirParent) {
			//largeur d'abord
			StringBuilder spacer = new StringBuilder();
			for(int i=0;i<nbEspace;i++){
				spacer.append(" - ");
			}
			System.out.println(spacer+" "+((FsDirectoryFromFile)dirParent).getSector()+" DIR "+dirParent.getPath()+" "+dirParent.getId());
			for(FsDirectory dir : dirParent.getDirs()){
				System.out.println(spacer+" |->dir "+((FsDirectoryFromFile)dir).getSector());
			}
			for(FsFile fic : dirParent.getFiles()){
				System.out.println(spacer+" |->fic "+((FsFileFromFile)fic).getSector());
			}
			for(FsDirectory dir : dirParent.getDirs()){
//				System.out.println(dirParent.getPath()+" "+dirParent.getId()+" visit "+dir.getPath()+" "+dir.getId());
//				System.out.println(spacer+" |->dir "+((FsDirectoryFromFile)dir).getSector()+ " : ");
				nbEspace++;
				visit(dir);
			}
			nbEspace--;
		}

		@Override
		public void visit(FsFile fic) {
			objectId2LocalCluster.put(fic.getId(), fic);
		}
		
	}

	class Loader implements FsObjectVisitor{

		@Override
		public void visit(FsDirectory dirParent) {
			objectId2LocalCluster.put(dirParent.getId(), dirParent);
			System.out.println(" dir "+dirParent.getPath());
			for(FsDirectory dir : new ArrayList<>(dirParent.getDirs())){
				try{
					System.out.println("contains "+dir.getName());
					visit(dir);
				}catch(WrongSectorTypeException ex){
					ex.printStackTrace();
					//recover : del this
					dirParent.getDirs().remove(dir);
					((FsDirectoryFromFile)dirParent).setDirty(true);
//					dirParent.flush();
				}
				System.out.println(dir.getPath() + " "+dir.getId());
			}
			for(FsFile fic : dirParent.getFiles()){
				visit(fic);
			}
			for(FsObject obj : dirParent.getDelete()){
				if(obj == dirParent) System.err.println("error, dir is inside deletes");
				System.out.println(obj.getPath()+" (D) " + obj.getId());
				obj.accept(this);
				objectId2LocalCluster.put(obj.getId(), obj);
			}
		}

		@Override
		public void visit(FsFile fic) {
			try{
				fic.getName(); // important, to trigger a load
				System.out.println(fic.getPath()+" (F) " + fic.getId());
				objectId2LocalCluster.put(fic.getId(), fic);
			}catch(WrongSectorTypeException ex){
				ex.printStackTrace();
				//recover : del this
				fic.getParent().getDirs().remove(fic);
				((FsDirectoryFromFile)fic.getParent()).setDirty(true);
//				fic.getParent().flush();
			}
		}
		
	}
	
//	private void loadFs(FsDirectory fsDirectory) {
//		objectId2LocalCluster.put(fsDirectory.getId(), fsDirectory);
//		System.out.println(fsDirectory.getPath());
//		for(FsDirectory dir : fsDirectory.getDirs()){
//			loadFs(dir);
//		}
//		for(FsFile fic : fsDirectory.getFiles()){
//			objectId2LocalCluster.put(fic.getId(), fic);
//			System.out.println(fsDirectory.getPath()+" (F)");
//		}
//		for(FsObject obj : fsDirectory.getDelete()){
//			objectId2LocalCluster.put(obj.getId(), obj);
//			System.out.println(fsDirectory.getPath()+" (D)");
//		}
//	}

	public FileSystemManager getManager() {
		return manager;
	}



	protected FsDirectory readOrCreateRoot(int sectorPos) {
		
		try {
			fsFile.position(FS_SECTOR_SIZE*sectorPos);
			
			buffer.rewind();
			
			if(fsFile.size()<FS_SECTOR_SIZE){
				//create!
				FsDirectoryFromFile obj = new FsDirectoryFromFile(this, 0);
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
			 * 0: erased / nothing
			 * 1: directory
			 * 2: file
			 * 3: chunk
			 * 4: extended
			 * 5: removed object
			 * 6: in trasferts?
			 */
			byte state = buffer.get();
			if(state == DIRECTORY){
				//ok, read!
				
				FsDirectoryFromFile dir = new FsDirectoryFromFile(this, sectorPos);
				buffer.rewind();
				dir.load(buffer);
				if(dir.getParentId()==0){
					dir.setParent(dir);
				}

				return dir;
			
			}else if(state == ERASED){
				//erased, return null;
				System.err.println("error, shouldn't happen: you try to read a sector ("+sectorPos+") that is erased.");
				return null;
			}else if(state == FILE){
				//TODO: get/create file
				System.err.println("error, shouldn't happen: root file.");
			}else if(state == CHUNK){
				//TODO: get/create chunk
			}else if(state == EXTENSION){
				//error
				System.err.println("error, shouldn't happen: you try to read a sector that is an extension of an other? entry.");
				return null;
			}else if(state == MOVING){
				//this entry is moved
				//TODO: check te new sector
				System.err.println("error, shouldn't happen: root moved.");
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

	public synchronized long requestNewSector() {
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

	public short getComputerId() {
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



	@Override
	public FsObject getDirect(long id) {
		if(objectId2LocalCluster.containsKey(id)){
			FsObject obj =  objectId2LocalCluster.get(id);
			return obj;
		}
		return null;
	}



	public void unregisterFile(long id, FsObject fsObjectImplFromFile) {
		objectId2LocalCluster.remove(id);
	}
	public void registerNewFile(long id, FsObject fsObjectImplFromFile) {
		objectId2LocalCluster.put(id, fsObjectImplFromFile);
	}
	
	
}
