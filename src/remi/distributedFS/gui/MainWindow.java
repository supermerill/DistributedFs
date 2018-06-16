package remi.distributedFS.gui;

import java.io.File;
import java.util.Optional;

import it.unimi.dsi.fastutil.shorts.ShortList;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.db.impl.FsFileFromFile;
import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.fs.StandardManager;
import remi.distributedFS.log.Logs;
import remi.distributedFS.net.ClusterManager;

public class MainWindow extends Application {

	Stage myFrame;
	Scene mainScene;
	// tabs;
	PanelPeers peers;
	PanelRequest req;
	TabPane tabs;

	StandardManager manager;

	@Override
	public void start(Stage frame) throws Exception {


		tabs = new TabPane();

		req = new PanelRequest();
		Tab tab = new Tab();
        tab.setText("Requests");
        tab.setContent(req);
        tabs.getTabs().add(tab);

        peers = new PanelPeers();
        tab = new Tab();
        tab.setText("Network");
        tab.setContent(peers);
        tabs.getTabs().add(tab);

		mainScene = new Scene(tabs);

		myFrame = frame;
		frame.setOnCloseRequest(e->{
			manager.close();
			System.exit(0);
		});
		myFrame.setScene(mainScene);
		
		frame.setTitle("Virtual Distributed Hard drive instance");
		frame.setWidth(900);
		frame.setHeight(900);
		frame.centerOnScreen();
		frame.show();
		
		
		initProcesses();
	}
	
	protected void initProcesses() {
		

		//get config
		File mainDir = new File(".");
		//standardMananger properties
		remi.distributedFS.fs.Parameters paramsMana = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/standardManager.properties");
		remi.distributedFS.fs.Parameters paramsNet = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/network.properties");
		//cleaner.properties
//		remi.distributedFS.fs.Parameters paramsClean = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/cleaner.properties");
		//current net instance repo

		//TODO: read config file
			Logs.logGui.info("====================start client ====================");

			String drivePath = paramsMana.get("DriveLetter");
			int port = paramsMana.getInt("ListenPort");
				StandardManager manager = new StandardManager();
				Logs.logGui.info("INIT BD & NET");
				manager.initBdNet(mainDir.getAbsolutePath(), port);
				Logs.logGui.info("CONNECT");
				int nbConnection = manager.getNet().connect();
				
				boolean iAmTheMaster =false;
				if(nbConnection < 2) {
					//wait to be sure you are really connected
					Logs.logGui.info("WAIT CONNECTION");
					try {
						do{
							Thread.sleep(10000);
						}while(manager.getNet().isConnecting());
						//TODO: loop while connection has not failed.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					nbConnection = manager.getNet().getNbPeers();
					Logs.logGui.info("nb connected peers = "+nbConnection);
					boolean waitAbit = true;
					do{
						Alert alert = new Alert(AlertType.CONFIRMATION);
						alert.setTitle("Can't connect to cluster");
						alert.setHeaderText("The cluster seems unreacheable.");
						alert.setContentText("Wait a bit more ?");
						Optional<ButtonType> ret = alert.showAndWait();
						if(ret.get().getButtonData().isDefaultButton()) {
							waitAbit =true;
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
							}
						}else {
							waitAbit = false;
						}
						nbConnection = manager.getNet().getNbPeers();
					}while(nbConnection == 0 && waitAbit);
					
					if(nbConnection == 0) {
						Logs.logGui.info("can't connect, create my new server ? " + !paramsNet.getBoolOrDef("FirstConnection", false));
						//start a new net cluster
						//TODO: if "connect to existing cluter" is checked, please do not create a new cluster without user permission
						if(paramsNet.getBoolOrDef("FirstConnection", false)) {
							Alert alert = new Alert(AlertType.CONFIRMATION);
							alert.setTitle("Can't connect to cluster");
							alert.setHeaderText("The cluster seems unreacheable.");
							alert.setContentText("Do we abandon to connect to the cluster and create our own \n(click cancel if you want to try again alter) ?");
							Optional<ButtonType> ret = alert.showAndWait();
							if(ret.get().getButtonData().isDefaultButton()) {
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
					Logs.logGui.info("MANY connection");
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
				
				Logs.logGui.info("INIT OS");
				manager.initOs("./data"+drivePath, drivePath);

				Logs.logGui.info("READY!");
				
				this.manager = manager;
				this.peers.startListen(manager);
				this.req.init(manager);

	}


	public static void main(String[] args) {
		Application.launch(args);
	}

}
