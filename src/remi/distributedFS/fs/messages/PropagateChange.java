package remi.distributedFS.fs.messages;

import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getDir;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getFile;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getPathDir;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getPathFile;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getPathObjectName;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getPathParentDir;
import static remi.distributedFS.datastruct.FsFile.FsFileMethods.getChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.net.ClusterManager;
import remi.distributedFS.util.ByteBuff;

public class PropagateChange extends AbstractFSMessageManager implements FsObjectVisitor {
	
	FileSystemManager manager;

	public PropagateChange(FileSystemManager manager) {
		super();
		this.manager = manager;
	}

	@Override
	public void visit(FsDirectory dir) {
		manager.getNet().writeBroadcastMessage(SEND_DIR, createDirectoryMessage(dir));
	}
	@Override
	public void visit(FsFile fic) {
		manager.getNet().writeBroadcastMessage(SEND_FILE_DESCR, createFileDescrMessage(fic));
	}
	
	public void putHeader(ByteBuff message, FsObject obj){
		message.putLong(obj.getId());
		message.putLong(obj.getModifyDate());
		
		message.putLong(obj.getParentId());
		message.putLong(obj.getUserId());
		message.putLong(obj.getGroupId());
		message.putShort(obj.getPUGA());
		
		message.putLong(obj.getCreationDate());
		message.putLong(obj.getDeleteDate());
		
		message.putUTF8(obj.getName());
		
		message.putLong(obj.getCreatorUID());
		message.putLong(obj.getModifyUID());
		message.putLong(obj.getDeleteUID());
	}
	
	public boolean isDeleted(ByteBuff message){
		//check if parentId is -1
		int pos = message.position();
		long myId = message.getLong();
		long modifyDate = message.getLong();
		long parentId = message.getLong();
		
		message.position(pos);
		
		return parentId < 0;
	}
	
	public boolean getHeader(ByteBuff message, FsObject obj){
		long id = message.getLong(); //useless for now (maybe later, to use a "rename" function)
//		if(obj.getId() != id) System.err.println("Error, file id is <"+id+"> for "+obj.getPath());
		long modifyDate = message.getLong();
		if(modifyDate > obj.getModifyDate()){
			System.out.println(this.manager.getComputerId()%100+" sended dir is new! nice! : "+modifyDate+ " > "+obj.getModifyDate());
			//more recent, update!
			obj.setParentId(message.getLong());
			obj.setUserId(message.getLong());
			obj.setGroupId(message.getLong());
			obj.setPUGA(message.getShort());
			
			obj.setCreationDate(message.getLong());
			obj.setModifyDate(modifyDate);
			obj.setDeleteDate(message.getLong());

			obj.setName(message.getUTF8());

			obj.setCreatorUID(message.getLong());
			obj.setModifyUID(message.getLong());
			obj.setDeleteUID(message.getLong());
			return true;
		}else{
			System.out.println(this.manager.getComputerId()%100+" sended dir is OOOOld: "+modifyDate+ " < "+obj.getModifyDate());
			//just parse, ignore it.
			message.getLong(); // pid
			message.getLong(); // uid
			message.getLong(); // gid
			message.getShort(); // puga

			message.getLong(); // dateC
			message.getLong(); // dateD
			
			message.getUTF8();
			
			message.getLong(); // cuid
			message.getLong(); // muid
			message.getLong(); // duid
			
			return false;
			
		}
	}

	private ByteBuff createNotFindPathMessage(String path) {
		ByteBuff message = new ByteBuff();
		message.put((byte)0);
		//send path
		message.putUTF8(path);
		return message.flip();
	}

	public void requestDirPath(String path){
		System.out.println(this.manager.getComputerId()%100+" WRITE GET DIR "+path+" for all");
		ByteBuff buff = new ByteBuff();
		buff.putUTF8(path);
		buff.flip();
		manager.getNet().writeBroadcastMessage(GET_DIR, buff);
	}
	
	public void requestFilePath(String path){
//		System.out.println(this.manager.getComputerId()%100+" WRITE GET DIR "+path+" for "+senderId);
		ByteBuff buff = new ByteBuff();
		buff.putUTF8(path);
		buff.flip();
		manager.getNet().writeBroadcastMessage(GET_FILE_DESCR, buff);
	}

