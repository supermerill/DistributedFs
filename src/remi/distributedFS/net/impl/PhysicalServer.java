package remi.distributedFS.net.impl;

import java.awt.TrayIcon.MessageType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.net.AbstractMessageManager;
import remi.distributedFS.net.ClusterManager;
import remi.distributedFS.net.impl.Peer.PeerConnectionState;
import remi.distributedFS.util.ByteBuff;


/**
 * 
 * ids: <br>
 * * clusterId (long) -> id of the cluster, ie the virtual drive. It's public<br>
 * * peerId (long)(serverId/connectionId/senderId) -> id of the program that is ending and recieving stuff ont he network. it's the working id, to identify the current connection.<br>
 * * computerId (short) -> id of an instance of the network. it's a verified public/private AES verifiedaprotected id to identify a peer/computer that can grab data and maybe make modifications.<br> 
 * 
 * <p>
 * note: this part is made from memory 1 year after coding, so take it with a grain of salt<br>
 * When connecting, we verify the clusterId, it's the public id for the network, and so the virtual drive.<br>
 * then the newly connected receive the list of conencted peers on the network and ask all of them their list of peers.<br>
 * With this list, he can now choose his peerId.<br>
 * If there are a peerId collision, this is resolved. note: maybe we should add protection for already-connected peers? (if same id as a connected peer -> disconnect it brutally)
 * Then we need to create a secure connectino, so we exchange public keys.<br>
 * To verify, we send a short message back but public-encrytped.<br>
 * Then, we can speak in RSA.<br>
 * We exchange our computerId.<br>
 * We verify if the publickey/computerID pair is ok, if we already seen the computer.<br>
 * 
 * 
 * TODO: add cluster priv/pub key to be sure if somone has created an other cluster with the same id/password, he can't connect to our.
 * 
 * 
 * @author centai
 *
 */
public class PhysicalServer implements ClusterManager {

	protected long myPeerId;

	protected ServerConnectionState myInformalState = ServerConnectionState.JUST_BORN;
	enum ServerConnectionState{
		JUST_BORN,
		CONNECTING,
		CONNECTED,
		
		SOLO,
		HAS_FRIENDS;
	}
	
	// should be inetAddress ut it's implier with this to test in 1 pc.
	// private Map<PeerKey ,Peer> peers = new HashMap<>();
	// private Semaphore peersSemaphore = new Semaphore(1);
	protected PeerList peers = new PeerList(); // don't access this outside of semaphore. Use getters instead.
	protected ServerSocket mySocket;
	protected Thread socketListener = null;
	protected Thread updaterThread = null;

	protected FileSystemManager myFs = null;
	protected ConnectionMessageManager messageManager = null;

	protected List<AbstractMessageManager>[] listeners;

	protected String jarFolder = "./jars";

	protected long lastDirUpdate = 0;

	protected ServerIdDb clusterIdMananger;

	 @SuppressWarnings("unchecked")
	protected PhysicalServer() {
			listeners = new List[256];
			messageManager = new ConnectionMessageManager(this);
	 }

	@SuppressWarnings("unchecked")
	public PhysicalServer(FileSystemManager fs, boolean update, String folderPath) {
		myPeerId = new Random().nextLong();
		if (myPeerId <= 0)
			myPeerId = -myPeerId;
		this.myFs = fs;
		listeners = new List[256];
		clusterIdMananger = new ServerIdDb(this, folderPath + "/clusterIds");
		clusterIdMananger.load();
		messageManager = new ConnectionMessageManager(this);
		if (update) {
			launchUpdater();
		}
	}

	public void launchUpdater() {
		if (updaterThread == null) {
			updaterThread = new Thread(() -> {
				while (true) {
					update();
				}
			});
			updaterThread.start();
		}
	}

