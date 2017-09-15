package remi.distributedFS.fs.messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import remi.distributedFS.net.AbstractMessageManager;
import remi.distributedFS.util.ByteBuff;


public abstract class AbstractFSMessageManager extends AbstractMessageManager {
	
		// ----------------------------------- data save & balancing ---------------------------------------------
		
		/**
		 * GetDirectoryContent
		 * GET_DIR
		 * SEND_DIR
		 * GetFileDescriptor
		 * SEND_FILE_DESCR
		 * GetFileChunk
		 * SEND_FILE_CHUNK
		 */

		/**
		 */
		public static final byte  GET_DIR = (byte) 20;
		public static final byte  SEND_DIR = (byte) 21;
		public static final byte  GET_FILE_DESCR = (byte) 22;
		public static final byte  SEND_FILE_DESCR = (byte) 23;
		public static final byte  GET_FILE_CHUNK = (byte) 24;
		public static final byte  SEND_FILE_CHUNK = (byte) 25;
		public static final byte  GET_OBJECT = (byte) 26;
		public static final byte  SEND_OBJECT = (byte) 27;
		
	
}
