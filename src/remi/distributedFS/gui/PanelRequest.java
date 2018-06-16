package remi.distributedFS.gui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import remi.distributedFS.datastruct.PUGA;
import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.log.Logs;

//TODO: add details when clicking on a file or directory
//TODO: contextual menu -> to restore deleted files ? or a button?
public class PanelRequest extends GridPane{

	TextField path;
	Button btValidate;
	Label currentPath;
	ListView<String> listData;
	ObservableListWrapper<String> listDataString = new ObservableListWrapper<String>(new ArrayList<String>());
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
		listData = new ListView();
		listData.setItems(listDataString);
		listDirs = new ListView<>();
		listDirs.setItems(listDirsString);
		listFiles = new ListView<>();
		listFiles.setItems(listFilesString);

		createLayout();	
		//setMinimumSize(new Dimension(600, 600));

//		directories.setText("directories");
//		files.setText("files");
		listDirs.setOnMouseClicked((me)->{
			String item = listDirs.getSelectionModel().getSelectedItem();
			if(item != null){
				if(me.getClickCount()==1) {
					refreshDir(currentPath.getText()+"/"+item);
				}else {
					//double clic -> goto
					if(item.equals("..")) {
						if(path.getText().lastIndexOf("/") == 0) {
							path.setText("/");
						}else {
							String newPath = path.getText().substring(path.getText().lastIndexOf("/"));
							path.setText(newPath);
						}
					}else {
						StringBuilder newPath = new StringBuilder(path.getText());
						if(!path.getText().endsWith("/")) newPath.append("/");
						newPath.append(item);
						path.setText(newPath.toString());
					}
					refreshList();
				}
			}
		});

		listFiles.setOnMouseClicked((me)->{
			Logs.logGui.info("mouse click");
			String item = listFiles.getSelectionModel().getSelectedItem();
			Logs.logGui.info("mouse click on "+item);
			if(item != null){
				refreshDir(currentPath.getText()+"/"+item);
			}
		});
		
	}
	
	private void refreshDir(String path) {
		FsObject obj = FsDirectoryMethods.getPathObj(this.manager.getRoot(), path);
		if(obj == null){
			System.err.println("Error, can't access "+path);
			return;
		}
		List<String> lstStr = new ArrayList<>();
		StringBuilder str = new StringBuilder();
		str = new StringBuilder(); lstStr.add(addItem(str,"id: ",20).append(obj.getId()).toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"name: ",20).append(obj.getName()).toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"permissions: :",20).append(new PUGA(obj.getPUGA()).toStringShort()).toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"computer id: ",20).append(obj.getComputerId()).toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"group id: ",20).append(obj.getGroupId()).toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"user id: ",20).append(obj.getUserId()).toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"creator uid: ",20).append(obj.getCreatorUID()).toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"creation date: ",20).append(new SimpleDateFormat("yyyy.MM.dd HH.mm.ss SSS").format(new Date(obj.getCreationDate()))).toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"modify uid: ",20).append(obj.getModifyUID()).toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"modify date: ",20).append(new SimpleDateFormat("yyyy.MM.dd HH.mm.ss SSS").format(new Date(obj.getModifyDate()))).toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"delete uid: ",20).append(obj.getDeleteUID()>0?obj.getDeleteUID():"").toString());
		str = new StringBuilder(); lstStr.add(addItem(str,"delete date: ",20).append(obj.getDeleteDate()>0?new SimpleDateFormat("yyyy.MM.dd HH.mm.ss").format(new Date(obj.getDeleteDate())):"").toString());
//		addItem(str,": ",20).append(obj.get);
		FsFile fic = obj.asFile();
		if(fic != null){
			str = new StringBuilder(); addItem(str,"size: ",20);
			if(fic.getSize()>1000000000){ str.append((fic.getSize()/1000000000)%1000).append("go ");}
			if(fic.getSize()>1000000){ str.append((fic.getSize()/1000000)%1000).append("mo ");}
			if(fic.getSize()>1000){ str.append((fic.getSize()/1000)%1000).append("ko ");}
			str.append(fic.getSize()%1000).append("o");
			lstStr.add(str.toString());
			str = new StringBuilder(); lstStr.add(addItem(str,"nb chunks: ",20).append(fic.getAllChunks().size()).toString());
			str = new StringBuilder(); lstStr.add(addItem(str,"nb local chunks: ",20).append(fic.getChunks().size()).toString());
		}
		FsDirectory dir = obj.asDirectory();
		if(dir != null){
			str = new StringBuilder(); lstStr.add(addItem(str,"nb dirs: ",20).append(dir.getDirs().size()).toString());
			str = new StringBuilder(); lstStr.add(addItem(str,"nb files: ",20).append(dir.getFiles().size()).toString());
			str = new StringBuilder(); lstStr.add(addItem(str,"nb deleted things: ",20).append(dir.getDelete().size()).toString());
		}
		listDataString.setAll(lstStr);
	}
	
	public static StringBuilder addItem(StringBuilder str, String toAdd, int max) {
		str.append(toAdd);
		for(int i=toAdd.length(); i<max;i++) {
			str.append(' ');
		}
		return str;
	}

	public void init(FileSystemManager manager) {
		this.manager = manager;
		btValidate.setOnAction((ActionEvent)->{
			refreshList();
		});
		refreshList();
	}
	
	public void refreshList() {
		FsDirectory mainDir = FsDirectoryMethods.getPathDir(this.manager.getRoot(), path.getText());
		if(mainDir != null) {
			
			List<String> lstDirStr = new ArrayList<>();
			if(path.getText().length()>1) {
				lstDirStr.add("..");
			}
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
		grid.add(listData, 0, 2, 1, 2);
		PanelRequest.setConstraints(listData, 0, 2, 1, 2, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, Insets.EMPTY);
		listData.setMinWidth(450);
		grid.add(listDirs, 1, 2, 2, 1);
		PanelRequest.setConstraints(listDirs, 1, 2, 2, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.SOMETIMES, Insets.EMPTY);
		grid.add(listFiles, 1, 3, 2, 1);
		PanelRequest.setConstraints(listFiles, 1, 3, 2, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.SOMETIMES, Insets.EMPTY);
		
	}
	
}
