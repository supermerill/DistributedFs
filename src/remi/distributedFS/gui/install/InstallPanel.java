package remi.distributedFS.gui.install;

import javafx.scene.Parent;
import javafx.scene.Scene;

public abstract class InstallPanel extends Scene {

//	Group root;
	
//	public InstallPanel() {
//		super(new Group());
//		root = (Group) rootProperty().get();
//	}

	public InstallPanel(Parent gridPane) {
		super(gridPane);
	}

	protected MainInstall manager;

	public void init(MainInstall mainInstall) {
		this.manager = mainInstall;
	}
	
	public abstract void construct();

	public abstract void destroy();
	
}
