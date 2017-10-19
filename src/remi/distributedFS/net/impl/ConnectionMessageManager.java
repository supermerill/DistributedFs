package remi.distributedFS.net.impl;

import java.util.List;

import remi.distributedFS.net.AbstractMessageManager;
import remi.distributedFS.util.ByteBuff;

public class ConnectionMessageManager extends AbstractMessageManager {

	PhysicalServer clusterMananger;
	
	
	public ConnectionMessageManager(PhysicalServer physicalServer) {
		this.clusterMananger = physicalServer;
		register();
	}

	public void register(){
//		clusterMananger.registerListener(GET_SERVER_ID, this); passed directly by peer
//		clusterMananger.registerListener(GET_LISTEN_PORT, this); passed directly by peer
		clusterMananger.registerListener(GET_SERVER_LIST, this);
		clusterMananger.registerListener(GET_SERVER_PUBLIC_KEY, this);
		clusterMananger.registerListener(GET_SERVER_AES_KEY, this);
//		clusterMananger.registerListener(SEND_LISTEN_PORT, this); used directly by peer
//		clusterMananger.registerListener(SEND_SERVER_ID, this); used directly by peer
		clusterMananger.registerListener(SEND_SERVER_LIST, this);
		clusterMananger.registerListener(SEND_SERVER_PUBLIC_KEY, this);
		clusterMananger.registerListener(SEND_SERVER_AES_KEY, this);
	}

	@Override
	public void receiveMessage(long senderId, byte messageId, ByteBuff message) {
		System.out.println(clusterMananger.getId()%100+" receive message from "+senderId%100);
		if (messageId == AbstractMessageManager.GET_SERVER_PUBLIC_KEY) {
			System.out.println(clusterMananger.getId()%100+" receive GET_SERVER_PUBLIC_KEY from "+senderId%100);
			clusterMananger.getServerIdDb().sendPublicKey(clusterMananger.getPeer(senderId), message.getUTF8());
		}
		if (messageId == AbstractMessageManager.SEND_SERVER_PUBLIC_KEY) {
			System.out.println(clusterMananger.getId()%100+" receive SEND_SERVER_PUBLIC_KEY from "+senderId%100);
			clusterMananger.getServerIdDb().receivePublicKey(clusterMananger.getPeer(senderId), message);
		}
		if (messageId == AbstractMessageManager.GET_SERVER_AES_KEY) {
			System.out.println(clusterMananger.getId()%100+" receive GET_SERVER_AES_KEY from "+senderId%100);
			clusterMananger.getServerIdDb().sendAesKey(clusterMananger.getPeer(senderId));
		}
		if (messageId == AbstractMessageManager.SEND_SERVER_PUBLIC_KEY) {
			System.out.println(clusterMananger.getId()%100+" receive SEND_SERVER_AES_KEY from "+senderId%100);
			clusterMananger.getServerIdDb().receiveAesKey(clusterMananger.getPeer(senderId), message);
		}
		if(messageId == GET_SERVER_LIST){
			System.out.println(clusterMananger.getId()%100+"he ( "+senderId%100+" ) want my server list");
			synchronized (this.clusterMananger.getServerIdDb().getRegisteredPeers()) {
				sendServerList(senderId, this.clusterMananger.getServerIdDb().getRegisteredPeers());
			}
		}
		if(messageId == SEND_LISTEN_PORT){
			Peer p = clusterMananger.getPeer(senderId);
			p.setPort(message.getInt());
		}
		if(messageId == SEND_SERVER_LIST){

//			System.out.println(p.getMyServer().getId()%100+" read "+myId+" for "+p.getKey().getOtherServerId()%100);
			short peerId = message.getShort();
			//add this id in our list, to be sure we didn't use it and we can transmit it.
			synchronized (this.clusterMananger.getServerIdDb()) {
				System.out.println(clusterMananger.getId()%100+" receive peer computerId:  "+peerId+" from "+senderId%100);
				if(!this.clusterMananger.getServerIdDb().id2PublicKey.containsKey(peerId)){
					this.clusterMananger.getServerIdDb().id2PublicKey.put(peerId, null);
				}
			}
			int nb = message.getTrailInt();
			for(int i=0;i<nb;i++){
				long id = message.getLong();
				String ip = message.getUTF8();
				int port = message.getTrailInt();
				short computerId = message.getShort();
				
				//add this id in our list, to be sure we didn't use it and we can transmit it.
				synchronized (this.clusterMananger.getServerIdDb()) {
					System.out.println(clusterMananger.getId()%100+" receive a distant computerId:  "+computerId+" of "+id%100+" from "+senderId%100);
					if(!this.clusterMananger.getServerIdDb().id2PublicKey.containsKey(computerId) && id != clusterMananger.getId()){
						this.clusterMananger.getServerIdDb().id2PublicKey.put(computerId, null);
					}
				}
//				System.out.println(p.getMyServer().getId()%100+" i have found "+ip+":"+ port);
				
				
//					InetSocketAddress addr = new InetSocketAddress(ip,port);
				clusterMananger.connectTo(ip, port);
			}
			clusterMananger.getServerIdDb().receivedServerList.add(this.clusterMananger.getPeer(senderId));
			clusterMananger.chooseClusterId();
		}

	}


	public void sendServerList(long sendTo, List<Peer> list) {
		ByteBuff buff = new ByteBuff();
		//put our id
		buff.putShort(clusterMananger.getServerIdDb().myId);
		//put data
		buff.putTrailInt(list.size());
		for(Peer peer : list){
			buff.putLong(peer.getConnectionId());
			buff.putUTF8(peer.getIP());
			buff.putTrailInt(peer.getPort());
			buff.putShort(peer.getComputerId());
//				System.out.println(/*serv.getListenPort()+*/" SEND SERVER "+peer.getPort()+ " to "+p.getKey().getPort());
		}
		buff.flip();
		clusterMananger.writeMessage(sendTo, SEND_SERVER_LIST, buff);
	}


	public void sendServerId(Peer peer) {
		ByteBuff buff = new ByteBuff();
		buff.putLong(clusterMananger.getId());
		buff.putLong(clusterMananger.getServerIdDb().clusterId);
		clusterMananger.writeMessage(peer, SEND_SERVER_ID, buff.flip());
	}


	public void sendListenPort(Peer peer) {
		ByteBuff buff = new ByteBuff();
		buff.putInt(clusterMananger.getListenPort());
		clusterMananger.writeMessage(peer, SEND_LISTEN_PORT, buff.flip());
	}

}
