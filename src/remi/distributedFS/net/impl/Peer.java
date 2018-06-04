package remi.distributedFS.net.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.management.RuntimeErrorException;

import remi.distributedFS.net.AbstractMessageManager;
import remi.distributedFS.util.ByteBuff;

/**
 * Manage the connection with an other physical server: Check if it's alive, send and receive data.
 * 
 * @author Admin
 *
 */
public class Peer implements Runnable {
	
	public static enum PeerConnectionState{
		DEAD(0),
		JUST_BORN(1),
		HAS_ID(2),
		HAS_PUBLIC_KEY(3),
		HAS_VERIFIED_COMPUTER_ID(4),
		CONNECTED_W_AES(5);
		
		private int v ;

		PeerConnectionState(int value){ this.v = value;}

		public boolean lowerThan(PeerConnectionState hasId) {
			return v < hasId.v;
		}
		
	}

	public static class PeerKey {
		private long otherPeerId = 0; //use for leader election, connection establishment
		private final InetAddress address;
		private int port = 0;

		public PeerKey(InetAddress inetAddress) {
			this.address = inetAddress;
		}

		public PeerKey(InetAddress address, int port) {
			this.address = address;
			this.port = port;
		}

		@Override
		public boolean equals(Object arg0) {
			if (arg0 instanceof PeerKey) {
				PeerKey o = (PeerKey) arg0;
				if (o.address.equals(address)) {
					if (o.port != 0 && port != 0) {
						return o.port == port;
					} else if (otherPeerId != 0 && o.otherPeerId != 0) {
						return otherPeerId == o.otherPeerId;
					}
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			return address.hashCode();
		}

		public long getOtherServerId() {
			return otherPeerId;
		}

		public InetAddress getAddress() {
			return address;
		}

		public int getPort() {
			return port;
		}

	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Peer) {
			Peer o = (Peer) arg0;
			return o.myKey.address.equals(myKey.address) && o.myKey.otherPeerId == myKey.otherPeerId
					&& o.myKey.port == myKey.port;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return myKey.hashCode();
	}

	private PhysicalServer myServer;

	// private Socket connexion;
	// private InetSocketAddress address;
	private short distComputerId = -1; //unique id for the computer, private-public key protected.
	private PeerKey myKey;
	private Socket sock;
	private InputStream streamIn;
	private BufferedOutputStream streamOut;

	private AtomicBoolean alive = new AtomicBoolean(false);
	private volatile boolean aliveAndSet = false;
	private int aliveFail = 0;
	private Socket sockWaitToDelete;
	
	//my state, i added this after the dev of this class, so it's spartly used/updated for the dead/born/hasid state, need some debug to be barely reliable.
	// used mainly to see if HAS_PUBLIC_KEY, HAS_VERIFIED_COMPUTER_ID, CONNECTED_W_AES is done.
	protected PeerConnectionState myState = PeerConnectionState.DEAD;

	private Thread myCurrentThread = null;

	// private long otherServerId = 0;

	private long nextTimeSearchMoreServers = 0;

	// public Peer(PhysicalServer physicalServer, InetSocketAddress inetSocketAddress) {
	// myServer = physicalServer;
	// myKey = new PeerKey(inetSocketAddress.getAddress());
	// myKey.port = inetSocketAddress.getPort();
	// // address = inetSocketAddress;
	// }
	private static final ByteBuff nullmsg = new ByteBuff();

	Cipher encoder = null;
	Cipher decoder = null;

	public Peer(PhysicalServer physicalServer, InetAddress inetAddress, int port) {
		myServer = physicalServer;
		myKey = new PeerKey(inetAddress);
		myKey.port = port;
	}

	// write only
	/**
	 * update connection status.
	 * @return true if you should call ping quickly afterwards (connection phase)
	 */
	public boolean ping() {
		System.out.println("peer "+this.myKey.otherPeerId+" compId:"+this.distComputerId+" : is alive? "+this.alive.get());
		if (!alive.get())
			return false;
		try {
			
			if (myKey.port <= 0) {
				myServer.writeMessage(this, AbstractMessageManager.GET_LISTEN_PORT, nullmsg);
				// GetListenPort.get().write(getOut(), this);
				// getOut().flush();
			}
			if (myKey.otherPeerId == 0 || myState.lowerThan(PeerConnectionState.HAS_ID)) {
				myServer.writeMessage(this, AbstractMessageManager.GET_SERVER_ID, nullmsg);
				// GetServerId.get().write(getOut(), this);
				// getOut().flush();
				return true;
			}

			// get the server list of the other one every 10-15 min.
//			myServer.writeMessage(this, AbstractMessageManager.GET_SERVER_LIST, nullmsg);
			if (nextTimeSearchMoreServers < System.currentTimeMillis()) {
				if (nextTimeSearchMoreServers == 0) {
					nextTimeSearchMoreServers = System.currentTimeMillis() + 1000;
				} else {
					nextTimeSearchMoreServers = System.currentTimeMillis()
							+ 1000 * 60 * (10 + new Random().nextInt(10)); // +10-19min
					// }
					myServer.writeMessage(this, AbstractMessageManager.GET_SERVER_LIST, nullmsg);
					// GetServerList.get().write(getOut(), this);
					// getOut().flush();
				}

			}

			if(myState.lowerThan(PeerConnectionState.HAS_PUBLIC_KEY)){
				myServer.getServerIdDb().requestPublicKey(this);
				return true;
			}

			if(myState.lowerThan(PeerConnectionState.HAS_VERIFIED_COMPUTER_ID)){
				myServer.getServerIdDb().sendIdentity(this, myServer.getServerIdDb().createMessageForIdentityCheck(this, false), true);
				return true;
			}

			if(myState.lowerThan(PeerConnectionState.CONNECTED_W_AES)){
				myServer.getServerIdDb().requestSecretKey(this);
				return true;
			}
			

			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// receive only (read)
	@Override
	public void run() {
		myCurrentThread = Thread.currentThread();
		try {
			while (true) {
				readMessage();
			}
		} catch (Exception e) {
			 e.printStackTrace();
			System.err.println( myServer.getPeerId() % 100+" error in the communication stream between peers" + myServer.getPeerId() % 100 + " and "
					+ getKey().otherPeerId % 100 + " : " + e);
		}

		// check if i'm not a duplicate
		if (myServer.getPeers().getAll(this).size() > 1 || myServer.getPeerId() == getPeerId()) {
			// i'm a duplicate, kill me!
			myServer.removeExactPeer(this);
			return;
		}

		// try to reconnect with the second connection if already enabled
		try {
			if (sock.isClosed()) {
				if (sockWaitToDelete != null) {
					if (!sockWaitToDelete.isClosed()) {
						this.sock = sockWaitToDelete;
						this.streamIn = new BufferedInputStream(sock.getInputStream());
						this.streamOut = new BufferedOutputStream(sock.getOutputStream());
						sockWaitToDelete = null;
						new Thread(this).start();
						return; // dont launch the reconnection protocol, we are already connected.
					}
					sockWaitToDelete = null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		myCurrentThread = null;
		changeState(PeerConnectionState.DEAD, false);

		if (alive.get()) {
			aliveAndSet = false;
			aliveFail = 0;
			alive.set(false);
			// reconnect();
		}
		// else, it's a kill from someone else who doesn't want me

	}

	public void reconnect() {
		if (myKey.address == null || myKey.port <= 0) {
			System.err.println("can't reconnect because i didn't know the address");
			return;
		}
		while (!alive.get() && aliveFail < 10) {

			// try to connect
			System.err.println("try to reconnect to " + myKey.address);

			try (Socket tempSock = new Socket();) {
				tempSock.connect(new InetSocketAddress(myKey.address, myKey.port), 2000);
				if (connect(sock)) {
					startListen();
				}
				aliveFail++;
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

		}

	}

	public boolean connect(Socket sock) throws InterruptedException, IOException {
		System.out.println(
				myServer.getPeerId() % 100 + " " + myServer.getListenPort() + " going to connect with " + sock.getPort());
		// System.out.println(this +" "+ alive.get()+" SETTO true "+myServer.getId()%100+ " for " + sock.getPort());
		boolean alreadyAlive = alive.getAndSet(true);
		if (alreadyAlive) {
			// two connections established, keep the one with the higher number

			// wait field sets
			while (!aliveAndSet) {
				// System.out.println(this +" "+ alive.get()+" SETTO "+myServer.getId()%100+ " alive but not set yet for " + sock.getPort());
				Thread.sleep(100);
			}

			boolean iWin = false;
			// now compare the numbers
			if (myServer.getPeerId() == myKey.otherPeerId) {
				System.out.println(myServer.getPeerId() % 100 + " i have the same id as " + myKey.otherPeerId % 100);
				// me and the other server must recreate a hash id.
				// compare ips to choose
				int winner = sock.getLocalAddress().getHostAddress().compareTo(sock.getInetAddress().getHostAddress());
				if (winner == 0)
					winner = Integer.compare(sock.getLocalPort(), sock.getPort());
				if (winner > 0) {
					iWin = true;
					// close();
					// myServer.rechooseId();
				} else {
					iWin = false;
				}
			} else if (myServer.getPeerId() > myKey.otherPeerId) {
				iWin = true;
			} else {
				iWin = false;
			}

			if (iWin) {
				// i can kill this new one.
				sock.close();
				System.out.println(myServer.getPeerId() % 100 + " now close the socket to " + myKey.otherPeerId % 100);
			} else {
				// I'm not the one to kill one connection. I have to wait the close event from the other computer.
				sockWaitToDelete = sock;
			}

			System.out.println(myServer.getPeerId() % 100 + "fail to connect (already connected) to " + sock.getPort());
			return false;
		} else {
			System.out.println(myServer.getPeerId() % 100 + " win to connect with " + sock.getPort());
			// connect
			this.sock = sock;
			this.streamIn = new BufferedInputStream(sock.getInputStream());
			this.streamOut = new BufferedOutputStream(sock.getOutputStream());
			// BufferedOutputStream tempStreamOut = new BufferedOutputStream(sock.getOutputStream());

			// get the sockId;
			// MessageId.use[MessageId.SET_SERVER_ID.id].emit(tempStreamOut, );
			// SetServerId.get().write(myServer.getId(), streamOut);
			changeState(PeerConnectionState.JUST_BORN, true);
			myServer.message().sendServerId(this);
			// MyListenPort.get().write(this);
			myServer.message().sendListenPort(this);
			myServer.getServerIdDb().requestPublicKey(this);
			streamOut.flush();

			// while (myKey.otherServerId == 0) {
			System.out.println(
					myServer.getPeerId() % 100 + " " + myServer.getListenPort() + " want to read " + getIn().available());
			readMessage();
			System.out.println(myServer.getPeerId() % 100 + " " + myServer.getListenPort() + " read id "
					+ getKey().getOtherServerId());
			// }
			// while (myKey.port == 0) {
			readMessage();
			System.out.println(myServer.getPeerId() % 100 + " " + myServer.getListenPort() + " read port " + getKey().port);
			// }

			aliveAndSet = true;

			System.out.println(myServer.getPeerId() % 100 + " " + myServer.getListenPort() + " succeed to connect to "
					+ sock.getPort());
			return true;
		}
	}

	public void startListen() {

		if (myCurrentThread != null) {
			System.err.println(myServer.getPeerId() % 100 + " error, a listening thread is already started for addr "
					+ myKey.address + "...");
		} else {
			myCurrentThread = new Thread(this);
			myCurrentThread.start();
		}
	}
	


	public synchronized void writeMessage(byte messageId, ByteBuff message) {
		if(message==null){
			System.err.println("Warn : emit null message, id :"+messageId);
		}
		try {
			
			//encode mesage
			if(encoder==null) encoder = myServer.getServerIdDb().getSecretCipher(this, Cipher.ENCRYPT_MODE);
			byte[] encodedMsg = null;
			if(messageId > AbstractMessageManager.LAST_UNENCODED_MESSAGE){
				if(hasState(PeerConnectionState.CONNECTED_W_AES)){
					if(message!=null && message.limit()-message.position()>0){
						encodedMsg = encoder.doFinal(message.array(), message.position(), message.limit());
					}
				}else{
					System.err.println("Erro, ttry to send a "+messageId+" message when we don't have a aes key!");
					return;
				}
			}else{
				if(message!=null && message.limit()-message.position()>0){
					encodedMsg = message.toArray();
				}
			}
			
			OutputStream out = this.getOut();
			out.write(5);
			out.write(5);
			out.write(5);
			out.write(5);
			out.write(messageId);
			out.write(messageId);
			// System.out.println("WRITE 5 5 "+myId.id);
			ByteBuff buffInt = new ByteBuff();
			if (encodedMsg != null) {
				buffInt.putInt(encodedMsg.length)
						.putInt(encodedMsg.length)
						.flip();
			} else {
				buffInt.putInt(0)
						.putInt(0)
						.flip();
			}
			out.write(buffInt.array(), 0, 8);
			if (encodedMsg != null) {
				out.write(encodedMsg,  0, encodedMsg.length);
			}
			out.flush();
			if(encodedMsg != null && message.position() != 0){
				System.err.println("Warn, you want to send a buffer which is not rewinded : " + message.position());
			}
			System.out.println("WRITE MESSAGE : "+messageId+" : "+(message==null?"null":(message.limit() - message.position())));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	protected void readMessage() {
		try {
			// go to a pos where there are the two byte [5,5]
			int newByte = 0;
			int nb5 = 0;
			do {

				newByte = streamIn.read();
				if(newByte==5){
					nb5++;
				}else{
					nb5 = 0;
					System.err.println("stream error: receive "+newByte+" isntead of 5");
				}
//				System.out.println(myServer.getId() % 100 + " read second 5 :" + newByte);
				if (newByte == -1)
					throw new IOException("End of stream");
			} while (nb5 < 4);

			// read messagetype
			newByte = streamIn.read();
			int sameByte = streamIn.read();
			if(sameByte != newByte){
				System.err.println("Stream error: not same byte for message id : "+newByte+" != "+sameByte);
				return;
			}
			System.out.println(myServer.getPeerId() % 100 + " read message id :" + newByte);
			if (newByte == -1)
				throw new IOException("End of stream");
			if(newByte<0 || newByte >50){
				System.err.println("error, receive byte: "+newByte);
				return;
			}
			// read message
			try {
				ByteBuff buffIn = new ByteBuff(8);
				streamIn.read(buffIn.array(), 0, 8);
				int nbBytes = buffIn.getInt();
				int nbBytes2 = buffIn.getInt();
				if(nbBytes<0){
					System.err.println("Stream error: stream want me to read a negative number of bytes");
					return;
				}
				if(nbBytes!=nbBytes2){
					System.err.println("Stream error: not same number of bytes to read : "+nbBytes+" != "+nbBytes2);
					return;
				}
				System.out.println("Read "+nbBytes+" from the stream");
				buffIn.limit(nbBytes).rewind();
				if(nbBytes>0) {
					int pos = 0;
					//while mandatory, because it's not a buffered stream.
					while(pos<nbBytes){
						pos += streamIn.read(buffIn.array(), pos, nbBytes-pos);
					}
				}
				//decode mesage
				if(decoder==null) decoder = myServer.getServerIdDb().getSecretCipher(this, Cipher.DECRYPT_MODE);
				byte[] decodedMsg = null;
				if(newByte > AbstractMessageManager.LAST_UNENCODED_MESSAGE){
					if(hasState(PeerConnectionState.CONNECTED_W_AES)){
						if(buffIn!=null && nbBytes> 0 && buffIn.limit()-buffIn.position()>0){
							decodedMsg = decoder.doFinal(buffIn.array(), buffIn.position(), buffIn.limit());
							//put decoded message into the read buffer
							buffIn.reset().put(decodedMsg).rewind();
						}
					}else{
						System.err.println("Error, try to receive a "+newByte+" message when we don't have a aes key!");
						return;
					}
				}//else : nothing to do, it's not encoded
				//use message
				if (newByte == AbstractMessageManager.GET_SERVER_ID) {
					// special case, give the peer object directly.
					myServer.message().sendServerId(this);
				}
				if (newByte == AbstractMessageManager.SEND_SERVER_ID) {
					// special case, give the peer object directly.
					setServerId(buffIn.getLong());
					//check if the cluster is ok
					long clusterId = buffIn.getLong();
					if(clusterId >0 && myServer.getServerIdDb().getClusterId() < 0){
						//set our cluster id
//						myServer.getServerIdDb().setClusterId(clusterId);
						throw new RuntimeException("Error, we haven't a clusterid !! Can we pick one from an existing network? : not anymore!");
//						changeState(PeerConnectionState.HAS_ID, true);
					}else if(clusterId >0 && myServer.getServerIdDb().getClusterId() != clusterId){
						//error, not my cluster!
						System.err.println("Error, trying to connect with "+getPeerId()%100+" but his cluster is "+clusterId+" and mine is "
						+myServer.getServerIdDb().getClusterId()+" => closing connection");
						this.close();
					}
				}
				if (newByte == AbstractMessageManager.GET_LISTEN_PORT) {
					// special case, give the peer object directly.
					myServer.message().sendListenPort(this);
				}
				if (newByte == AbstractMessageManager.SEND_LISTEN_PORT) {
					// special case, give the peer object directly.
					setPort(buffIn.getInt());
				}
				
				// standard case, give the peer id. Our physical server should be able to retrieve us.
				myServer.propagateMessage(getPeerId(), (byte) newByte, buffIn);
				
			} catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

			// if (MessageId.use[newByte] != null) {
			// MessageId.use[newByte].read(this);
			// System.out.println(myServer.getId()%100+" read done for :"+newByte);
			// }else{
			// System.err.println("Error, message id "+newByte+" has no listener");
			// }

		} catch (IOException e) {
			// try {
			// Thread.sleep(Math.abs(new Random().nextInt())%1000);
			// } catch (InterruptedException e1) {
			// // TODO Auto-generated catch block
			// e1.printStackTrace();
			// }
			// System.err.print(getMyServer().getId()%100+" : ");e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	// public static void writeHeader(BufferedOutputStream out, String messsage){
	// out.write(b);
	// }
	// private void read(ByteBuffer buff, int sizeToRead) throws IOException {
	// int pos = 0;
	// // int size = buff.array().length;
	// buff.rewind();
	// buff.limit(sizeToRead);
	// while (pos < sizeToRead && pos >= 0) {
	// int num = streamIn.read(buff.array(), pos, sizeToRead - pos);
	// if (num == -1) {
	// // exit case
	// throw new EOFException();
	// }
	// pos += num;
	// }
	// buff.rewind();
	// }

	//// functions calleds by messages ///////////////////////////////////////////////////////////////////
	public void setServerId(long newId) {
		myKey.otherPeerId = newId;
	}

	public PhysicalServer getMyServer() {
		return myServer;
	}

	protected OutputStream getOut() {
		return streamOut;
	}

	protected InputStream getIn() {
		return streamIn;
	}

	public long getPeerId() {
		return myKey.otherPeerId;
	}

	public String getIP() {
		return myKey.address.getHostAddress();
	}

	public int getPort() {
		return myKey.port;
	}

	public boolean isAlive() {
		return alive.get();
	}

	public void setPort(int port) {
		if (myKey.port != port) {
			myKey.port = port;
		}
	}

	public PeerKey getKey() {
		return myKey;
	}

	public void close() {
		alive.set(false);
		changeState(PeerConnectionState.DEAD, false);
		try {
			if (sockWaitToDelete != null)
				sockWaitToDelete.close();
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void setComputerId(short distId) {
		this.distComputerId = distId;
	}

	public short getComputerId() {
		return distComputerId;
	}

	public void flush() throws IOException {
		getOut().flush();
	}

	public void changeState(PeerConnectionState newState, boolean changeOnlyIfHigher) {
		synchronized (myState) {
			if(!changeOnlyIfHigher || myState.lowerThan(newState))
				myState = newState;
		}
		
	}

	public boolean hasState(PeerConnectionState stateToVerify) {
		return !myState.lowerThan(stateToVerify);
	}
	
	public PeerConnectionState getState() {
		return myState;
	}

}
