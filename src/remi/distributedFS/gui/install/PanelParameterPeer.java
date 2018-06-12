package remi.distributedFS.gui.install;

import java.io.File;
import java.util.regex.Pattern;

import com.sun.javafx.collections.ImmutableObservableList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

/**
 * choix du role(4 boutons): - standard - sauvegarde - nas/cache - client léger
 * 
 * qui set tes parametres:
 * 
 * taille max idéale taille max max elagage agressif temps conservation suppression data temps conservation suppression fs
 * 
 * 
 * bouton next -> create peer parameter and first launch!
 * 
 * @author centai
 *
 */
public class PanelParameterPeer extends InstallPanel {

	Label lblInstallPath = new Label("Install directory");
	Label lblDrivePath = new Label("Drive path");
	Label lblListenPort = new Label("Instance port");
	Label lblSizeIdeal = new Label("Ideal size (in mio)");
	Label lblSizeMax = new Label("Maximum size (in mio)");
	Label lblElagage = new Label("ReduceSize aggressively");
	Label lblTimeDelFic = new Label("Time before deletion (files)");
	Label lblTimeDelFS = new Label("Time before deletion (metadata)");
	Label lblStoreOnlyPlainFiles = new Label("Store only plain files");
	Label lblCanDelFic = new Label("Never remove files");
	Label lblCleaner = new Label("Cleaner");

	GridPane panelCleaner = new GridPane();
	TextField txtInstallPath = new TextField();
	Button btInstallPath = new Button();
	TextField txtDrivePath = new TextField();
	TextField txtListenPort = new TextField();
	TextField txtSizeIdeal = new TextField();
//	CheckBox chkCanReduce = new CheckBox();
	TextField txtSizeMax = new TextField();
	CheckBox chkElagage = new CheckBox();
	TextField txtTimeDelFic = new TextField();
	CheckBox chkCanDelFic = new CheckBox();
	TextField txtTimeDelFS = new TextField();
	CheckBox chkStoreOnlyPlainFiles = new CheckBox();
	ComboBox<String> cmbCleaner = new ComboBox<>();
	ImmutableObservableList<String> cmbItems = new ImmutableObservableList<>(
			"Remove not used chunks",
			"Remove old files",
			"Don't remove",
			"Remove less requested by network (niy)");

	Button btNext = new Button();

