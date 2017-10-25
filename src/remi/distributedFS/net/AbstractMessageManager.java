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
		
		/**
		 * request a SEND_SERVER_PUBLIC_KEY
		 */
		public static final byte  GET_SERVER_PUBLIC_KEY = (byte) 7;
		/**
		 * Send his public key.<br>
		 */
		public static final byte  SEND_SERVER_PUBLIC_KEY = (byte) 8;
		
		/**
		 * send a public-private encrypted message to be encoded.
		 */
		public static final byte  GET_VERIFY_IDENTITY = (byte) 9;
		/**
		 * Send back the message with a public-private encryption, to tell the other one i am really me.
		 */
		public static final byte  SEND_VERIFY_IDENTITY = (byte) 10;
		/**
		 * request a AES
		 */
		public static final byte  GET_SERVER_AES_KEY = (byte) 11;
		/**
		 * emit our AES (encrypted with our private & his public key)
		 */
		public static final byte  SEND_SERVER_AES_KEY = (byte) 12;
		

	
	public abstract void receiveMessage(long senderId, byte messageId, ByteBuff message);
	
	
}
