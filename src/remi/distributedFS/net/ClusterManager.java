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
	public void writeBroadcastMessage(byte sendFileDescr, ByteBuff message);
	public void writeMessage(long senderId, byte sendDir, ByteBuff messageRet);
	public void registerListener(byte getDir, AbstractMessageManager propagateChange);
	public void init(int listenPort);
	
	
	public void connect(String string, int port);
	public short getComputerId();
	public void launchUpdater();
	public void initializeNewCluster();
	public short getComputerId(long senderId); //get a computerId from a peerId (senderId)

}
