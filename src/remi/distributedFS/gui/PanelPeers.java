package remi.distributedFS.gui;

import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.collections.ObservableListWrapper;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.net.impl.Peer;
import remi.distributedFS.net.impl.PhysicalServer;

@SuppressWarnings("restriction")
public class PanelPeers extends GridPane implements Runnable {

	//TODO: add me (my info and my listening port)
	//TOOD change the list to a table
	ListView<String> listPeers;
	ObservableListWrapper<String> peersString = new ObservableListWrapper<String>(new ArrayList<String>());
	private boolean alive = true;
	PhysicalServer net;
	Label lbl;
	Label lbl_myInfo;
	protected FileSystemManager manager;

	public PanelPeers() {
		
		listPeers = new ListView<>();
		listPeers.setItems(peersString);
		lbl = new Label("List of connected peers");

		lbl_myInfo = new Label("");
		
		dolayout();


	}
	
	public void startListen(FileSystemManager manager) {
		this.manager = manager;

		net = (PhysicalServer) manager.getNet();
		new Thread(this).start();

		StringBuilder strB = new StringBuilder();
		int pos = 0;
		pos = addItem(strB, net.getListening().getHostString(), pos, 15);
		pos = addItem(strB, ""+net.getListening().getPort(), pos, 15+6);
		pos = addItem(strB, ""+net.getComputerId(), pos, 15+6+10);
		pos = addItem(strB, ""+net.getPeerId(), pos, 15+6+10+20);
		pos = addItem(strB, ""+net.getState(), pos, 15+6+10+20+30);
		lbl_myInfo.setText(strB.toString());
	}

	protected void dolayout(){
		GridPane grid = this;

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

		

		grid.add(lbl_myInfo, 0, 0, 1, 1);
		grid.add(lbl, 0, 1, 1, 1);
		grid.add(listPeers, 0, 2, 1, 1);
		PanelRequest.setConstraints(listPeers, 0, 2, 1, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, Insets.EMPTY);
		
	}

	@Override
	public void run() {
		while (alive) {
			try {
				Thread.sleep(1100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			List<String> lstStr = new ArrayList<>();
			
//			System.out.println("update peers list");
			// update data
			// ListModel<Peer> model = listPeers.setCellRenderer();
			// remove the ones that are not available
			// add new ones
			for (Peer p : net.getPeers()) {
				StringBuilder strB = new StringBuilder();
				int pos = 0;
				pos = addItem(strB, p.getIP(), pos, 15);
				pos = addItem(strB, ""+p.getPort(), pos, 15+6);
				pos = addItem(strB, ""+p.getComputerId(), pos, 15+6+10);
				pos = addItem(strB, ""+p.getPeerId(), pos, 15+6+10+20);
				pos = addItem(strB, ""+p.getState(), pos, 15+6+10+20+30);
				pos = addItem(strB, ""+p.isAlive(), pos, 15+6+10+20+30+10);
				lstStr.add(strB.toString());
//				System.out.println(strB);
			}
			lstStr.sort((s1,s2)->s1.compareTo(s2));

			StringBuilder strB = new StringBuilder();
			int pos = 0;
			pos = addItem(strB, " IP", pos, 15);
			pos = addItem(strB, " PORT ", pos, 15+6);
			pos = addItem(strB, " Computer Id ", pos, 15+6+10);
			pos = addItem(strB, " Connection Id ", pos, 15+6+10+20);
			pos = addItem(strB, " state ", pos, 15+6+10+20+30);
			pos = addItem(strB, " alive ", pos, 15+6+10+20+30+10);
			lstStr.add(0, strB.toString());
			Platform.runLater(()->peersString.setAll(lstStr));
		}

	}

	public static int addItem(StringBuilder str, String toAdd, int deb, int max) {
		str.append(toAdd);
		for(int i=deb+toAdd.length(); i<max;i++) {
			str.append(' ');
		}
		return Math.max(max, str.length());
	}

}