	public void requestDirPath(long serverId, String path){
//		System.out.println(this.manager.getComputerId()%100+" WRITE GET DIR "+path+" for "+senderId);
		ByteBuff buff = new ByteBuff();
		buff.putUTF8(path);
		buff.flip();
		manager.getNet().writeMessage(serverId, GET_DIR, buff);
	}
	
	public void requestFilePath(long serverId, String path){
//		System.out.println(this.manager.getComputerId()%100+" WRITE GET DIR "+path+" for "+senderId);
		ByteBuff buff = new ByteBuff();
		buff.putUTF8(path);
		buff.flip();
		manager.getNet().writeMessage(serverId, GET_FILE_DESCR, buff);
	}
	
	public ByteBuff createFileDescrMessage(FsFile fic){
		String path = fic.getPath();
		//create dir data update.
//		System.out.println(manager.getNet().getId()%100+" WRITE SEND FILE "+path+" for "+senderId);
		ByteBuff message = new ByteBuff();
		message.put((byte)1);
		//send path
		message.putUTF8(path);
		//send metadata
		putHeader(message, fic);
		//send nbChunks
		message.putTrailInt(fic.getNbChunks());
		message.putTrailInt(fic.getChunkSize());
		//send my chunks timestamps (0 if i didn't have this one
		for(int i=0; i<fic.getNbChunks();i++){
			FsChunk chunk = getChunk(fic, i);
			if(chunk!=null && chunk.isPresent()){
				message.putLong(chunk.lastModificationTimestamp());
			}else{
				message.putLong(-1);
			}
		}
		message.flip();
		return message;
	}

	private void getAFileFrom(long senderId, ByteBuff message) {
		System.out.println(this.manager.getComputerId()%100+" READ SEND FileChunk from "+senderId%100);
		if (message.get()==1){
			String path = message.getUTF8();
			FsDirectory root = manager.getRoot();
			FsFile fic = getPathFile(root, path);
			boolean changes = false;
			if(fic == null){
				//check if it's not a delete notification
				if(isDeleted(message)){
					System.out.println("notif of detion on an not-existant file -> no-event!");
					return;
				}else{
					System.out.println(this.manager.getComputerId()%100+" can't find fic "+path);
					//Create new dir!
					
					FsDirectory ficParent = getPathParentDir(root, path);
					fic = ficParent.createSubFile(getPathObjectName(root,path));
					changes = true;
				}
			}
			else{
				System.out.println(this.manager.getComputerId()%100+" dir "+path+ " already there : "+fic.getPath());
				System.out.println(fic.getPath());
			}
			changes = changes || getHeader(message, fic);
			
			//don't verify changes if we are more recent than them
			//TODO change this by a better way
			if(changes){
				int nbChunks = message.getTrailInt();
				int chunkSize = message.getTrailInt();
				if(fic.getNbChunks() != nbChunks && chunkSize != fic.getChunkSize()){
					fic.rearangeChunks(chunkSize, nbChunks);
					//invalidate all
					System.out.println(this.manager.getComputerId()%100+" READ SEND FileChunk : INVALIDATE ALL");
					for(FsChunk fsChunk : fic.getChunks()){
						fsChunk.setPresent(false);
					}
				}else{
					//check last modification timestamp
					for(int i=0;i<nbChunks;i++){
						if(fic.getChunks().get(i).lastModificationTimestamp() < message.getLong()){
							System.out.println(this.manager.getComputerId()%100+" READ SEND FileChunk : INVALIDATE "+i);
							fic.getChunks().get(i).setPresent(false);
						}
					}
				}
				
				
			}
			//flush changes
			if(changes){
				fic.flush();
			}
		}else{
			String mypath = message.getUTF8();
			System.out.println(this.manager.getComputerId()%100+" READ SEND FILE : NO file "+mypath+" from "+senderId);
			System.err.println("Requested an not existant file : "+mypath);
			// request his par<ent dir, to see if it's not deleted.
			FsDirectory dirParent = getPathParentDir( manager.getRoot(), mypath);
			if(dirParent != null) requestDirPath(senderId, dirParent.getPath());
		}
	}

	
	public ByteBuff createDirectoryMessage(FsDirectory dir){

		String path = dir.getPath();
		if(path.equals("") && dir.getParent()==dir){
			path = "/";
		}

//		System.out.println(this.manager.getComputerId()%100+" WRITE SEND DIR "+path+" for "+senderId);
		ByteBuff message = new ByteBuff();
		message.put((byte)1);
		message.putUTF8(path);
		putHeader(message, dir);
		List<FsDirectory> arrayDir = new ArrayList<>(dir.getDirs());
		message.putTrailInt(arrayDir.size());
		for(FsDirectory dchild : arrayDir){
			message.putUTF8(dchild.getName());
			message.putLong(dchild.getModifyDate());
		}
		List<FsFile> arrayFile = new ArrayList<>(dir.getFiles());
		message.putTrailInt(arrayFile.size());
		for(FsFile fchild : arrayFile){
			message.putUTF8(fchild.getName());
			message.putLong(fchild.getModifyDate());
		}
		Map<String, Long> map = new HashMap<>(dir.getDelete());
		message.putTrailInt(map.size());
		for(Entry<String, Long> item : map.entrySet()){
			message.putUTF8(item.getKey());
			message.putLong(item.getValue());
		}
		message.flip();
		return message;
	}
	
