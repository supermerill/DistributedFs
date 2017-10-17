package remi.distributedFS.net;

import remi.distributedFS.util.ByteBuff;

public interface ClusterManager {
	
	//not used ... yet?
	public void requestUpdate(long since);
	public void requestChunk(long fileId, long chunkId);
	public void propagateDirectoryChange(long directoryId, byte[] changes ); //TODO
	public void propagateFileChange(long directoryId, byte[] changes ); //TODO
	public void propagateChunkChange(long directoryId, byte[] changes ); //TODO
	
	
	
	//used
	/**
	 * 
	 * @param sendFileDescr
	 * @param message
	 * @return number of peer conteacted (do not assumed they will all answer)
	 */
	public int writeBroadcastMessage(byte sendFileDescr, ByteBuff message);
	/**
	 * 
	 * @param senderId
	 * @param sendDir
	 * @param messageRet
	 * @return true if the message should be emitted (no guaranty)
	 */
	public boolean writeMessage(long senderId, byte sendDir, ByteBuff messageRet);
	public void registerListener(byte getDir, AbstractMessageManager propagateChange);
	public void init(int listenPort);
	
	
	public void connect(String string, int port);
	public short getComputerId();
	public void launchUpdater();
	public void initializeNewCluster();
	public short getComputerId(long senderId); //get a computerId from a peerId (senderId)
	public long getSenderId(short compId); //get a peerId (senderId) from a computerId

}
