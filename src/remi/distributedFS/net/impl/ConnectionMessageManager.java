package remi.distributedFS.net.impl;

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
//		clusterMananger.registerListener(SEND_LISTEN_PORT, this); used directly by peer
//		clusterMananger.registerListener(SEND_SERVER_ID, this); used directly by peer
		clusterMananger.registerListener(SEND_SERVER_LIST, this);
	}

	@Override
	public void receiveMessage(long senderId, byte messageId, ByteBuff message) {
		if(messageId == GET_SERVER_LIST){
			sendServerList(senderId, this.clusterMananger.getPeers());
		}
		if(messageId == SEND_LISTEN_PORT){
			Peer p = clusterMananger.getPeer(senderId);
			p.setPort(message.getInt());
		}
		if(messageId == SEND_SERVER_LIST){

//			System.out.println(p.getMyServer().getId()%100+" read "+myId+" for "+p.getKey().getOtherServerId()%100);
			int nb = message.getTrailInt();
			for(int i=0;i<nb;i++){
				long id = message.getLong();
				String ip = message.getUTF8();
				int port = message.getTrailInt();
//				System.out.println(p.getMyServer().getId()%100+" i have found "+ip+":"+ port);
				
//					InetSocketAddress addr = new InetSocketAddress(ip,port);
				clusterMananger.connectTo(ip, port);
			}
		}

	}


	public void sendServerList(long sendTo, PeerList peers) {
		ByteBuff buff = new ByteBuff();
		
		//put data
		buff.putTrailInt(peers.size());
		for(Peer peer : peers){
			buff.putLong(peer.getConnectionId());
			buff.putUTF8(peer.getIP());
			buff.putTrailInt(peer.getPort());
//				System.out.println(/*serv.getListenPort()+*/" SEND SERVER "+peer.getPort()+ " to "+p.getKey().getPort());
		}
		buff.flip();
		clusterMananger.writeMessage(sendTo, SEND_SERVER_LIST, buff);
	}


	public void sendServerId(Peer peer) {
		ByteBuff buff = new ByteBuff();
		buff.putLong(clusterMananger.getId());
		clusterMananger.writeMessage(peer, SEND_SERVER_ID, buff.flip());
	}


	public void sendListenPort(Peer peer) {
		ByteBuff buff = new ByteBuff();
		buff.putInt(clusterMananger.getListenPort());
		clusterMananger.writeMessage(peer, SEND_LISTEN_PORT, buff.flip());
	}

}
