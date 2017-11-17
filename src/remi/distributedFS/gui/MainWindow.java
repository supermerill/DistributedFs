package remi.distributedFS.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import it.unimi.dsi.fastutil.shorts.ShortList;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.db.StorageManager;
import remi.distributedFS.db.impl.FsFileFromFile;
import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.net.ClusterManager;

public class MainWindow extends JFrame {
	private static final long serialVersionUID = 1L;

	//tabs;
	PanelPeers peers;
	PanelRequest req;
	JTabbedPane tabs;
	
	public MainWindow(FileSystemManager manager) {
		
		tabs = new JTabbedPane();
		
//		peers = new PanelPeers(manager);
//		tabs.addTab("network", peers);
		
		req = new PanelRequest();
		tabs.addTab("req", req);
		
		setLayout(new BorderLayout());
		add(tabs, BorderLayout.CENTER);
		
	}
	
	public static void main(String[] args) {
		
		MainWindow test = new MainWindow(new FileSystemManager() {
			
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
			public short getComputerId() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public FsChunk requestChunk(FsFileFromFile file, FsChunk chunk, ShortList serverIdPresent) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getDrivePath() {
				// TODO Auto-generated method stub
				return "";
			}

			@Override
			public String getRootFolder() {
				// TODO Auto-generated method stub
				return null;
			}
		});
		test.setSize(1000,700);
		test.setVisible(true);
	}
	
	
}
