package remi.distributedFS.fs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
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
	String rootFolder = ".";
	
	Cleaner cleaner;
	
	public static void main(String[] args) throws IOException {
		//TODO: read config file
		System.out.println("args.length="+args.length);
		if(args.length>1){
			System.out.println("====================start client ====================");

			new Thread(()->{
				StandardManager manager = new StandardManager();
				manager.initBdNet("./data"+args[0], Integer.parseInt(args[1]));
				if(args.length<3){
					manager.initializeNewCluster();
				}

				if(args.length>2){
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						manager.net.connect("localhost",Integer.parseInt(args[2]));
				}
				
				manager.initOs("./data"+args[0], args[0].charAt(0));

			}).start();
		}
		
		
//		System.out.println("====================start second client ====================");
//		new Thread(()->{
//			StandardManager manager2 = new StandardManager();
//			
//			manager2.initBdNet("./data2", 17831);
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			manager2.net.connect("localhost",17830);
//
//			manager2.initOs("./data2", (char)0//'R'
//					);
//		}).start();
	}
	
	
	public StandardManager() {
		super();
	}




	public void initializeNewCluster() {
		//check if we havn't any id
		if(getComputerId() < 0){
			
			//choose id
			net.initializeNewCluster();
			
		}
		
	}

	public void initBdNet(String dataPath, int port) {
		try{
			rootFolder = dataPath;
			
			//check if folder exist
			if(!new File(rootFolder).exists()){
				new File(rootFolder).mkdirs();
			}
			
			System.out.println("begin");
			storage = new FsTableLocal(dataPath, dataPath+"/"+"localdb.data", this, true);
	
			if(port>0){
				net = new PhysicalServer(this, false, dataPath);
				net.init(port);
				algoPropagate.register(this.net);
				chunkRequester.register(this.net);
			}
			

			//TODO: serialize & gui
//			cleaner = new Cleaner(this, 1024*1024*256, 1024*1024*1024, 1000*60);
			cleaner = new Cleaner(this);
			cleaner.start();
			
		}catch (Exception e) {
			e.printStackTrace();
			close();
		}
	}


	public void initOs(String dataPath, char letter) {
		try{
	
			//wait initialization before giving hand to men
			try {
				while(getComputerId()<0){
					System.err.println("Warn : waiting for connection");
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		}catch (Exception e) {
			e.printStackTrace();
			close();
		}
	}

	private void close() {
		// TODO Auto-generated method stub
		
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
	public short getComputerId() {
		//TODO
		if(net == null) return -1;
//		return os.getContext().pid.longValue();
		return net.getComputerId();
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
		algoPropagate.requestDirPath("/",-1);
	}

	@Override
	public FsChunk requestChunk(FsFileFromFile file, FsChunk chunk, List<Long> serverIdPresent) {
		System.out.println("REQUEST CHUNK "+chunk.getId());
		//request chunk to all servers
		int nbReq = chunkRequester.requestchunk(file, chunk);
		//register to the chunk requester
		boolean ok = false;
		FsChunk chunkReceived = null;
		try{
//			while(!ok){
			chunkReceived = chunkRequester.waitReceiveChunk(chunk.getId(), file.getId(), file.getModifyDate(), nbReq);
//				if(chunk.)
				//TODO : check if it's our chunk
				if(chunkReceived != null){
					ok = true;
				}else{
					System.out.println("Can't find chunk(1) "+file.getPath()+" id:"+chunk.getId());
				}
//			}
		}catch(RuntimeException ex){
			ex.printStackTrace();
			System.out.println("Can't find chunk(2) "+file.getPath()+" id:"+chunk.getId());
		}
		return chunkReceived;
	}

	@Override
	public String getRootFolder() {
		return rootFolder;
	}


}
