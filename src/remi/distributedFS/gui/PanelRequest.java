package remi.distributedFS.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.sun.javafx.collections.ObservableListWrapper;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.fs.FileSystemManager;

//TODO: add details when clicking on a file or directory
//TODO: contextual menu -> to restore deleted files ? or a button?
public class PanelRequest extends GridPane{

	TextField path;
	Button btValidate;
	Label currentPath;
	TextArea data;
	ListView<String> listDirs;
	ObservableListWrapper<String> listDirsString = new ObservableListWrapper<String>(new ArrayList<String>());
	ListView<String> listFiles;
	ObservableListWrapper<String> listFilesString = new ObservableListWrapper<String>(new ArrayList<String>());
	private Label me;
	
	FileSystemManager manager;
	
	public PanelRequest() {
//		super(new GridPane());
		path = new TextField("/");
		path.setPrefColumnCount(60);
		btValidate = new Button("goto");
		currentPath = new Label("/");
		data = new TextArea();
		data.setEditable(false);
//		data.setText("sfqsfqfqffqs");
		data.setDisable(true); // it's enabled when there are something
		listDirs = new ListView<>();
		listDirs.setItems(listDirsString);
		listFiles = new ListView<>();
		listFiles.setItems(listFilesString);

		createLayout();	
		//setMinimumSize(new Dimension(600, 600));

//		directories.setText("directories");
//		files.setText("files");
		
		
	}
	
	public void init(FileSystemManager manager) {
		this.manager = manager;
		btValidate.setOnAction((ActionEvent)->{
			refreshList();
			
		});
	}
	
	public void refreshList() {
		FsDirectory mainDir = FsDirectoryMethods.getPathDir(this.manager.getRoot(), path.getText());
		if(mainDir != null) {
			
			List<String> lstDirStr = new ArrayList<>();
			for(FsDirectory dir : mainDir.getDirs()) {
				lstDirStr.add(dir.getName());
			}

			List<String> lstFileStr = new ArrayList<>();
			for(FsFile fic : mainDir.getFiles()) {
				lstFileStr.add(fic.getName());
			}
			for(FsObject obj : mainDir.getDelete()) {
				if(obj.asDirectory() != null) {
					lstDirStr.add("Deleted ("+obj.getName()+")");
				}
				if(obj.asFile() != null) {
					lstFileStr.add("Deleted ("+obj.getName()+")");
				}
			}
			listDirsString.setAll(lstDirStr);
			listFilesString.setAll(lstFileStr);
		}
	}
	
	public void createLayout(){

		PanelRequest grid = this;

	    ColumnConstraints column = new ColumnConstraints();
	    grid.getColumnConstraints().add(column);
	    column = new ColumnConstraints();
	    column.setFillWidth(true);
	    column.setHgrow(Priority.ALWAYS);
	    grid.getColumnConstraints().add(column);
	    grid.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

		grid.setHgap(10);
	    grid.setVgap(10);
	    grid.setPadding(new Insets(3, 10, 2, 10));

		grid.add(path, 0, 0, 2, 1);
		grid.add(btValidate, 2, 0, 1, 1);
		grid.add(currentPath, 0, 1, 3, 1);
		grid.add(data, 0, 2, 1, 2);
		PanelRequest.setConstraints(data, 0, 2, 1, 2, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, Insets.EMPTY);
		grid.add(listDirs, 1, 2, 2, 1);
		PanelRequest.setConstraints(listDirs, 1, 2, 2, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.SOMETIMES, Insets.EMPTY);
		grid.add(listFiles, 1, 3, 2, 1);
		PanelRequest.setConstraints(listFiles, 1, 3, 2, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.SOMETIMES, Insets.EMPTY);
		
	}
	
}
