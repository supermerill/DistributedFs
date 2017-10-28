package remi.distributedFS.fs.bigdata;

import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getPathDir;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getPathObjectName;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getPathParentDir;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.db.impl.bigdata.FsChunkStreamable;
import remi.distributedFS.net.AbstractMessageManager;
import remi.distributedFS.net.ClusterManager;
import remi.distributedFS.util.ByteBuff;

public class BigDataExchange extends AbstractMessageManager {
	

	public static final byte  APPEND = (byte) 40; //send a "line" to a file
	public static final byte  WANT_APPEND = (byte) 41; // want to receive all append from a file
	public static final byte  SUBMIT_FUNC = (byte) 42; // send a jar
	public static final byte  DO_MAP = (byte) 43; //use a jar to map data from a file to another
	public static final byte  DO_SORT = (byte) 44; // use a jar to compare and store result into an other file
	public static final byte  DO_MERGE = (byte) 45; // like DO_MAP but with 2 files in parameters, and each iteration choose what to next(). You should do sort before this
	public static final byte  DO_SPLIT = (byte) 46; // like DO_MAP but with a filemane returned for every line
	
	public static final byte  FAIL = (byte) 49; // when a command is in error
	
	DistributedBigDataClientMananger manager;
	public BigDataExchange(DistributedBigDataClientMananger manager) {
		super();
		this.manager = manager;
	}

	@Override
	public void receiveMessage(long senderId, byte messageId, ByteBuff message) {
		if(manager.getNet().getComputerId(senderId) <0){
			//error: not a estabished peer
			System.err.println(manager.getNet().getComputerId()+"$ Error, peer "+senderId%100+" ask us a file/dir and he doens't have a computerid !"+manager.getNet().getComputerId(senderId));
			return;
		}
		if(messageId == APPEND){
			System.out.println(this.manager.getComputerId()+"$ RECEIVE APPEND from "+senderId);
			//get File
			receiveAppend(senderId, message);
		}
		if(messageId == WANT_APPEND){
			System.out.println(this.manager.getComputerId()+"$ RECEIVE WANT_APPEND from "+senderId);
			emitWholeFile(senderId, message);
		}
		if(messageId == SUBMIT_FUNC){
			System.out.println(this.manager.getComputerId()+"$ RECEIVE SUBMIT_FUNC from "+senderId);
			receiveJar(senderId, message);
		}
		if(messageId == DO_MAP){
			System.out.println(this.manager.getComputerId()+"$ RECEIVE DO_MAP from "+senderId);
			long fileIn = message.getLong();
			String jarName = message.getUTF8();
			String funcName = message.getUTF8();
			long fileOut = message.getLong();
			manager.map(getChunkStreamable(fileIn), jarName, funcName, getChunkStreamable(fileOut), message);
		}
		if(messageId == DO_SORT){
			System.out.println(this.manager.getComputerId()+"$ RECEIVE DO_SORT from "+senderId);
			long fileIn = message.getLong();
			String jarName = message.getUTF8();
			String funcName = message.getUTF8();
			long fileOut = message.getLong();
			manager.sort(getChunkStreamable(fileIn), jarName, funcName, getChunkStreamable(fileOut), message);
		}
		if(messageId == DO_MERGE){
			System.out.println(this.manager.getComputerId()+"$ RECEIVE DO_MERGE from "+senderId);
			long fileIn1 = message.getLong();
			long fileIn2 = message.getLong();
			String jarName = message.getUTF8();
			String funcName = message.getUTF8();
			long fileOut = message.getLong();
			manager.merge(getChunkStreamable(fileIn1), getChunkStreamable(fileIn2), jarName, funcName, getChunkStreamable(fileOut), message);
		}
		if(messageId == DO_SPLIT){
			System.out.println(this.manager.getComputerId()+"$ RECEIVE DO_SPLIT from "+senderId);
			long fileIn = message.getLong();
			String jarName = message.getUTF8();
			String funcName = message.getUTF8();
			manager.split(getChunkStreamable(fileIn), jarName, funcName, message);
		}
	}
	
	private FsChunkStreamable getChunkStreamable(long fileIn) {
		FsChunk chunk = manager.getDb().getFileDirect(fileIn).getChunks().get(0);
		return (FsChunkStreamable) chunk;
	}

