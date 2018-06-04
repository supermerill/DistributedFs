package remi.distributedFS.net.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

/**
 * This object is used to test the connection with a cluster.
 * 
 * @author centai
 *
 */
public class PhysicalServerDummy extends PhysicalServer{

	public PhysicalServerDummy(long clusterId, String clusterPwd){
		super();

		myPeerId = new Random().nextLong();
		if (myPeerId <= 0)
			myPeerId = -myPeerId;
		clusterIdMananger = new ServerIdDbDummy(this, clusterId, clusterPwd);
	}
	
	/**
	 * Init a connection with an other (hypothetical) peer.
	 * @param peer peer to try to connect
	 * @param sock our socket to know what to tell him if he want to connect to us.
	 * @return true if i am listening to him at the end, but do not trust this too much.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Override
	protected boolean initConnection(Peer peer, Socket sock) throws InterruptedException, IOException {

		if (peer.connect(sock)) {

			try {
				synchronized (peers) {
					// check if we already have this peer id.
					Peer otherPeer = null;
					for (Peer p : peers) {
						if (p.getPeerId() == peer.getPeerId()) {
							otherPeer = p;
							break;
						}
					}
					if (otherPeer == null) {
						// peers.put(peer.getKey(), peer);
						if (!peers.contains(peer)) {
							// new peer: propagate!
							peers.add(peer);
						}
						peer.startListen();
						return true;
					}
				}
				// TODO: test if id is inside
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			peer.close();
		}
		return false;
	}

	/**
	 * Try to connect to a peer at this address/port
	 * @param ip address
	 * @param port port
	 * @return true if it's maybe connected, false if it's maybe not connected
	 */
	@SuppressWarnings("resource")
	@Override
	public boolean connectTo(final String ip, final int port) {
		System.out.println(getPeerId() % 100 + " want to CONNECT with " + port);
		final InetSocketAddress addr = new InetSocketAddress(ip, port);
		final Socket tempSock = new Socket();
		try {
			tempSock.connect(addr, 200);
			Peer peer = null;
			for (Peer searchPeer : getPeers()) {
				if (searchPeer.getPort() == port && searchPeer.getIP().equals(ip)) {
					peer = searchPeer;
					break;
				}
			}
			if (peer == null) {
				peer = new Peer(this, addr.getAddress(), addr.getPort());
			}
			if (peer != null && !peer.isAlive()) {
				try {
					return initConnection(peer, tempSock);
				} catch (InterruptedException | IOException e) {
					// e.printStackTrace();
					System.err.println(getPeerId() % 100 + " error in initialization : connection close with "
							+ peer.getPeerId() % 100 + " (" + peer.getPort() + ")");
					peer.close();
				}
			} else {
				System.out.println(getPeerId() % 100 + " already CONNECTED with " + port);
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				tempSock.close();
			} catch (IOException e1) {
			}
			throw new RuntimeException(e);
		}
		return false;
	}

}
