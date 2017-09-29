package remi.distributedFS.fs;

import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.db.impl.FsFileFromFile;
import remi.distributedFS.net.ClusterManager;

public interface FileSystemManager {

	//not used
	public void updateDirectory(long dirId, byte[] datas); //TODO
	public void updateFile(long dirId, byte[] datas); //TODO
	public void updateChunk(long dirId, byte[] datas); //TODO
	
	//used by fs
	FsDirectory getRoot();
	public void propagateChange(FsObject fic);
	
	
	//not used
	public StorageManager getDb();
	public ClusterManager getNet();

	public short getComputerId();
	public long getUserId();
	public long getGroupId();
	
	public void requestDirUpdate();
	public FsChunk requestChunk(FsFileFromFile file, FsChunk chunk, List<Long> serverIdPresent);
	
	
	
	public char getLetter();
	public String getRootFolder();

}
