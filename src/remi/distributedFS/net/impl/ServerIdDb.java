package remi.distributedFS.net.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import remi.distributedFS.net.AbstractMessageManager;
import remi.distributedFS.util.ByteBuff;

public class ServerIdDb {

	PrivateKey privateKey;
	PublicKey publicKey;

	List<Peer> receivedServerList;
	List<Peer> registeredPeers;
	Map<Short, PublicKey> id2PublicKey;
	private PhysicalServer serv;
	
	short myId = -1;
	long timeChooseId = 0;
	
	
	private Map<Peer, String> emittedMsg = new HashMap<>();
	private String filepath;
	public long clusterId = -1; // the id to identify the whole cluster
	
	public ServerIdDb(PhysicalServer serv, String filePath){
		this.serv = serv;
		this.filepath = filePath;
		id2PublicKey= new HashMap<>();
		registeredPeers = new ArrayList<>();
		receivedServerList = new ArrayList<>();
		// Get an instance of the Cipher for RSA encryption/decryption
	}
	
	public void createNew(){
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			SecureRandom random = SecureRandom.getInstanceStrong();
			random.setSeed(System.currentTimeMillis());
			generator.initialize(1024, random);
			System.out.println(serv.getId()%100+" generate rsa");
			KeyPair pair = generator.generateKeyPair();
			System.out.println(serv.getId()%100+" generate rsa: ended");
			privateKey = pair.getPrivate();
			publicKey = pair.getPublic();
			
			System.out.println(serv.getId()%100+" Priv key algo : "+createPrivKey(privateKey.getEncoded()).getAlgorithm());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public void load(){
		//choose the file.
		File fic = new File(filepath);
		if(!fic.exists()){
			fic = new File(filepath+"_1");
			if(!fic.exists()){
				//no previous data, load nothing plz.
				//but create new data!
				createNew();
				requestSave();
				return;
			}
		}
		
		try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(fic))){
			ByteBuff bufferReac = new ByteBuff(1024); 
			
			//read
			clusterId = bufferReac.reset().read(in,8).flip().getLong();
			System.out.println(serv.getId()%100+" clusterId : "+clusterId);
//			System.out.println("clusterId : "+Long.toHexString(clusterId));
			
			//read id
			myId = bufferReac.reset().read(in,2).flip().getShort();
			System.out.println(serv.getId() % 100 + " LOAD MY COMPUTER ID as =" + myId);
			System.out.println(serv.getId()%100+" myId : "+myId);
//			System.out.println("myId : "+Integer.toHexString(bufferReac.rewind().getShort()));
			
			
			//read pubKey
			int nbBytes = bufferReac.reset().read(in,4).flip().getInt();
			byte[] encodedPubKey = new byte[nbBytes];
			bufferReac.reset().read(in,nbBytes).flip().get(encodedPubKey, 0, nbBytes);
			X509EncodedKeySpec bobPubKeySpec = new X509EncodedKeySpec(encodedPubKey);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			publicKey = keyFactory.generatePublic(bobPubKeySpec);
			System.out.println( serv.getId()%100+" publicKey => "+Arrays.toString(encodedPubKey));

			//read privKey
			nbBytes = bufferReac.reset().read(in,4).flip().getInt();
			byte[] encodedPrivKey = new byte[nbBytes];
			bufferReac.reset().read(in,nbBytes).flip().get(encodedPrivKey, 0, nbBytes);
			privateKey = createPrivKey(encodedPrivKey);
			
			//read peers
			int nbPeers = bufferReac.reset().read(in,4).flip().getInt();
			System.out.println(serv.getId()%100+" i have "+nbPeers+" peers");
			for(int i=0;i<nbPeers;i++){
				nbBytes = bufferReac.reset().read(in, 4).flip().getInt();
				String distIp = bufferReac.reset().read(in, nbBytes).flip().getUTF8();
				int distPort = bufferReac.reset().read(in, 4).flip().getInt();
				short distId =  bufferReac.reset().read(in, 2).flip().getShort();
				nbBytes = bufferReac.reset().read(in,4).flip().getInt();
				encodedPubKey = new byte[nbBytes];
				PublicKey distPublicKey = null;
				if(nbBytes>0){
					System.out.println(serv.getId()%100+" (ext)publicKey size : "+nbBytes);
					bufferReac.reset().read(in,nbBytes).flip().get(encodedPubKey, 0, nbBytes);
					System.out.println( serv.getId()%100+"  => "+Arrays.toString(encodedPubKey));
					bobPubKeySpec = new X509EncodedKeySpec(encodedPubKey);
					distPublicKey = keyFactory.generatePublic(bobPubKeySpec);
					//save
					Peer p = new Peer(serv, new InetSocketAddress(distIp, distPort).getAddress(), distPort);
					p.setComputerId(distId);
					id2PublicKey.put(distId, distPublicKey);
				}else{
					System.err.println(serv.getId()%100+" error, i have a peer but he has no public key.");
				}
			}
			
			
		} catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e1) {
			e1.printStackTrace();
		}
	}
	
	public static PrivateKey createPrivKey(byte[] datas){
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PKCS8EncodedKeySpec bobPrivKeySpec = new PKCS8EncodedKeySpec(datas);
			return keyFactory.generatePrivate(bobPrivKeySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
//	public static void main(String[] args) {
////		byte[] arr = new byte[]{48, -126, 2, 118, 2, 1, 0, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 4, -126, 2, 96, 48, -126, 2, 92, 2, 1, 0, 2, -127, -127, 0, -127, -83, -115, 114, -35, 8, 54, -43, -68, -74, -122, -79, 118, 42, 102, -112, 20, -41, -26, -27, -59, -101, -48, -37, 55, 46, 56, 52, 18, 125, 34, -85, -51, 59, 13, -90, 61, 86, -117, -39, -98, 53, 28, 69, 106, 88, -51, 97, -128, 38, 75, -42, -99, -50, 97, -69, -61, 29, -114, -93, 100, 70, 74, -103, 31, -26, -125, 67, -22, 55, 117, -114, 12, 18, -108, -24, 36, -18, -27, 58, 112, -52, -127, -2, -75, -23, -122, 7, 2, 9, -126, -20, -66, 23, 98, 71, 37, -125, -77, 8, 56, -70, 58, 20, 91, -90, -4, 64, 100, 3, -32, 11, -97, -125, 30, -3, -110, -117, 111, 107, 13, -126, -50, 106, -59, -8, -112, -65, 2, 3, 1, 0, 1, 2, -127, -128, 34, 81, 10, 48, -114, 91, -127, 31, 88, -68, 56, -78, -73, -95, -118, -40, -80, 27, 94, 104, 9, -66, 45, 44, 5, -45, 62, 94, 81, 82, 58, 29, -102, -58, -8, -38, -72, 58, -79, -15, -103, -45, 86, 50, -20, 108, -87, -107, 22, -77, -117, -72, 52, -76, -117, -38, -125, 76, -52, 21, 99, 16, -46, -26, -121, -40, 30, -124, -5, -36, 1, -116, 105, 44, -6, 61, 21, -125, -22, -11, -112, -65, -34, -89, 46, -128, -25, -32, 38, -30, 40, -121, 52, -76, -95, -2, 2, 38, -57, 34, -110, -66, -83, 91, -6, 95, 16, 16, -37, 116, 12, 52, 119, -106, 33, -1, -111, -123, -53, 44, -94, 107, -99, -96, -70, 99, 63, -55, 2, 65, 0, -57, -17, 16, -94, -49, 59, 34, -36, 36, -97, -79, -79, -98, 39, -48, -67, -108, -121, -67, 44, 109, 75, 7, -17, 95, -25, 102, 95, -83, 63, -7, 13, -126, -41, 14, -1, -105, -102, -21, 67, -27, 88, -60, -11, -24, -1, 89, 0, -71, 54, 30, 31, -27, -15, 5, 58, 75, 101, 86, 68, 53, -107, 87, -51, 2, 65, 0, -90, 10, -19, 74, -84, -106, -103, 85, -67, -112, -21, -119, -117, -60, 80, 14, 85, -104, -3, 56, -15, -80, -73, 127, 58, -59, -84, 13, 85, -35, -61, 65, -4, -16, -25, -7, 48, -17, 92, -45, -36, 115, 119, 94, -97, 35, -90, 19, -51, -73, 2, 28, 57, 65, 97, -124, -7, -54, 55, 114, -73, 61, 38, -69, 2, 64, 9, -104, -10, 73, 122, 125, 50, 61, 51, 28, -33, 96, -47, 96, -61, -22, 117, -40, -42, 65, -19, -75, 46, 90, 85, 86, 60, 89, -41, 109, 60, -67, 99, 76, -125, -111, -51, 107, 72, 99, -25, -4, -116, -25, -23, 25, 104, -30, 90, 1, -71, 12, 122, -13, 72, -10, -11, 107, -107, -22, -116, 79, -16, -7, 2, 64, 58, -43, -88, 91, 75, 104, 89, -112, -50, 8, -23, -52, -27, 31, 124, -106, 119, -78, 44, 23, -33, 92, 20, -55, 26, 84, 44, -80, -44, -6, 45, 83, -42, -126, -82, 79, -40, 13, 24, -63, 97, 93, -16, -80, 48, -121, 123, 51, -115, 51, 9, -90, 98, -117, 78, 56, -58, 33, -25, 31, -40, -39, -20, 61, 2, 65, 0, -109, -31, -106, -105, 17, -33, -16, 2, -127, -78, 110, 11, 11, -31, -124, 1, -89, -95, 6, 85, 105, -81, 15, -122, -40, 28, 29, 48, 26, -32, -66, -98, -17, -77, 121, -66, 34, -75, 61, 92, -81, 103, 85, 6, 70, -98, -83, -7, -14, -33, -42, 40, -108, -11, -74, -49, 110, 92, -25, -15, 85, -29, -59, -20};
//		byte[] arr = new byte[]{48, -126, 2, 118, 2, 1, 0, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 4, -126, 2, 96, 48, -126, 2, 92, 2, 1, 0, 2, -127, -127, 0, -127, -83, -115, 114, -35, 8, 54, -43, -68, -74, -122, -79, 118, 42, 102, -112, 20, -41, -26, -27, -59, -101, -48, -37, 55, 46, 56, 52, 18, 125, 34, -85, -51, 59, 13, -90, 61, 86, -117, -39, -98, 53, 28, 69, 106, 88, -51, 97, -128, 38, 75, -42, -99, -50, 97, -69, -61, 29, -114, -93, 100, 70, 74, -103, 31, -26, -125, 67, -22, 55, 117, -114, 12, 18, -108, -24, 36, -18, -27, 58, 112, -52, -127, -2, -75, -23, -122, 7, 2, 9, -126, -20, -66, 23, 98, 71, 37, -125, -77, 8, 56, -70, 58, 20, 91, -90, -4, 64, 100, 3, -32, 11, -97, -125, 30, -3, -110, -117, 111, 107, 13, -126, -50, 106, -59, -8, -112, -65, 2, 3, 1, 0, 1, 2, -127, -128, 34, 81, 10, 48, -114, 91, -127, 31, 88, -68, 56, -78, -73, -95, -118, -40, -80, 27, 94, 104, 9, -66, 45, 44, 5, -45, 62, 94, 81, 82, 58, 29, -102, -58, -8, -38, -72, 58, -79, -15, -103, -45, 86, 50, -20, 108, -87, -107, 22, -77, -117, -72, 52, -76, -117, -38, -125, 76, -52, 21, 99, 16, -46, -26, -121, -40, 30, -124, -5, -36, 1, -116, 105, 44, -6, 61, 21, -125, -22, -11, -112, -65, -34, -89, 46, -128, -25, -32, 38, -30, 40, -121, 52, -76, -95, -2, 2, 38, -57, 34, -110, -66, -83, 91, -6, 95, 16, 16, -37, 116, 12, 52, 119, -106, 33, -1, -111, -123, -53, 44, -94, 107, -99, -96, -70, 99, 63, -55, 2, 65, 0, -57, -17, 16, -94, -49, 59, 34, -36, 36, -97, -79, -79, -98, 39, -48, -67, -108, -121, -67, 44, 109, 75, 7, -17, 95, -25, 102, 95, -83, 63, -7, 13, -126, -41, 14, -1, -105, -102, -21, 67, -27, 88, -60, -11, -24, -1, 89, 0, -71, 54, 30, 31, -27, -15, 5, 58, 75, 101, 86, 68, 53, -107, 87, -51, 2, 65, 0, -90, 10, -19, 74, -84, -106, -103, 85, -67, -112, -21, -119, -117, -60, 80, 14, 85, -104, -3, 56, -15, -80, -73, 127, 58, -59, -84, 13, 85, -35, -61, 65, -4, -16, -25, -7, 48, -17, 92, -45, -36, 115, 119, 94, -97, 35, -90, 19, -51, -73, 2, 28, 57, 65, 97, -124, -7, -54, 55, 114, -73, 61, 38, -69, 2, 64, 9, -104, -10, 73, 122, 125, 50, 61, 51, 28, -33, 96, -47, 96, -61, -22, 117, -40, -42, 65, -19, -75, 46, 90, 85, 86, 60, 89, -41, 109, 60, -67, 99, 76, -125, -111, -51, 107, 72, 99, -25, -4, -116, -25, -23, 25, 104, -30, 90, 1, -71, 12, 122, -13, 72, -10, -11, 107, -107, -22, -116, 79, -16, -7, 2, 64, 58, -43, -88, 91, 75, 104, 89, -112, -50, 8, -23, -52, -27, 31, 124, -106, 119, -78, 44, 23, -33, 92, 20, -55, 26, 84, 44, -80, -44, -6, 45, 83, -42, -126, -82, 79, -40, 13, 24, -63, 97, 93, -16, -80, 48, -121, 123, 51, -115, 51, 9, -90, 98, -117, 78, 56, -58, 33, -25, 31, -40, -39, -20, 61, 2, 65, 0, -109, -31, -106, -105, 17, -33, -16, 2, -127, -78, 110, 11, 11, -31, -124, 1, -89, -95, 6, 85, 105, -81, 15, -122, -40, 28, 29, 48, 26, -32, -66, -98, -17, -77, 121, -66, 34, -75, 61, 92, -81, 103, 85, 6, 70, -98, -83, -7, -14, -33, -42, 40, -108, -11, -74, -49, 110, 92, -25, -15, 85, -29, -59, -20};
//		System.out.println(createPrivKey(arr).getFormat());
//	}


	protected void save(String filePath){
		
		try(BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filePath,false))){
			ByteBuff bufferReac = new ByteBuff(1024);
			
			//write
			System.out.println(serv.getId()%100+" write Cid : "+clusterId);
			bufferReac.reset().putLong(clusterId).flip().write(out);
			
			//write id
			System.out.println(serv.getId()%100+" write id : "+myId);
			bufferReac.reset().putShort(myId).flip().write(out);
			
			
			//write pubKey
			byte[] encodedPubKey = publicKey.getEncoded();
			bufferReac.reset().putInt(encodedPubKey.length).put(encodedPubKey).flip().write(out);
//			System.out.println("publicKeyEncodingOfrmat : "+publicKey.getFormat());
//			System.out.println("publickey size : "+encodedPubKey.length + " => "+Arrays.toString(encodedPubKey));
//			try {
//				X509EncodedKeySpec bobPubKeySpec = new X509EncodedKeySpec(encodedPubKey);
//				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//				System.out.println("my pub key = "+keyFactory.generatePublic(bobPubKeySpec).getFormat());
//			} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
			//write privKey
			byte[] encodedPrivKey = privateKey.getEncoded();
			bufferReac.reset().putInt(encodedPrivKey.length).put(encodedPrivKey).flip().write(out);
//			System.out.println("privateKey size : "+encodedPrivKey.length + " => "+Arrays.toString(encodedPrivKey));
//			System.out.println("privateKeyencodingformat : "+privateKey.getFormat());
//			createPrivKey(encodedPrivKey);
//			PKCS#8
			
			
			
			//write peers
			int nbPeers = registeredPeers.size();
			bufferReac.reset().putInt(nbPeers).flip().write(out);
			for(int i=0;i<nbPeers;i++){
				Peer p = registeredPeers.get(i);
				//save ip
				bufferReac.reset().putInt(0).putUTF8(p.getIP()).flip();
				bufferReac.putInt(bufferReac.limit()-4).rewind().write(out);	
				//save port
				bufferReac.reset().putInt(p.getPort()).flip().write(out);
				//save id
				bufferReac.reset().putShort(p.getComputerId()).flip().write(out);
				//savePubKey
				encodedPubKey = id2PublicKey.get(p.getComputerId()).getEncoded();
				bufferReac.reset().putInt(encodedPubKey.length).put(encodedPubKey).flip().write(out);
			}
			
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public void requestPublicKey(Peer peer, boolean force){
		String msg = emittedMsg.get(peer);
		if(msg==null || force){
			if(msg == null) msg = Long.toHexString(new Random().nextLong());
			System.out.println(serv.getId()%100+" emit GET_SERVER_PUBLIC_KEY to "+peer.getConnectionId()%100+" with message "+msg);
			emittedMsg.put(peer, msg);
			serv.writeMessage(peer, AbstractMessageManager.GET_SERVER_PUBLIC_KEY, new ByteBuff().putUTF8(msg).flip());
			//todo: encrypt it with our public key.
		}else{
			System.out.println("We already emit a GET_SERVER_PUBLIC_KEY to "+peer.getConnectionId()%100+" with message "+emittedMsg.get(peer));
		}
	}

	//send our public key to the peer, with the message encoded
	public void sendPublicKey(Peer peer, String messageToEncrypt) {
		System.out.println(serv.getId()%100+" emit SEND_SERVER_PUBLIC_KEY to "+peer.getConnectionId()%100+", with message 2encrypt : "+messageToEncrypt);
		//check if we have the public key of this peer
		if(peer.getComputerId()<0 
				|| !this.id2PublicKey.containsKey(peer.getComputerId())
				|| this.id2PublicKey.get(peer.getComputerId()) == null){
			//request his key
			System.out.println(serv.getId()%100+" and request his one ");
			requestPublicKey(peer, false);
		}
		
		//don't emit myId if it's -1
		if(myId<0){
			System.out.println(serv.getId()%100+" but i have on id!! ");
			ByteBuff buff = new ByteBuff();
			buff.putShort((short) -1);
			//send packet
			serv.writeMessage(peer, AbstractMessageManager.SEND_SERVER_PUBLIC_KEY, buff.flip());
			return;
		}
		
		//TODO: encrypt it also with his public key if we have it?
		try{
			ByteBuff buff = new ByteBuff();
			// my id
			buff.putShort(myId);
			//my pub key
			byte[] encodedPubKey = publicKey.getEncoded();
			buff.putInt(encodedPubKey.length).put(encodedPubKey);
			//encode msg
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, privateKey);
			ByteBuff buffEncoded = blockCipher(new ByteBuff().putUTF8(messageToEncrypt).flip().toArray(), Cipher.ENCRYPT_MODE, cipher);
			buff.putInt(buffEncoded.limit()).put(buffEncoded);
			System.out.println(serv.getId()%100+" message : "+messageToEncrypt);
			System.out.println(serv.getId()%100+"Encryptmessage : "+Arrays.toString(buffEncoded.rewind().array()));
			cipher.init(Cipher.DECRYPT_MODE, publicKey);
			ByteBuff buffDecoded = blockCipher(buffEncoded.array(), Cipher.DECRYPT_MODE, cipher);
			System.out.println(serv.getId()%100+"DecryptMessage : "+buffDecoded.getUTF8());
			
			//send packet
			serv.writeMessage(peer, AbstractMessageManager.SEND_SERVER_PUBLIC_KEY, buff.flip());

		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
			e1.printStackTrace();
		}
	}

	public void receivePublicKey(Peer p, ByteBuff buffIn) {
		System.out.println(serv.getId()%100+" receive SEND_SERVER_PUBLIC_KEY to "+p.getConnectionId()%100);
		try{
		
			//get id
			short distId = buffIn.getShort();
			if(distId <0 || !emittedMsg.containsKey(p)){
				System.out.println(serv.getId()%100+" BAD receive computerid "+distId+" and i "+emittedMsg.containsKey(p)+" emmitted a message");
				//the other peer doesn't have an computerid (yet)
				emittedMsg.remove(p);
				return;
			}else{
				System.out.println(serv.getId()%100+" GOOD receive computerid "+distId+" and i "+emittedMsg.containsKey(p)+" emmitted a message");
			}
			//get pub Key
			int nbBytes = buffIn.getInt();
			byte[] encodedPubKey = new byte[nbBytes];
			buffIn.get(encodedPubKey, 0, nbBytes);
			X509EncodedKeySpec bobPubKeySpec = new X509EncodedKeySpec(encodedPubKey);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey distPublicKey = keyFactory.generatePublic(bobPubKeySpec);
			//get msg
			nbBytes = buffIn.getInt();
			byte[] dataIn = new byte[nbBytes];
			buffIn.get(dataIn, 0, nbBytes);
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, distPublicKey);
			ByteBuff buffDecoded = blockCipher(dataIn, Cipher.DECRYPT_MODE, cipher);
			String msgDecoded = buffDecoded.getUTF8();

			System.out.println(serv.getId()%100+" i have sent to "+p.getConnectionId()%100+" the message "+emittedMsg.get(p)+" to encode. i have received "+msgDecoded +" !!!");
			
			//check if this message is inside our peer
			if(emittedMsg.get(p).equals(msgDecoded)){
				//now check if this id isn't already taken.
				boolean alreadyTaken = false;
				for(Peer storedP : this.registeredPeers){
					if(storedP.getComputerId() == distId){
						System.err.println("error, cluster id "+distId+" already taken for "+p.getConnectionId()%100+" ");
						alreadyTaken = true;
						break;
					}
				}
				if(!alreadyTaken){
					//check if the public key is the same
					if(!this.id2PublicKey.containsKey(distId) || this.id2PublicKey.get(distId) == null){
						System.out.println(serv.getId()%100+" assign new publickey  for computerid "+distId+" , connId="+p.getConnectionId()%100);
						//validate this peer
						this.id2PublicKey.put(distId, distPublicKey);
						this.registeredPeers.add(p);
						p.setComputerId(distId);
						this.emittedMsg.remove(p);
						requestSave();
					}else{
						if(!Arrays.equals(this.id2PublicKey.get(distId).getEncoded(), distPublicKey.getEncoded())){
							System.err.println(serv.getId()%100+" error, cluster id "+distId+"has a wrong public key (not the one i registered) "+p.getConnectionId()%100+" ");
							System.err.println(serv.getId()%100+" what i have : "+Arrays.toString(this.id2PublicKey.get(distId).getEncoded()));
							System.err.println(serv.getId()%100+" what i received : "+Arrays.toString(this.id2PublicKey.get(distId).getEncoded()));
						}else{
							System.out.println(serv.getId()%100+" publickey ok for computerid "+distId);
							p.setComputerId(distId);
							if(!registeredPeers.contains(p)) this.registeredPeers.add(p);
						}
					}
					
				}
			}else{
				System.err.println("Errror: message '"+emittedMsg.get(p)+"' emmited to "+p.getConnectionId()%100+" is different than "+msgDecoded+" received!");
			}

		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException e1) {
			e1.printStackTrace();
		}
	}
	
	public void requestSave() {
		// save the current state into the file.
		if(clusterId<0 || myId<0) return; //don't save if we are not registered on the server yet.
		// get synch
		synchronized (this) {
			// create an other file
			save(filepath+"_1");
			// remove first file
			File fic = new File(filepath);
			if(fic.exists()) fic.delete();
			// rename file
			new File(filepath+"_1").renameTo(fic);
		}
		
	}

	//using skeletton from http://coding.westreicher.org/?p=23 ( Florian Westreicher)
	private ByteBuff blockCipher(byte[] bytes, int mode, Cipher cipher) throws IllegalBlockSizeException, BadPaddingException{
		// string initialize 2 buffers.
		// scrambled will hold intermediate results
		byte[] scrambled = new byte[0];

		// toReturn will hold the total result
		ByteBuff toReturn = new ByteBuff();
		// if we encrypt we use 100 byte long blocks. Decryption requires 128 byte long blocks (because of RSA)
		int length = (mode == Cipher.ENCRYPT_MODE)? 100 : 128;
//		int length = (mode == Cipher.ENCRYPT_MODE) ? (keyLength / 8 ) - 11 : (keyLength / 8 );

		// another buffer. this one will hold the bytes that have to be modified in this step
		byte[] buffer = new byte[(bytes.length > length ? length : bytes.length)];

		for (int i=0; i< bytes.length; i++){

			// if we filled our buffer array we have our block ready for de- or encryption
			if ((i > 0) && (i % length == 0)){
				//execute the operation
				scrambled = cipher.doFinal(buffer);
				// add the result to our total result.
				toReturn.put(scrambled);
				// here we calculate the length of the next buffer required
				int newlength = length;

				// if newlength would be longer than remaining bytes in the bytes array we shorten it.
				if (i + length > bytes.length) {
					 newlength = bytes.length - i;
				}
				// clean the buffer array
				buffer = new byte[newlength];
			}
			// copy byte into our buffer.
			buffer[i%length] = bytes[i];
		}

		// this step is needed if we had a trailing buffer. should only happen when encrypting.
		// example: we encrypt 110 bytes. 100 bytes per run means we "forgot" the last 10 bytes. they are in the buffer array
		scrambled = cipher.doFinal(buffer);

		// final step before we can return the modified data.
		toReturn.put(scrambled);

		return toReturn.flip();
	}

	public List<Peer> getRegisteredPeers() {
		return registeredPeers;
	}

	public boolean isChoosen(short choosenId) {
		for(short s : id2PublicKey.keySet()){
			if(s == choosenId){
				return true;
			}
		}
		return false;
	}

	
	
	
}
