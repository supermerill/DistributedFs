package remi.distributedFS.gui.install;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 
 * ip/port d'un host +bouton test
 * id du cluster
 * passcode
 * optional: ma clé privé/publique & id
 * 
 * bouton next-> creer info connection + demander options locales (peer parameter
 * 
 * @author centai
 *
 */
public class PanelConnectToCluster extends InstallPanel {

	private static final long serialVersionUID = -8412251672293919001L;

	JLabel lblClusterIpPort = new JLabel();
	JLabel lblClusterId = new JLabel();
	JLabel lblClusterPwd = new JLabel();
	JLabel lblNewOld = new JLabel("Reuse a previous used computer id");
	JTextField txtClusterIpPort = new JTextField();
	JTextField txtClusterId = new JTextField();
	JTextField txtClusterPwd = new JTextField();
	JCheckBox chkNewOld = new JCheckBox();
	
	JPanel panelPubPrivKey = new JPanel();
	JLabel lblPubKey = new JLabel("Public key");
	JLabel lblPrivKey = new JLabel("Private Key");
	JTextField txtPubKey = new JTextField();
	JTextField txtPrivKey = new JTextField();
//	JComboBox hasAesEncoding = new JTextField();
	
	JButton btNext = new JButton();
	
	public PanelConnectToCluster() {
		setLayout(new GridBagLayout());
		lblClusterIpPort.setText("A cluster ip:port");
		lblClusterId.setText("Cluster name");
		lblClusterPwd.setText("Cluster password");
		txtClusterIpPort.setToolTipText(" ip:port where we can find a computer inside the cluster.");
		txtClusterId.setToolTipText("Your cluster name, It's what it's used to identify this new drive");
		txtClusterPwd.setToolTipText("The passcode that is used to see who is authorized to connect.");
		txtClusterIpPort.setText("127.0.0.1:30400");
		txtClusterId.setText("My 1st cluster drive");
		txtClusterPwd.setText("nøtQWERTYplz");
		btNext.setText("Next");
		btNext.setToolTipText("Ask peer paremeters");
		btNext.addActionListener((actEvent)->{
			//check data TODO
			System.out.println(txtClusterId.getText().length());
			if(txtClusterId.getText().length()<6 || txtClusterId.getText().length()>32) {
				JOptionPane.showMessageDialog(this, "Error: you must have a cluster name with at least 6 characaters and maximum 32", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if(txtClusterPwd.getText().length()<6 || txtClusterPwd.getText().length()>32) {
				JOptionPane.showMessageDialog(this, "Error: you must have a cluster password with at least 6 characaters and maximum 32", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if(!txtClusterIpPort.getText().matches("^[0-9:\\.]+:[0-9][0-9][0-9]?[0-9]?[0-9]?$")) {
				JOptionPane.showMessageDialog(this, "Error: you must have a ip:port well formatted string (example1: 192.168.0.1:300) (example2: ::1:300)", "Error", JOptionPane.WARNING_MESSAGE);
				return;	
			}

			if(chkNewOld.isSelected()) {
				//TODO
			}
			manager.goToPanel(new PanelParameterPeer());
		});
		chkNewOld.addChangeListener((chgEvent)->{
			panelPubPrivKey.setEnabled(chkNewOld.isSelected());
			txtPrivKey.setEditable(chkNewOld.isSelected());
			txtPubKey.setEditable(chkNewOld.isSelected());
		});
		panelPubPrivKey.setEnabled(chkNewOld.isSelected());
		txtPrivKey.setEditable(chkNewOld.isSelected());
		txtPubKey.setEditable(chkNewOld.isSelected());
//		txtClusterId.wid
		GridBagConstraints cst = new GridBagConstraints();
		
		panelPubPrivKey.setLayout(new GridBagLayout());

		cst.fill = 1;
		cst.ipadx = 20;
		cst.ipady = 5;

		cst.gridy = 0;
		cst.gridx = 0;
		cst.weightx = 0;
		cst.gridwidth = 1;
		add(lblClusterIpPort, cst);
		panelPubPrivKey.add(lblPrivKey, cst);
		cst.gridx = 1;
		cst.gridwidth = 3;
		cst.weightx = 1;
		add(txtClusterIpPort, cst);
		panelPubPrivKey.add(txtPrivKey, cst);

		cst.weightx = 0;
		cst.gridy ++;
		cst.gridx = 0;
		cst.gridwidth = 1;
		add(lblClusterId, cst);
		panelPubPrivKey.add(lblPubKey, cst);
		cst.gridx = 1;
		cst.gridwidth = 3;
		cst.weightx = 1;
		add(txtClusterId, cst);
		panelPubPrivKey.add(txtPubKey, cst);

		cst.weightx = 0;
		cst.gridy ++;
		cst.gridx = 0;
		cst.gridwidth = 1;
		add(lblClusterPwd, cst);
		cst.gridx = 1;
		cst.gridwidth = 3;
		cst.weightx = 1;
		add(txtClusterPwd, cst);

		cst.weightx = 0;
		cst.gridy ++;
		cst.gridx = 0;
		cst.gridwidth = 2;
		add(lblNewOld, cst);
		cst.gridx = 2;
		cst.gridwidth = 2;
		cst.weightx = 1;
		add(chkNewOld, cst);

		cst.gridy ++;
		cst.gridx = 0;
		cst.gridwidth = 4;
		add(panelPubPrivKey, cst);

		cst.weightx = 0;
		cst.gridwidth = 1;
		cst.gridy ++;
		cst.gridx = 4;
		add(btNext, cst);
	}
	@Override
	public void construct() {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		manager.savedData.put("ClusterIpPort", txtClusterIpPort.getText());
		manager.savedData.put("ClusterId", txtClusterId.getText());
		manager.savedData.put("ClusterPwd", txtClusterPwd.getText());
		if(chkNewOld.isSelected()) {
			manager.savedData.put("PrivKey", txtPrivKey.getText());
			manager.savedData.put("PubKey", txtPubKey.getText());
		}
	}

}
