package remi.distributedFS.gui.install;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

public class MainInstall extends JFrame {
	private static final long serialVersionUID = 1L;

	InstallPanel currentPanel;
	Map<String, Object> savedData = new HashMap<>();
	
	public MainInstall() {
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setLayout(new BorderLayout());
		// create the first panel
		goToPanel(new PanelChoiceNewConnect());
	}
	
	
	public void goToPanel(InstallPanel nextPanel) {
		if(currentPanel != null) {
			System.out.println("destroy "+currentPanel);
			currentPanel.destroy();
			this.remove(currentPanel);
		}
		currentPanel = nextPanel;
		System.out.println("add nextPanel "+nextPanel);
		currentPanel.init(this);
		this.add(currentPanel, BorderLayout.CENTER);
		currentPanel.construct();
		currentPanel.invalidate();
		this.invalidate();
		this.revalidate();
		this.repaint();
	}
	
	public static void main(String[] args) {
		MainInstall fen = new MainInstall();
		fen.setSize(900, 900);
		fen.setVisible(true);
//		System.out.println("127.0.0.1:3033".matches("^[0-9:\\.]+:[0-9][0-9][0-9]?[0-9]?[0-9]?$"));
//		System.out.println("K".matches("[A-Z]"));
//		System.out.println("ooKoo".matches("[A-Z]"));
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
		this.dispose();
	}
	
}
