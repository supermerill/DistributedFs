package remi.distributedFS.fs;

import it.unimi.dsi.fastutil.shorts.ShortList;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.db.impl.FsFileFromFile;
import remi.distributedFS.net.ClusterManager;

public interface FileSystemManager {
	
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
	public FsChunk requestChunk(FsFileFromFile file, FsChunk chunk, ShortList serverIdPresent);
	
	
	
	public String getDrivePath();
	public String getRootFolder();

}
