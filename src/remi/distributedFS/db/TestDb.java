package remi.distributedFS.db;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;

import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.*;

import remi.distributedFS.db.impl.FsFileFromFile;
import remi.distributedFS.db.impl.FsObjectImplFromFile;
import remi.distributedFS.db.impl.FsTableLocal;
import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.net.ClusterManager;
import remi.distributedFS.util.ByteBuff;

public class TestDb {
	FsTableLocal db;
	
	public static void main(String[] args) throws IOException {
		TestDb testeur = new TestDb();
		FileSystemManager manager = new FileSystemManager() {
			
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
			public void propagateChange(FsObject fic) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public long getUserId() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public FsDirectory getRoot() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public ClusterManager getNet() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public long getGroupId() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public StorageManager getDb() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public long getComputerId() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public void requestDirUpdate() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public FsChunk requestChunk(FsFileFromFile parent, int idx,  List<Long> serverIdPresent) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public char getLetter() {
				// TODO Auto-generated method stub
				return 0;
			}
		};
		testeur.db = new FsTableLocal(".", "fs.data", manager);
		testeur.test();
		
	}
	
	public void test(){

		FsDirectory dir = db.getRoot();

		System.out.println("dir root = "+dir);
		System.out.println("dir root = "+dir.getPath());
		

		FsDirectory d1 = getOrCreateDir(dir, "dir1");
		FsDirectory d2 = getOrCreateDir(dir, "dir2");
		FsDirectory d21 = getOrCreateDir(d2, "dir21");
		d2.flush();
		d1.flush();
		d21.flush();
		dir.flush();
		FsFile f21 = getOrCreateFile(d2, "fic21.txt");
		f21.flush();
		d2.flush();

    	ByteBuff buff = new ByteBuff(1024);
    	buff.limit((int)f21.getSize());
    	FsFile.FsFileMethods.read(f21, buff, 0);
    	buff.flip();
    	System.out.println(Charset.forName("UTF-8").decode(buff.toByteBuffer()));
//		System.out.println("=========dir=====>\n ");((FsObjectImplFromFile)dir).print();
//		System.out.println("=========d1=====>\n ");((FsObjectImplFromFile)d1).print();
//		System.out.println("=========d2=====>\n ");((FsObjectImplFromFile)d2).print();
//		System.out.println("=========d21=====>\n ");((FsObjectImplFromFile)d21).print();
		
	}
	
	public FsDirectory getOrCreateDir(FsDirectory parent, String name){;

		FsDirectory d1 = getDir(parent, name);
		if(d1==null){
			System.out.println("create "+name);
			d1 = parent.createSubDir(name);
		}
		System.out.println(name+" = "+d1);
		System.out.println(name+" = "+d1.getPath());
		return d1;
	}
	
	public FsFile getOrCreateFile(FsDirectory parent, String name){

		FsFile d1 = getFile(parent, name);
		if(d1==null){
			System.out.println("create "+name);
			d1 = parent.createSubFile(name);
			d1.rearangeChunks(128, 1);
		}
		System.out.println(name+" = "+d1);
		System.out.println(name+" = "+d1.getPath());
		return d1;
	}

}
