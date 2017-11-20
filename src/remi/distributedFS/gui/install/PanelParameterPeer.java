package remi.distributedFS.gui.install;

import java.io.File;
import java.util.regex.Pattern;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

	Label lblInstallPath = new Label("Install path");
	Label lblDrivePath = new Label("Drive path");
	Label lblListenPort = new Label("Install path");
	Label lblSizeIdeal = new Label("Ideal size (in mio)");
	Label lblSizeMax = new Label("Maximum size (in mio)");
	Label lblElagage = new Label("ReduceSize aggressively");
	Label lblTimeDelFic = new Label("Time before deletion (files)");
	Label lblTimeDelFS = new Label("Time before deletion (metadata)");

	TextField txtInstallPath = new TextField();
	Button btInstallPath = new Button();
	TextField txtDrivePath = new TextField();
	TextField txtListenPort = new TextField();
	TextField txtSizeIdeal = new TextField();
	CheckBox chkCanReduce = new CheckBox();
	TextField txtSizeMax = new TextField();
	CheckBox chkElagage = new CheckBox();
	TextField txtTimeDelFic = new TextField();
	CheckBox chkCanDelFic = new CheckBox();
	TextField txtTimeDelFS = new TextField();

	Button btNext = new Button();

	public PanelParameterPeer() {
		super(new GridPane());
		GridPane grid = (GridPane) rootProperty().get();
		txtInstallPath
				.setTooltip(new Tooltip("Directory where to install this instance (where local data will be stored)."));
		txtDrivePath.setTooltip(new Tooltip("Drive path in the OS. (a drive letter for windows, like 'E')"));
		txtListenPort.setTooltip(new Tooltip("Tcp port where we listen the connection from other peers."));
		txtSizeIdeal.setTooltip(new Tooltip("Ideal maximum size that this drive can take in your local hard drive."));
		chkCanReduce.setTooltip(new Tooltip("Allow this instance to delete local content to make space for new one."));
		txtSizeMax.setTooltip(
				new Tooltip("Absolute maximum size this drive can take in your hard drive (should be at least 1gio)."));
		txtTimeDelFic.setTooltip(new Tooltip("Number of seconds before a file can be really deleted on this disk."));
		chkCanDelFic.setTooltip(new Tooltip(
				"Allow this instance to delete files stored locally when they are deleted in the distributed filesystem."));
		txtTimeDelFS.setTooltip(new Tooltip("Number of seconds before the knowledge of the deletion is deleted. "
				+ "Must be greater than the value behind, should be at least the maximum time you can be disconnected from the cluster."));
		btNext.setText("Next");
		btNext.setTooltip(new Tooltip("Create your instance and connect it."));
		String localPath = new File(".").getAbsolutePath();
		txtInstallPath.setText(localPath.substring(0, localPath.length() - 1) + "myNewDrive");
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
			if (!txtDrivePath.getText().matches("^[A-Z]$") /* TODO: && isWindows() */) {
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
					txtTimeDelFic.setDisable(!newValue);
					txtTimeDelFic.setEditable(newValue);
				});
		chkCanDelFic.setSelected(true);

		chkCanReduce.selectedProperty()
				.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
					txtSizeIdeal.setDisable(!newValue);
					chkElagage.setDisable(!newValue);
				});
		chkCanReduce.setSelected(true);

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
		grid.add(lblSizeIdeal, 0, y, 1, 1);
		grid.add(txtSizeIdeal, 1, y, 1, 1);
		grid.add(chkCanReduce, 2, y, 1, 1);
		y++;
		grid.add(lblSizeMax, 0, y, 1, 1);
		grid.add(txtSizeMax, 1, y, 1, 1);
		y++;
		grid.add(lblElagage, 0, y, 1, 1);
		grid.add(chkElagage, 1, y, 1, 1);
		y++;
		grid.add(lblTimeDelFS, 0, y, 1, 1);
		grid.add(txtTimeDelFS, 1, y, 1, 1);
		y++;
		grid.add(lblTimeDelFic, 0, y, 1, 1);
		grid.add(txtTimeDelFic, 1, y, 1, 1);
		grid.add(chkCanDelFic, 2, y, 1, 1);
		y++;
		grid.add(btNext, 3, y, 1, 1);
	}

	@Override
	public void construct() {

	}

	@Override
	public void destroy() {

		manager.savedData.put("InstallPath", txtInstallPath.getText());
		manager.savedData.put("DrivePath", txtDrivePath.getText());
		manager.savedData.put("ListenPort", txtListenPort.getText());
		if (chkCanReduce.isSelected()) {
			manager.savedData.put("CanElage", true);
			manager.savedData.put("CanElageAggressively", chkElagage.isSelected());
			manager.savedData.put("SizeIdeal", txtSizeIdeal.getText());
		} else {
			manager.savedData.put("CanElage", false);
			manager.savedData.put("CanElageAggressively", false);
			manager.savedData.put("SizeIdeal", -1);
		}
		manager.savedData.put("SizeMax", txtSizeMax.getText());
		if (chkCanDelFic.isSelected()) {
			manager.savedData.put("NoDelete", false);
			manager.savedData.put("TimeDelFic", txtTimeDelFic.getText());
		} else {
			manager.savedData.put("NoDelete", true);
			manager.savedData.put("TimeDelFic", -1);
		}
		manager.savedData.put("TimeDelFS", txtTimeDelFS.getText());

	}
}
