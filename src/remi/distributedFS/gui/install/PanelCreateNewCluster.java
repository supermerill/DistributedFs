package remi.distributedFS.gui.install;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * id du cluster (generateur aleat)
 * passcode du cluster
 * protection aes?
 * 
 * bouton next -> create data pour creation + ask cluster parameters
 * 
 * @author centai
 *
 */
public class PanelCreateNewCluster extends InstallPanel {

	private static final long serialVersionUID = 8779324129474868505L;

	JLabel lblClusterId = new JLabel();
	JLabel lblClusterPwd = new JLabel();
	JTextField txtClusterId = new JTextField();
	JTextField txtClusterPwd = new JTextField();
//	JCheckBox hasAesEncoding = new JCheckBox();
	
	JButton btNext = new JButton();
	
	public PanelCreateNewCluster() {
		setLayout(new GridBagLayout());
		lblClusterId.setText("Cluster name");
		lblClusterPwd.setText("Cluster password");
		txtClusterId.setToolTipText("Your cluster name, It's what it's used to identify this new drive");
		txtClusterPwd.setToolTipText("The passcode that is used to see who is authorized to connect.");
		txtClusterId.setText("My 1st cluster drive");
		txtClusterPwd.setText("nøtQWERTYplz");
		btNext.setText("Next");
		btNext.setToolTipText("Ask peer paremeters");
		btNext.addActionListener((actEvent)->{
			//check data
			System.out.println(txtClusterId.getText().length());
			if(txtClusterId.getText().length()<6 || txtClusterId.getText().length()>32) {
				JOptionPane.showMessageDialog(this, "Error: you must have a cluster name with at least 6 characaters and maximum 32", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if(txtClusterPwd.getText().length()<6 || txtClusterPwd.getText().length()>32) {
				JOptionPane.showMessageDialog(this, "Error: you must have a cluster password with at least 6 characaters and maximum 32", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			manager.goToPanel(new PanelParameterPeer());
		});
//		txtClusterId.wid
		//TODO: set max character count
		GridBagConstraints cst = new GridBagConstraints();

		cst.fill = 1;
		cst.ipadx = 20;
		cst.ipady = 5;

		cst.weightx = 0;
		add(lblClusterId, cst);
		cst.gridx = 1;
		cst.weightx = 1;
		add(txtClusterId, cst);
		
		cst.weightx = 0;
		cst.gridy = 1;
		cst.gridx = 0;
		add(lblClusterPwd, cst);
		cst.gridx = 1;
		add(txtClusterPwd, cst);
		
		cst.gridy=3;
		cst.gridx = 2;
		add(btNext, cst);
	}
	
	@Override
	public void construct() {
	}

	@Override
	public void destroy() {
		manager.savedData.put("createNewKey", true);
		manager.savedData.put("createEmptyDrive", true);
		manager.savedData.put("clusterId", txtClusterId.getText());
		manager.savedData.put("clusterPwd", txtClusterPwd.getText());
	}
}
