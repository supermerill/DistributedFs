package remi.distributedFS.db.impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.fs.FileSystemManager;

public class FsTableLocal implements StorageManager{

	public static final byte ERASED = 0; // no data
	public static final byte DIRECTORY = 1;
	public static final byte FILE = 2;
	public static final byte CHUNK = 3;
	public static final byte EXTENSION = 4;
	@Deprecated
	public static final byte DELETED = 5; //deleted object (but it's kept here) (NOT USED)
	public static final byte MOVING = 6; //not used (yet) -> to mark sectors which are in construction, but the swap hasn't been done yet. (useful for shutdown-resistant fs)

	FileSystemManager manager;
	
	FsDirectory root;

	public static final int FS_SECTOR_SIZE = 512; // bytes, at least 384 to have enough place to allocate other sector in a linked chain way
	ByteBuffer buffer;
	
	FileChannel fsFile;
	
	LongSet unusedSectors = new LongOpenHashSet();
	
	long fileSize = 0;
	private String rootRep;

	public ObjectFactory factory;
	
	//TODO: create a separate thread for flushing/updating

	//this is a stub to help me tracking pos of objects.
	Long2ObjectMap<FsObject> objectId2LoadedObj = new Long2ObjectOpenHashMap<>();
	Long2ObjectMap<FsChunk> chunkId2LoadedObj = new Long2ObjectOpenHashMap<>();
	
	
	public static class FsTableLocalFactory{
		public String rootRep = ".";
		public String filename = "./fstab.data";
		public FileSystemManager manager;
		public ObjectFactory factory = new ObjectFactory.StandardFactory();
		
		public FsTableLocal create(){
			try{
				FsTableLocal obj = new FsTableLocal();
				obj.manager = manager;
				obj.rootRep = rootRep;
				obj.factory = factory;
				
				//check root rep 
				File rootF = new File(rootRep);
				if(!rootF.exists()){
					rootF.mkdirs();
				}
				
				
				obj.buffer = ByteBuffer.allocate(FS_SECTOR_SIZE);
				
				File fic = new File(filename);
				if(!fic.exists()) fic.createNewFile();
				obj.fsFile = FileChannel.open(fic.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
				//read root
				obj.root = obj.readOrCreateRoot(0);
				//get each unused position inside
				obj.fileSize = obj.fsFile.size();
				
				
				//load all content from fs
				//it's a stub, this content should be stored in a 'small' separate file.
				//			getRoot().accept(new Visu());
				obj.getRoot().accept(obj.new Loader());
				return obj;
			}catch(IOException e){
				throw new RuntimeException(e);
			}
		}
		
	}
	
	
	protected FsTableLocal(){
		
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
//			objectId2LocalCluster.put(fic.getId(), fic);
			
		}

		@Override
		public void visit(FsChunk chunk) {
		}
		
	}

	class Loader implements FsObjectVisitor{

		String pref = "";
		
		@Override
		public void visit(FsDirectory dirParent) {
			objectId2LoadedObj.put(dirParent.getId(), dirParent);
//			System.out.print(" dir "+((FsObjectImplFromFile)dirParent).getSectorsUsed()); System.out.println(" : "+dirParent.getPath());
			for(FsDirectory dir : new ArrayList<>(dirParent.getDirs())){
				try{
					System.out.print(pref+"(D) "+((FsObjectImplFromFile)dir).getSectorsUsed()); System.out.println(" :"+dir.getName()+" ("+dir.getPath()+")");
					pref = pref + "    ";
					visit(dir);
					pref = pref.substring(4);
				}catch(WrongSectorTypeException ex){
					ex.printStackTrace();
					//recover : del this
					dirParent.getDirs().remove(dir);
					((FsDirectoryFromFile)dirParent).setDirty(true);
					dirParent.flush();
				}
			}
			for(FsFile fic : dirParent.getFiles()){
				try{
					System.out.print(pref+"(F) "+((FsObjectImplFromFile)fic).getSectorsUsed()); System.out.print(" :"+fic.getName());
					visit(fic);
				}catch(WrongSectorTypeException ex){
					ex.printStackTrace();
					//recover : del this
					dirParent.getFiles().remove(fic);
					((FsDirectoryFromFile)dirParent).setDirty(true);
					dirParent.flush();
				}
			}
			for(FsObject obj : dirParent.getDelete()){
				if(obj == dirParent) System.err.println("error, dir is inside deletes");
				System.out.print(pref+"(R) "+((FsObjectImplFromFile)obj).getSectorsUsed()); System.out.println(obj.getPath()+" : " + obj.getId());
				obj.accept(this);
//				objectId2LocalCluster.put(obj.getId(), obj);
			}
		}

		@Override
		public void visit(FsFile fic) {
			try{
				LongList sectUsed = new LongArrayList();
				fic.getName(); // important, to trigger a load
//				System.out.println(fic.getPath()+" (F) " + fic.getId());
				objectId2LoadedObj.put(fic.getId(), fic);
				for(FsChunk ch : fic.getAllChunks()){
					visit(ch);
					sectUsed.addAll(((FsChunkFromFile)ch).getSectorsUsed());
				}
				Collections.sort(sectUsed);
				System.out.println(" (chunks : "+sectUsed+" )");
			}catch(WrongSectorTypeException ex){
				ex.printStackTrace();
				//recover : del this
				fic.getParent().getDirs().remove(fic);
				((FsDirectoryFromFile)fic.getParent()).setDirty(true);
//				fic.getParent().flush();
			}
		}