	public void getADirFrom(long senderId, ByteBuff message){
		System.out.println(this.manager.getComputerId()%100+" READ SEND DIR from "+senderId);
		if (message.get()==1){
			String path = message.getUTF8();
			System.out.println(this.manager.getComputerId()%100+" READ SEND DIR  "+path+" from "+senderId);
			FsDirectory root = manager.getRoot();
			FsDirectory dir = getPathDir(root, path);
			boolean changes = false;
			if(dir == null){
				//check if it's not a delete notification
				if(isDeleted(message)){
					System.out.println("notif of detion on an not-existant folder -> no-event!");
					return;
				}else{
					System.out.println(this.manager.getComputerId()%100+" can't find dir "+path);
					//Create new dir!
					
					FsDirectory dirParent = getPathParentDir(root, path);
					dir = dirParent.createSubDir(getPathObjectName(root,path));
					changes = true;
				}
			}
			else{
				System.out.println(this.manager.getComputerId()%100+" dir "+path+ " already there : "+dir.getPath());
				System.out.println(dir.getPath());
			}
			changes = changes || getHeader(message, dir);
			
			//don't verify changes if we are more recent than them
			//TODO change this by a better way
//			if(changes){
			System.out.println(this.manager.getComputerId()%100+" check dir inside "+dir.getPath());
				int nbDir = message.getTrailInt();
				List<FsDirectory> direxist = new ArrayList<FsDirectory>(dir.getDirs());
				for(int i=0;i<nbDir;i++){
					String dirName = message.getUTF8();
					long dirDate = message.getLong();
					FsDirectory childDir = getDir(dir,dirName);
					System.out.println("oh, a dir '"+dirName+"' path.endsWith(/)?="+path.endsWith("/"));
					if(childDir == null){
						//TODO: check if it's not already in our delete file name
						changes = true;
						requestDirPath(senderId, path+(path.endsWith("/")?"":"/")+dirName);
						System.out.println(this.manager.getComputerId()%100+" new dir: "+path+(path.endsWith("/")?"":"/")+dirName);
					}else{
						Iterator<FsDirectory> it = direxist.iterator();
						while(it.hasNext()){
							if(it.next().getName().equals(dirName)){
								it.remove();
							}
						}
					}
				}
				
				
				int nbFile = message.getTrailInt();
				List<FsFile> fileExist = new ArrayList<>(dir.getFiles());
				for(int i=0;i<nbFile;i++){
					String fileName = message.getUTF8();
					long fileDate = message.getLong();
					FsFile childFile = getFile(dir,fileName);
					if(childFile == null){
						changes = true;
						//TODO: check if it's not already in our delete file name
						requestFilePath(senderId, path+(path.endsWith("/")?"":"/")+fileName);
						System.out.println(this.manager.getComputerId()%100+" new file: "+path+(path.endsWith("/")?"":"/")+fileName);
					}else{
						Iterator<FsFile> it = fileExist.iterator();
						while(it.hasNext()){
							if(it.next().getName().equals(fileName)){
								it.remove();
							}
						}
					}
				}

				int nbDel = message.getTrailInt();
				for(int i=0;i<nbDel;i++){
					String objectName = message.getUTF8();
					long deleteDate = message.getLong();
					//try to find them in dir/file exist 
					FsObject obj = null;
					boolean finded = false;
					Iterator<FsDirectory> itD = direxist.iterator();
					while(itD.hasNext() && !finded){
						obj = itD.next();
						if(obj.getName().equals(objectName)){
							finded = true;
						}
					}
					Iterator<FsFile> itF = fileExist.iterator();
					while(itF.hasNext() && !finded){
						obj = itF.next();
						if(obj.getName().equals(objectName)){
							finded = true;
						}
					}
					if(finded){
						//check date to see if we have to delete our
						if(obj.getModifyDate()<=deleteDate){
							System.out.println(this.manager.getComputerId()%100+" erase: "+path+(path.endsWith("/")?"":"/")+objectName);
							FsDirectory.FsDirectoryMethods.deleteAndFlush(obj);
							changes = true;
						}
					}
				}
				
				
//				//remove directories that are superflus
//				for(FsDirectory toRemoveDir : direxist){
//					changes = true;
//					dir.getDirs().remove(toRemoveDir);
//					toRemoveDir.setParent(null);
//					toRemoveDir.setParentId(-1);
//					toRemoveDir.flush();
//				}
//				//remove files that are superflus
//				for(FsFile toRemovFile : fileExist){
//					changes = true;
//					dir.getFiles().remove(toRemovFile);
//					toRemovFile.setParent(null);
//					toRemovFile.setParentId(-1);
//					toRemovFile.flush();
//				}
//			}
			//flush changes
			if(changes){
				dir.flush();
			}
		}else{
			String mypath = message.getUTF8();
			System.out.println(this.manager.getComputerId()%100+" READ SEND DIR : NO dir "+mypath+" from "+senderId);
			System.err.println("Requested an not existant file/folder : "+mypath);
			// request his par<ent dir, to see if it's not deleted.
			FsDirectory dirParent = getPathParentDir( manager.getRoot(), mypath);
			if(dirParent != null) requestDirPath(senderId, dirParent.getPath());
		}
	}

