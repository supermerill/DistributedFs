package remi.distributedFS.fs;

import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.db.StorageManager;
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

	public long getComputerId();
	public long getUserId();
	public long getGroupId();
	
	public void requestDirUpdate();

}
