package remi.distributedFS.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.net.impl.Peer;
import remi.distributedFS.net.impl.PhysicalServer;

public class PanelPeers extends JPanel implements Runnable {

	private static final long serialVersionUID = 1L;

	javax.swing.JList<String> listPeers;
	private boolean alive = true;
	PhysicalServer net;
	JLabel me;

	public PanelPeers(FileSystemManager manager) {
		net = (PhysicalServer) manager.getNet();
		listPeers = new JList<>();
		listPeers.setModel(new PeerModel());
		Border visibleBorder = BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED), "other peers");
		listPeers.setBorder(visibleBorder);
		me = new JLabel();

		dolayout();

		new Thread(this).start();

	}

	protected void dolayout(){
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(layout);

		c.gridx=0;
		c.gridy=0;
		c.gridheight=1;
		c.gridwidth=1;
//		JButton test = new JButton("3");
		layout.setConstraints(me, c);
		add(me);
		c.gridy++;
		layout.setConstraints(listPeers, c);
		add(listPeers);
//		c.gridy=2;
//		test = new JButton("4");
//		layout.setConstraints(test, c);
//		add(test);
//		
//		test.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				JOptionPane.showMessageDialog(PanelPeers.this, "msg: "+net.getPeers().size());
//			}
//		});
		
	}

	@Override
	public void run() {
		while (alive) {
			try {
				Thread.sleep(1100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			((PeerModel)listPeers.getModel()).sync(net.getPeers());
			// update data
			// ListModel<Peer> model = listPeers.setCellRenderer();
			// remove the ones that are not available
			// add new ones
			for (Peer p : net.getPeers()) {

			}
		}

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

	public class PeerModel implements ListModel<String> {

		private List<Peer> lst = new ArrayList<>();
		private List<WeakReference<ListDataListener>> listeners = new ArrayList<>();

		public void sync(Collection<Peer> lstPeer) {
			
			me.setText("my port:"+net.getListenPort()+", my id: "+(net.getId()%1000));
			
			ArrayList<Peer> toRemove = new ArrayList<>();
			toRemove.addAll(lst);
			toRemove.removeAll(lstPeer);
			ArrayList<Peer> toAdd = new ArrayList<>();
			toRemove.addAll(lstPeer);
			toRemove.removeAll(lst);

			int oldSize = lst.size();
			lst = new ArrayList<>(lstPeer);
			for (WeakReference<ListDataListener> li : listeners) {
				if (li.get() != null) {
					if (oldSize > 0)
						li.get().intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, oldSize));
					if (lst.size() > 0)
						li.get().intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, lst.size()));
				}
			}
		}

		@Override
		public int getSize() {
			return lst.size();
		}

		@Override
		public String getElementAt(int index) {
			Peer p = lst.get(index);

			// format data
			StringBuilder str = new StringBuilder();

			str.append(p.getIP()).append(":").append(p.getPort()).append(" , id:").append(p.getConnectionId()%1000);

			return str.toString();
		}

		@Override
		public void addListDataListener(ListDataListener l) {
			synchronized (listeners) {
				listeners.add(new WeakReference<ListDataListener>(l));
			}
		}

		@Override
		public void removeListDataListener(ListDataListener l) {
			synchronized (listeners) {
				Iterator<WeakReference<ListDataListener>> it = listeners.iterator();
				while (it.hasNext()) {
					WeakReference<ListDataListener> listener = it.next();
					if (listener.get() == null) {
						it.remove();
					} else if (listener.get() == l) {
						it.remove();
					}
				}
			}
		}

	}

}