	@Override
	public void receiveMessage(long senderId, byte messageId, ByteBuff message) {
		if(messageId == SEND_FILE_DESCR){
			//TODO ajout/fusion
			//notTODO request chunks? -> i think it's more a db thing, to know if we want one.
			getAFileFrom(senderId, message);
		}
		if(messageId == SEND_DIR){
			System.out.println(this.manager.getComputerId()%100+" RESEIVE SEND DIR from "+senderId);
			getADirFrom(senderId, message);
		}
		if(messageId == GET_DIR){
			System.out.println(this.manager.getComputerId()%100+" RESEIVE GET DIR from "+senderId);
			String path = message.getUTF8();
			FsDirectory dir = getPathDir(manager.getRoot(), path);
			ByteBuff messageRet = null;
			if(dir==null){
				messageRet = createNotFindPathMessage(path);
				System.out.println(this.manager.getComputerId()%100+" NOT SEND DIR for "+senderId);
			}else{
				messageRet = createDirectoryMessage(dir);
				System.out.println(this.manager.getComputerId()%100+" WRITE SEND DIR for "+senderId);
			}
			manager.getNet().writeMessage(senderId, SEND_DIR, messageRet);
		}
		if(messageId == GET_FILE_DESCR){
			String path = message.getUTF8();
			FsFile file = getPathFile(manager.getRoot(), path);
			ByteBuff messageRet = null;
			if(file==null){
				messageRet = createNotFindPathMessage(path);
			}else{
				messageRet = createFileDescrMessage(file);
			}
			manager.getNet().writeMessage(senderId, SEND_FILE_DESCR, messageRet);
		}
	}

	public void register(ClusterManager net) {
		net.registerListener(GET_DIR, this);
		net.registerListener(GET_FILE_DESCR, this);
		net.registerListener(SEND_DIR, this);
		net.registerListener(SEND_FILE_DESCR, this);
	}
	

}
