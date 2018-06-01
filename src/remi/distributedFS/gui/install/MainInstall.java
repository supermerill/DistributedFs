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
	Map<String, Object> savedData = new HashMap<>();

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
	}
	
	public static void main(String[] args) {
		Application.launch(args);
	}
	


	public void finish() {
		if(currentPanel != null) {
			System.out.println("destroy "+currentPanel);
			currentPanel.destroy();
		}
		
		File mainDir = (File)savedData.get("InstallPath");
		
		//create parameter files
		
		//standardManager.properties
		remi.distributedFS.fs.Parameters paramsMana = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/standardManager.properties");
		paramsMana.getStringOrDef("DriveLetter", savedData.get("DrivePath").toString());
		paramsMana.getIntOrDef("ListenPort", Integer.parseInt(savedData.get("ListenPort").toString()));
		paramsMana.getStringOrDef("StorageType", ((Boolean)savedData.get("PlainFileOnly"))?
				"remi.distributedFS.db.impl.ObjectFactory.StandardFactory":
				"remi.distributedFS.db.impl.readable.FsChunkOneFile.StorageFactory");
		paramsMana.getStringOrDef("MainDir", mainDir.getAbsolutePath());
		
		//cleaner.properties
		remi.distributedFS.fs.Parameters paramsClean = new remi.distributedFS.fs.Parameters(mainDir.getAbsolutePath()+"/cleaner.properties");
		paramsClean.getIntOrDef("minKnownDuplicate", 2);
		paramsClean.getBoolOrDef("canDelete", true);
		paramsClean.getIntOrDef("maxSize", Integer.parseInt(savedData.get("SizeMax").toString()));
		paramsClean.getIntOrDef("idealSize", Integer.parseInt(savedData.get("SizeIdeal").toString()));
		paramsClean.getLongOrDef("stimeBeforeDelete", Integer.parseInt(savedData.get("TimeDelFic").toString()));
		
		
		//create auto-launch
		
		//launch first instance
		
		System.out.println("end it!");
		myFrame.close();
	}

	
}