	public PanelParameterPeer() {
		super(new GridPane());
		GridPane grid = (GridPane) rootProperty().get();
		txtInstallPath
				.setTooltip(new Tooltip("Directory where to install this instance (where local data will be stored)."));
		txtDrivePath.setTooltip(new Tooltip("Drive path in the OS. (a drive letter for windows, like 'E')"));
		txtListenPort.setTooltip(new Tooltip("Tcp port where we listen the connection from other peers."));
		txtSizeIdeal.setTooltip(new Tooltip("Ideal maximum size that this drive can take in your local hard drive."));
//		chkCanReduce.setTooltip(new Tooltip("Allow this instance to delete local content to make space for new one."));
		txtSizeMax.setTooltip(
				new Tooltip("Absolute maximum size this drive can take in your hard drive (should be at least 1gio)."));
		chkElagage.setTooltip(new Tooltip("Check it to allow the fs to erase file locally even if it's not sure that "
				+ "it was copied to a 'trusted' host (ie permanent storage or at least one with enough free space)"));
		txtTimeDelFic.setTooltip(new Tooltip("Number of seconds before a file can be really deleted on this disk."));
		chkCanDelFic.setTooltip(new Tooltip(
				"Allow this instance to delete files stored locally when they are deleted in the distributed filesystem."));
		txtTimeDelFS.setTooltip(new Tooltip("Number of seconds before the knowledge of the deletion is deleted. "
				+ "Must be greater than the value behind, should be at least the maximum time you can be disconnected from the cluster."));
		chkStoreOnlyPlainFiles.setTooltip(new Tooltip("Set it to false to be able to store only some parts of the files, to be more space-efficient.\nSet it to true if you want to be able to read stored files even if the program isn't launched."));
		cmbCleaner.setTooltip(new Tooltip("When the space reserved for this instance is full, how to make some space for the new documents?"));
		cmbCleaner.setItems(cmbItems);
		btNext.setText("Finish");
		btNext.setTooltip(new Tooltip("Create your instance and connect it."));
		String localPath = new File(".").getAbsolutePath();
//		txtInstallPath.setText(localPath.substring(0, localPath.length()-1)+"myNewDrive");
//		txtInstallPath.setMultiSelectionEnabled(false);
//		txtInstallPath.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		txtInstallPath.setText(new File(".").getAbsolutePath());
		btInstallPath.setText("...");
		txtDrivePath.setText("K");
		txtListenPort.setText("30400");
		txtSizeIdeal.setText("8000");
		txtSizeMax.setText("16000");
		txtTimeDelFic.setText("" + (3600 * 24 * 15));
		txtTimeDelFS.setText("" + (3600 * 24 * 60));
		btNext.setOnAction((ActionEvent) -> {
			Pattern isNumber = Pattern.compile("^[0-9]+$");
			// check data
			if (!isNumber.matcher(txtSizeIdeal.getText()).matches() || !isNumber.matcher(txtSizeMax.getText()).matches()
					|| !isNumber.matcher(txtTimeDelFic.getText()).matches()
					|| !isNumber.matcher(txtTimeDelFS.getText()).matches()) {
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.setTitle("Error");
				alert.setHeaderText("Error:");
				alert.setContentText("Error: size & time must be numbers!");
				alert.showAndWait();
				return;
			}
			if (!txtDrivePath.getText().matches("^[A-Z]$") && System.getProperty("os.name").contains("Windows")) {
				System.out.println("txtDrivePath: " + txtDrivePath.getText().length());
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.setTitle("Error");
				alert.setHeaderText("Error:");
				alert.setContentText("On windows, the drive path must be an uppercase letter and not '"
						+ txtDrivePath.getText() + "'");
				alert.showAndWait();
				return;
			}
			if (!new File(txtInstallPath.getText()).exists()) {
				new File(txtInstallPath.getText()).mkdirs();
			}
			if (!new File(txtInstallPath.getText()).exists()) {
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.setTitle("Error");
				alert.setHeaderText("Error:");
				alert.setContentText("Failed to create directory '" + txtInstallPath.getText() + "'");
				alert.showAndWait();
				return;
			}
			manager.finish();
		});

		btInstallPath.setOnAction(ActionEvent -> {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle("Choose the directory where the drive data will be stored");
			File dir = new File(txtInstallPath.getText());
			if (!dir.exists()) {
				if (dir.getParentFile() != null) {
					dir = dir.getParentFile();
				} else {
					dir = new File(".");
				}
			}
			if (!dir.isDirectory()) {
				dir = new File(".");
			}
			chooser.setInitialDirectory(dir);
			dir = chooser.showDialog(manager.myFrame);
			if (dir != null) {
				txtInstallPath.setText(dir.getAbsolutePath());
			}
		});

		chkCanDelFic.selectedProperty()
				.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
					txtTimeDelFic.setDisable(newValue);
					txtTimeDelFic.setEditable(!newValue);
				});
		chkCanDelFic.setSelected(true);

