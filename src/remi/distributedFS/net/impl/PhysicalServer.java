package remi.distributedFS.net.impl;

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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.net.AbstractMessageManager;
import remi.distributedFS.net.ClusterManager;
import remi.distributedFS.util.ByteBuff;

public class PhysicalServer implements ClusterManager {

	private long id = new Random().nextLong();

	//should be inetAddress ut it's implier with this to test in 1 pc.
//	private Map<PeerKey ,Peer> peers = new HashMap<>();
	private Semaphore peersSemaphore = new Semaphore(1);
	private PeerList peers = new PeerList(); // don't access this outside of semaphore. Use getters instead.
	private ServerSocket mySocket;
	private Thread socketListener = null;
	private Thread updaterThread = null;
	
	private FileSystemManager myFs;
	private ConnectionMessageManager messageManager;
	
	private List<AbstractMessageManager>[] listeners;

	private String jarFolder = "./jars";

	private long lastDirUpdate = 0;
	
//	public PhysicalServer() {
//		this(false);
//	}

	@SuppressWarnings("unchecked")
	public PhysicalServer(FileSystemManager fs, boolean update) {
		this.myFs = fs;
		listeners = new List[256];
		messageManager = new ConnectionMessageManager(this);
		if(update){
			launchUpdater();
		}
	}
	
	public void launchUpdater(){
		if(updaterThread==null){
			updaterThread = new Thread(()->{while(true){update();}});
			updaterThread.start();
		}
	}
	
	public void update(){
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(getId()%100+" update "+myFs.getLetter());
		for(Peer peer : getPeers()){
			System.out.println(getId()%100+" update peer "+peer.getConnectionId()%100);
			peer.ping();
		}
		//toutes les heures
		if(lastDirUpdate+1000*60*60 < System.currentTimeMillis()){
			System.out.println(id%100+" REQUEST DIR UPDATE");
			myFs.requestDirUpdate();
			lastDirUpdate = System.currentTimeMillis();
		}
		
	}

	public long getId() {
		return id;
	}

