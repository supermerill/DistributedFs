package remi.distributedFS.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import remi.distributedFS.util.ByteBuff;


public abstract class AbstractMessageManager {
	
	//list of message id
	
	// ----------------------------------- connection & leader election ---------------------------------------------
		/**
		 * get the distant server id, for leader election
		 * no data
		 */
		public static final byte GET_SERVER_ID = (byte)1;
		 /** if i detect an other server with my id, i have to create a new id, and use this command to send my new id to everyone.
		  * or just a response to GET_SERVER_ID
		  * data : byte[8] -> long -> serverid
		  */
		public static final byte SEND_SERVER_ID = (byte) 2;

		/**
		 * Ask the list of servers in this cluster
		 */
		public static final byte  GET_SERVER_LIST = (byte) 3;
		/**
		 * Send his current server list
		 */
		public static final byte  SEND_SERVER_LIST = (byte) 4;
		/**
		 * Ask the port needed to contact him
		 */
		public static final byte  GET_LISTEN_PORT = (byte) 5;
		/**
		 * Send his port where he listen new connections
		 */
		public static final byte  SEND_LISTEN_PORT = (byte) 6;
		
		//TODO, for encrypted connection
		/**
		 * request a SEND_SERVER_PUBLIC_KEY (and a message to be encrypted)
		 */
		public static final byte  GET_SERVER_PUBLIC_KEY = (byte) 7;
		/**
		 * Send his public key (and a message encrypted with the private key)
		 */
		public static final byte  SEND_SERVER_PUBLIC_KEY = (byte) 8;
		/**
		 * request a SEND_SERVER_RSA_KEY
		 */
//		public static final byte  GET_SERVER_RSA_KEY = (byte) 9;
		/**
		 * 'Server A' send an rsa key, encrypted with 'Server A' private key then 'Server B' public key
		 */
//		public static final byte  SEND_SERVER_RSA_KEY = (byte) 10;
		// also, the public key is a way to get the real server id (for file permission) when it's verified to work (with a rsa exchange)

	
	public abstract void receiveMessage(long senderId, byte messageId, ByteBuff message);
	
	
}
