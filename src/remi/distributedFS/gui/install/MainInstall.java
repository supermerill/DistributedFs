package remi.distributedFS.gui.install;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.util.Duration;
import remi.distributedFS.log.Logs;


public class MainInstall extends Application {

	InstallPanel currentPanel;
	public final Map<String, Object> savedData = new HashMap<>();

	Stage myFrame;

	static {
		//change behavior of tooltip : show for 30s instead of 5s
		try {
			
			Class<?> TooltipBehaviorClass = Class.forName("javafx.scene.control.Tooltip$TooltipBehavior");
			Constructor<?> cons_TB = TooltipBehaviorClass.getDeclaredConstructor(Duration.class, Duration.class, Duration.class, Boolean.TYPE);
			cons_TB.setAccessible(true);
			Object tb = cons_TB.newInstance(new Duration(1000), new Duration(30000), new Duration(200), false);
			
			Field f_BEHAVIOR = Tooltip.class.getDeclaredField("BEHAVIOR");
			f_BEHAVIOR.setAccessible(true);
			f_BEHAVIOR.set(null, tb);
			
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException | ClassNotFoundException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void start(Stage frame) throws Exception {
		myFrame = frame;
		frame.setOnCloseRequest(e->System.exit(0));
		// create the first panel
		goToPanel(new PanelChoiceNewConnect());
		frame.setTitle("Install & configure a distributed hard drive instance");
		frame.setWidth(900);
		frame.setHeight(900);
		frame.centerOnScreen();
		frame.show();
	}
	
	
	public void goToPanel(InstallPanel nextPanel) {
		Logs.logGui.info("next panel : "+currentPanel);
		if(currentPanel != null) {
//			Logs.logGui.info("destroy "+currentPanel);
			currentPanel.destroy();
//			this.remove(currentPanel);
		}
//		currentPanel = nextPanel;
//		Logs.logGui.info("add nextPanel "+nextPanel);
//		currentPanel.init(this);
//		this.add(currentPanel, BorderLayout.CENTER);
//		currentPanel.construct();
//		currentPanel.invalidate();
//		this.invalidate();
//		this.revalidate();
//		this.repaint();
		nextPanel.init(this);
		myFrame.setScene(nextPanel);
		currentPanel = nextPanel;
	}
	
	public static void main(String[] args) {
		Application.launch(args);
	}
	


	public void finish() {
		if(currentPanel != null) {
			Logs.logGui.info("destroy "+currentPanel);
			currentPanel.destroy();
		}
		
		File mainDir = new File(savedData.get("InstallPath").toString());//+"/Data"+savedData.get("DrivePath").toString());
		mainDir.mkdirs();
		Logs.logGui.info(savedData.get("InstallPath").toString());
		Logs.logGui.info(savedData.toString());
		
		// create parameter files
		
		// standardManager.properties
		remi.distributedFS.fs.Parameters paramsMana = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/standardManager.properties");
		paramsMana.setString("DriveLetter", savedData.get("DrivePath").toString());
		paramsMana.setInt("ListenPort", Integer.parseInt(savedData.get("ListenPort").toString()));
		paramsMana.setString("StorageType", ((Boolean)savedData.get("PlainFileOnly"))?
				"remi.distributedFS.db.impl.readable.FsChunkOneFile.StorageFactory":
					"remi.distributedFS.db.impl.ObjectFactory.StandardFactory");
		paramsMana.setString("MainDir", mainDir.getAbsolutePath());
		paramsMana.setString("AlgoPropagate", savedData.get("AlgoPropagate").toString());
		
		// cleaner.properties
		remi.distributedFS.fs.Parameters paramsClean = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/cleaner.properties");
		paramsClean.setLong("MaxSizeKB", 1000 * Integer.parseInt(savedData.get("SizeMax").toString()));
		
		paramsClean.setBool("CanElage", (Boolean)savedData.get("CanElage"));
		paramsClean.setInt("MinKnownDuplicate", ((Boolean)savedData.get("CanElageAggressively"))?1:2);
		paramsClean.setLong("IdealSizeKB", 1000 * Integer.parseInt(savedData.get("SizeIdeal").toString()));
		
		paramsClean.setBool("CanDelete", !(Boolean)savedData.get("NoDelete"));
		paramsClean.setLong("SecTimeBeforeDelete", Integer.parseInt(savedData.get("TimeDelFic").toString()));
		
		// network parameters (stored in "clear" only before it start and then it can erase this file)
		remi.distributedFS.fs.Parameters paramsNet = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/network.properties");
		paramsNet.setLong("ClusterId", Math.abs(savedData.get("ClusterId").hashCode()));
		paramsNet.setString("ClusterPassphrase", savedData.get("ClusterPwd").toString());
		
		if(savedData.containsKey("ClusterIpPort") && savedData.containsKey("ClusterChoice") && savedData.get("ClusterChoice").equals("connect")) {
			paramsNet.setString("PeerIp", savedData.get("ClusterIpPort").toString().split(":")[0]);
			paramsNet.setLong("PeerPort", Long.parseLong(savedData.get("ClusterIpPort").toString().split(":")[1]));
			paramsNet.setBool("FirstConnection", true);
		}
		
		if(savedData.containsKey("CreateNewKey") && !(Boolean)savedData.get("CreateNewKey")) {
			paramsNet.setString("PrivKey", savedData.get("PrivKey").toString());
			paramsNet.setString("PubKey", savedData.get("PubKey").toString());
		}
		switch(savedData.get("Cleaner").toString()) {
			case "Remove not used chunks" : paramsClean.setString("Type", "CleanerDefault"); break;
			case "Remove old files" : paramsClean.setString("Type", "CleanerKeepNewFiles"); break;
			case "Don't remove" : paramsClean.setString("Type", "CleanerNone"); break;
			default : paramsClean.setString("Type", "CleanerDefault");
		}
		
		
		/*
		 * {clusterPwd=nøtQWERTYplz, PlainFileOnly=false, SizeIdeal=8000, TimeDelFS=5184000, 
		 * createEmptyDrive=true, TimeDelFic=1296000, createNewKey=true, clusterId=My 1st cluster drive, 
		 * InstallPath=C:\Users\centai\Documents\virt_dd, SizeMax=16000, DrivePath=K, ListenPort=30400}

		 */
		// create auto-launch
		
		// launch first instance
		
		Logs.logGui.info("end it!");
		myFrame.close();
	}

	
}
