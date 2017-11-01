package remi.distributedFS.gui.install;

import javax.swing.JPanel;

public abstract class InstallPanel extends JPanel {

	protected MainInstall manager;

	public void init(MainInstall mainInstall) {
		this.manager = mainInstall;
	}
	
	public abstract void construct();

	public abstract void destroy();
	
}
