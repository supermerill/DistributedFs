package remi.distributedFS.gui.install;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import remi.distributedFS.log.Logs;

public class PanelChoiceNewConnect extends InstallPanel {
	private static final long serialVersionUID = -4861532843787270076L;

	Button btNew;
	Button btConnect;
	
	public PanelChoiceNewConnect(){
		super(new VBox());
	    VBox vbox = (VBox) rootProperty().get();
	    vbox.setPadding(new Insets(10));
	    vbox.setSpacing(8);
		btNew = new Button("Create new cluster");
		btNew.setOnAction((ActionEvent)->{
			Logs.logGui.info("new cluster");
			manager.goToPanel(new PanelCreateNewCluster());
		});
		vbox.getChildren().add(btNew);
		btConnect = new Button("Connect to a cluster");
		btConnect.setOnAction((ActionEvent)->{
			manager.goToPanel(new PanelConnectToCluster());
		});
		vbox.getChildren().add(btConnect);
	}

	@Override
	public void construct() {
	}

	@Override
	public void destroy() {
	}

}
