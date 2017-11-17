package remi.distributedFS.gui.install;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * choix du role(4 boutons):
 * - standard
 * - sauvegarde
 * - nas/cache
 * - client léger
 * 
 * qui set tes parametres:
 * 
 * taille max idéale
 * taille max max
 * elagage agressif
 * temps conservation suppression data
 * temps conservation suppression fs
 * 
 * 
 * bouton next -> create peer parameter and first launch!
 * 
 * @author centai
 *
 */
public class PanelParameterPeer extends InstallPanel {

	private static final long serialVersionUID = 2401294359531446800L;

	JLabel lblInstallPath = new JLabel("Install path");
	JLabel lblDrivePath = new JLabel("Drive path");
	JLabel lblListenPort = new JLabel("Install path");
	JLabel lblSizeIdeal = new JLabel("Ideal size (in mio)");
	JLabel lblSizeMax = new JLabel("Maximum size (in mio)");
	JLabel lblElagage = new JLabel("ReduceSize aggressively");
	JLabel lblTimeDelFic = new JLabel("Time before deletion (files)");
	JLabel lblTimeDelFS = new JLabel("Time before deletion (metadata)");
	JLabel lblStoreOnlyPlainFiles = new JLabel("Store only plain files");

	JFileChooser txtInstallPath = new JFileChooser("./myNewDrive");
	JTextField txtDrivePath = new JTextField();
	JTextField txtListenPort = new JTextField();
	JTextField txtSizeIdeal = new JTextField();
	JTextField txtSizeMax = new JTextField();
	JTextField txtElagage = new JTextField();
	JTextField txtTimeDelFic = new JTextField();
	JTextField txtTimeDelFS = new JTextField();
	JCheckBox chkStoreOnlyPlainFiles = new JCheckBox();

	JButton btNext = new JButton();

	public PanelParameterPeer() {
		setLayout(new GridBagLayout());
		txtInstallPath.setToolTipText("Directory where to install this instance (where local data will be stored).");
		txtDrivePath.setToolTipText("Drive path in the OS. (a drive letter for windows, like 'E')");
		txtListenPort.setToolTipText("Tcp port where we listen the connection from other peers.");
		txtSizeIdeal.setToolTipText("Ideal maximum size that this drive can take in your local hard drive.");
		txtSizeMax.setToolTipText("Absolute maximum size this drive can take in your hard drive (should be at least 1gio).");
		txtTimeDelFic.setToolTipText("Number of seconds before a file can be really deleted on this disk.");
		txtTimeDelFS.setToolTipText("Number of seconds before the knowledge of the deletion is deleted. "
				+ "Must be greater than the value behind, should be at least the maximum time you can be disconnected from the cluster.");
		chkStoreOnlyPlainFiles.setToolTipText("Set it to false to be able to store only some parts of the files, to be more space-efficient.\nSet it to true if you want to be able to read stored files even if the program isn't launched.");
		btNext.setText("Next");
		btNext.setToolTipText("Create your instance and connect it.");
		String localPath = new File(".").getAbsolutePath();
//		txtInstallPath.setText(localPath.substring(0, localPath.length()-1)+"myNewDrive");
		txtInstallPath.setMultiSelectionEnabled(false);
		txtInstallPath.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		txtDrivePath.setText("K");
		txtListenPort.setText("30400");
		txtSizeIdeal.setText("8000");
		txtSizeMax.setText("16000");
		txtTimeDelFic.setText(""+(3600*24*15));
		txtTimeDelFS.setText(""+(3600*24*60));
		chkStoreOnlyPlainFiles.setSelected(false);
		
		btNext.addActionListener((actEvent)->{
			Pattern isNumber = Pattern.compile("^[0-9]+$");
			//check data
			if(!isNumber.matcher(txtSizeIdeal.getText()).matches()
					|| !isNumber.matcher(txtSizeMax.getText()).matches()
					|| !isNumber.matcher(txtTimeDelFic.getText()).matches()
					|| !isNumber.matcher(txtTimeDelFS.getText()).matches()) {
				JOptionPane.showMessageDialog(this, "Error: size & time must be numbers!", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if(!txtDrivePath.getText().matches("^[A-Z]$") /* TODO: && isWindows() */) {
				System.out.println("txtDrivePath: "+txtDrivePath.getText().length());
				JOptionPane.showMessageDialog(this, "Error: On windows, the drive path must be an uppercase letter and not '"+txtDrivePath.getText()+"'", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if(!txtInstallPath.getSelectedFile().exists()) {
				txtInstallPath.getSelectedFile().mkdirs();
			}
			if(!txtInstallPath.getSelectedFile().exists()) {
				JOptionPane.showMessageDialog(this, "Error: Failed to create directory '"+txtInstallPath.getSelectedFile()+"'", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			manager.finish();
		});
//		txtClusterId.wid
		//TODO: set max character count
		GridBagConstraints cst = new GridBagConstraints();

		cst.fill = 1;
		cst.ipadx = 20;
		cst.ipady = 5;

		cst.weightx = 0;
		add(lblInstallPath, cst);
		cst.gridx = 1;
		cst.weightx = 1;
		add(txtInstallPath, cst);
		
		cst.weightx = 0;
		cst.gridy = 1;
		cst.gridx = 0;
		add(lblDrivePath, cst);
		cst.gridx = 1;
		add(txtDrivePath, cst);

		cst.gridy++;
		cst.gridx = 0;
		add(lblSizeIdeal, cst);
		cst.gridx = 1;
		add(txtSizeIdeal, cst);

		cst.gridy++;
		cst.gridx = 0;
		add(lblSizeMax, cst);
		cst.gridx = 1;
		add(txtSizeMax, cst);

		cst.gridy++;
		cst.gridy++;
		cst.gridx = 0;
		add(lblTimeDelFic, cst);
		cst.gridx = 1;
		add(txtTimeDelFic, cst);

		cst.gridy++;
		cst.gridx = 0;
		add(lblTimeDelFS, cst);
		cst.gridx = 1;
		add(txtTimeDelFS, cst);

		cst.gridy++;
		cst.gridx = 0;
		add(lblTimeDelFic, cst);
		cst.gridx = 1;
		add(txtTimeDelFic, cst);
		
		
		cst.gridy++;
		cst.gridx = 2;
		add(btNext, cst);
	}
	
	@Override
	public void construct() {
		
	}

	@Override
	public void destroy() {
		
		manager.savedData.put("InstallDir", txtInstallPath.getSelectedFile());
		manager.savedData.put("DrivePath", txtDrivePath.getText());
		manager.savedData.put("ListenPort", txtListenPort.getText());
		manager.savedData.put("SizeIdeal", txtSizeIdeal.getText());
		manager.savedData.put("SizeMax", txtSizeMax.getText());
		manager.savedData.put("TimeDelFic", txtTimeDelFic.getText());
		manager.savedData.put("TimeDelFS", txtTimeDelFS.getText());
		manager.savedData.put("PlainFileOnly", chkStoreOnlyPlainFiles.isSelected());
	}
}
