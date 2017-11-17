package remi.distributedFS.gui.install;

import java.awt.FlowLayout;

import javax.swing.JButton;

public class PanelChoiceNewConnect extends InstallPanel {
	private static final long serialVersionUID = -4861532843787270076L;

	JButton btNew;
	JButton btConnect;
	
	public PanelChoiceNewConnect(){
		setLayout(new FlowLayout());
		btNew = new JButton("Create new cluster");
		btNew.addActionListener((actionEvent)->{
			System.out.println("new cluster");
			manager.goToPanel(new PanelCreateNewCluster());
		});
		add(btNew);
		btConnect = new JButton("Connect to a cluster");
		btConnect.addActionListener((actionEvent)->{
			manager.goToPanel(new PanelConnectToCluster());
		});
		add(btConnect);
	}

	@Override
	public void construct() {
	}

	@Override
	public void destroy() {
	}

}
