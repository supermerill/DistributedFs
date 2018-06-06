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
		clusterMananger.registerListener(GET_VERIFY_IDENTITY, this);
		clusterMananger.registerListener(GET_SERVER_AES_KEY, this);
//		clusterMananger.registerListener(SEND_LISTEN_PORT, this); used directly by peer
//		clusterMananger.registerListener(SEND_SERVER_ID, this); used directly by peer
		clusterMananger.registerListener(SEND_SERVER_LIST, this);
		clusterMananger.registerListener(SEND_SERVER_PUBLIC_KEY, this);
		clusterMananger.registerListener(SEND_VERIFY_IDENTITY, this);
		clusterMananger.registerListener(SEND_SERVER_AES_KEY, this);
	}

	@Override
	public void receiveMessage(long senderId, byte messageId, ByteBuff message) {
		System.out.println(clusterMananger.getPeerId()%100+" receive message from "+senderId%100);
		if (messageId == AbstractMessageManager.GET_SERVER_PUBLIC_KEY) {
			System.out.println(clusterMananger.getPeerId()%100+" receive GET_SERVER_PUBLIC_KEY from "+senderId%100);
			clusterMananger.getServerIdDb().sendPublicKey(clusterMananger.getPeer(senderId));
		}
		if (messageId == AbstractMessageManager.SEND_SERVER_PUBLIC_KEY) {
			System.out.println(clusterMananger.getPeerId()%100+" receive SEND_SERVER_PUBLIC_KEY from "+senderId%100);
			clusterMananger.getServerIdDb().receivePublicKey(clusterMananger.getPeer(senderId), message);
		}
		if (messageId == AbstractMessageManager.GET_VERIFY_IDENTITY) {
			System.out.println(clusterMananger.getPeerId()%100+" receive GET_VERIFY_IDENTITY from "+senderId%100);
//			sendIdentity(p, createMessageForIdentityCheck(p, true), true);
			clusterMananger.getServerIdDb().answerIdentity(clusterMananger.getPeer(senderId), message);
		}
		if (messageId == AbstractMessageManager.SEND_VERIFY_IDENTITY) {
			System.out.println(clusterMananger.getPeerId()%100+" receive SEND_VERIFY_IDENTITY from "+senderId%100);
			clusterMananger.getServerIdDb().receiveIdentity(clusterMananger.getPeer(senderId), message);
		}
		if (messageId == AbstractMessageManager.GET_SERVER_AES_KEY) {
			System.out.println(clusterMananger.getPeerId()%100+" receive GET_SERVER_AES_KEY from "+senderId%100);
			clusterMananger.getServerIdDb().sendAesKey(clusterMananger.getPeer(senderId), ServerIdDb.AES_PROPOSAL);
		}
		if (messageId == AbstractMessageManager.SEND_SERVER_AES_KEY) {
			System.out.println(clusterMananger.getPeerId()%100+" receive SEND_SERVER_AES_KEY from "+senderId%100);
			clusterMananger.getServerIdDb().receiveAesKey(clusterMananger.getPeer(senderId), message);
		}
		if(messageId == GET_SERVER_LIST){
			System.out.println(clusterMananger.getPeerId()%100+"he ( "+senderId%100+" ) want my server list");
			synchronized (this.clusterMananger.getServerIdDb().getRegisteredPeers()) {
				sendServerList(senderId, this.clusterMananger.getServerIdDb().getRegisteredPeers());
			}
		}
		if(messageId == SEND_LISTEN_PORT){
			System.out.println(clusterMananger.getPeerId()%100+" received SEND_LISTEN_PORT from "+senderId%100);
			Peer p = clusterMananger.getPeer(senderId);
			p.setPort(message.getInt());
		}
		if(messageId == SEND_SERVER_LIST){
			System.out.println(clusterMananger.getPeerId()%100+" received SEND_SERVER_LIST from "+senderId%100);

//			System.out.println(p.getMyServer().getId()%100+" read "+myId+" for "+p.getKey().getOtherServerId()%100);
			short senderComputerId = message.getShort();
			//add this id in our list, to be sure we didn't use it and we can transmit it.
			synchronized (this.clusterMananger.getServerIdDb()) {
				System.out.println(clusterMananger.getPeerId()%100+" receive peer computerId:  "+senderComputerId+" from "+senderId%100);
				this.clusterMananger.getServerIdDb().addPeer(senderComputerId);
			}
			int nb = message.getTrailInt();
			for(int i=0;i<nb;i++){
				long id = message.getLong();
				String ip = message.getUTF8();
				int port = message.getTrailInt();
				short computerId = message.getShort();
				
				//add this id in our list, to be sure we didn't use it and we can transmit it.
				synchronized (this.clusterMananger.getServerIdDb()) {
					System.out.println(clusterMananger.getPeerId()%100+" receive a distant computerId:  "+computerId+" of "+id%100+" from "+senderId%100);
					this.clusterMananger.getServerIdDb().addPeer(computerId);
				}
//				System.out.println(p.getMyServer().getId()%100+" i have found "+ip+":"+ port);
				
				
//					InetSocketAddress addr = new InetSocketAddress(ip,port);
				if(computerId >0 && computerId != clusterMananger.getComputerId() && computerId != senderComputerId
						&& id != clusterMananger.getPeerId() && id != senderId){
					clusterMananger.connectTo(ip, port);
				}
			}
			clusterMananger.getServerIdDb().receivedServerList.add(this.clusterMananger.getPeer(senderId));
			clusterMananger.chooseComputerId();
		}

	}


	public void sendServerList(long sendTo, List<Peer> list) {
		ByteBuff buff = new ByteBuff();
		//put our id
		buff.putShort(clusterMananger.getServerIdDb().myComputerId);
		//put data
		buff.putTrailInt(list.size());
		for(Peer peer : list){
			buff.putLong(peer.getPeerId());
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
		buff.putLong(clusterMananger.getPeerId());
		buff.putLong(clusterMananger.getServerIdDb().getClusterId());
		clusterMananger.writeMessage(peer, SEND_SERVER_ID, buff.flip());
	}


	public void sendListenPort(Peer peer) {
		ByteBuff buff = new ByteBuff();
		buff.putInt(clusterMananger.getListenPort());
		clusterMananger.writeMessage(peer, SEND_LISTEN_PORT, buff.flip());
	}

}
