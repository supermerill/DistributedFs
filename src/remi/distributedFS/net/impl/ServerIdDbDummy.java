package remi.distributedFS.net.impl;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This object is used to test the connection with a cluster (so it doesn't load or save anything).
 * 
 * @author centai
 *
 */
public class ServerIdDbDummy extends ServerIdDb {
	
	public ServerIdDbDummy(PhysicalServerDummy serv, long clusterId, String clusterPwd){
		this.serv = serv;
		this.filepath = null;
		tempPubKey = new HashMap<>(); 
		id2PublicKey = new HashMap<>();
		id2AesKey =  new HashMap<>();
		registeredPeers = new ArrayList<>();
		receivedServerList = new ArrayList<>();
		// Get an instance of the Cipher for RSA encryption/decryption
		this.clusterId = clusterId;
		this.passphrase = clusterPwd;
	}
	
	
	@Override
	public void load(){
	}
	
	@Override
	protected void save(String filePath){
	}
	
	@Override
	public void requestSave() {
	}
	
}