//		chkCanReduce.selectedProperty()
//				.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
//					txtSizeIdeal.setDisable(!newValue);
//					chkElagage.setDisable(!newValue);
//				});
//		chkCanReduce.setSelected(true);
		
		chkStoreOnlyPlainFiles.selectedProperty()
				.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
					if(!newValue) {
						chkCanDelFic.setSelected(false);
					}
					chkCanDelFic.setDisable(!newValue);
		});
		chkStoreOnlyPlainFiles.setSelected(true);
		
		cmbCleaner.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			if(oldValue != null && oldValue.equals("Don't remove")) {
				txtSizeIdeal.setDisable(false);
				chkElagage.setDisable(false);
			}
			if(newValue != null && newValue.equals("Don't remove")) {
				txtSizeIdeal.setDisable(true);
				chkElagage.setDisable(true);
			}
		});

		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(3, 10, 2, 10));
		// Setting columns size
		ColumnConstraints column = new ColumnConstraints();
		column = new ColumnConstraints();
		column.setFillWidth(true);
		column.setHgrow(Priority.ALWAYS);
		grid.getColumnConstraints().add(new ColumnConstraints());
		grid.getColumnConstraints().add(new ColumnConstraints());
		grid.getColumnConstraints().add(column);
		grid.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

	    panelCleaner.setHgap(0);
	    panelCleaner.setVgap(0);
	    panelCleaner.setPadding(new Insets(3, 10, 2, 10));
	    panelCleaner.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
	    panelCleaner.getColumnConstraints().add(new ColumnConstraints());
	    panelCleaner.getColumnConstraints().add(column);

		int y = 0;
		grid.add(lblInstallPath, 0, y, 1, 1);
		grid.add(txtInstallPath, 1, y, 2, 1);
		grid.add(btInstallPath, 3, y, 1, 1);
		y++;
		grid.add(lblDrivePath, 0, y, 1, 1);
		grid.add(txtDrivePath, 1, y, 2, 1);
		y++;
		grid.add(lblListenPort, 0, y, 1, 1);
		grid.add(txtListenPort, 1, y, 1, 1);
		y++;
		grid.add(lblStoreOnlyPlainFiles, 0, y, 1, 1);
		grid.add(chkStoreOnlyPlainFiles, 1, y, 1, 1);
		y++;
		grid.add(lblCanDelFic, 0, y, 1, 1);
		grid.add(chkCanDelFic, 1, y, 1, 1);
		
		{

			panelCleaner.add(lblSizeIdeal, 0, 0, 1, 1);
			panelCleaner.add(txtSizeIdeal, 1, 0, 1, 1);
//			panelCleaner.add(chkCanReduce, 2, 0, 1, 1);

			panelCleaner.add(lblSizeMax, 0, 1, 1, 1);
			panelCleaner.add(txtSizeMax, 1, 1, 1, 1);

			panelCleaner.add(lblElagage, 0, 2, 1, 1);
			panelCleaner.add(chkElagage, 1, 2, 1, 1);

			panelCleaner.add(lblTimeDelFS, 0, 3, 1, 1);
			panelCleaner.add(txtTimeDelFS, 1, 3, 1, 1);

			panelCleaner.add(lblTimeDelFic, 0, 4, 1, 1);
			panelCleaner.add(txtTimeDelFic, 1, 4, 1, 1);

			panelCleaner.add(lblCleaner, 0, 5, 1, 1);
			panelCleaner.add(cmbCleaner, 1, 5, 1, 1);
		}
		
		y++;
		grid.add(panelCleaner, 0, y, 3, 1);
		y++;
		grid.add(btNext, 3, y, 1, 1);
		

		chkCanDelFic.setSelected(false);
		chkStoreOnlyPlainFiles.setSelected(false);
		cmbCleaner.setValue(cmbItems.get(0));
	}

	@Override
	public void construct() {

	}

	@Override
	public void destroy() {

		manager.savedData.put("InstallPath", txtInstallPath.getText());
		manager.savedData.put("DrivePath", txtDrivePath.getText());
		manager.savedData.put("ListenPort", txtListenPort.getText());
		manager.savedData.put("Cleaner", cmbCleaner.getValue());
		if (cmbCleaner.getValue().equals("Don't remove")) {
			manager.savedData.put("CanElage", false);
			manager.savedData.put("CanElageAggressively", false);
			manager.savedData.put("SizeIdeal", -1);
		}else {
			manager.savedData.put("CanElage", true);
			manager.savedData.put("CanElageAggressively", chkElagage.isSelected());
			manager.savedData.put("SizeIdeal", txtSizeIdeal.getText());
		}
		manager.savedData.put("SizeMax", txtSizeMax.getText());
		manager.savedData.put("SizeMax", txtSizeMax.getText());
		if (chkCanDelFic.isSelected()) {
			manager.savedData.put("NoDelete", false);
			manager.savedData.put("TimeDelFic", txtTimeDelFic.getText());
		} else {
			manager.savedData.put("NoDelete", true);
			manager.savedData.put("TimeDelFic", -1);
		}
		manager.savedData.put("TimeDelFS", txtTimeDelFS.getText());
		manager.savedData.put("PlainFileOnly", chkStoreOnlyPlainFiles.isSelected());
	}
}