	public void update() {
		boolean quickUpdate = false;
		while (getServerIdDb().myComputerId < 0) {
			try {
				if(quickUpdate){
					Thread.sleep(500);
				}else{
					Thread.sleep(5000);
				}
				quickUpdate = false;
				// in case of something went wrong, recheck.

				if (getServerIdDb().getClusterId() > 0) {
					chooseComputerId();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//clean peer list
		synchronized (peers) {
			Iterator<Peer> itPeers = peers.iterator();
			while(itPeers.hasNext()){
				Peer nextP = itPeers.next();
				if(nextP.getComputerId() == this.getComputerId()){
					System.err.println("Error: a peer is me (same computerid) "+this.getComputerId());
					getServerIdDb().loadedPeers.remove(nextP);
					getServerIdDb().id2PublicKey.remove(this.getComputerId());
					nextP.close();
					itPeers.remove();
				}else if(nextP.getKey().getPeerId() == this.getPeerId()){
					System.err.println("Error: a peer is me (same peerId) "+this.getPeerId());
					getServerIdDb().loadedPeers.remove(nextP);
					getServerIdDb().id2PublicKey.remove(this.getComputerId());
					nextP.close();
					itPeers.remove();
				}
			}
		}
		
		System.out.println(getPeerId() % 100 + " update " + myFs.getDrivePath());
		for (Peer peer : getPeers()) {
			System.out.println(getPeerId() % 100 + " update peer " + peer.getPeerId() % 100);
			//if we emit something to someone, wait a bit more for the next update.
			quickUpdate = quickUpdate || peer.ping();
		}
		// toutes les heures
		if (lastDirUpdate + 1000 * 60 * 60 < System.currentTimeMillis()) {
			System.out.println(getPeerId() % 100 + " REQUEST DIR UPDATE");
			myFs.requestDirUpdate();
			lastDirUpdate = System.currentTimeMillis();
		}

	}

	public long getPeerId() {
		return myPeerId;
	}

	public short getComputerId() {
		return getServerIdDb().myComputerId;
	}

	public void listen(final int port) {
		if (socketListener == null) {
			try {
				mySocket = new ServerSocket(port);
				socketListener = new Thread(() -> {
					try {
						while (true) {
							// System.err.println("wait a connect...");
							final Socket connexion = mySocket.accept();
							// System.err.println("connected to a socketserver");
							Peer peer = new Peer(this, connexion.getInetAddress(), 0);
							final Peer argPeer = peer;
							new Thread(() -> {
								try {
									if(initConnection(argPeer, connexion)) {
										if(myInformalState == ServerConnectionState.SOLO) {
											myInformalState = ServerConnectionState.HAS_FRIENDS;
										}
									}
								} catch (InterruptedException | IOException e) {
									e.printStackTrace();
								}
							}).start();
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					socketListener = null;
				});
				socketListener.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Init a connection with an other (hypothetical) peer.
	 * @param peer peer to try to connect
	 * @param sock our socket to know what to tell him if he want to connect to us.
	 * @return true if i am listening to him at the end, but do not trust this too much.
	 * @throws InterruptedException
	 * @throws IOException
	 */
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
						System.out.println(getPeerId() % 100 + " 'new' accept connection to "
								+ peer.getKey().getPeerId() % 100);
						// peers.put(peer.getKey(), peer);
						if (!peers.contains(peer)) {
							System.err.println(
									getPeerId() % 100 + " 'warn' PROPAGATE " + peer.getPeerId() % 100 + "!");
							// new peer: propagate!
							peers.add(peer);
							for (Peer oldPeer : peers) {
								if(oldPeer.isAlive()){
									System.err.println(
											getPeerId() % 100 + " 'warn' PROPAGATE to " + oldPeer.getPeerId() % 100);
									// MyServerList.get().write(peers, oldPeer);
									messageManager.sendServerList(oldPeer.getPeerId(), peers);
									oldPeer.flush();
								}
							}
						} else {
							peers.add(peer);
						}
						peer.startListen();
						return true;
					} else {
						if (otherPeer.isAlive() && !peer.isAlive()) {
							System.err.println(getPeerId() % 100 + " 'warn' , close a new dead connection to "
									+ peer.getKey().getPeerId() % 100 + " is already here.....");
							peer.close();
						} else if (!otherPeer.isAlive() && peer.isAlive()) {
							System.err.println(getPeerId() % 100 + " warn,  close an old dead  a connection to "
									+ peer.getKey().getPeerId() % 100 + " is already here.....");
							otherPeer.close();
							// peers.put(peer.getKey(), peer);
							peers.add(peer);
							peer.startListen();
							return true;
						} else if (otherPeer.getKey().getPeerId() != 0) {
							if (otherPeer.getKey().getPeerId() < getPeerId()) {
								System.err.println(getPeerId() % 100 + " warn, (I AM LEADER) a connection to "
										+ peer.getKey().getPeerId() % 100 + " is already here.....");
								peer.close();
							} else {
								System.err.println(getPeerId() % 100 + " warn, (I am not leader) a connection to "
										+ peer.getKey().getPeerId() % 100 + " is already here.....");
								peers.add(peer);
								peer.startListen();
								return true;
							}
						} else if (peer.getKey().getPeerId() != 0) {
							if (peer.getKey().getPeerId() < getPeerId()) {
								System.err.println(getPeerId() % 100 + " warn, (I AM LEADER) a connection to "
										+ peer.getKey().getPeerId() % 100 + " is already here.....");
								otherPeer.close();
								// peers.put(peer.getKey(), peer);
								peers.add(peer);
								peer.startListen();
								return true;
							} else {
								System.err.println(getPeerId() % 100 + " warn, (I am not leader) a connection to "
										+ peer.getKey().getPeerId() % 100 + " is already here.....");
								peers.add(peer);
								peer.startListen();
								return true;
							}
						} else {
							System.err.println(getPeerId() % 100 + " warn, an unknown connection to "
									+ peer.getKey().getPeerId() % 100 + " is already here.....");
							peer.close();
						}
						System.out.println(getPeerId() % 100 + " 'old' accept connection to "
								+ peer.getKey().getPeerId() % 100);
					}
				}

				// test if id is ok
				if (peer.getPeerId() == getPeerId()) {
					System.err.println(getPeerId() % 100 + " my peerid is equal to  "
							+ peer.getKey().getPeerId() % 100);
					return rechooseId();
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
	@SuppressWarnings("resource") // because it's managed by the peer object, but created in this function.
	public boolean connectTo(final String ip, final int port) {
		// check if it's not me
		if (mySocket.getLocalPort() == port
				&& (mySocket.getInetAddress().getHostAddress().equals(ip) || ip.equals("127.0.0.1"))) {
			System.out.println("DON't connect TO ME MYSELF");
			return false;
		} else {
			// System.out.println(mySocket.getLocalPort()+" =?= "+port);
			// System.out.println(mySocket.getInetAddress().getHostAddress()+" =?= "+ip);
			// System.out.println(mySocket.getInetAddress().getHostName()+" =?= "+ip);
		}

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
			// Peer peer = peers.get(new PeerKey(addr.getAddress(), addr.getPort()));
			if (peer == null) {
				peer = new Peer(this, addr.getAddress(), addr.getPort());
			}
			if (peer != null && !peer.isAlive()) {
				// new Thread(()->{
				try {
					if(myInformalState==ServerConnectionState.JUST_BORN) myInformalState = ServerConnectionState.CONNECTING;
					if( initConnection(peer, tempSock) ) {
						if(myInformalState==ServerConnectionState.CONNECTING) myInformalState = ServerConnectionState.CONNECTED;
						return true;
					}else {
						return false;
					}
				} catch (InterruptedException | IOException e) {
					// e.printStackTrace();
					System.err.println(getPeerId() % 100 + " error in initialization : connection close with "
							+ peer.getPeerId() % 100 + " (" + peer.getPort() + ")");
					peer.close();
				}
				// }).start();
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

	public PeerList getPeers() {
		synchronized (peers) {
			PeerList list = new PeerList(peers);
			return list;
		}
	}

	public int getListenPort() {
		return mySocket.getLocalPort();
	}

	public void removeExactPeer(Peer peer) {
		synchronized (peers) {
			java.util.ListIterator<Peer> lit = peers.listIterator();
			while (lit.hasNext()) {
				if (lit.next() == peer) {
					lit.remove();
				}
			}
		}
	}

	public File getJar(String name) {
		// list available files
		// java.io.File dir = new File(jarFolder);
		// File finded = null;
		// for(File f : dir.listFiles()){
		// if(f.getName().equals(name+".jar")){
		// finded = f;
		// break;
		// }
		// }

		return new File(jarFolder + "/" + name + ".jar");
	}

	public File getOrCreateJar(String name) {
		// list available files
		java.io.File file = getJar(name);
		if (file == null) {
			file = new File(jarFolder + "/" + name + ".jar");
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return file;
	}

	@SuppressWarnings("rawtypes")
	public void runAlgo(String jarName, String className) {
		// TODO: maybe use https://github.com/kamranzafar/JCL to isolate each run
		URLClassLoader child;
		try {
			child = new URLClassLoader(new URL[] { getJar(jarName).toURI().toURL() }, this.getClass().getClassLoader());

			Class classToLoad = Class.forName(className, true, child);
			Object instance = classToLoad.newInstance();
			@SuppressWarnings("unchecked")
			Method method = classToLoad.getDeclaredMethod("run", PhysicalServer.class);
			/* Object result = */ method.invoke(instance, this);
		} catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| InstantiationException e) {
			e.printStackTrace();
		}

	}

	public FileSystemManager getFileSystem() {
		return myFs;
	}

	public boolean rechooseId() {
		System.out.println("ERROR: i have to choose an other id....");
		// change id
		myPeerId = new Random().nextLong();
		if (myPeerId <= 0)
			myPeerId = -myPeerId;
		// reconnect all connections
		ArrayList<Peer> oldConnection = null;
		synchronized (peers) {
			oldConnection = new ArrayList<>(peers);
			peers.clear();
		}
		for (Peer oldp : oldConnection) {
			oldp.close();
			System.out.println("RECONNECT " + oldp.getIP() + " : " + oldp.getPort());
			return connectTo(oldp.getIP(), oldp.getPort());
		}
		return false;
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected void writeEmptyMessage(byte messageId, OutputStream out) {
		try {
			out.write(5);
			out.write(5);
			out.write(messageId);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	protected void writeMessage(byte messageId, OutputStream out, ByteBuff message) {
	}

	protected ByteBuff readMessage(InputStream in) {
		try {
			ByteBuff buffIn = new ByteBuff(4);
			in.read(buffIn.array(), 0, 4);
			int nbBytes = buffIn.getInt();
			buffIn.limit(nbBytes).rewind();
			in.read(buffIn.array(), 0, nbBytes);
			return buffIn;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void requestUpdate(long since) {
		// TODO Auto-generated method stub

	}

	@Override
	public void requestChunk(long fileId, long chunkId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void propagateDirectoryChange(long directoryId, byte[] changes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void propagateFileChange(long directoryId, byte[] changes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void propagateChunkChange(long directoryId, byte[] changes) {
		// TODO Auto-generated method stub

	}

	@Override
	public int writeBroadcastMessage(byte messageId, ByteBuff message) {
		int nbEmit = 0;
		synchronized (this.clusterIdMananger.getRegisteredPeers()) {
			for (Peer peer : this.clusterIdMananger.getRegisteredPeers()) {
				if(peer != null && peer.isAlive()){
					System.out.println("write msg "+messageId+" to "+peer.getKey().getPeerId()%100);
					writeMessage(peer, messageId, message);
					nbEmit ++;
				}else{
					System.out.println("peer "+peer.getKey().getPeerId()%100+" is not alive, can't send msg");
				}
			}
		}
		return nbEmit;
	}

	@Override
	public boolean writeMessage(long senderId, byte messageId, ByteBuff message) {
		Peer p = getPeer(senderId);
		if(p != null){
			writeMessage(p, messageId, message);
			return true;
		}else{
			return false;
		}
	}

	public void writeMessage(Peer p, byte messageId, ByteBuff message) {
		p.writeMessage(messageId, message);
	}

	public Peer getPeer(long senderId) {
		Peer p = null;
		synchronized (peers) {
			for (Peer peer : peers) {
				if (peer.getPeerId() == senderId) {
					p = peer;
					break;
				}
			}
		}
		return p;
	}

	@Override
	public void registerListener(byte messageId, AbstractMessageManager listener) {
		List<AbstractMessageManager> lst = listeners[messageId];
		if (lst == null) {
			lst = new ArrayList<>();
			listeners[messageId] = lst;
		}
		lst.add(listener);
	}

	public void propagateMessage(long senderId, byte messageId, ByteBuff message) {
		List<AbstractMessageManager> lst = listeners[messageId];
		if (lst != null) {
			for (AbstractMessageManager listener : lst) {
				listener.receiveMessage(senderId, messageId, message);
			}
		}
	}

	public ConnectionMessageManager message() {
		return messageManager;
	}

	@Override
	public void init(int listenPort) {
		this.listen(listenPort);
	}

	@Override
	public boolean connect(String path, int port) {
		return this.connectTo(path, port);
	}

	public ServerIdDb getServerIdDb() {
		return clusterIdMananger;
	}

	public void chooseComputerId() {
		System.out.println(getPeerId() % 100 + " chooseComputerId");
		// do not run this method in multi-thread (i use getServerIdDb() because i'm the only one to use it, to avoid possible dead sync issues)
		synchronized (getServerIdDb()) {

			// check if we have a clusterId
			System.out.println(getPeerId() % 100 + " getServerIdDb().myId=" + getServerIdDb().myComputerId);
			if (getServerIdDb().myComputerId < 0) {
				//first, wait a bit to let us contact everyone.
				List<Peer> lstpeerCanConnect = null;
				synchronized (peers) {
					lstpeerCanConnect = new ArrayList<>(peers);
				}
				System.out.println(getPeerId() % 100 + " lstpeerCanConnect.size=" + lstpeerCanConnect.size());
				System.out.println( getPeerId() % 100 + " receivedServerList=" + getServerIdDb().receivedServerList);
				for (Peer okP : getServerIdDb().receivedServerList) {
					if (lstpeerCanConnect.contains(okP)) {
						lstpeerCanConnect.remove(okP);
					}
				}
				System.out.println(getPeerId() % 100 + " lstpeerCanConnect.restsize=" + lstpeerCanConnect.size());
				if (lstpeerCanConnect.isEmpty()) {
					// ok, i am connected with everyone i can access and i have received every serverlist.

					// choose a random one
					Random rand = new Random();
					short choosenId = (short) rand.nextInt(Short.MAX_VALUE);
					if (choosenId < 0){
						System.out.println(getPeerId() % 100 + " choosenId " + choosenId+" < 0");
						choosenId = (short) -choosenId;
						System.out.println(getPeerId() % 100 + " now choosenId " + choosenId+" > 0 !!");
					}
					// while it's not already taken
					while (getServerIdDb().isChoosen(choosenId)) {
						System.out.println(getPeerId() % 100 + " ClusterId " + choosenId
								+ " is already taken, i will choose a new one");
						choosenId = (short) rand.nextInt(Short.MAX_VALUE);
						if (choosenId < 0){
							System.out.println(getPeerId() % 100 + " choosenId " + choosenId+" < 0");
							choosenId = (short) -choosenId;
							System.out.println(getPeerId() % 100 + " now choosenId " + choosenId+" > 0 !!");
						}
					}

					System.out.println(getPeerId() % 100 + " CHOOSE A NEW COMPUTER ID is =" + choosenId);
					getServerIdDb().myComputerId = choosenId;
					// emit this to everyone
					// writeBroadcastMessage(AbstractMessageManager.GET_SERVER_PUBLIC_KEY, new ByteBuff());
					synchronized (peers) {
						System.out.println(getPeerId() % 100 + " want to send it to " + peers.size());
						for (Peer peer : peers) {
							System.out.println(getPeerId() % 100 + " want to send it to peers " + peer.getPeerId()%100 + "  "+peer.getComputerId());
							// as we can't emit directly (no message to encode), a request to them should trigger a request from them.
							getServerIdDb().requestPublicKey(peer);
						}
					}
				}

			} else {
				// yes
				// there are a conflict?
				
				if (getServerIdDb().id2PublicKey.get(getServerIdDb().myComputerId) != null && !Arrays.equals(getServerIdDb().id2PublicKey.get(getServerIdDb().myComputerId).getEncoded(), getServerIdDb().publicKey.getEncoded())) {
					try{
						System.out.println("id is already there, with my id : "+getServerIdDb().myComputerId);
						System.out.println(", my pub key : "+getServerIdDb().publicKey);
						System.out.println(",  their: "+getServerIdDb().id2PublicKey.get(getServerIdDb().myComputerId));
						System.out.println(", my pub key : "+Arrays.toString(getServerIdDb().publicKey.getEncoded()));
						System.out.println(",  their: "+Arrays.toString(getServerIdDb().id2PublicKey.get(getServerIdDb().myComputerId).getEncoded()));
					}catch(Exception e){
						e.printStackTrace();
					}
					// yes
					// i choose my id recently?
					if (System.currentTimeMillis() - getServerIdDb().timeChooseId < 10000) {
						// change!
						// how: i have to reset all my connections?
						// getServerIdDb().timeChooseId
						System.err.println(getPeerId() % 100
								+ " ERROR! my ClusterId is already taken. Please destroy this instance/peer/server and create an other one.");
						throw new RuntimeException(
								"ERROR! my ClusterId is already taken. Please destroy this instance/peer/server and create an other one.");
					} else {
						System.err.println(getPeerId() % 100
								+ " Error, an other server/instance has picked the same ClusterId as me. Both of us should be destroyed and re-created, at a time when we can communicate with each other.");
					}
				} else {
					// no
					// nothing todo do ! everything is ok!!
					System.out.println("ClusterId : everything ok, nothing to do");
				}

			}
		}

	}

	@Override
	public void initializeNewCluster() {
		myInformalState = ServerConnectionState.SOLO;
		
		System.out.println(getPeerId() % 100 + " CHOOSE A NEW COMPUTER ID to 1 ");
		getServerIdDb().myComputerId = 1;
		if (getServerIdDb().getClusterId() == -1)
			getServerIdDb().newClusterId();
		System.out.println(getPeerId() % 100 + " creation of cluster " + getServerIdDb().getClusterId());
		getServerIdDb().requestSave();
	}

	@Override
	public short getComputerId(long senderId) {
		if(senderId == this.myPeerId) return getComputerId();
		Peer p = getPeer(senderId);
		if(p != null && p.hasState(PeerConnectionState.CONNECTED_W_AES)){
			return p.getComputerId();
		}
		return -1;
	}

	@Override
	public long getPeerId(short compId) {
		if(this.getComputerId() == compId) return this.myPeerId;
		for(Peer p : peers){
			if(p.isAlive() && p.getComputerId() == compId){
				return p.getPeerId();
			}
		}
		return -1;
	}

	@Override
	public int connect() {
		int success = 0;
		for(Peer falsePeer : getServerIdDb().loadedPeers) {
			try {
				if(this.connect(falsePeer.getIP(), falsePeer.getPort())) {
					success++;
				}
			}catch(Exception e) {
				System.out.println("can't connect to "+falsePeer.getIP()+" : "+falsePeer.getPort());
			}
		}
		return success;
	}

	@Override
	public int getNbPeers() {
		return (int) peers.stream().filter(p->p.isAlive()&& p.hasState(PeerConnectionState.CONNECTED_W_AES)).count();
	}

	@Override
	public InetSocketAddress getListening() {
		return (InetSocketAddress) mySocket.getLocalSocketAddress();
	}

	public ServerConnectionState getState() {
		return myInformalState;
	}

	@Override
	public void close() {
		synchronized(peers) {
			for(Peer peer : peers) {
				try {
					peer.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				if(mySocket != null)
					mySocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
