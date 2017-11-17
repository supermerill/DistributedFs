package remi.distributedFS.gui.install;

import javax.swing.JPanel;

public abstract class InstallPanel extends JPanel {
	private static final long serialVersionUID = -7153979588149206400L;

	protected MainInstall manager;

	public void init(MainInstall mainInstall) {
		this.manager = mainInstall;
	}
	
	public abstract void construct();

	public abstract void destroy();
	
}
