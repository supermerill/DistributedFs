package remi.distributedFS.fs.messages;

import java.util.Random;
import java.util.concurrent.Semaphore;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import remi.distributedFS.fs.StandardManager;
import remi.distributedFS.log.Logs;
import remi.distributedFS.net.ClusterManager;
import remi.distributedFS.net.impl.Peer;
import remi.distributedFS.net.impl.PhysicalServer;
import remi.distributedFS.net.impl.PhysicalServer.ServerConnectionState;
import remi.distributedFS.util.ByteBuff;

/**
 * This class try to see the speed of the link between us and a peer.
 * It emit some message and see with how much time it respond.
 * 
 * function only with Physical server:
 *    create & use a special highpriority-uncrytped emit-answer tunnel between us and peer for exchange.
 * 
 * @author remi
 *
 */
public class FlowAndPing extends AbstractFSMessageManager{

	// nbBytes = 10^idx
//	static final int[] NB_BYTES_TEST = new int[] {1,10,100,1000,10000,100000,1000000,10000000};
	static final int MAX_NB_IDX = 7;
	//10^3 = 1kio
	//10^6 = 1mio
	//10^9 = 1gio
	
	static final Semaphore SEMAPHORE = new Semaphore(1);
	
	static class PingData{
		long[] timestamp = new long[MAX_NB_IDX];
		int[] ping = new int[MAX_NB_IDX];
		RefreshPing refresher;
		
	}

	private Long2ObjectMap<PingData> peerId2PingData;
	
	
	private StandardManager manager;

	public FlowAndPing(StandardManager standardManager) {
		this.manager = standardManager;
		peerId2PingData = new Long2ObjectOpenHashMap<>();
	}

	public int getMaxPing(long peerId, long byteSize) {
		PingData storage = peerId2PingData.get(peerId);
		if(storage != null) {
			//get the one higher, if possible.
			int best = 0;
//			long max = Math.pow(10, MAX_NB_IDX);
			for(int i=0,val= 1; i<MAX_NB_IDX; i++,val=val*10) {
				if(val < byteSize) {
					if(storage.ping[i] != 0) best = storage.ping[i];
				}else {
					if(storage.ping[i] != 0) {
						best = storage.ping[i];
						break;
					}
				}
			}
			if(best>0) return best;
		}
		return Integer.MAX_VALUE;
	}
	
	Thread autoRefresh = null;
	public void startAutoRefresh() {
		if(autoRefresh == null) {
			autoRefresh = new Thread(() -> {
				Random rand = new Random();
				PhysicalServer net = (PhysicalServer) manager.getNet();
				try {
					while(net.getState() != ServerConnectionState.CONNECTED) {
						Thread.sleep(1000);
					}
					Thread.sleep(1000);
					for(Peer p : net.getPeers()) {
						updatePeerId(p.getPeerId());
					}
					while(net.getState() != ServerConnectionState.DISCONNECTED) {
						Thread.sleep(1000);
						
						for(Peer p : net.getPeers()) {
							//check if it's the first time we see it
							if(getMaxPing(p.getPeerId(), 1) > 0) {
								updatePeerId(p.getPeerId());
							}else {
								//every ~10min, we refresh a peer
								if(rand.nextInt(600)==0) {
									Logs.logManager.info("FlowAndPingMananger: refresh ping for peer "+p.getPeerId());
									updatePeerId(p.getPeerId());
								}
							}
						}
					}
				}catch(Exception e) {e.printStackTrace();}
			});
			autoRefresh.start();
		}
	}
	
	public void updatePeerId(long peerId) {
		PingData storage = peerId2PingData.get(peerId);
		if(storage == null) {
			storage = new PingData();
			peerId2PingData.put(peerId, storage);
		}
		if(storage.refresher == null) storage.refresher = new RefreshPing(peerId, storage);
		storage.refresher.start();
	}
	

	class RefreshPing implements Runnable {
		
		private Thread current = null;
		private long peerId;
		private PingData storage;
		
		public RefreshPing(long peerId, PingData storage) {
			this.peerId = peerId;
			this.storage = storage;
		}

		public void start() {
			try {
				if(current == null) {
					current = new Thread(this);
					current.start();
				}
			}catch(Exception e) {
				current = null;
			}
		}