	public void listen(final int port) {
		if (socketListener == null) {
			try {
				mySocket = new ServerSocket(port);
				socketListener = new Thread(() -> {
					try {
						while (true) {
//							 System.err.println("wait a connect...");
							final Socket connexion = mySocket.accept();
//							 System.err.println("connected to a socketserver");
							Peer peer = new Peer(this, connexion.getInetAddress(), 0);
							final Peer argPeer = peer;
							new Thread(()->{
								try {
									initConnection(argPeer, connexion);
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
	
	private void initConnection(Peer peer, Socket sock) throws InterruptedException, IOException{

		if(peer.connect(sock)){

			try {
				peersSemaphore.acquire();
				//check if we already have this peer id.
				Peer otherPeer = null;
				for(Peer p : peers){
					if(p.getConnectionId()==peer.getConnectionId()){
						otherPeer = p;
						 break;
					}
				}
			if(otherPeer==null){
				System.err.println(getId()%100+" 'error' accept connection to "+peer.getKey().getOtherServerId()%100);
//				peers.put(peer.getKey(), peer);
				if(!peers.contains(peer)){
					System.err.println(getId()%100+" 'error' PROPAGATE "+peer.getConnectionId()%100+"!");
					//new peer: propagate!
					peers.add(peer);
					for(Peer oldPeer : peers){
						System.err.println(getId()%100+" 'error' PROPAGATE to "+oldPeer.getConnectionId()%100);
//						MyServerList.get().write(peers, oldPeer);
						messageManager.sendServerList(oldPeer.getConnectionId(), peers);
						oldPeer.getOut().flush();
					}
				}else{
					peers.add(peer);
				}
				peer.startListen();
			}else{
				if(otherPeer.isAlive() && !peer.isAlive()){
					System.err.println(getId()%100+" 'error' , close a new dead connection to "+peer.getKey().getOtherServerId()%100+" is already here.....");
					peer.close();
				}else if(!otherPeer.isAlive() && peer.isAlive()){
					System.err.println(getId()%100+" error,  close an old dead  a connection to "+peer.getKey().getOtherServerId()%100+" is already here.....");
					otherPeer.close();
//					peers.put(peer.getKey(), peer);
					peers.add(peer);
					peer.startListen();
				}else if(otherPeer.getKey().getOtherServerId() != 0){
					if(otherPeer.getKey().getOtherServerId()<getId()){
						System.err.println(getId()%100+" error, (I AM LEADER) a connection to "+peer.getKey().getOtherServerId()%100+" is already here.....");
						peer.close();
					}else{
						System.err.println(getId()%100+" error, (I am not leader) a connection to "+peer.getKey().getOtherServerId()%100+" is already here.....");
						peers.add(peer);
						peer.startListen();
					}
				}else if(peer.getKey().getOtherServerId() != 0){
					if(peer.getKey().getOtherServerId()<getId()){
						System.err.println(getId()%100+" error, (I AM LEADER) a connection to "+peer.getKey().getOtherServerId()%100+" is already here.....");
						otherPeer.close();
//						peers.put(peer.getKey(), peer);
						peers.add(peer);
						peer.startListen();
					}else{
						System.err.println(getId()%100+" error, (I am not leader) a connection to "+peer.getKey().getOtherServerId()%100+" is already here.....");
						peers.add(peer);
						peer.startListen();
					}
				}else{
					System.err.println(getId()%100+" error, an unknown connection to "+peer.getKey().getOtherServerId()%100+" is already here.....");
					peer.close();
				}
			}
			peersSemaphore.release();
			
			//test if id is ok
			if(peer.getConnectionId() == getId()){
				rechooseId();
			}
			//TODO: test if id is inside

			} catch (Exception e) {
				peersSemaphore.tryAcquire();
				peersSemaphore.release();
				throw new RuntimeException(e);
			}
		}else{
			peer.close();
		}
	}
	
	public void connectTo(final String ip, final int port){
		//check if it's not me
		if(mySocket.getLocalPort() == port && 
				(mySocket.getInetAddress().getHostAddress().equals(ip) || ip.equals("127.0.0.1"))){
			System.out.println("DON't connect TO ME MYSELF");
			return;
		}else{
//			System.out.println(mySocket.getLocalPort()+" =?= "+port);
//			System.out.println(mySocket.getInetAddress().getHostAddress()+" =?= "+ip);
//			System.out.println(mySocket.getInetAddress().getHostName()+" =?= "+ip);
		}
		
		
		System.out.println(getId()%100+" want to CONNECT with "+port);
		final InetSocketAddress addr = new InetSocketAddress(ip, port);
		final Socket tempSock = new Socket();
		try{
			tempSock.connect(addr, 200);
			Peer peer = null;
			for(Peer searchPeer : getPeers()){
				if(searchPeer.getPort() == port && searchPeer.getIP().equals(ip)){
					peer = searchPeer;
					break;
				}
			}
//			Peer peer = peers.get(new PeerKey(addr.getAddress(), addr.getPort()));
			if(peer==null){
				peer = new Peer(this, addr.getAddress(), addr.getPort());
			}
			if(peer!= null && !peer.isAlive()){
			//new Thread(()->{
				try {
//					peer.connect(tempSock);
					initConnection(peer, tempSock);
				} catch (InterruptedException | IOException e) {
//					e.printStackTrace();
					System.err.println(getId()%100+" error in initialization : connection close with "+peer.getConnectionId()%100+" ("+peer.getPort()+")");
					peer.close();
				}
			//}).start();
			}else{
				System.out.println(getId()%100+" already CONNECTED with "+port);
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				tempSock.close();
			} catch (IOException e1) {
			}
			throw new RuntimeException(e);
		}
	}

	public PeerList getPeers() {
		try {
			peersSemaphore.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		PeerList list = new PeerList(peers);
		peersSemaphore.release();
		return list;
	}

	public int getListenPort() {
		return mySocket.getLocalPort();
	}

	public void removeExactPeer(Peer peer) {
		try {
			peersSemaphore.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		java.util.ListIterator<Peer> lit = peers.listIterator();
		while(lit.hasNext()){
			if(lit.next() == peer){
				lit.remove();
			}
		}
		peersSemaphore.release();
	}

	public File getJar(String name){
		//list available files
//		java.io.File dir = new File(jarFolder);
//		File finded = null;
//		for(File f : dir.listFiles()){
//			if(f.getName().equals(name+".jar")){
//				finded = f;
//				break;
//			}
//		}
		
		return new File(jarFolder+"/"+name+".jar");
	}
	
	public File getOrCreateJar(String name){
		//list available files
		java.io.File file = getJar(name);
		if(file == null){
			file = new File(jarFolder+"/"+name+".jar");
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return file;
	}
	
	@SuppressWarnings("rawtypes")
	public void runAlgo(String jarName, String className){
		//TODO: maybe use https://github.com/kamranzafar/JCL to isolate each run
		URLClassLoader child;
		try {
			child = new URLClassLoader (new URL[]{getJar(jarName).toURI().toURL()}, this.getClass().getClassLoader());

			Class classToLoad = Class.forName (className, true, child);
			Object instance = classToLoad.newInstance();
			@SuppressWarnings("unchecked")
			Method method = classToLoad.getDeclaredMethod("run", PhysicalServer.class);
			/*Object result =*/ method.invoke(instance, this);
		} catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException 
				| SecurityException | IllegalAccessException | IllegalArgumentException 
				| InvocationTargetException | InstantiationException e) {
			e.printStackTrace();
		}
		
	}

	public FileSystemManager getFileSystem() {
		return myFs;
	}

	public void rechooseId() {
		System.out.println("ERROR: i have to choose an other id....");
		//change id
		id = new Random().nextLong();
		//reconnect all connections
		try {
			peersSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ArrayList<Peer> oldConnection = new ArrayList<>(peers);
		peers.clear();
		peersSemaphore.release();
		for(Peer oldp : oldConnection){
			oldp.close();
			System.out.println("RECONNECT "+oldp.getIP()+" : "+oldp.getPort());
			connectTo(oldp.getIP(),oldp.getPort());
		}
	}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected void writeEmptyMessage(byte messageId, OutputStream out){
		try {
			out.write(5);
			out.write(5);
			out.write(messageId);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	protected void writeMessage(byte messageId, OutputStream out, ByteBuff message){
	}
	
	protected ByteBuff readMessage(InputStream in){
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
	public void writeBroadcastMessage(byte messageId, ByteBuff message) {
		for(Peer peer : peers){
			writeMessage(peer,messageId,message);
		}
	}

	@Override
	public void writeMessage(long senderId, byte messageId, ByteBuff message) {
		writeMessage(getPeer(senderId), messageId, message);
	}
	
	public void writeMessage(Peer p, byte messageId, ByteBuff message) {
		try {
			OutputStream out = p.getOut();
			out.write(5);
			out.write(5);
			out.write(messageId);
//			System.out.println("WRITE 5 5 "+myId.id);
			ByteBuff buffInt = new ByteBuff();
			if(message != null){
				buffInt.putInt(message.limit()).flip();
			}else{
				buffInt.putInt(0).flip();
			}
			out.write(buffInt.array(),0,4);
			if(message != null){
				out.write(message.array(), message.position(), message.limit()-message.position());
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Peer getPeer(long senderId) {
		Peer p = null;
		for(Peer peer : peers){
			if(peer.getConnectionId() == senderId){
				p = peer;
				break;
			}
		}
		return p;
	}

	@Override
	public void registerListener(byte messageId, AbstractMessageManager listener) {
		List<AbstractMessageManager> lst = listeners[messageId];
		if(lst == null){
			lst = new ArrayList<>();
			listeners[messageId] = lst;
		}
		lst.add(listener);
	}
	
	public void propagateMessage(long senderId, byte messageId, ByteBuff message){
		List<AbstractMessageManager> lst = listeners[messageId];
		if(lst != null){
			for(AbstractMessageManager listener : lst){
				listener.receiveMessage(senderId, messageId, message);
			}
		}
	}
	
	public ConnectionMessageManager message(){
		return messageManager;
	}

	@Override
	public void init(int listenPort) {
		this.listen(listenPort);
	}

	@Override
	public void connect(String path, int port) {
		this.connectTo(path, port);
	}

}
