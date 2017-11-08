package remi.distributedFS.gui.install;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainInstall extends Application {
	private static final long serialVersionUID = 1L;

	InstallPanel currentPanel;
	Map<String, Object> savedData = new HashMap<>();

	Stage myFrame;

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
		//create parameter files
		
		//create auto-launch
		
		//launch first instance
		
		System.out.println("end it!");
		myFrame.close();
	}

	
}
