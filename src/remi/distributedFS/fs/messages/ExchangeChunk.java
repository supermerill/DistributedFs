package remi.distributedFS.fs.messages;

import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getPathFile;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.fs.StandardManager;
import remi.distributedFS.util.ByteBuff;

/**
 * Gere les echanges de chunks dans le réseau.
 * 
 * TODO: utiliser des stats réseau pour demander des morceuaux de chunks de façon répartie sur els hosts qui répondent "oui je l'ai".
 * plutot que demander à tous de tout passer d'un coup.
 * 
 * @author Admin
 *
 */
public class ExchangeChunk extends AbstractFSMessageManager {
	
	class Request{
		long arrivalDate;
		String path;
		long modifyDate;
		long modifyUID;
		int idx;
		ByteBuff msg;
	}
	
	List<Semaphore> waiters = new java.util.concurrent.CopyOnWriteArrayList<>();
	
	List<Request> requests = new java.util.concurrent.CopyOnWriteArrayList<>();
	

	private StandardManager manager;

	public ExchangeChunk(StandardManager standardManager) {
		this.manager = standardManager;
	}

	@Override
	public void receiveMessage(long senderId, byte messageId, ByteBuff message) {
		if (messageId == SEND_FILE_CHUNK) {
			readChunk(senderId, message);
		}
		if (messageId == GET_FILE_CHUNK) {
			//check if we have it
			String filePath = message.getUTF8();
			long modifyDateMin = message.getLong();
			int idx = message.getInt();
			FsChunk chunkOk = null;

			FsDirectory root = manager.getRoot();
			FsFile fic = getPathFile(root, filePath);
			if(fic != null && fic.getNbChunks()>idx && fic.getChunks().size()>idx){
				FsChunk chunk = fic.getChunks().get(idx);
				if(chunk.isPresent() && chunk.lastModificationTimestamp()>=modifyDateMin){
					//find
					chunkOk = chunk;
				}
			}
			
			if(chunkOk != null){
				sendChunk(senderId, fic, idx, chunkOk);
			}else{
				System.out.println("GET CHUNK : can't find it!");
				ByteBuff buff = new ByteBuff();
				buff.put((byte)0);
				buff.putUTF8(filePath);
				buff.putLong(modifyDateMin);
				buff.putInt(idx);
				buff.flip();
				manager.getNet().writeMessage(senderId, SEND_FILE_CHUNK, buff);
			}
		}

	}

	private void readChunk(long senderId, ByteBuff message) {
		System.out.println("READ SEND CHUNK : read it!");
		if(message.get()==1){
			Request req = new Request();
			req.arrivalDate = System.currentTimeMillis();
			req.path = message.getUTF8();
			req.modifyDate = message.getLong();
			req.modifyUID = message.getLong();
			req.idx = message.getInt();
			int nbBytes = message.getTrailInt();
			req.msg = message;
			synchronized (requests) {
				requests.add(req);
			}
			for(Semaphore w : waiters){
				w.release();
			}
		}else{
			System.out.println("the other server doesn't have my chunk for "+message.getUTF8());
		}
	}

	private void sendChunk(long senderId, FsFile fic, int idx, FsChunk chunkOk) {

		System.out.println("SEND CHUNK : send it!");
		ByteBuff buff = new ByteBuff();
		buff.put((byte)1);
		buff.putUTF8(fic.getPath());
		buff.putLong(chunkOk.lastModificationTimestamp());
		buff.putLong(chunkOk.lastModificationUID());
		buff.putInt(idx);
		//send chunk data
		buff.putTrailInt(chunkOk.currentSize());
		buff.limit(buff.position()+chunkOk.currentSize());
		chunkOk.read(buff, 0, chunkOk.currentSize());
		buff.flip();
		manager.getNet().writeMessage(senderId, SEND_FILE_CHUNK, buff);
	}

	public void requestchunk(String path, long modifyDate, int idx) {
		System.out.println(this.manager.getComputerId()%100+" WRITE GET CHUNK "+path);
		ByteBuff buff = new ByteBuff();
		buff.putUTF8(path);
		buff.putLong(modifyDate);
		buff.putInt(idx);
		buff.flip();
		manager.getNet().writeBroadcastMessage(GET_FILE_CHUNK, buff);
	}
	
	public FsChunk waitReceiveChunk(String path, long modifyDate, int idx) {
		Semaphore mySema= new Semaphore(1);
		waiters.add(mySema);
		try {
			FsChunk retVal = null;
			while(retVal == null){
				mySema.tryAcquire(1, 10, TimeUnit.SECONDS);
				System.out.println("check msgs for chunks");
				//check received messages
				synchronized (requests) {
					Iterator<Request> it = requests.iterator();
					while(it.hasNext()){
						Request req = it.next();
						if(req.path.equals(path) && modifyDate <= req.modifyDate && idx == req.idx){
							//my message!
							System.out.println("check msgs for chunks : FIND MY ONE!");
							retVal = new FsChunkBuffer(req.msg, req.modifyDate, req.modifyUID);
							it.remove();
							break;
						}
					}
				}
			}
			return retVal;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
