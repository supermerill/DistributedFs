package remi.distributedFS.os;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jnr.ffi.Platform;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.*;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.datastruct.FsObjectImpl;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.db.impl.FsFileFromFile;
import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.net.ClusterManager;
import remi.distributedFS.util.ByteBuff;
import ru.serce.jnrfuse.FuseStubFS;

public class Testjnr {
	static class FsBout implements FsChunk{

		ByteBuff data;

		public FsBout(int currentSize, int maxSize) {
			data = new ByteBuff(maxSize);
			data.limit(currentSize);
		}

		@Override
		public int currentSize() {
			System.out.println("chunk size : "+data.limit()+" => '"+Charset.forName("UTF-8").decode(ByteBuffer.wrap(data.array()))+"'");
			return data.limit();
		}

		@Override
		public int maxSize() {
			return data.array().length;
		}

		@Override
		public boolean read(ByteBuff toAppend, int offset, int size) {
			if(offset<0 || size<1 || offset+size >currentSize()){
				return false;
			}
//			toAppend.ensureFreeSpace(size);
//			System.arraycopy(data.array(), offset, toAppend.array(), toAppend.position(), size);
			data.position(offset);
			toAppend.put(data, size);
			return true;
		}

		@Override
		public boolean write(ByteBuff toWrite, int offset, int size) {
			if(offset<0 || size<1 || offset+size >maxSize()){
				return false;
			}
			data.position(offset);
			data.put(toWrite, size);
			return true;
		}

		@Override
		public boolean isPresent() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public List<Long> serverIdPresent() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long lastModificationTimestamp() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public long lastModificationUID() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setPresent(boolean isPresentLocally) {
			// TODO Auto-generated method stub
			
		}
		
	}
	static class FsFic extends FsObjectImpl implements FsFile{

		int currentChunkSize = 0;
		List<FsBout> chunks = new ArrayList<>();
		
		FsFic(String name){
			super.name = name;
			super.PUGA = 0x0FF;
			this.id = hashCode();
		}

		@Override
		public int getNbChunks() { return chunks.size(); }

		@Override
		public List<FsChunk> getChunks() { return new ArrayList<>(chunks); }

		@Override
		public int getChunkSize() { return currentChunkSize; }

		@Override
		public void rearangeChunks(int newSizeChunk, int newNbChunks) {
			currentChunkSize = newSizeChunk;
			//create chunks
			List<FsBout> newchunks = new ArrayList<>();
			for(int i=0;i<newNbChunks-1;i++){
				newchunks.add(new FsBout(newSizeChunk,newSizeChunk));
			}
			newchunks.add(new FsBout(0,newSizeChunk));
			
			this.chunks = newchunks;
		}
		@Override
		public void accept(FsObjectVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public long getSize() {
			return FsFile.FsFileMethods.getSize(this);
		}

		@Override
		public void flush() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void truncate(long size) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setId() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setId(long newId) {
			// TODO Auto-generated method stub
			
		}
	}
	
	static class FsDir extends FsObjectImpl implements FsDirectory{
		List<FsDirectory> dirs = new ArrayList<>();
		List<FsFile> files = new ArrayList<>();
		
		FsDir(String name){
			super();
			super.name = name;
			super.PUGA = 0x1FF;
		}
		
		public FsDir() {
			this("");
			parent = this;
		}

		@Override
		public List<FsFile> getFiles() { return files; }
		
		@Override
		public List<FsDirectory> getDirs() { return dirs; }
		
		@Override
		public FsFile createSubFile(String name) {
			FsFile newone = new FsFic(name);
			files.add(newone);
			return newone;
		}
		
		@Override
		public FsDirectory createSubDir(String name) {
			FsDir newone = new FsDir(name);
			dirs.add(newone);
			return newone;
		}

		@Override
		public void accept(FsObjectVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public void flush() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public List<FsObject> getDelete() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setId() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setId(long newId) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeFile(FsFile obj) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeDir(FsDirectory obj) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void moveFile(FsFile obj, FsDirectory newDir) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void moveDir(FsDirectory obj, FsDirectory newDir) {
			// TODO Auto-generated method stub
			
		}
	}
	
	public static void main(String[] args) {
		JnrfuseImpl myFuseManager = new JnrfuseImpl();

		final FsDir root = new FsDir();
		root.setUserId(myFuseManager.getContext().uid.get());
		root.setGroupId(myFuseManager.getContext().gid.get());
		root.createSubDir("dir1");
		getDir(root, "dir1").setUserId(myFuseManager.getContext().uid.get());
		getDir(root, "dir1").setGroupId(myFuseManager.getContext().gid.get());
		root.createSubDir("dir2");
		getDir(root, "dir2").setUserId(myFuseManager.getContext().uid.get());
		getDir(root, "dir2").setGroupId(myFuseManager.getContext().gid.get());
		getDir(root, "dir2").createSubDir("dir21");
		FsFic f = (FsFic) getDir(root, "dir1").createSubFile("fic1");
		f.setUserId(myFuseManager.getContext().uid.get());
		f.setGroupId(myFuseManager.getContext().gid.get());
		f.rearangeChunks(10, 1);
		String str = "Hello world!";
		f.chunks.get(0).data.put( Arrays.copyOf(Charset.forName("UTF-8").encode(str).array(),str.length()));
//		System.out.println("Create a file with "+Charset.forName("UTF-8").encode("Hello world!").array().length+"bytes : "+(int)(Charset.forName("UTF-8").encode("Hello world!").array()[12]));
		
		
		FuseStubFS memfs = myFuseManager;
		myFuseManager.manager = new FileSystemManager() {
			
			@Override
			public void updateFile(long dirId, byte[] datas) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void updateDirectory(long dirId, byte[] datas) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void updateChunk(long dirId, byte[] datas) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public FsDirectory getRoot() {
				return root;
			}

			@Override
			public void propagateChange(FsObject fic) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public StorageManager getDb() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ClusterManager getNet() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public short getComputerId() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public long getUserId() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public long getGroupId() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public void requestDirUpdate() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public FsChunk requestChunk(FsFileFromFile parent, int idx, List<Long> serverIdPresent) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public char getLetter() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public String getRootFolder() {
				// TODO Auto-generated method stub
				return null;
			}
		};
//		memfs = new MyMemoryFs2();
	    try {
		    try {
		        String path;
		        switch (Platform.getNativePlatform().getOS()) {
		            case WINDOWS:
		                path = "Y:\\";
		                break;
		            default:
		                path = "/tmp/mntm";
		        }
		        System.out.println("prepare to mount into "+path);
			    try {
			    	memfs.mount(Paths.get(path), true, false);
			    }catch(Exception e1){
			    	e1.printStackTrace();
			    	System.out.println(e1.getLocalizedMessage());
			    	throw new RuntimeException(e1);
			    }
		        System.out.println("mounted "+path);
		        Thread.sleep(100);
		    }catch(Throwable e2){
		    	e2.printStackTrace();
		    	System.out.println(e2.getLocalizedMessage());
		    } finally {
		    	memfs.umount();
		    }
	    }catch(Throwable e3){
	    	e3.printStackTrace();
	    }
	}
}

