package remi.distributedFS.fs.messages;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.*;
import static remi.distributedFS.datastruct.FsFile.FsFileMethods.*;
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
	
	public boolean getHeader(ByteBuff message, FsObject obj){
		if(obj.getId() != message.getLong()) System.err.println("Error, file id is <> for "+obj.getPath());
		long modifyDate = message.getLong();
		if(modifyDate > obj.getModifyDate()){
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
	
	public ByteBuff createDirectoryMessage(FsDirectory dir){

		String path = dir.getPath();

//		System.out.println(p.getMyServer().getId()%100+" WRITE SEND DIR "+path+" for "+p.getKey().getOtherServerId()%100);
		ByteBuff message = new ByteBuff();
		message.put((byte)1);
		message.putUTF8(path);
		putHeader(message, dir);
		message.putTrailInt(dir.getDirs().size());
		for(FsDirectory dchild : dir.getDirs()){
			message.putUTF8(dchild.getName());
		}
		message.putTrailInt(dir.getFiles().size());
		for(FsFile fchild : dir.getFiles()){
			message.putUTF8(fchild.getName());
		}
		message.flip();
		return message;
	}

	private ByteBuff createNotFindPathMessage(String path) {
		ByteBuff message = new ByteBuff();
		message.put((byte)0);
		//send path
		message.putUTF8(path);
		return message.flip();
	}
	
	public ByteBuff createFileDescrMessage(FsFile fic){
		String path = fic.getPath();
		//create dir data update.
//		System.out.println(manager.getNet().getId()%100+" WRITE SEND FILE "+path+" for "+p.getKey().getOtherServerId()%100);
		ByteBuff message = new ByteBuff();
		message.put((byte)1);
		//send path
		message.putUTF8(path);
		//send metadata
		putHeader(message, fic);
		//send nbChunks
		message.putTrailInt(fic.getNbChunks());
		//send my chunks timestamps (0 if i didn't have this one
		for(int i=0; i<fic.getNbChunks();i++){
			FsChunk chunk = getChunk(fic, i);
			if(chunk!=null && chunk.isPresent()){
				message.putLong(chunk.lastModificationTimestamp());
			}else{
				message.putLong(0);
			}
		}
		message.flip();
		return message;
	}

	public void requestDirPath(String path){
//		System.out.println(p.getMyServer().getId()%100+" WRITE GET DIR "+path+" for "+p.getKey().getOtherServerId()%100);
		ByteBuff buff = new ByteBuff();
		buff.putUTF8(path);
		buff.flip();
		manager.getNet().writeBroadcastMessage(GET_DIR, buff);
	}
	
	public void requestFilePath(String path){
//		System.out.println(p.getMyServer().getId()%100+" WRITE GET DIR "+path+" for "+p.getKey().getOtherServerId()%100);
		ByteBuff buff = new ByteBuff();
		buff.putUTF8(path);
		buff.flip();
		manager.getNet().writeBroadcastMessage(GET_FILE_DESCR, buff);
	}

	public void requestDirPath(long serverId, String path){
//		System.out.println(p.getMyServer().getId()%100+" WRITE GET DIR "+path+" for "+p.getKey().getOtherServerId()%100);
		ByteBuff buff = new ByteBuff();
		buff.putUTF8(path);
		buff.flip();
		manager.getNet().writeMessage(serverId, GET_DIR, buff);
	}
	
	public void requestFilePath(long serverId, String path){
//		System.out.println(p.getMyServer().getId()%100+" WRITE GET DIR "+path+" for "+p.getKey().getOtherServerId()%100);
		ByteBuff buff = new ByteBuff();
		buff.putUTF8(path);
		buff.flip();
		manager.getNet().writeMessage(serverId, GET_FILE_DESCR, buff);
	}

	
	public void getADirFrom(long senderId, ByteBuff message){
//		System.out.println(p.getMyServer().getId()%100+" READ SEND DIR from "+p.getKey().getOtherServerId()%100);
		if (message.get()==1){
			String path = message.getUTF8();
//			System.out.println(p.getMyServer().getId()%100+" READ SEND DIR "+mypath+" => "+path+" from "+p.getKey().getOtherServerId()%100);
			FsDirectory root = manager.getRoot();
			FsDirectory dir = getPathDir(root, path);
			boolean changes = false;
			if(dir == null){
//				System.out.println(p.getMyServer().getId()%100+" can't find dir "+path);
				//Create new dir!
				
				FsDirectory dirParent = getPathParentDir(root, path);
				dir = dirParent.createSubDir(getPathObjectName(root,path));
				changes = true;
			}
			else{
//				System.out.println(p.getMyServer().getId()%100+" dir "+path+ " already there : "+dir.getPath());
				System.out.println(dir.getPath());
			}
			changes = changes || getHeader(message, dir);
			int nbDir = message.getTrailInt();
			for(int i=0;i<nbDir;i++){
				String dirName = message.getUTF8();
				FsDirectory childDir = getDir(dir,dirName);
//				System.out.println("oh, a dir '"+dirName+"' path.endsWith(/)?="+path.endsWith("/"));
				if(childDir == null){
					changes = true;
					requestDirPath(senderId, path+(path.endsWith("/")?"":"/")+dirName);
				}
			}
			int nbFile = message.getTrailInt();
			for(int i=0;i<nbFile;i++){
				String fileName = message.getUTF8();
				FsFile childFile = getFile(dir,fileName);
				if(childFile == null){
					changes = true;
					requestFilePath(senderId, path+(path.endsWith("/")?"":"/")+fileName);
				}
			}
		}else{
			String mypath = message.getUTF8();
//			System.out.println(p.getMyServer().getId()%100+" READ SEND DIR : NO dir "+mypath+" from "+p.getKey().getOtherServerId()%100);
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
		}
		if(messageId == SEND_DIR){
			getADirFrom(senderId, message);
		}
		if(messageId == GET_DIR){
			String path = message.getUTF8();
			FsDirectory dir = getPathDir(manager.getRoot(), path);
			ByteBuff messageRet = null;
			if(dir==null){
				messageRet = createNotFindPathMessage(path);
			}else{
				messageRet = createDirectoryMessage(dir);
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
