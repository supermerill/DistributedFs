package remi.distributedFS.db;

import remi.distributedFS.datastruct.FsDirectory;

public interface StorageManager {
	
	//unused
//	void writeDir(long id, byte[] newdata);
//	void writeFile(long id, byte[] newdata);
//	void writeChunk(long id, byte[] newdata);
//
//	byte[] readDir(long id);
//	byte[] readFile(long id);
//	byte[] readChunk(long id);
	
	//used
	FsDirectory getRoot();

}
