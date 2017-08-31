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

}
