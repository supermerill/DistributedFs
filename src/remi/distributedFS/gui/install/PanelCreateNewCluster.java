package remi.distributedFS.gui.install;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

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

	Label lblClusterId = new Label();
	Label lblClusterPwd = new Label();
	TextField txtClusterId = new TextField();
	TextField txtClusterPwd = new TextField();
	
	Button btNext = new Button();
	
	public PanelCreateNewCluster() {
		super(new GridPane());
		GridPane grid = (GridPane) rootProperty().get();
		lblClusterId.setText("Cluster name");
		lblClusterPwd.setText("Cluster password");
		txtClusterId.setTooltip(new Tooltip("Your cluster name, It's what it's used to identify this new drive"));
		txtClusterPwd.setTooltip(new Tooltip("The passcode that is used to see who is authorized to connect."));
		txtClusterId.setText("My 1st cluster drive");
		txtClusterPwd.setText("nøtQWERTYplz");
		btNext.setText("Next");
		btNext.setTooltip(new Tooltip("Ask peer paremeters"));
		btNext.setOnAction((ActionEvent)->{
			//check data
			System.out.println(txtClusterId.getText().length());
			if(txtClusterId.getText().length()<6 || txtClusterId.getText().length()>32) {
				Alert alert = new Alert(Alert.AlertType.WARNING);alert.setTitle("Error");alert.setHeaderText("Error:");
				alert.setContentText("you must have a cluster name with at least 6 characaters and maximum 32");
				alert.showAndWait();
				return;
			}
			if(txtClusterPwd.getText().length()<6 || txtClusterPwd.getText().length()>32) {
				Alert alert = new Alert(Alert.AlertType.WARNING);alert.setTitle("Error");alert.setHeaderText("Error:");
				alert.setContentText("you must have a cluster password with at least 6 characaters and maximum 32");
				alert.showAndWait();
				return;
			}
			manager.goToPanel(new PanelParameterPeer());
		});

		grid.setHgap(10);
	    grid.setVgap(10);
	    grid.setPadding(new Insets(3, 10, 2, 10));
	    // Setting columns size
	    ColumnConstraints column = new ColumnConstraints();
	    grid.getColumnConstraints().add(column);
	    column = new ColumnConstraints();
	    column.setFillWidth(true);
	    column.setHgrow(Priority.ALWAYS);
	    grid.getColumnConstraints().add(column);
	    grid.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
	    

		grid.add(lblClusterId, 0, 0, 1, 1);
		grid.add(txtClusterId, 1, 0, 1, 1);

		grid.add(lblClusterPwd, 0, 1, 1, 1);
		grid.add(txtClusterPwd, 1, 1, 1, 1);

		grid.add(btNext, 2, 2, 1, 1);
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
