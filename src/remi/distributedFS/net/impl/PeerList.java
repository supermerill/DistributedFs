package remi.distributedFS.net.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PeerList extends ArrayList<Peer> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	
	public PeerList() {
		super();
	}

	public PeerList(Collection<Peer> c) {
		super(c);
	}

	public PeerList(int initialCapacity) {
		super(initialCapacity);
	}
	
	public Peer get(Peer other){
		for(Peer e : this){
			if(e.getConnectionId()==other.getConnectionId()){
				return e;
			}
		}
		return null;
	}
	public List<Peer> getAll(Peer other){
		ArrayList<Peer> list = new ArrayList<>();
		for(Peer e : this){
			if(e.getConnectionId()==other.getConnectionId()){
				list.add(e);
			}
		}
		return list;
	}
	
}
