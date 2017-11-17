package remi.distributedFS.fs.bigdata;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;

import remi.distributedFS.db.impl.FsTableLocal;
import remi.distributedFS.db.impl.FsTableLocal.FsTableLocalFactory;
import remi.distributedFS.db.impl.bigdata.FsChunkStreamable;
import remi.distributedFS.db.impl.bigdata.FsChunkStreaming;
import remi.distributedFS.fs.Cleaner;
import remi.distributedFS.fs.StandardManager;
import remi.distributedFS.fs.messages.PropagateChange;
import remi.distributedFS.net.impl.PhysicalServer;
import remi.distributedFS.util.ByteBuff;
import securitytools.sandbox.PermissionsSecurityManager;
import securitytools.sandbox.Sandbox;
import securitytools.sandbox.UntrustedAction;

public class DistributedBigDataClientMananger extends StandardManager {
	
	BigDataExchange bigdataExchange = new BigDataExchange(this); 

	public void initBdNet(String dataPath, int port) {
		try{
			rootFolder = dataPath;
			
			//check if folder exist
			if(!new File(rootFolder).exists()){
				new File(rootFolder).mkdirs();
			}
			
			System.out.println("begin");
			FsTableLocalFactory storageFactory = new FsTableLocal.FsTableLocalFactory();
			storageFactory.rootRep = dataPath;
			storageFactory.filename = dataPath+"/"+"localdb.data";
			storageFactory.manager = this;
			storageFactory.factory = new FsChunkStreaming.StorageFactory(); // <======= the change is here
			storage = storageFactory.create();
			storage.cleanUnusedSectors(true);
	
			if(port>0){
				net = new PhysicalServer(this, false, dataPath);
				net.init(port);
				algoPropagate.register(this.net);
				chunkRequester.register(this.net);
				bigdataExchange.register(this.net); // <======= a change is here
				net.connect();
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
	
	PropagateChange getDirFileExchange() {
		return super.algoPropagate;
	}

	public void map(FsChunkStreamable fileIn, String jarName, String objectName, FsChunkStreamable fileOut, ByteBuff funcInitData) {
		new Thread() {
			public void run() {
				try {
					//get jar
					File jarFile = new File("./jar/"+jarName);
					if(!jarFile.exists()) throw new RuntimeException("Error, now jar "+jarName);
					
					URLClassLoader child = new URLClassLoader (new URL[] {jarFile.toURI().toURL()}, this.getClass().getClassLoader());
					Class classToLoad = Class.forName (objectName, true, child);
					if(classToLoad==null) {
						System.err.println("Error, class "+objectName+" isn't findable insinde the jar");
						return;
					}
					if(!classToLoad.isInstance(MapAlgo.class)) {
						System.err.println("Error, class "+objectName+" is not an instance of MapAlgo");
						return;
					}
					Method method = classToLoad.getDeclaredMethod ("mapAlgo", ByteBuff.class, ByteBuff.class);
					if(method==null) {
						System.err.println("Error, class "+objectName+" cant' find function mapAlgo(bytebuff, bytebuff)");
						return;
					}
	//				Object instance = classToLoad.newInstance ();
	//				Object result = method.invoke (instance);
					Thread.currentThread().setContextClassLoader(child);

					try {
						MapAlgo algo = (MapAlgo)classToLoad.newInstance();
						algo.init(funcInitData);
						ByteBuff dataOut = new ByteBuff();
						fileOut.setCurrentSize(0);

						Iterator<ByteBuff> itIn = fileIn.iterator();
						while(itIn.hasNext()) {
							dataOut.reset();
							algo.mapAlgo(itIn.next(), dataOut);
							fileOut.append(dataOut);
						}
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
					
					//compiqué si on lit dedans en meme temps...
					// on va plutot blinder la possibilité de se connecter au cluster
//					Sandbox.doUntrusted(new UntrustedAction<Void>(new PermissionsSecurityManager()) {
//					    protected Void run() {
//					        // [untrusted code goes here]
//					        // this code will have absolutely no permissions
//					        // to do any potentially unsafe actions
//					    	
//					    	
//					    	
//					    	
//					        return null;
//					    }
//					});
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void sort(FsChunkStreamable fileIn, String jarName, String funcName, FsChunkStreamable fileOut, ByteBuff funcInitData) {
		// TODO Auto-generated method stub
		System.err.println("SORT : TODO");
	}

	public void merge(FsChunkStreamable fileIn1, FsChunkStreamable jarName, String fileIn2, String funcName, FsChunkStreamable fileOut, ByteBuff funcInitData) {
		// TODO Auto-generated method stub
		System.err.println("MERGE : TODO");
	}

	public void split(FsChunkStreamable fileIn, String jarName, String funcName, ByteBuff funcInitData) {
		// TODO Auto-generated method stub
		System.err.println("SPLIT : TODO");
		
	}

}
