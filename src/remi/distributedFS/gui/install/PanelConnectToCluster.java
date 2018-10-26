package remi.distributedFS.gui.install;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
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
import remi.distributedFS.log.Logs;
import remi.distributedFS.net.impl.PhysicalServerDummy;

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

	Label lblClusterIpPort = new Label();
	Label lblClusterId = new Label();
	Label lblClusterPwd = new Label();
	Label lblNewOld = new Label("Reuse a previous used computer id");
	TextField txtClusterIpPort = new TextField();
	TextField txtClusterId = new TextField();
	TextField txtClusterPwd = new TextField();
	CheckBox chkNewOld = new CheckBox();
	
	GridPane panelPubPrivKey = new GridPane();
	Label lblPubKey = new Label("Public key");
	Label lblPrivKey = new Label("Private Key");
	TextField txtPubKey = new TextField();
	TextField txtPrivKey = new TextField();
//	ComboBox hasAesEncoding = new TextField();

	Button btTestConnection = new Button();
	Button btNext = new Button();
	Button btPrevious = new Button();
	
	public PanelConnectToCluster() {
		super(new GridPane());
		GridPane grid = (GridPane) rootProperty().get();
		lblClusterIpPort.setText("A cluster ip:port");
		lblClusterId.setText("Cluster name");
		lblClusterPwd.setText("Cluster password");
		txtClusterIpPort.setTooltip(new Tooltip(" ip:port where we can find a computer inside the cluster."));
		txtClusterId.setTooltip(new Tooltip("Your cluster name, It's what it's used to identify this new drive"));
		txtClusterPwd.setTooltip(new Tooltip("The passcode that is used to see who is authorized to connect."));
		txtClusterIpPort.setText("127.0.0.1:30400");
		txtClusterId.setText("My 1st cluster drive");
		txtClusterPwd.setText("nøtQWERTYplz");
		btNext.setText("Next");
		btNext.setTooltip(new Tooltip("Ask peer paremeters"));
		btNext.setOnAction((ActionEvent)->{
			//check data TODO
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
			if(!txtClusterIpPort.getText().matches("^[0-9:\\.]+:[0-9][0-9][0-9]?[0-9]?[0-9]?$")) {
				Alert alert = new Alert(Alert.AlertType.WARNING);alert.setTitle("Error");alert.setHeaderText("Error:");
				alert.setContentText("you must have a ip:port well formatted string (example1: 192.168.0.1:300) (example2: ::1:300)");
				alert.showAndWait();
				return;	
			}

			if(chkNewOld.isSelected()) {
				//TODO
			}
			manager.goToPanel(new PanelParameterPeer());
		});
		btPrevious.setText("Previous");
		btPrevious.setTooltip(new Tooltip("Return to choices"));
		btPrevious.setOnAction((ActionEvent)->{
			manager.goToPanel(new PanelChoiceNewConnect());
		});
		btTestConnection.setText("Test cluster connection");
		btTestConnection.setTooltip(new Tooltip("Try to see if this peer is joinable."));
		btTestConnection.setOnAction((ActionEvent)->{
			//TODO: check values
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
			if(!txtClusterIpPort.getText().matches("^[0-9:\\.]+:[0-9][0-9][0-9]?[0-9]?[0-9]?$")) {
				Alert alert = new Alert(Alert.AlertType.WARNING);alert.setTitle("Error");alert.setHeaderText("Error:");
				alert.setContentText("you must have a ip:port well formatted string (example1: 192.168.0.1:300) (example2: ::1:300)");
				alert.showAndWait();
				return;	
			}
			//try to connect with a dummy peer
			//TODO test it.
			// I fear this will create "gosty" peers inside the cluster.
			PhysicalServerDummy servDummy = new PhysicalServerDummy(txtClusterId.getText().hashCode(), txtClusterPwd.getText());
			servDummy.connect(txtClusterIpPort.getText().split(":")[0], Integer.parseInt(txtClusterIpPort.getText().split(":")[1]));
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("Number of acessible peers (in 5s)");
			alert.setHeaderText("Number of acessible peers (in 5s)");
			alert.setContentText("You can connect with these parameters to "+servDummy.getNbPeers()+" peers.");
			alert.showAndWait();
			
		});
		chkNewOld.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)->{
			panelPubPrivKey.setDisable(!newValue);
			txtPrivKey.setEditable(newValue);
			txtPubKey.setEditable(newValue);
		});
		panelPubPrivKey.setDisable(!chkNewOld.isSelected());
		txtPrivKey.setEditable(chkNewOld.isSelected());
		txtPubKey.setEditable(chkNewOld.isSelected());

		grid.setHgap(10);
	    grid.setVgap(10);
	    grid.setPadding(new Insets(3, 10, 2, 10));
	    // Setting columns size
	    ColumnConstraints column = new ColumnConstraints();
	    grid.getColumnConstraints().add(column);
	    panelPubPrivKey.getColumnConstraints().add(column);
	    column = new ColumnConstraints();
	    column.setFillWidth(true);
	    column.setHgrow(Priority.ALWAYS);
	    grid.getColumnConstraints().add(column);
	    grid.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
	    
	    panelPubPrivKey.setHgap(0);
	    panelPubPrivKey.setVgap(0);
	    panelPubPrivKey.setPadding(new Insets(3, 10, 2, 10));
	    panelPubPrivKey.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
	    column = new ColumnConstraints();
	    column.setFillWidth(true);
	    column.setHgrow(Priority.ALWAYS);
	    panelPubPrivKey.getColumnConstraints().add(column);


		grid.add(lblClusterIpPort, 0, 0, 1, 1);
		grid.add(txtClusterIpPort, 1, 0, 3, 1);

		grid.add(lblClusterId, 0, 1, 1, 1);
		grid.add(txtClusterId, 1, 1, 3, 1);

		grid.add(lblClusterPwd, 0, 2, 1, 1);
		grid.add(txtClusterPwd, 1, 2, 3, 1);

		grid.add(lblNewOld, 0, 3, 2, 1);
		grid.add(chkNewOld, 2, 3, 1, 1);
		
		{

			panelPubPrivKey.add(lblPrivKey, 0, 0, 1, 1);
			panelPubPrivKey.add(txtPrivKey, 1, 0, 3, 1);

			panelPubPrivKey.add(lblPubKey, 0, 1, 1, 1);
			panelPubPrivKey.add(txtPubKey, 1, 1, 3, 1);
		}
		grid.add(panelPubPrivKey, 0, 4, 4, 1);

		grid.add(btPrevious, 0, 5, 1, 1);
		grid.add(btNext, 4, 5, 1, 1);
	}
	
	@Override
	public void construct() {
		if(manager.savedData.containsKey("ClusterIpPort")) {
			txtClusterIpPort.setText(manager.savedData.get("ClusterIpPort").toString());
			txtClusterId.setText(manager.savedData.get("ClusterId").toString());
			txtClusterPwd.setText(manager.savedData.get("ClusterPwd").toString());
			if(manager.savedData.containsKey("CreateNewKey")) {
				if((Boolean)(manager.savedData.get("CreateNewKey"))){
					chkNewOld.setSelected(false);
				}else {
					chkNewOld.setSelected(true);
				}
			}
			if(manager.savedData.containsKey("PrivKey")) {
				txtPrivKey.setText(manager.savedData.get("PrivKey").toString());
			}
			if(manager.savedData.containsKey("PubKey")) {
				txtPubKey.setText(manager.savedData.get("PubKey").toString());
			}
		}
	}

	@Override
	public void destroy() {
		manager.savedData.put("ClusterChoice", "connect");
		manager.savedData.put("ClusterIpPort", txtClusterIpPort.getText());
		manager.savedData.put("ClusterId", txtClusterId.getText());
		manager.savedData.put("ClusterPwd", txtClusterPwd.getText());
		if(chkNewOld.isSelected()) {
			manager.savedData.put("CreateNewKey", false);
			manager.savedData.put("PrivKey", txtPrivKey.getText());
			manager.savedData.put("PubKey", txtPubKey.getText());
		}else {
			manager.savedData.put("CreateNewKey", true);
		}
	}

}
