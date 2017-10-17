package remi.distributedFS.fs.messages;

import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.getPathFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.db.NotFindedException;
import remi.distributedFS.db.TimeoutException;
import remi.distributedFS.fs.StandardManager;
import remi.distributedFS.net.ClusterManager;
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
		public Request(boolean finded) {
			this.finded = finded;
		}
		//message
		long arrivalDate;
		ByteBuff msg;
		boolean finded;
		
		//file
		public long fileId;
		String path;
		long modifyDate;
		long modifyUID;
		//chunk
		public long chunkId;
		public long chunkDate;
		public long chunkDateUID;
		public int nbBytes; // = chunkSize
		public int chunkMaxSize;
		public LongList serverIds;
	}
	
	List<Semaphore> waiters = new java.util.concurrent.CopyOnWriteArrayList<>();
	
	List<Request> requests = new ArrayList<>();
	

	private StandardManager manager;

	public ExchangeChunk(StandardManager standardManager) {
		this.manager = standardManager;
	}

	@Override
	public void receiveMessage(long senderId, byte messageId, ByteBuff message) {
		if(manager.getNet().getComputerId(senderId) <0){
			//error: not a estabished peer
			System.err.println("Error, peer "+senderId%100+" ask us a chunk and he doens't have a computerid !");
			return;
		}
		if (messageId == SEND_FILE_CHUNK) {
			System.out.println(this.manager.getComputerId()+"$ RECEIVE SEND_FILE_CHUNK from "+senderId);
			readChunk(senderId, message);
		}
		if (messageId == GET_FILE_CHUNK) {
			System.out.println(this.manager.getComputerId()+"$ RECEIVE GET_FILE_CHUNK from "+senderId);
			//check if we have it
			long fileId = message.getLong();
			String filePath = message.getUTF8();
			long modifyDateMin = message.getLong();
			long chunkId = message.getLong();
			long chunkModDate = message.getLong();
			FsChunk chunkOk = null;

			FsDirectory root = manager.getRoot();
			FsFile fic = getPathFile(root, filePath);
			if(fic != null){
				FsChunk chunk = FsFile.getChunk(fic, chunkId);
				if(chunk == null){
					//nothing to do : we don't have it.
					System.out.println("GET CHUNK : can't find it!");
					ByteBuff buff = new ByteBuff();
					buff.put((byte)0);
					buff.putLong(fileId);
					buff.putUTF8(filePath);
					buff.putLong(modifyDateMin);
					buff.putLong(chunkId);
					buff.flip();
					System.out.println(this.manager.getComputerId()+"$ Can't send unknown chunk, send file instead "+senderId);
					manager.getNet().writeMessage(senderId, SEND_FILE_CHUNK, buff);
					return;
				}
				if(chunk.isPresent() && chunk.getModifyDate()>=chunkModDate){
					System.out.println(this.manager.getComputerId()+"$ Good chunk "+chunk.getId());
					//find
					chunkOk = chunk;
				}else{
					System.out.println(this.manager.getComputerId()+"$ Old/nothere chunk: our:"+chunk.getModifyDate()+">=their:"+chunkModDate+", present:"+chunk.isPresent());
				}
			}
			
			if(chunkOk != null){
				sendChunk(senderId, fic, chunkOk);
			}else{
				System.out.println(this.manager.getComputerId()+"$ GET CHUNK : not good enough (or can't find it)!");
				ByteBuff buff = new ByteBuff();
				buff.put((byte)0);
				buff.putLong(fileId);
				buff.putUTF8(filePath);
				buff.putLong(modifyDateMin);
				buff.putLong(chunkId);
				buff.putLong(chunkModDate);
				buff.flip();
				manager.getNet().writeMessage(senderId, SEND_FILE_CHUNK, buff);
				return;
			}
		}

	}

	private void readChunk(long senderId, ByteBuff message) {
		System.out.println("READ SEND CHUNK : read it!");
		if(message.get()==1){
			Request req = new Request(true);
			req.arrivalDate = System.currentTimeMillis();
			req.fileId = message.getLong();
			req.path = message.getUTF8();
			req.modifyDate = message.getLong();
			req.modifyUID = message.getLong();
			req.chunkId = message.getLong();
			req.chunkDate = message.getLong();
			req.chunkDateUID = message.getLong();
			req.chunkMaxSize = message.getInt();
			//serverId list
			int nbServers = message.getTrailInt();
			req.serverIds = new LongArrayList(nbServers);
			for(int i=0;i<nbServers;i++){
				long sid = message.getLong();
				if(sid >= 0 && sid != this.manager.getNet().getComputerId()){
					req.serverIds.add(sid);
				}
			}
			if(!req.serverIds.contains(this.manager.getNet().getComputerId(senderId)))
				req.serverIds.add(this.manager.getNet().getComputerId(senderId));
			//read chunk data
			req.nbBytes = message.getTrailInt();
			req.msg = message;
			synchronized (requests) {
				requests.add(req);
			}
			for(Semaphore w : waiters){
				w.release();
			}
		}else{
			Request req = new Request(false);
			req.fileId = message.getLong();
			req.path = message.getUTF8();
			req.modifyDate = message.getLong();
			req.chunkId = message.getLong();
			req.chunkDate = message.getLong();
			System.out.println("the other server doesn't have my chunk for "+req.path);
			req.msg = message;
			synchronized (requests) {
				requests.add(req);
			}
			for(Semaphore w : waiters){
				w.release();
			}
		}
	}

	private void sendChunk(long senderId, FsFile fic, FsChunk chunkOk) {

		System.out.println(this.manager.getComputerId()+"$ SEND CHUNK : send it! : "+chunkOk.currentSize()+" / "+chunkOk.getMaxSize());
		ByteBuff buff = new ByteBuff();
		buff.put((byte)1);
		buff.putLong(fic.getId());
		buff.putUTF8(fic.getPath());
		buff.putLong(fic.getModifyDate());
		buff.putLong(fic.getModifyUID());
		buff.putLong(chunkOk.getId());
		buff.putLong(chunkOk.getModifyDate());
		buff.putLong(chunkOk.getModifyUID());
		buff.putInt(chunkOk.getMaxSize());
		//serverId list
		LongList serverIdList = chunkOk.serverIdPresent();
		buff.putTrailInt(serverIdList.size());
		for(int i=0;i<serverIdList.size();i++){
			buff.putLong(serverIdList.getLong(i));
		}
		//send chunk data
		buff.putTrailInt(chunkOk.currentSize());
		buff.limit(buff.position()+chunkOk.currentSize());
		chunkOk.read(buff, 0, chunkOk.currentSize());
		buff.flip();
		manager.getNet().writeMessage(senderId, SEND_FILE_CHUNK, buff);
	}

	public int requestchunk(FsFile fic, FsChunk chunk) {
		System.out.println(this.manager.getComputerId()%100+" WRITE GET CHUNK "+fic.getPath()+" : "+chunk.getId());
		ByteBuff buff = new ByteBuff();
		buff.putLong(fic.getId());
		buff.putUTF8(fic.getPath());
		buff.putLong(fic.getModifyDate());
		buff.putLong(chunk.getId());
		buff.putLong(chunk.getModifyDate());
		buff.flip();
		return manager.getNet().writeBroadcastMessage(GET_FILE_CHUNK, buff);
	}
	
	public FsChunk waitReceiveChunk(final long idChunk, final long idFile, final long modifyDateFile, final int nbReqEmitted) {
		int nbreceived = 0;
		final long timeToStop = System.currentTimeMillis() + (1000 * 100); // max wait : 1 min30 (for max chunk size : 1go => 10mo/s => 100mb/s) TODO :parameterize it.
		System.out.println("timeToStop = "+timeToStop+" == "+System.currentTimeMillis() +" + "+ (1000 * 100));
		Semaphore mySema= new Semaphore(1);
		waiters.add(mySema);
		try {
			FsChunk retVal = null;
			while(retVal == null && nbreceived < nbReqEmitted && timeToStop > System.currentTimeMillis()){
				mySema.tryAcquire(1, 3, TimeUnit.SECONDS);
				System.out.println("check msgs for chunks (i need "+idChunk+")");
				//check received messages
				synchronized (requests) {
					Iterator<Request> it = requests.iterator();
					while(it.hasNext()){
						Request req = it.next();
						if(idFile == req.fileId && idChunk == req.chunkId && req.finded){
							//my message!
							//TODO: do not use modifyDateFile?
							System.out.println("check msgs for chunks : FIND MY ONE!");
							retVal = new FsChunkBuffer(req.msg, req.modifyDate, req.modifyUID, idChunk);
							retVal.setCurrentSize(req.nbBytes);
							retVal.setMaxSize(req.chunkMaxSize);
							retVal.serverIdPresent().addAll(req.serverIds);
							it.remove();
							System.out.println("check msgs for chunks : FIND MY ONE: "+retVal);
							break;
						}else if(idChunk == req.chunkId){
							//the chunk has been reused in an other file?
							System.out.println("check msgs for chunks ,find "+req.chunkId+"  but wrong file "+idFile +" != "+req.fileId);
							return null;
						}else if(idChunk == req.chunkId && !req.finded){
							System.out.println("check msgs for chunks ,find "+req.chunkId+"  but he doesn't have it "+idFile +" != "+req.fileId);
							nbreceived ++;
						}else{
							System.out.println("check msgs for chunks ,find "+req.chunkId+"  but i need "+idChunk+"");
						}
					}
					System.out.println("check msgs for chunks : CANNOT FIND MY ONE :'(");
				}

				if(retVal == null){
					if(nbreceived==nbReqEmitted ){
						//clean
						Iterator<Request> it = requests.iterator();
						while(it.hasNext()){
							Request req = it.next();
							if(idChunk == req.chunkId){
								it.remove();
							}
						}
						System.err.println("Warn : can't find chunk "+idChunk+" in the net ("+nbreceived+"/"+nbReqEmitted+")");
						throw new NotFindedException("Warn : can't find chunk "+idChunk+" in the network");
					}else if(timeToStop<System.currentTimeMillis()){
						//clean
						Iterator<Request> it = requests.iterator();
						while(it.hasNext()){
							Request req = it.next();
							if(idChunk == req.chunkId){
								it.remove();
							}
						}
						System.err.println("Warn : can't find chunk "+idChunk+" in the net in less than the max timeout ("+System.currentTimeMillis()+" > "+timeToStop);
						throw new TimeoutException("Warn : can't find chunk "+idChunk+" in the net in less than the max timeout");
					}
				}
				System.out.println("relance : "+nbreceived+" < "+nbReqEmitted+" && timeok?"+(timeToStop > System.currentTimeMillis()));
			}
			System.out.println("check msgs for chunks : return: "+retVal);
			return retVal;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}finally {
			waiters.remove(mySema);
		}
	}

	public void register(ClusterManager net) {
		net.registerListener(GET_FILE_CHUNK, this);
		net.registerListener(SEND_FILE_CHUNK, this);
	}

}
