package remi.distributedFS.db;

import java.io.IOException;

import remi.distributedFS.datastruct.FsDirectory;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.*;

import remi.distributedFS.db.impl.FsObjectImplFromFile;
import remi.distributedFS.db.impl.FsTableLocal;

public class TestDb {
	FsTableLocal db;
	
	public static void main(String[] args) throws IOException {
		TestDb testeur = new TestDb();
		testeur.db = new FsTableLocal("./fs.data");
		
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
//		System.out.println("=========dir=====>\n ");((FsObjectImplFromFile)dir).print();
//		System.out.println("=========d1=====>\n ");((FsObjectImplFromFile)d1).print();
//		System.out.println("=========d2=====>\n ");((FsObjectImplFromFile)d2).print();
//		System.out.println("=========d21=====>\n ");((FsObjectImplFromFile)d21).print();
		
	}
	
	public FsDirectory getOrCreateDir(FsDirectory parent, String name){;

		FsDirectory d1 = getDir(db.getRoot(), name);
		if(d1==null){
			System.out.println("create "+name);
			d1 = db.getRoot().createSubDir(name);
		}
		System.out.println(name+" = "+d1);
		System.out.println(name+" = "+d1.getPath());
		return d1;
	}

}
