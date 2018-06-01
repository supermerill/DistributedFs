package remi.distributedFS.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import javafx.application.Application;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import it.unimi.dsi.fastutil.shorts.ShortList;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.db.impl.FsFileFromFile;
import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.net.ClusterManager;

public class MainWindow extends Application {

	// tabs;
	PanelPeers peers;
	PanelRequest req;
	TabPane tabs;

	FileSystemManager manager;

	@Override
	public void start(Stage primaryStage) throws Exception {

		tabs = new TabPane();

		// peers = new PanelPeers(manager);
		// tabs.addTab("network", peers);

		req = new PanelRequest();
		// tabs.getTabs().add(new Tab("req", req);

	}

	private void createFake() {

		manager = new FileSystemManager() {

			@Override
			public void requestDirUpdate() {
				// TODO Auto-generated method stub

			}

			@Override
			public void propagateChange(FsObject fic) {
				// TODO Auto-generated method stub

			}

			@Override
			public long getUserId() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public FsDirectory getRoot() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ClusterManager getNet() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public long getGroupId() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public StorageManager getDb() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public FsChunk requestChunk(FsFileFromFile file, FsChunk chunk, ShortList serverIdPresent) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getRootFolder() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public short getComputerId() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public String getDrivePath() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	public static void main(String[] args) {
		Application.launch(args);
	}

}