		@Override
		public void run() {
			try {
				SEMAPHORE.acquire();
				Random rand = new Random();
				for(int idx=0,val= 1; idx<MAX_NB_IDX; idx++,val=val*10) {
					//request ping
					storage.ping[idx] = 0;
					requestPing(peerId, idx);
					//wait for it to finish (and a bit more)
					for(int i=0;i<100;i++) {
						Thread.sleep(1000);
						if(storage.ping[idx]>0) break;
					}
					if(storage.ping[idx] == 0) {
						//can't go higher, it's already broken.
						break;
					}
					Thread.sleep(1 + rand.nextInt(idx*1000));
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}finally{
				current = null;
				SEMAPHORE.release();
			}
		}
	}


	@Override
	public void receiveMessage(long senderId, byte messageId, ByteBuff message) {
		if(manager.getNet().getComputerId(senderId) <0){
			//error: not a estabished peer
			Logs.logManager.warning("Error, peer "+senderId%100+" ask us a chunk and he doens't have a computerid !");
			return;
		}
		if (messageId == GET_MAX_FLOW) {
			Logs.logManager.info(this.manager.getComputerId()+"$ RECEIVE GET_MAX_FLOW from "+senderId);
			long id = message.getLong();
			long timestampEmit = message.getLong();
			int idx = message.getInt();
			byte nbBytesMod10 = message.get();
			int nbBytes = message.getInt();
			int nbLong = nbBytes/4;
			if(nbBytes != (int)Math.pow(10, nbBytesMod10)) return;
			ByteBuff buff = new ByteBuff();
			buff.putLong(id);
			buff.putLong(timestampEmit);
			buff.putLong(System.currentTimeMillis());
			buff.putInt(nbBytesMod10);
			buff.putInt(nbBytes);
			for(int i=0;i<nbLong;i++) {
				buff.putLong(42l);
			}
			for(int i=0;i<nbBytes-nbLong*4;i++) {
				buff.put((byte)42);
			}
			buff.flip();
//			manager.getNet().writeMessage(senderId, SEND_MAX_FLOW, buff);
			PhysicalServer net = (PhysicalServer) manager.getNet();
			net.getPeer(senderId).writeMessagePriorityClear(SEND_MAX_FLOW, buff);
			
		}
		if (messageId == SEND_MAX_FLOW) {
			long id = message.getLong();
			long timestampEmit = message.getLong();
			long timestampPeer = message.getLong();
			byte nbBytesMod10 = message.get();
			int nbBytes = message.getInt();
			if(nbBytes != (int)Math.pow(10, nbBytesMod10) || nbBytesMod10 >= MAX_NB_IDX) return;
			PingData storage = peerId2PingData.get(senderId);
			if(storage == null) {
				storage = new PingData();
				peerId2PingData.put(senderId, storage);
			}
			long sendTime = storage.timestamp[nbBytesMod10];
			if(sendTime != timestampEmit) return;
			storage.ping[nbBytesMod10] = Math.max(1, (int)(System.currentTimeMillis() - timestampEmit));
			Logs.logManager.info("FlowAndPingMananger: new ping for peer "+senderId+" : datasize="+nbBytes+" => "+storage.ping[nbBytesMod10]);
		}
	}

	
	public void requestPing(long peerId, int nbByteTestMod10) {
		if(nbByteTestMod10 >= MAX_NB_IDX) return;
		Logs.logManager.info(this.manager.getComputerId()+"$ SEND GET_MAX_FLOW for "+peerId);
		int nbByteTest = (int) Math.pow(10, nbByteTestMod10);
		int nbLong = nbByteTest/4;
		ByteBuff buff = new ByteBuff();
		long time = System.currentTimeMillis();
		long id = new Random().nextLong();
		PingData storage = this.peerId2PingData.get(peerId);
		if(storage == null) {
			storage = new PingData();
			peerId2PingData.put(peerId, storage);
		}
		if(storage.timestamp[nbByteTestMod10] > time) return;
		storage.timestamp[nbByteTestMod10] = time;
		buff.putLong(id);
		buff.putLong(time);
		buff.put((byte)nbByteTestMod10);
		buff.putInt(nbByteTest);
		for(int i=0;i<nbLong;i++) {
			buff.putLong(42l);
		}
		for(int i=0;i<nbByteTest-nbLong*4;i++) {
			buff.put((byte)42);
		}
		buff.flip();
//		manager.getNet().writeMessage(peerId, GET_MAX_FLOW, buff);
		PhysicalServer net = (PhysicalServer) manager.getNet();
		net.getPeer(peerId).writeMessagePriorityClear(GET_MAX_FLOW, buff);
	}
	
	public void register(ClusterManager net) {
		net.registerListener(GET_MAX_FLOW, this);
		net.registerListener(SEND_MAX_FLOW, this);
	}

}
