package remi.distributedFS.db.impl.readable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.db.UnreachableChunkException;
import remi.distributedFS.db.impl.FsChunkFromFile;
import remi.distributedFS.db.impl.FsDirectoryFromFile;
import remi.distributedFS.db.impl.FsFileFromFile;
import remi.distributedFS.db.impl.FsTableLocal;
import remi.distributedFS.db.impl.ObjectFactory;
import remi.distributedFS.util.ByteBuff;

public class FsChunkOneFile extends FsChunkFromFile {

	public static class StorageFactory implements ObjectFactory{

		@Override
		public FsChunkFromFile createChunk(FsTableLocal master, long sectorId, FsFileFromFile parent, long id) {
			return new FsChunkOneFile(master, sectorId, parent, id);
		}

		@Override
		public FsFileFromFile createFile(FsTableLocal master, long sectorId, FsDirectoryFromFile parent) {
			return new FsFileFromFile(master, sectorId, parent);
		}
		
	}

	public FsChunkOneFile(FsTableLocal master, long sectorId, FsFileFromFile parent, long id) {
		super(master, sectorId, parent, id);
	}

	@Override
	public boolean read(ByteBuff toAppend, int offset, int size) {
		ensureLoaded();
		ensureDatafield();
		if(!data.exists()){
			System.err.println("data '"+data.getPath()+"' doesn't exist!");
			return false;
		}

		//get our offset
		long myOffset = 0;
		for(FsChunk ch : parentFile.getChunks()){
			if(ch == this){
				break;
			}
			myOffset += ch.currentSize();
		}
		

		synchronized (this) {

			try(FileChannel dataChannel = FileChannel.open(data.toPath(), StandardOpenOption.READ)){
				
				System.out.println("pos before = "+toAppend.position()+", wanted to go "+size+" more. Size = "+toAppend.array().length+" == ms:"+this.getMaxSize()+" >= s:"+this.currentSize);
				ByteBuffer buff = toAppend.toByteBuffer();
				buff.limit(buff.position()+size);
//				buff.position(buff.position()); //already done in toByteBuffer()
				dataChannel.read(buff, offset+myOffset);

				toAppend.position(toAppend.position()+size);
				System.out.println("pos after = "+toAppend.position());
				lastaccess = System.currentTimeMillis();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		toAppend.position(toAppend.position()+size);
		return false;
		
	}

	@Override
	public boolean write(ByteBuff toWrite, int offset, int size) {
		ensureLoaded();
		ensureDatafield();
		ensureFileExist();
		
		//get our offset
		long myOffset = 0;
		for(FsChunk ch : parentFile.getChunks()){
			if(ch == this){
				break;
			}
			myOffset += ch.currentSize();
		}
		
		synchronized (this) {

			try(FileChannel dataChannel = FileChannel.open(data.toPath(), StandardOpenOption.WRITE)){
				
				ByteBuffer buff = toWrite.toByteBuffer();
				buff.limit(toWrite.position()+size);
//				buff.position(toWrite.position()); //already done in toByteBuffer()
				dataChannel.write(buff, offset+myOffset);
				currentSize = Math.max(currentSize, offset+size);

				System.out.println("toWrite.position = "+toWrite.position()+"+"+size+"=="+(toWrite.position()+size));
				toWrite.position(toWrite.position()+size);
				lastChange = System.currentTimeMillis();
				lastaccess = System.currentTimeMillis();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		toWrite.position(toWrite.position()+size);
		return false;
	}

	@Override
	protected void ensureDataPath(){
		if(data==null){
			data=new File(master.getRootRep()+parentFile.getPath());
		}
	}

	@Override
	protected void ensureDatafield(){
		if(data==null){
			//it never change & is unique (TODO: multiple dirs to not e with 1million files in the same dir)
			//create folder path
//			if(!data.exists()){
//				try {
//					//we now store all on our root folder, no need to check if the dir exist
//					//TODO: create more rep to not make too much files in the same rep.
//					//		for example, a rep for each 1000 sectorid.
//					//create file
//					data.createNewFile();
////					System.out.println("data '"+data.getPath()+"' is now created");
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
			if(isValid){
				ensureDataPath();
			}else{
				//request it (via network)
				System.out.println("REQUEST DATA FOR CHUNK "+getId());
				FsChunk meWithData = master.getManager().requestChunk(this.parentFile, this, serverIdPresent());
				if(meWithData == null){
					//can't find it!
					throw new UnreachableChunkException("Error: can't find the chunk "+this.getId()+" in the cluster. Maybe you should reconnect with more peers.");
				}
				this.currentSize = meWithData.currentSize();
				this.maxSize = meWithData.getMaxSize();
				//create file & copy data
//				if(!data.exists()){
//					try {
//						data.createNewFile();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
				
				//copy
				synchronized(this){
					isValid = true; //to not proc this method again
					//too complicated, TODO : useful?
//					ByteBuff buff = null;
//					if(currentSize<=1024*1024){
//						buff = new ByteBuff(currentSize);
//					}else{
//						int fact = currentSize / (1024*1024);
//						buff = new ByteBuff(currentSize / fact);
//					}
//					for(int i=0;i<meWithData.currentSize();i+=buff.limit()){
//						buff.rewind();
//						meWithData.read(buff, i*buff.limit(), buff.limit());
//						this.write(buff, i*buff.limit(), buff.limit());
//					}
					
					//simplier version
					ByteBuff buff = new ByteBuff(currentSize);
					meWithData.read(buff, 0, buff.limit());
					ensureDataPath();
					this.write(buff, 0, buff.limit());
					
				}
				//ready!
//				lastChange = meWithData.lastModificationTimestamp();
//				parent.setDirty(true);
//				parent.flush();
			}
		}
	}
	
	@Override
	protected void ensureFileExist() {
		if(!data.exists()){
			try {
				//create folder path
				
				File myRep = new File(master.getRootRep()+parentFile.getParent().getPath());
				if(!myRep.exists()){
//					//going to first good rep
//					LinkedList<File> repToCreate = new LinkedList<>();
//					File currentRep = myRep;
//					FsDirectory currentFsRep = parentFile.getParent();
//					while(!currentRep.exists()){
//						repToCreate.addFirst(currentRep);
//						currentRep = new File(master.getRootRep()+currentFsRep.getParent().getPath());
//						if(currentFsRep == currentFsRep.getParent()){
//							//we are at root => end it!
//							break;
//						}
//						currentFsRep = currentFsRep.getParent();
//					}
//					for(File f : repToCreate){
//						f.mkdir();
//					}
					//oups, there are something easier >o<
					myRep.mkdirs();
				}
				
				data.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	

}
