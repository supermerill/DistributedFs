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
import remi.distributedFS.db.impl.FsDirectoryFromFile;
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
		System.out.println(this.manager.getComputerId()%100+" getHeader ");
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

	//TODO: sendobject, and receive for the 2 of them.
	public void requestPath(long serverId, long objId) {
//		System.out.println(this.manager.getComputerId()%100+" WRITE GET DIR "+path+" for "+senderId);
		ByteBuff buff = new ByteBuff();
		buff.putLong(objId);
		buff.flip();
		manager.getNet().writeMessage(serverId, GET_OBJECT, buff);
	}
	
	public ByteBuff createFileDescrMessage(FsFile fic){
		String path = fic.getPath();
		//create dir data update.
//		System.out.println(manager.getNet().getId()%100+" WRITE SEND FILE "+path+" for "+senderId);
		ByteBuff message = new ByteBuff();
		message.put((byte)1);
		//send path
		message.putUTF8(path);
		//id
		message.putLong(fic.getId());
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
			long  fileId = message.getLong();
			if(fileId<0){
				System.out.println(this.manager.getComputerId()%100+" READ SEND File  id = "+fileId+" << USELESSS!!!!!!");
				return;
			}
			FsDirectory root = manager.getRoot();
			FsFile fic = getPathFile(root, path);
			boolean changes = false;
			if(fic == null){
				//try to see if it has moved
				FsObject obj = manager.getDb().getDirect(fileId);
				if(obj != null){
					if(obj instanceof FsFile){
						System.out.println(this.manager.getComputerId()%100+" Finded a displaced file : "+obj.getPath()+" -> "+path);
						fic = (FsFile) obj;
						System.err.println("TODO: move the file");
					}else{
						System.err.println(this.manager.getComputerId()%100+" Finded a displaced thing! : "+obj+" which isn't a file : "+obj.getPath()+" -> "+path);
						System.err.println("TODO: correct the error");
					}
				}
			}
			if(fic == null){
				//check if it's not a delete notification
				if(isDeleted(message)){
					System.out.println(this.manager.getComputerId()%100+" notif of detion on an not-existant file -> no-event!");
					return;
				}else{
					System.out.println(this.manager.getComputerId()%100+" can't find fic "+path);
					//Create new dir!
					
					FsDirectory ficParent = getPathParentDir(root, path);
					if(ficParent==null){
						ficParent = getPathDir(root, path.substring(0, path.lastIndexOf('/')));
					}
					if(ficParent==null){
						//request parent before child.
						requestDirPath(senderId, path.substring(0, path.lastIndexOf('/')));
						System.out.println(this.manager.getComputerId()%100+" FICCG can't find parent : request parent and terminate this message parsing. "+path+"=="+getPathParentDir(root, path)+" -> "+path.substring(0, path.lastIndexOf('/')));
						return;
					}
					fic = ficParent.createSubFile(getPathObjectName(root,path));
					fic.setId(fileId);
					ficParent.flush();
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
		message.putLong(dir.getId());
		putHeader(message, dir);
		List<FsDirectory> arrayDir = new ArrayList<>(dir.getDirs());
		message.putTrailInt(arrayDir.size());
		System.out.println("WRITE DIR : "+arrayDir.size());
		for(FsDirectory dchild : arrayDir){
			message.putUTF8(dchild.getName());
			message.putLong(dchild.getId());
			message.putLong(dchild.getModifyDate());
		}
		List<FsFile> arrayFile = new ArrayList<>(dir.getFiles());
		message.putTrailInt(arrayFile.size());
		System.out.println("WRITE FIC : "+arrayFile.size());
		for(FsFile fchild : arrayFile){
			message.putUTF8(fchild.getName());
			message.putLong(fchild.getId());
			message.putLong(fchild.getModifyDate());
		}
		List<FsObject> arrayDel = new ArrayList<>(dir.getDelete());
		System.out.println("WRITE DEL : "+arrayDel.size());
		message.putTrailInt(arrayDel.size());
		for(FsObject ochild : arrayDel){
			System.out.println("WRITE DEL NAME : "+ochild.getName().length()+" "+ochild.getName());
			message.putUTF8(ochild.getName());
			message.putLong(ochild.getId());
			message.putLong(ochild.getDeleteDate());
		}
		message.flip();
		return message;
	}
	
	private <E extends FsObject> E containsId(List<E> lst, long id){
		for(E e : lst){
			if(e.getId() == id){
				return e;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unused")
	public void getADirFrom(long senderId, ByteBuff message){
		System.out.println(this.manager.getComputerId()%100+" READ SEND DIR from "+senderId);
		if (message.get()==1){
			String path = message.getUTF8();
			long rootId = message.getLong();
			if(rootId<0){
				System.out.println(this.manager.getComputerId()%100+" READ SEND DIR  Id = "+rootId+" << USELESSS!!!!!!");
				return;
			}
			System.out.println(this.manager.getComputerId()%100+" READ SEND DIR  "+path+" from "+senderId);
			FsDirectory root = manager.getRoot();
			FsDirectory dir = getPathDir(root, path);
			if(dir == null){
				//try to see if it has moved
				FsObject obj = manager.getDb().getDirect(rootId);
				if(obj != null){
					if(obj instanceof FsDirectory){
						System.out.println(this.manager.getComputerId()%100+" DIRCG Finded a displaced dir : "+obj.getPath()+" -> "+path);
						dir = (FsDirectory) obj;
						System.err.println("TODO: move the directory");
					}else{
						System.err.println(this.manager.getComputerId()%100+" DIRCG Finded a displaced thing! : "+obj+" which isn't a dir : "+obj.getPath()+" -> "+path);
						System.err.println("TODO: correct the error");
					}
				}
			}
			boolean changes = false;
			if(dir == null){
				//check if it's not a delete notification
				if(isDeleted(message)){
					System.out.println(this.manager.getComputerId()%100+" DIRCG notif of detion on an not-existant folder -> no-event!");
					return;
				}else{
					System.out.println(this.manager.getComputerId()%100+" DIRCG can't find dir "+path);
					//Create new dir!
					FsDirectory dirParent = getPathParentDir(root, path);
					if(dirParent==null){
						dirParent = getPathDir(root, path.substring(0, path.lastIndexOf('/')));
					}
					if(dirParent==null){
						//request parent before child.
						requestDirPath(senderId, path.substring(0, path.lastIndexOf('/')));
						System.out.println(this.manager.getComputerId()%100+" DIRCG can't find parent : request parent and terminate this emssage parsing. "+path+"=="+getPathParentDir(root, path)+" -> "+path.substring(0, path.lastIndexOf('/')));
						return;
					}
					dir = dirParent.createSubDir(getPathObjectName(root,path));
					dir.setId(rootId);
					dirParent.flush();
					changes = true;
				}
			}
			else{
				//TODO: check if it's not deleted?
				System.out.println(this.manager.getComputerId()%100+" DIRCG dir "+path+ " already there : "+dir.getPath());
				System.out.println(dir.getPath());
			}
			
			changes = getHeader(message, dir) || changes;


			//don't verify changes if we are more recent than them
			//TODO change this by a better way
//			if(changes){
			System.out.println(this.manager.getComputerId()%100+" DIRCG check dir inside "+dir.getPath());
				int nbDir = message.getTrailInt();
				System.out.println(this.manager.getComputerId()%100+" DIRCG "+dir.getPath()+" has "+nbDir+" dirs");
				List<FsDirectory> direxist = new ArrayList<FsDirectory>(dir.getDirs());
				for(int i=0;i<nbDir;i++){
					String dirName = message.getUTF8();
					long dirId = message.getLong();
					long dirDate = message.getLong();
					FsDirectory childDir = getDir(dir,dirName);
					if(childDir == null){
						//if can't retrieve by name, try with id.
						childDir = containsId(dir.getDirs(), dirId);
						if(childDir != null){
							System.out.println(this.manager.getComputerId()%100+" DIRCG oh, a dir "+childDir==null?"null":childDir.getName()+" was name differently in the peer"+senderId%100+" : "+dirName);
						}
					}
					if(childDir == null){
						// it was moved?
						FsObject obj = manager.getDb().getDirect(dirId);
						if(obj != null){
							if(obj instanceof FsDirectory){
								System.out.println(this.manager.getComputerId()%100+" DIRCG oh, a dir "+obj.getPath()+" was moved "+senderId%100+" : "+path+"/"+dirName);
								childDir = (FsDirectory) obj;
							}else{
								System.err.println(this.manager.getComputerId()%100+" DIRCG oh, a obj (which is not a dir) "+obj.getPath()+" was moved"+senderId%100+" : "+path+"/"+dirName);
								System.err.println("TODO: correct the error");
							}
							//request more info to make the move
							requestDirPath(senderId, path+"/"+dirName);
						}
					}
					System.out.println(this.manager.getComputerId()%100+" DIRCG oh, a dir '"+dirName+"' path.endsWith(/)?="+path.endsWith("/")+" finded? "+childDir!=null);
					if(childDir == null){
						//check if it's not already in our delete file name
						FsObject deletedDir = containsId(dir.getDelete(), dirId);
						if(deletedDir != null){
							//TODO: check if the creation is not more recent than the deletion
							System.out.println(this.manager.getComputerId()%100+" DIRCG directory "+deletedDir.getName()+" was deleted, i won't add it at "+dirName);
							System.err.println("TODO: move the dir(2)");
						}else{
							changes = true;
							requestDirPath(senderId, path+(path.endsWith("/")?"":"/")+dirName);
							System.out.println(this.manager.getComputerId()%100+" DIRCG new dir: "+path+(path.endsWith("/")?"":"/")+dirName);
						}
					}else{
						System.out.println(this.manager.getComputerId()%100+" DIRCG directory was already here "+dirName+", time: local:"+childDir.getModifyDate()+" =?= dist:"+dirDate);
						if(childDir.getModifyDate() < dirDate){
							//ask for updates
							requestDirPath(senderId, childDir.getPath());
							System.out.println(this.manager.getComputerId()%100+" DIRCG dir req update");
						}
						Iterator<FsDirectory> it = direxist.iterator();
						while(it.hasNext()){
							if(it.next().getName().equals(dirName)){
								it.remove();
							}
						}
					}
				}
				
				
				int nbFile = message.getTrailInt();
				System.out.println(this.manager.getComputerId()%100+" DIRCG "+dir.getPath()+" has "+nbFile+" files");
				List<FsFile> fileExist = new ArrayList<>(dir.getFiles());
				for(int i=0;i<nbFile;i++){
					String fileName = message.getUTF8();
					long fileId = message.getLong();
					long fileDate = message.getLong();
					FsFile childFile = getFile(dir,fileName);
					if(childFile == null){
						//if can't retreive by name, try with id.
						childFile = containsId(dir.getFiles(), fileId);
						if(childFile != null)
							System.out.println(this.manager.getComputerId()%100+" DIRCG oh, a file '"+childFile==null?"null":childFile.getName()+" was name differently in the peer"+senderId%100+" : "+fileName);
					}
					if(childFile == null){
						// it was moved?
						FsObject obj = manager.getDb().getDirect(fileId);
						if(obj != null){
							if(obj instanceof FsFile){
								System.out.println(this.manager.getComputerId()%100+" DIRCG oh, a dir "+childFile.getPath()+" was moved"+senderId%100+" : "+path+"/"+fileName);
								childFile = (FsFile) obj;
								System.err.println("TODO: move the file(2)");
							}else{
								System.err.println(this.manager.getComputerId()%100+" DIRCG oh, a obj (which is not a file) "+obj.getPath()+" was moved"+senderId%100+" : "+path+"/"+fileName);
								System.err.println("TODO: correct the error");
							}
							//request more info to make the move
							requestFilePath(senderId, path+"/"+fileName);
						}
					}
					if(childFile == null){
						//check if it's not already in our delete file name
						FsObject deletedFile = containsId(dir.getDelete(), fileId);
						if(deletedFile != null){
							System.out.println(this.manager.getComputerId()%100+" DIRCG file "+deletedFile.getName()+" was deleted, i won't add it at "+fileName);
						}else{
							changes = true;
							//TODO: check if it's not already in our delete file name
							requestFilePath(senderId, path+(path.endsWith("/")?"":"/")+fileName);
							System.out.println(this.manager.getComputerId()%100+" DIRCG new file: "+path+(path.endsWith("/")?"":"/")+fileName);
						}
					}else{
						System.out.println(this.manager.getComputerId()%100+" DIRCG file was already here "+fileName+", time: local:"+childFile.getModifyDate()+" =?= dist:"+fileDate);
						if(childFile.getModifyDate() < fileDate){
							//ask for updates
							requestPath(senderId, childFile.getId());
							System.out.println(this.manager.getComputerId()%100+" DIRCG file req update");
						}
						Iterator<FsFile> it = fileExist.iterator();
						while(it.hasNext()){
							if(it.next().getName().equals(fileName)){
								it.remove();
							}
						}
					}
				}

				int nbDel = message.getTrailInt();
				System.out.println(this.manager.getComputerId()%100+" DIRCG "+dir.getPath()+" has "+nbDel+" del objects");
				for(int i=0;i<nbDel;i++){
					String objectName = message.getUTF8();
					long objectId = message.getLong();
					long deleteDate = message.getLong();
					//try to find them in dir/file exist 
					FsObject obj = null;
					boolean finded = false;
//					Iterator<FsDirectory> itD = direxist.iterator();
//					while(itD.hasNext() && !finded){
//						obj = itD.next();
//						if(obj.getName().equals(objectName) ){
//							finded = true;
//						}
//					}
//					Iterator<FsFile> itF = fileExist.iterator();
//					while(itF.hasNext() && !finded){
//						obj = itF.next();
//						if(obj.getName().equals(objectName)){
//							finded = true;
//						}
//					}
					obj = containsId(dir.getDirs(), objectId);
					if(obj==null){
						obj = containsId(dir.getFiles(), objectId);
						if(obj==null){
							finded = false;
						}else{
							finded = true;
							System.out.println(this.manager.getComputerId()%100+" need to delete file "+obj.getName()+" == "+objectName);
						}
					}else{
						finded = true;
						System.out.println(this.manager.getComputerId()%100+" need to delete dir "+obj.getName()+" == "+objectName);
					}
					
					if(finded){
						//check date to see if we have to delete our
						if(obj.getModifyDate()<=deleteDate){
							System.out.println(this.manager.getComputerId()%100+" erase: "+path+(path.endsWith("/")?"":"/")+objectName);
//							FsDirectory.FsDirectoryMethods.deleteAndFlush(obj);
							obj.accept(FsDirectory.FsDirectoryMethods.REMOVER);
							changes = true;
						}
					}else{
						obj = containsId(dir.getDelete(), objectId);
						if(obj==null){

							// it was moved?
							obj = manager.getDb().getDirect(objectId);
							if(obj != null){
								System.out.println(this.manager.getComputerId()%100+" DIRCG oh, a dir "+obj.getPath()+" was moved AND deleted "+senderId%100+" : "+path+"/"+objectName);
								System.err.println("TODO: delete this entry");
							}else{
								System.out.println(this.manager.getComputerId()%100+" need ??  to add delete obj  ?? "+objectName);
							}
						}else{
							System.out.println(this.manager.getComputerId()%100+" already del obj "+obj.getName()+" == "+objectName);
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
				System.out.println(this.manager.getComputerId()%100+" changes for "+dir);
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
		if(manager.getNet().getComputerId(senderId) <0){
			//error: not a estabished peer
			System.err.println(manager.getNet().getComputerId()+"$ Error, peer "+senderId%100+" ask us a file/dir and he doens't have a computerid !"+manager.getNet().getComputerId(senderId));
			return;
		}
		if(messageId == SEND_FILE_DESCR){
			//TODO ajout/fusion
			//notTODO request chunks? -> i think it's more a db thing, to know if we want one.
			getAFileFrom(senderId, message);
		}
		if(messageId == SEND_DIR){
			System.out.println(this.manager.getComputerId()+"$ RESEIVE SEND DIR from "+senderId);
			getADirFrom(senderId, message);
		}
		if(messageId == GET_DIR){
			System.out.println(this.manager.getComputerId()+"$ RESEIVE GET DIR from "+senderId);
			String path = message.getUTF8();
			FsDirectory dir = getPathDir(manager.getRoot(), path);
			ByteBuff messageRet = null;
			if(dir==null){
				messageRet = createNotFindPathMessage(path);
				System.out.println(this.manager.getComputerId()+"$ NOT SEND DIR for "+senderId);
			}else{
				messageRet = createDirectoryMessage(dir);
				System.out.println(this.manager.getComputerId()+"$ WRITE SEND DIR for "+senderId);
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
