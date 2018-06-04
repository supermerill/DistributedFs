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
import remi.distributedFS.net.ClusterManager;

public class MainWindow extends Application {

	Stage myFrame;
	Scene mainScene;
	// tabs;
	PanelPeers peers;
	PanelRequest req;
	TabPane tabs;

	FileSystemManager manager;

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
		frame.setOnCloseRequest(e->System.exit(0));
		myFrame.setScene(mainScene);
		
		frame.setTitle("Install & configure a distributed hard drive instance");
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
			System.out.println("====================start client ====================");

			String drivePath = paramsMana.get("DriveLetter");
			int port = paramsMana.getInt("ListenPort");
				StandardManager manager = new StandardManager();
				System.out.println("INIT BD & NET");
				manager.initBdNet(mainDir.getAbsolutePath(), port);
				System.out.println("CONNECT");
				int nbConnection = manager.getNet().connect();
				
				if(nbConnection < 2) {
					//wait to be sure you are really connected
					System.out.println("WAIT CONNECTION");
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					nbConnection = manager.getNet().getNbPeers();
					System.out.println("nb connected peers = "+nbConnection);
					if(nbConnection == 0) {
						System.out.println("can't connect, create my new server ? " + !paramsNet.getBoolOrDef("FirstConnection", false));
						//start a new net cluster
						//TODO: if "connect to existing cluter" is checked, please do not create a new cluster without user permission
						if(paramsNet.getBoolOrDef("FirstConnection", false)) {
							Alert alert = new Alert(AlertType.CONFIRMATION);
							alert.setTitle("Can't connect to cluster");
							alert.setHeaderText("The cluster seems unreacheable.");
							alert.setContentText("Do we abandon to connect to the cluster and create our own (click cancel if you want to try again alter) ?");
							Optional<ButtonType> ret = alert.showAndWait();
							if(ret.get().getButtonData().isDefaultButton()) {
								manager.getNet().initializeNewCluster();
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
						}
					}
				}else{
					System.out.println("MANY connection");
					//update later
					nbConnection = manager.getNet().getNbPeers();
				}

				System.out.println("INIT OS");
				manager.initOs("./data"+drivePath, drivePath);

				System.out.println("READY!");
				
				this.manager = manager;
				this.peers.startListen(manager);
				this.req.init(manager);

	}


	public static void main(String[] args) {
		Application.launch(args);
	}

}