	private void receiveJar(long senderId, ByteBuff message) {
		try {
			String jarName = message.getUTF8();
			//save jar byte[] from message into a file.
			File jarDir = new File("./jar");
			if(!jarDir.exists()) jarDir.mkdir();
			File newJarFile = new File("./jar/"+jarName);
			//write/erase
			try(BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(newJarFile))){
				fout.write(message.array(), message.position(), message.limit()-message.position());
				fout.flush();
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public void emitJar(long peerId, String filepath, String filename) {
		
		try {
			// get byte[] from file
			byte[] fileData = Files.readAllBytes(FileSystems.getDefault().getPath(filepath));
			
			ByteBuff msg = new ByteBuff();
			msg.putUTF8(filename);
			msg.put(fileData);

			manager.getNet().writeMessage(peerId, SUBMIT_FUNC, msg);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void emitWholeFile(final long senderId,final ByteBuff message) {
		final long fileId = message.getLong();
		final String filePath = message.getUTF8();
		FsFile fic = manager.getDb().getFileDirect(fileId);
		if(fic == null) {
			fic = FsDirectory.FsDirectoryMethods.getPathFile(manager.getDb().getRoot(), filePath);
		}
		if(fic == null) {
			System.out.println(this.manager.getComputerId()%100+" OBJCG can't find parent(2) : request parent and terminate this APPEND parsing. "
			+filePath+"=="+getPathParentDir(manager.getDb().getRoot(), filePath)+" -> "+filePath.substring(0, filePath.lastIndexOf('/')));
			//emit "fail" message?
			emitFail(new ByteBuff().put(WANT_APPEND).putLong(fileId).putUTF8(filePath).putUTF8("FILE_NOT_FOUND").flip());
			return;
		}else {
			final FsFile ficToSend = fic;
			new Thread() {
				public void run() {
					FsChunk chunk = ficToSend.getChunks().get(0);
					if(! (chunk instanceof FsChunkStreamable)) {
						System.err.println("Error, not a chunkStreamble : "+filePath);
						//emit "fail" message?
						emitFail(new ByteBuff().put(APPEND).putLong(fileId).putUTF8(filePath).putUTF8("BAD_CHUNK").flip());
						return;
					}else {
						for(ByteBuff buff : ((FsChunkStreamable)chunk)) {
							sendAppend(senderId, ficToSend, buff);
						}
					}
				}
			}.start();
		}
	}

	public void sendAppend(long senderId, FsFile fic, ByteBuff buff) {
		ByteBuff buffToEmit = new ByteBuff();
		buffToEmit.putLong(fic.getId());
		buffToEmit.putUTF8(fic.getPath());
		buffToEmit.put(buff);
		manager.getNet().writeMessage(senderId, APPEND, buffToEmit);
	}

	private void receiveAppend(long senderId, ByteBuff message) {
		long fileId = message.getLong();
		String filePath = message.getUTF8();
		FsFile fic = manager.getDb().getFileDirect(fileId);
		if(fic == null) {
			fic = FsDirectory.FsDirectoryMethods.getPathFile(manager.getDb().getRoot(), filePath);
		}
		if(fic == null) {
			FsDirectory root = manager.getDb().getRoot();
			//check dirParent
			FsDirectory dirParent = getPathParentDir(root, filePath);
			if(dirParent==null){
				dirParent = getPathDir(root, filePath.substring(0, filePath.lastIndexOf('/')));
			}
			if(dirParent==null){
				//request parent before child.
				manager.getDirFileExchange().requestDirPath(senderId, filePath.substring(0, filePath.lastIndexOf('/')),-1);
				System.out.println(this.manager.getComputerId()%100+" OBJCG can't find parent(2) : request parent and terminate this APPEND parsing. "
				+filePath+"=="+getPathParentDir(root, filePath)+" -> "+filePath.substring(0, filePath.lastIndexOf('/')));
				//emit "fail" message?
				emitFail(new ByteBuff().put(APPEND).putLong(fileId).putUTF8(filePath).putUTF8("DIR_NOT_FOUND").flip());
				return;
			}
			//create file
			fic = dirParent.createSubFile(getPathObjectName(root,filePath));
		}
		//create chunk if needed
		if(fic.getNbChunks() == 0) {
			fic.createNewChunk(-1);
		}
		//append
		FsChunk chunk = fic.getChunks().get(0);
		if(chunk instanceof FsChunkStreamable) {
			((FsChunkStreamable)chunk).append(message);
		}else {
			System.err.println("Error, not a chunkStreamble : "+filePath);
			//emit "fail" message?
			emitFail(new ByteBuff().put(APPEND).putLong(fileId).putUTF8(filePath).putUTF8("BAD_CHUNK").flip());
			return;
		}
	}
	
	public void emitFail(ByteBuff msg) {
		
	}

	public void register(ClusterManager net) {
		net.registerListener(APPEND, this);
		net.registerListener(WANT_APPEND, this);
		net.registerListener(SUBMIT_FUNC, this);
		net.registerListener(DO_MAP, this);
		net.registerListener(DO_SORT, this);
		net.registerListener(DO_MERGE, this);
		net.registerListener(DO_SPLIT, this);
	}
	

}
