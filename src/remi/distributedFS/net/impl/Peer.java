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

import remi.distributedFS.net.AbstractMessageManager;
import remi.distributedFS.util.ByteBuff;

/**
 * Manage the connection with an other physical server: Check if it's alive, send and receive data.
 * 
 * @author Admin
 *
 */
public class Peer implements Runnable {

	public static class PeerKey {
		private long otherServerId = 0;
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
					} else if (otherServerId != 0 && o.otherServerId != 0) {
						return otherServerId == o.otherServerId;
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
			return otherServerId;
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
			return o.myKey.address.equals(myKey.address) && o.myKey.otherServerId == myKey.otherServerId
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
	private PeerKey myKey;
	private Socket sock;
	private InputStream streamIn;
	private BufferedOutputStream streamOut;

	private AtomicBoolean alive = new AtomicBoolean(false);
	private volatile boolean aliveAndSet = false;
	private int aliveFail = 0;
	private Socket sockWaitToDelete;

	private Thread myCurrentThread = null;

	// private long otherServerId = 0;

	private long nextTimeSearchMoreServers = 0;

	// public Peer(PhysicalServer physicalServer, InetSocketAddress inetSocketAddress) {
	// myServer = physicalServer;
	// myKey = new PeerKey(inetSocketAddress.getAddress());
	// myKey.port = inetSocketAddress.getPort();
	// // address = inetSocketAddress;
	// }

	public Peer(PhysicalServer physicalServer, InetAddress inetAddress, int port) {
		myServer = physicalServer;
		myKey = new PeerKey(inetAddress);
		myKey.port = port;
	}

	// write only
	public void ping() {
		if (!alive.get())
			return;
		try {
			if (myKey.port <= 0) {
				myServer.writeMessage(this, AbstractMessageManager.GET_LISTEN_PORT, null);
				// GetListenPort.get().write(getOut(), this);
				// getOut().flush();
			}
			if (myKey.otherServerId == 0) {
				myServer.writeMessage(this, AbstractMessageManager.GET_SERVER_ID, null);
				// GetServerId.get().write(getOut(), this);
				// getOut().flush();
			}

			// get the server list of the other one every 10-15 min.
			if (nextTimeSearchMoreServers < System.currentTimeMillis()) {
				if (nextTimeSearchMoreServers == 0) {
					nextTimeSearchMoreServers = System.currentTimeMillis() + 1000;
				} else {
					nextTimeSearchMoreServers = System.currentTimeMillis()
							+ 1000 * 60 * (10 + new Random().nextInt(10)); // +10-19min
					// }
					myServer.writeMessage(this, AbstractMessageManager.GET_SERVER_LIST, null);
					// GetServerList.get().write(getOut(), this);
					// getOut().flush();
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
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
			// e.printStackTrace();
			System.err.println("error in the communication stream between peers" + myServer.getId() % 100 + " and "
					+ getKey().otherServerId % 100 + " : " + e);
		}

		// check if i'm not a duplicate
		if (myServer.getPeers().getAll(this).size() > 1 || myServer.getId() == getConnectionId()) {
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
				myServer.getId() % 100 + " " + myServer.getListenPort() + " going to connect with " + sock.getPort());
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
			if (myServer.getId() == myKey.otherServerId) {
				System.out.println(myServer.getId() % 100 + " i have the same id as " + myKey.otherServerId % 100);
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
			} else if (myServer.getId() > myKey.otherServerId) {
				iWin = true;
			} else {
				iWin = false;
			}

			if (iWin) {
				// i can kill this new one.
				sock.close();
				System.out.println(myServer.getId() % 100 + " now close the socket to " + myKey.otherServerId % 100);
			} else {
				// I'm not the one to kill one connection. I have to wait the close event from the other computer.
				sockWaitToDelete = sock;
			}

			System.out.println(myServer.getId() % 100 + "fail to connect (already connected) to " + sock.getPort());
			return false;
		} else {
			System.out.println(myServer.getId() % 100 + " win to connect with " + sock.getPort());
			// connect
			this.sock = sock;
			this.streamIn = new BufferedInputStream(sock.getInputStream());
			this.streamOut = new BufferedOutputStream(sock.getOutputStream());
			// BufferedOutputStream tempStreamOut = new BufferedOutputStream(sock.getOutputStream());

			// get the sockId;
			// MessageId.use[MessageId.SET_SERVER_ID.id].emit(tempStreamOut, );
			// SetServerId.get().write(myServer.getId(), streamOut);
			myServer.message().sendServerId(this);
			// MyListenPort.get().write(this);
			myServer.message().sendListenPort(this);
			streamOut.flush();

			// while (myKey.otherServerId == 0) {
			System.out.println(
					myServer.getId() % 100 + " " + myServer.getListenPort() + " want to read " + getIn().available());
			readMessage();
			System.out.println(myServer.getId() % 100 + " " + myServer.getListenPort() + " read id "
					+ getKey().getOtherServerId());
			// }
			// while (myKey.port == 0) {
			readMessage();
			System.out.println(myServer.getId() % 100 + " " + myServer.getListenPort() + " read port " + getKey().port);
			// }

			aliveAndSet = true;

			System.out.println(myServer.getId() % 100 + " " + myServer.getListenPort() + " succeed to connect to "
					+ sock.getPort());
			return true;
		}
	}

	public void startListen() {

		if (myCurrentThread != null) {
			System.err.println(myServer.getId() % 100 + " error, a listening thread is already started for addr "
					+ myKey.address + "...");
		} else {
			myCurrentThread = new Thread(this);
			myCurrentThread.start();
		}
	}

	private void readMessage() {
		try {
			// go to a pos where there are the two byte [5,5]
			int newByte = 0;
			do {
				do {
					newByte = streamIn.read();
					System.out.println(myServer.getId() % 100 + " read first 5 :" + newByte);
					if (newByte == -1)
						throw new IOException("End of stream");
				} while (newByte != 5);

				newByte = streamIn.read();
				System.out.println(myServer.getId() % 100 + " read second 5 :" + newByte);
				if (newByte == -1)
					throw new IOException("End of stream");
			} while (newByte != 5);

			// read messagetype
			newByte = streamIn.read();
			System.out.println(myServer.getId() % 100 + " read id :" + newByte);
			if (newByte == -1)
				throw new IOException("End of stream");
			// read message
			try {
				ByteBuff buffIn = new ByteBuff(4);
				streamIn.read(buffIn.array(), 0, 4);
				int nbBytes = buffIn.getInt();
				buffIn.limit(nbBytes).rewind();
				streamIn.read(buffIn.array(), 0, nbBytes);
				if (newByte == AbstractMessageManager.GET_SERVER_ID) {
					// special case, give the peer object directly.
					myServer.message().sendServerId(this);
				}
				if (newByte == AbstractMessageManager.SEND_SERVER_ID) {
					// special case, give the peer object directly.
					setServerId(buffIn.getLong());
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
				myServer.propagateMessage(getConnectionId(), (byte) newByte, buffIn);
				
			} catch (IOException e) {
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
		myKey.otherServerId = newId;
	}

	public PhysicalServer getMyServer() {
		return myServer;
	}

	public OutputStream getOut() {
		return streamOut;
	}

	public InputStream getIn() {
		return streamIn;
	}

	public long getConnectionId() {
		return myKey.otherServerId;
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
		try {
			if (sockWaitToDelete != null)
				sockWaitToDelete.close();
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
