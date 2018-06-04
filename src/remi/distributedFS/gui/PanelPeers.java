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
			
			System.out.println("update peers list");
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
				lstStr.add(strB.toString());
				System.out.println(strB);
			}
			lstStr.sort((s1,s2)->s1.compareTo(s2));

			StringBuilder strB = new StringBuilder();
			int pos = 0;
			pos = addItem(strB, " IP", pos, 15);
			pos = addItem(strB, " PORT ", pos, 15+6);
			pos = addItem(strB, " Computer Id ", pos, 15+6+10);
			pos = addItem(strB, " Connection Id ", pos, 15+6+10+20);
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

	// class MyRenderer implements ListCellRenderer<Peer>{
	//
	// @Override
	// public Component getListCellRendererComponent(JList<? extends Peer> list,
	// Peer value, int index,
	// boolean isSelected, boolean cellHasFocus) {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// }

//	public class PeerModel extends ArrayList<String> implements ObservableList<String> {
//
////		private List<Peer> lst = new ArrayList<>();
//		private List<WeakReference<InvalidationListener>> inv_listeners = new ArrayList<>();
//		private List<WeakReference<ListChangeListener<? super String>>> listeners = new ArrayList<>();
//
//		@Override
//		public void addListener(InvalidationListener listener) {
//			synchronized (inv_listeners) {
//				inv_listeners.add(new WeakReference<InvalidationListener>(listener));
//			}
//		}
//
//		@Override
//		public void removeListener(InvalidationListener listener) {
//			synchronized (inv_listeners) {
//				Iterator<WeakReference<InvalidationListener>> it = inv_listeners.iterator();
//				while (it.hasNext()) {
//					WeakReference<InvalidationListener> listenerCur = it.next();
//					if (listenerCur.get() == null) {
//						it.remove();
//					} else if (listenerCur.get() == listener) {
//						it.remove();
//					}
//				}
//			}
//		}
//
//		@Override
//		public void addListener(ListChangeListener<? super String> listener) {
//			synchronized (listeners) {
//				listeners.add(new WeakReference<ListChangeListener<? super String>>(listener));
//			}
//		}
//
//		@Override
//		public void removeListener(ListChangeListener<? super String> listener) {
//			synchronized (listeners) {
//				Iterator<WeakReference<ListChangeListener<? super String>>> it = listeners.iterator();
//				while (it.hasNext()) {
//					WeakReference<ListChangeListener<? super String>> listenerCur = it.next();
//					if (listenerCur.get() == null) {
//						it.remove();
//					} else if (listenerCur.get() == listener) {
//						it.remove();
//					}
//				}
//			}
//		}
//
//		@Override
//		public boolean addAll(String... elements) {
//			return this.addAll(Arrays.asList(elements));
//		}
//
//		@Override
//		public boolean setAll(String... elements) {
//			this.clear();
//			return this.addAll(Arrays.asList(elements));
//		}
//
//		@Override
//		public boolean setAll(Collection<? extends String> col) {
//			this.clear();
//			return addAll(col);
//		}
//
//		@Override
//		public boolean removeAll(String... elements) {
//			return this.removeAll(Arrays.asList(elements));
//		}
//
//		@Override
//		public boolean retainAll(String... elements) {
//			return this.retainAll(Arrays.asList(elements));
//		}
//
//		@Override
//		public void remove(int from, int to) {
//			this.remove(from, to);
//		}
//
//		public void sync(Collection<Peer> lstPeer) {
//			
//			me.setText("my port:"+net.getListenPort()+", my id: "+(net.getId()%1000));
//			
//			ArrayList<Peer> toRemove = new ArrayList<>();
//			toRemove.addAll(this);
//			toRemove.removeAll(lstPeer);
//			ArrayList<Peer> toAdd = new ArrayList<>();
//			toRemove.addAll(lstPeer);
//			toRemove.removeAll(this);
//
//			int oldSize = this.size();
//			lst = new ArrayList<>(lstPeer);
//			for (WeakReference<ListChangeListener<? super String>> li : listeners) {
//				if (li.get() != null) {
//					if (oldSize > 0) {
//						ListChangeListener.Change<String> change = new ListChangeListener.Change<String>() {
//
//							@Override
//							public boolean next() {
//								// TODO Auto-generated method stub
//								return false;
//							}
//
//							@Override
//							public void reset() {
//								// TODO Auto-generated method stub
//								
//							}
//
//							@Override
//							public int getFrom() {
//								// TODO Auto-generated method stub
//								return 0;
//							}
//
//							@Override
//							public int getTo() {
//								// TODO Auto-generated method stub
//								return 0;
//							}
//
//							@Override
//							public List<String> getRemoved() {
//								// TODO Auto-generated method stub
//								return null;
//							}
//
//							@Override
//							protected int[] getPermutation() {
//								// TODO Auto-generated method stub
//								return null;
//							}
//							
//						}
//						li.get().onChanged(change);
//					}
//						
//					intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, oldSize));
//					if (this.size() > 0)
//						li.get().intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, lst.size()));
//				}
//			}
//		}
////
////		@Override
////		public int getSize() {
////			return lst.size();
////		}
////
////		@Override
////		public String getElementAt(int index) {
////			Peer p = lst.get(index);
////
////			// format data
////			StringBuilder str = new StringBuilder();
////
////			str.append(p.getIP()).append(":").append(p.getPort()).append(" , id:").append(p.getConnectionId()%1000);
////
////			return str.toString();
////		}
////
////		@Override
////		public void addListDataListener(ListDataListener l) {
////			synchronized (listeners) {
////				listeners.add(new WeakReference<ListDataListener>(l));
////			}
////		}
////
////		@Override
////		public void removeListDataListener(ListDataListener l) {
////			synchronized (listeners) {
////				Iterator<WeakReference<ListDataListener>> it = listeners.iterator();
////				while (it.hasNext()) {
////					WeakReference<ListDataListener> listener = it.next();
////					if (listener.get() == null) {
////						it.remove();
////					} else if (listener.get() == l) {
////						it.remove();
////					}
////				}
////			}
////		}
//
//	}
//	
//	public static class PeerListChangeListener extends ListChangeListener.Change<String>{
//
//		public PeerListChangeListener(ObservableList<String> list) {
//			super(list);
//		}
//
//		@Override
//		public boolean next() {
//			// TODO Auto-generated method stub
//			return false;
//		}
//
//		@Override
//		public void reset() {
//			// TODO Auto-generated method stub
//			
//		}
//
//		@Override
//		public int getFrom() {
//			// TODO Auto-generated method stub
//			return 0;
//		}
//
//		@Override
//		public int getTo() {
//			// TODO Auto-generated method stub
//			return 0;
//		}
//
//		@Override
//		public List<String> getRemoved() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		protected int[] getPermutation() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//		
//	}

}
