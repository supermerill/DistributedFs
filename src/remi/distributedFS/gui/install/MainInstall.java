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
		System.out.println("next panel : "+currentPanel);
		if(currentPanel != null) {
//			System.out.println("destroy "+currentPanel);
			currentPanel.destroy();
//			this.remove(currentPanel);
		}
//		currentPanel = nextPanel;
//		System.out.println("add nextPanel "+nextPanel);
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
			System.out.println("destroy "+currentPanel);
			currentPanel.destroy();
		}
		
		File mainDir = new File(savedData.get("InstallPath").toString());//+"/Data"+savedData.get("DrivePath").toString());
		mainDir.mkdirs();
		System.out.println(savedData.get("InstallPath"));
		System.out.println(savedData);
		
		// create parameter files
		
		// standardManager.properties
		remi.distributedFS.fs.Parameters paramsMana = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/standardManager.properties");
		paramsMana.getStringOrDef("DriveLetter", savedData.get("DrivePath").toString());
		paramsMana.getIntOrDef("ListenPort", Integer.parseInt(savedData.get("ListenPort").toString()));
		paramsMana.getStringOrDef("StorageType", ((Boolean)savedData.get("PlainFileOnly"))?
				"remi.distributedFS.db.impl.ObjectFactory.StandardFactory":
				"remi.distributedFS.db.impl.readable.FsChunkOneFile.StorageFactory");
		paramsMana.getStringOrDef("MainDir", mainDir.getAbsolutePath());
		
		// cleaner.properties
		remi.distributedFS.fs.Parameters paramsClean = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/cleaner.properties");
		paramsClean.getIntOrDef("minKnownDuplicate", 2);
		paramsClean.getBoolOrDef("canDelete", true);
		paramsClean.getIntOrDef("maxSize", Integer.parseInt(savedData.get("SizeMax").toString()));
		paramsClean.getIntOrDef("idealSize", Integer.parseInt(savedData.get("SizeIdeal").toString()));
		paramsClean.getLongOrDef("stimeBeforeDelete", Integer.parseInt(savedData.get("TimeDelFic").toString()));
		
		// network parameters (stored in "clear" only before it start and then it can erase this file)
		remi.distributedFS.fs.Parameters paramsNet = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/network.properties");
		if(savedData.containsKey("ClusterIpPort")) {
			paramsNet.getStringOrDef("PeerIp", savedData.get("ClusterIpPort").toString().split(":")[0]);
			paramsNet.getLongOrDef("PeerPort", Long.parseLong(savedData.get("ClusterIpPort").toString().split(":")[1]));
			paramsNet.setBool("FirstConnection", true);
		}
		paramsNet.getLongOrDef("ClusterId", Math.abs(savedData.get("ClusterId").hashCode()));
		paramsNet.getStringOrDef("ClusterPassphrase", savedData.get("ClusterPwd").toString());
		if(savedData.containsKey("CreateNewKey") && !(Boolean)savedData.get("CreateNewKey")) {
			paramsNet.getStringOrDef("PrivKey", savedData.get("PrivKey").toString());
			paramsNet.getStringOrDef("PubKey", savedData.get("PubKey").toString());
		}
		
		/*
		 * {clusterPwd=nøtQWERTYplz, PlainFileOnly=false, SizeIdeal=8000, TimeDelFS=5184000, 
		 * createEmptyDrive=true, TimeDelFic=1296000, createNewKey=true, clusterId=My 1st cluster drive, 
		 * InstallPath=C:\Users\centai\Documents\virt_dd, SizeMax=16000, DrivePath=K, ListenPort=30400}

		 */
		// create auto-launch
		
		// launch first instance
		
		System.out.println("end it!");
		myFrame.close();
	}

	
}