		@Override
		public void visit(FsChunk chunk) {
			chunkId2LoadedObj.put(chunk.getId(), chunk);
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
				//get one and remove it.
				LongIterator it = unusedSectors.iterator();
				if(it.hasNext()){
					idRet = it.nextLong();
					it.remove();
				}
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
			throw new RuntimeException("Error, someone want me to write an incomplete sector! : "+(buff.limit()-buff.position())+" != "+FS_SECTOR_SIZE);
		}
		try {
			fsFile.write(buff, sectorId*FS_SECTOR_SIZE);
		} catch (IOException e) {
			throw new RuntimeException(e); 
		}
	}
	


	public void removeSector(long id) {
		buffDel.rewind();
		saveSector(buffDel, id);
		synchronized (unusedSectors) {
			unusedSectors.add(id);
		}
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
		if(objectId2LoadedObj.containsKey(id)){
			FsObject obj = objectId2LoadedObj.get(id);
			return obj;
		}
		return null;
	}


	@Override
	public FsFile getFileDirect(long id) {
		if(objectId2LoadedObj.containsKey(id)){
			FsObject obj = objectId2LoadedObj.get(id);
			return obj.asFile();
		}
		return null;
	}


	@Override
	public FsDirectory getDirDirect(long id) {
		if(objectId2LoadedObj.containsKey(id)){
			FsObject obj = objectId2LoadedObj.get(id);
			return obj.asDirectory();
		}
		return null;
	}


	@Override
	public FsChunk getChunkDirect(long id) {
		FsChunk obj = chunkId2LoadedObj.get(id);
		if(obj==null) System.out.println("bad file : "+id+" from "+chunkId2LoadedObj.keySet());
		return obj;
	}



	public void unregisterFile(long id, FsObject fsObjectImplFromFile) {
		objectId2LoadedObj.remove(id);
	}
	public void registerNewFile(long id, FsObject fsObjectImplFromFile) {
		objectId2LoadedObj.put(id, fsObjectImplFromFile);
	}

	ByteBuffer buffDel = ByteBuffer.allocate(FS_SECTOR_SIZE);
	/**
	 * remove content from sector which are not used
	 * @param removeMoving set to true if you haven't start yet. It will remove "MOVING" sector
	 */
	public void cleanUnusedSectors(boolean removeMoving){
//		Long2ObjectMap<FsChunk> id2Chunk = new Long2ObjectOpenHashMap<FsChunk>();
//		//get all chunks
//		for(FsObject obj : objectId2LocalCluster.values()){
//			FsFile fic = obj.asFile();
//			if(fic != null){
//				for(FsChunk ch : fic.getChunks()){
//					try{
//						System.out.println("add chunk "+ch.getId()+"  @"+((FsChunkFromFile)ch).getSector()+" from file "+fic.getName());
//						chunkId2LocalCluster.put(ch.getId(), ch);
//					}catch(Exception e){
//						System.out.println("add NOT error chunk  @"+((FsChunkFromFile)ch).getSector()+" from file "+fic.getName());
//					}
//				}
//			}
//		}
		
		
		ByteBuffer buff = ByteBuffer.allocate(9);
		Arrays.fill(buffDel.array(), (byte)0);
		//look at all sectors
		long maxSects = fileSize/FS_SECTOR_SIZE;
		int nbread = 0;
		for(long sector=0;sector<maxSects;sector++){
			buff.rewind();
			//getId
			try {
				nbread = fsFile.read(buff, sector*FS_SECTOR_SIZE);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			if(nbread<9){
				System.err.println("Error: can't read more than "+nbread+"/9 !! (cleanUnusedSectors) at sector "+sector+"/"+maxSects);
				return;
			}
			buff.rewind();
			byte type = buff.get();
			long id = buff.getLong();
			if(type == 0){
				//nothing, it's empty
				if(!unusedSectors.contains(sector)){
					synchronized (unusedSectors) {
						System.out.println("add unused sector "+sector);
						unusedSectors.add(sector);
					}
				}
			}else if(type == EXTENSION){
				//check if root exist?
				if(getDirect(id) == null){
					//remove!
					System.out.println("Cleaning: remove extension sector (obj_id:"+id+") @"+sector);
					removeSector(sector);
				}
			}else if(type == FILE || type == DIRECTORY){
				//check if root exist?
				FsObject obj = getDirect(id);
				if(obj == null || ((FsObjectImplFromFile)obj).getSector() != sector){
					//remove!
					System.out.println("Cleaning: remove object (obj_id:"+id+"@"+(obj==null?"null":((FsObjectImplFromFile)obj).getSector())+") @"+sector);
					removeSector(sector);
				}
			}else if(removeMoving && type == MOVING){
				//remove!
				System.out.println("Cleaning: remove moving sector @"+sector);
				removeSector(sector);
			}else if(type == DELETED){
				//remove!
				System.out.println("Cleaning: remove deleted sector (because the type is not used and i don't know what to do with it) @"+sector);
				removeSector(sector);
			}else if(type == CHUNK){
				FsChunk obj = chunkId2LoadedObj.get(id);
				if(obj == null || ((FsChunkFromFile)obj).getSector() != sector){
					//remove!
					System.out.println("Cleaning: remove chunk (chunk_id:"+id+"@"+(obj==null?"null":((FsChunkFromFile)obj))+") @"+sector);
					removeSector(sector);
				}
			}
		}
		
	}
	
	public void removeOldDelItem(long since){
		RemoveOldDeletedItems remover = new RemoveOldDeletedItems();
		remover.dateThreshold = since;
		remover.visit(root);
	}
	
}
