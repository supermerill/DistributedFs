package remi.distributedFS.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class PanelRequest extends JPanel{

	JTextField path;
	JButton btValidate;
	JLabel currentPath;
	JTextArea data;
	JTextArea directories; //toto: scrollable jlist
	JTextArea files; //toto: scrollable jlist
	private JLabel me;
	
	
	public PanelRequest() {
		path = new JTextField("/");
		path.setColumns(60);
		btValidate = new JButton("goto");
		currentPath = new JLabel("/");
		data = new JTextArea();
		data.setEditable(false);
		data.setEnabled(false);
		directories = new JTextArea();
		directories.setEditable(false);
		files = new JTextArea();
		files.setEditable(false);

		createLayout();	
		setMinimumSize(new Dimension(600, 600));

		directories.setText("directories");
		files.setText("files");
	}
	
	public void createLayout(){

		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(layout);

		c.gridx=0;
		c.gridy=0;
		c.gridheight=1;
		c.gridwidth=2;
//		JButton test = new JButton("3");
		layout.setConstraints(path, c);
		add(path);
		c.gridx=2;
		c.gridwidth=1;
		layout.setConstraints(btValidate, c);
		add(btValidate);
		c.gridy++;
		c.gridx=0;
		c.gridwidth=3;
		layout.setConstraints(currentPath, c);
		add(currentPath);
		c.gridy++;
		c.gridwidth=1;
		c.gridheight=2;
		c.weighty=1;
		c.weightx=0.5;
		c.fill = GridBagConstraints.BOTH;
		layout.setConstraints(data, c);
		add(data);
		c.weighty=0.5;
		c.gridx=1;
		c.gridwidth=2;
		c.gridheight=1;
		c.fill = GridBagConstraints.BOTH;
		layout.setConstraints(directories, c);
		add(directories);
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		layout.setConstraints(files, c);
		add(files);
	}
	
}
