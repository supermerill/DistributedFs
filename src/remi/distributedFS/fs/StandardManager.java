package remi.distributedFS.fs;

import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.fs.messages.PropagateChange;
import remi.distributedFS.net.ClusterManager;
import remi.distributedFS.os.JnrfuseImpl;

public class StandardManager implements FileSystemManager {
	
	StorageManager storage = null;

	ClusterManager net = null;

	private JnrfuseImpl os;

	PropagateChange algoPropagate = new PropagateChange(this);
	
	public static void main(String[] args) {
		//TODO: read config file
		StandardManager manager = new StandardManager();
		
		manager.storage = null;

		manager.net = null;
		
		manager.os = new JnrfuseImpl(manager);
		
		manager.algoPropagate.register(manager.net);
	}

	@Override
	public void propagateChange(FsObject obj) {
		obj.accept(algoPropagate);
	}

	@Override
	public StorageManager getDb() {
		return storage;
	}

	@Override
	public void updateDirectory(long dirId, byte[] datas) {
	}

	@Override
	public void updateFile(long dirId, byte[] datas) {
	}

	@Override
	public void updateChunk(long dirId, byte[] datas) {
	}

	@Override
	public FsDirectory getRoot() {
		return storage.getRoot();
	}

	@Override
	public ClusterManager getNet() {
		return net;
	}


}
