package remi.distributedFS.fs;

import java.io.IOException;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.db.impl.FsFileFromFile;
import remi.distributedFS.db.impl.FsTableLocal;
import remi.distributedFS.fs.messages.ExchangeChunk;
import remi.distributedFS.fs.messages.PropagateChange;
import remi.distributedFS.net.ClusterManager;
import remi.distributedFS.net.impl.PhysicalServer;
import remi.distributedFS.os.JnrfuseImpl;

public class StandardManager implements FileSystemManager {
	
	StorageManager storage = null;

	ClusterManager net = null;

	private JnrfuseImpl os;

	PropagateChange algoPropagate = new PropagateChange(this);
	ExchangeChunk chunkRequester = new ExchangeChunk(this);
	
	char driveletter;
	
	public static void main(String[] args) throws IOException {
		//TODO: read config file
		StandardManager manager = new StandardManager();
		manager.init("./data1", "localdb.data", (char)'Q'
				, 17830);
		StandardManager manager2 = new StandardManager();
		manager2.init("./data2", "localdb2.data", (char)0//'R'
				, 17831);
		manager2.net.connect("localhost",17830);
	}
	
	public void init(String dataPath, String dataFile, char letter, int port) throws IOException{
		System.out.println("begin");
		storage = new FsTableLocal(dataPath, dataFile, this);

		if(port>0){
			net = new PhysicalServer(this, false);
			net.init(port);
			algoPropagate.register(this.net);
		}
		
		if(letter >0){
			os = new JnrfuseImpl(this);
			os.init(letter);
			driveletter = letter;
		}else driveletter = ' ';
		
		System.out.println("end of init");
		
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		net.launchUpdater();
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

	@Override
	public long getComputerId() {
		//TODO
		if(net == null) return 0;
//		return os.getContext().pid.longValue();
		return net.getId();
	}

	@Override
	public long getUserId() {
		if(os == null) return 0;
		return os.getContext().uid.longValue();
	}

	public char getLetter() {
		return driveletter;
	}

	@Override
	public long getGroupId() {
		if(os == null) return 0;
		return os.getContext().gid.longValue();
	}

	@Override
	public void requestDirUpdate() {
		algoPropagate.requestDirPath("/");
	}

	@Override
	public FsChunk requestChunk(FsFileFromFile file, int idx, List<Long> serverIdPresent) {
		System.out.println("REQUEST CHUNK");
		//request chunk to all servers
		chunkRequester.requestchunk(file.getPath(), file.getModifyDate(), idx);
		//register to the chunk requester
		boolean ok = false;
		FsChunk chunk = null;
		try{
//			while(!ok){
				chunk = chunkRequester.waitReceiveChunk(file.getPath(), file.getModifyDate(), idx);
//				if(chunk.)
				//TODO : check if it's our chunk
				if(chunk != null){
					ok = true;
				}else{
					System.out.println("Can't find chunk "+file.getPath()+" idx:"+idx);
				}
//			}
		}catch(RuntimeException ex){
			System.out.println("Can't find chunk "+file.getPath()+" idx:"+idx);
		}
		return chunk;
	}


}
