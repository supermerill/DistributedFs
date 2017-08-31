package remi.distributedFS.net.impl.test;

import java.io.IOException;

import remi.distributedFS.net.impl.Peer;
import remi.distributedFS.net.impl.PhysicalServer;

public class TestPhysiscalServerPeerConnection {

	PhysicalServer serv1;
	PhysicalServer serv2;
	PhysicalServer serv3;
	PhysicalServer serv4;
	TestPhysiscalServerPeerConnection(){
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		TestPhysiscalServerPeerConnection tester = new TestPhysiscalServerPeerConnection();
		tester.connect();
		tester.draw();
		Thread.sleep(10000);
		tester.draw();
		Thread.sleep(10000);
		tester.draw();
		Thread.sleep(10000);
		tester.draw();
	}
	public void connect() throws InterruptedException{

		serv1 = new PhysicalServer(true); serv1.listen(17830);
		serv2 = new PhysicalServer(true); serv2.listen(17831);
		serv3 = new PhysicalServer(true); serv3.listen(17832);
		serv4 = new PhysicalServer(true); serv4.listen(17833);
		draw();
		serv1.connectTo("localhost", 17831);
//		serv1.connectTo("localhost", 17832);
//		serv1.connectTo("localhost", 17833);
		Thread.sleep(100);
		draw();

		serv2.connectTo("localhost", 17832);
//		serv2.connectTo("localhost", 17833);
		Thread.sleep(100);
		draw();

		serv3.connectTo("localhost", 17833);
		Thread.sleep(100);
		draw();
	}
	
	public void draw(){

		System.out.println("==================================");
		System.out.println("==================================");

		System.out.println("Serv1 has id: "+serv1.getId()+" ("+(serv1.getId()%100)+")");
		for(Peer peer : serv1.getPeers()){
			System.out.println("Serv1 has conn with peer "+peer.getConnectionId()+" ("+(peer.getConnectionId()%100)+") "+peer.isAlive());
		}

		System.out.println("==================================");

		System.out.println("Serv2 has id: "+serv2.getId()+" ("+(serv2.getId()%100)+")");
		for(Peer peer : serv2.getPeers()){
			System.out.println("Serv2 has conn with peer "+peer.getConnectionId()+" ("+(peer.getConnectionId()%100)+") "+peer.isAlive());
		}

		System.out.println("==================================");

		System.out.println("Serv3 has id: "+serv3.getId()+" ("+(serv3.getId()%100)+")");
		for(Peer peer : serv3.getPeers()){
			System.out.println("Serv3 has conn with peer "+peer.getConnectionId()+" ("+(peer.getConnectionId()%100)+") "+peer.isAlive());
		}

		System.out.println("==================================");

		System.out.println("Serv4 has id: "+serv4.getId()+" ("+(serv4.getId()%100)+")");
		for(Peer peer : serv4.getPeers()){
			System.out.println("Serv4 has conn with peer "+peer.getConnectionId()+" ("+(peer.getConnectionId()%100)+") "+peer.isAlive());
		}
	}
}
