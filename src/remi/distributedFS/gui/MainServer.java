package remi.distributedFS.gui;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import remi.distributedFS.fs.StandardManager;
import remi.distributedFS.net.impl.Peer;
import remi.distributedFS.net.impl.PhysicalServer;

public class MainServer {


	public static void main(String[] args) throws InterruptedException {
		MainServer serv = new MainServer();
		
		while(true) {
			Thread.sleep(60000);
			//write peers
			System.out.println("========== peers ============");
			for(Peer p : ((PhysicalServer)(serv.manager.getNet())).getPeers()) {
				System.out.println(
						p.getIP()+" : "+p.getPort()
						+"\t"+p.getComputerId()
						+"\t"+p.getPeerId()
						+"\t"+p.getState()
						+"\t"+(p.isAlive()?"alive":"dead"));
			}
			
		}
	}
	
	private StandardManager manager;
	
	public MainServer() {

		//get config
		File mainDir = new File(".");
		//standardMananger properties
		remi.distributedFS.fs.Parameters paramsMana = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/standardManager.properties");
		remi.distributedFS.fs.Parameters paramsNet = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/network.properties");
		//cleaner.properties
//		remi.distributedFS.fs.Parameters paramsClean = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/cleaner.properties");
		//current net instance repo

		//TODO: read config file
			System.out.println("====================start client ====================");

			String drivePath = paramsMana.get("DriveLetter");
			int port = paramsMana.getInt("ListenPort");
				StandardManager manager = new StandardManager();
				System.out.println("INIT BD & NET");
				manager.initBdNet(mainDir.getAbsolutePath(), port);
				System.out.println("CONNECT");
				int nbConnection = manager.getNet().connect();
				
				boolean iAmTheMaster =false;
				if(nbConnection < 2) {
					//wait to be sure you are really connected
					System.out.println("WAIT CONNECTION");
					try {
						do{
							Thread.sleep(3000);
						}while(manager.getNet().isConnecting());
						//TODO: loop while connection has not failed.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					nbConnection = manager.getNet().getNbPeers();
					while(nbConnection <= 0) {
						System.out.println("Can't connect to server, should we wait? (y/n): ");
						int resp;
						try {
							resp = System.in.read();
							nbConnection = manager.getNet().getNbPeers();
							if(resp == 'y') Thread.sleep(10000);
							else break;
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}
					System.out.println("nb connected peers = "+nbConnection);
					if(nbConnection == 0) {
						
						System.out.println("can't connect, create my new server ? " + !paramsNet.getBoolOrDef("FirstConnection", false));
						//start a new net cluster
						//TODO: if "connect to existing cluter" is checked, please do not create a new cluster without user permission
						if(paramsNet.getBoolOrDef("FirstConnection", false)) {
							System.out.println("Do we abandon to connect to the cluster and create our own? (y/n): ");
							int resp = 'n';
							try {
								resp = System.in.read();
							} catch (IOException e1) {
							}
							
							if(resp == 'y') {
								manager.getNet().initializeNewCluster();
								iAmTheMaster = true;
								paramsNet.setBool("FirstConnection", false);
							}else {
								paramsNet.setBool("FirstConnection", true);
								//close open threads & pipes
								manager.close();
								//remove nealy create files
								for(File f : new File(mainDir.getAbsolutePath()).listFiles()) {
									try {
										if(!f.getName().endsWith(".properties")){
											f.delete();
										}
									}catch(Exception e) {}
								}
								//exit
								System.exit(0);
							}
						}else {
							manager.getNet().initializeNewCluster();
							iAmTheMaster = true;
						}
					}
				}else{
					System.out.println("MANY connection");
					//update later
					nbConnection = manager.getNet().getNbPeers();
				}

				//ask the whole fstab
				if(!iAmTheMaster){
					manager.requestDirUpdate();
					//wait to receive the root
//					manager.getDb().getRoot().
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				System.out.println("INIT OS");
				manager.initOs("./data"+drivePath, drivePath);

				System.out.println("READY!");
				
				this.manager = manager;

	}

}
