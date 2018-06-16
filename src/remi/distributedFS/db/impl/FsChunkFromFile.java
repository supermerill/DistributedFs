package remi.distributedFS.db.impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.db.UnreachableChunkException;
import remi.distributedFS.log.Logs;
import remi.distributedFS.util.ByteBuff;
import remi.distributedFS.util.Ref;

public class FsChunkFromFile extends FsObjectImplFromFile implements FsChunk {
//	protected FsTableLocal master;
	protected FsFileFromFile parentFile;
//	protected long id;
//	protected long sector;
	protected int idx; //not used
	protected int currentSize;
	protected int maxSize;
	/**
	 * false if data isn't available locally
	 */
	protected boolean isValid;
	protected long lastChange = 0;
	protected long lastaccess = 0;
	protected ShortList serverIdPresent = new ShortArrayList(2);

//	protected boolean loaded = false;
	protected File data; //don't load data on memory, just on-demand. => maybe that's the problem with wmp : too much latency?

	public FsChunkFromFile(FsTableLocal master, long sectorId, FsFileFromFile parent, long id) {
		super(master, sectorId, parent.getParent());
//		this.sector = sectorId;
		this.parentFile = parent;
		this.idx = -1;
		this.lastChange = 0;
		this.maxSize = 0; //maybe we should use factories (with interface for them)
		this.currentSize = 0;
		this.isValid = false;
//		this.loaded = false;
		this.id = id;//master.getComputerId()<<48 | ( sectorId&0xFFFFFFFFFFFFL);
	}

	@Override
	public boolean read(ByteBuff toAppend, int offset, int size) {
		ensureLoaded();
		Logs.logDb.info("read chunk");
		ensureDatafield();
		if(!data.exists()){
			Logs.logDb.warning("data '"+data.getPath()+"' doesn't exist!");
			return false;
		}
//		String str = "Hello world!";
//		toAppend.put( Arrays.copyOf(Charset.forName("UTF-8").encode(str).array(),str.length()));
		

		synchronized (this) {

			try(FileChannel dataChannel = FileChannel.open(data.toPath(), StandardOpenOption.READ)){
				
//				Logs.logDb.info("pos before = "+toAppend.position()+", wanted to go "+size+" more. Size = "+toAppend.array().length+" == ms:"+this.getMaxSize()+" >= s:"+this.currentSize);
				ByteBuffer buff = toAppend.toByteBuffer();
				buff.limit(buff.position()+size);
//				buff.position(buff.position()); //already done in toByteBuffer()
				dataChannel.read(buff, offset);

				toAppend.position(toAppend.position()+size);
//				Logs.logDb.info("pos after = "+toAppend.position());
				lastaccess = System.currentTimeMillis();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		toAppend.position(toAppend.position()+size);
		return false;
		
	}

	@Override
	public boolean write(ByteBuff toWrite, int offset, int size) {
		ensureLoaded();
		ensureDatafield();
		ensureFileExist();
		synchronized (this) {

			try(FileChannel dataChannel = FileChannel.open(data.toPath(), StandardOpenOption.WRITE)){
				
				ByteBuffer buff = toWrite.toByteBuffer();
				buff.limit(toWrite.position()+size);
//				buff.position(toWrite.position()); //already done in toByteBuffer()
				dataChannel.write(buff, offset);
				//sometimes it's not correct (if we re-use a file we didn't delete)
//				currentSize = (int) dataChannel.size();
				currentSize = Math.max(currentSize, offset+size);
				if((int) dataChannel.size() != currentSize){
					Logs.logDb.warning("Error: the chunk "+idx+" of file "+parentFile.getPath()+" has a file size of "+currentSize+" and the read fileahs a size of "+dataChannel.size());
				}
				dataChannel.close();

				Logs.logDb.info("toWrite.position = "+toWrite.position()+"+"+size+"=="+(toWrite.position()+size));
				toWrite.position(toWrite.position()+size);
				lastChange = System.currentTimeMillis();
				lastaccess = System.currentTimeMillis();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		toWrite.position(toWrite.position()+size);
		return false;
	}
	
	public void ensureLoaded(){
		if(!loaded){
			ByteBuffer buff = ByteBuffer.allocate(FsTableLocal.FS_SECTOR_SIZE);
			master.loadSector(buff, getSector());
			load(buff);
		}
	}

	
	protected void ensureDataPath(){
		if(data==null){
			data=new File(master.getRootRep()+"/"+getId());
		}
	}
	
	/**
	 * grab the data from the network if not available locally
	 */
	protected void ensureDatafield(){
		Logs.logDb.info("ensureDatafield");
		if(data==null){
			//it never change & is unique (TODO: multiple dirs to not e with 1million files in the same dir)
			//create folder path
//			if(!data.exists()){
//				try {
//					//we now store all on our root folder, no need to check if the dir exist
//					//TODO: create more rep to not make too much files in the same rep.
//					//		for example, a rep for each 1000 sectorid.
//					//create file
//					data.createNewFile();
////					Logs.logDb.info("data '"+data.getPath()+"' is now created");
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
			if(isValid){
				Logs.logDb.info("isValid");
				ensureDataPath();
			}else{
				//request it (via network)
				Logs.logDb.info("REQUEST DATA FOR CHUNK "+getId());
				FsChunk meWithData = master.getManager().requestChunk(this.parentFile, this, serverIdPresent());
				if(meWithData == null){
					//can't find it!
					throw new UnreachableChunkException("Error: can't find the chunk "+this.getId()+" in the cluster. Maybe you should reconnect with more peers.");
				}
				this.currentSize = meWithData.currentSize();
				this.maxSize = meWithData.getMaxSize();
				//create file & copy data
//				if(!data.exists()){
//					try {
//						data.createNewFile();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
				//copy
				synchronized(this){
					isValid = true; //to not proc this method again
					//too complicated, TODO : useful?
//					ByteBuff buff = null;
//					if(currentSize<=1024*1024){
//						buff = new ByteBuff(currentSize);
//					}else{
//						int fact = currentSize / (1024*1024);
//						buff = new ByteBuff(currentSize / fact);
//					}
//					for(int i=0;i<meWithData.currentSize();i+=buff.limit()){
//						buff.rewind();
//						meWithData.read(buff, i*buff.limit(), buff.limit());
//						this.write(buff, i*buff.limit(), buff.limit());
//					}
					
					//simplier version
					ByteBuff buff = new ByteBuff(currentSize);
					meWithData.read(buff, 0, buff.limit());
					ensureDataPath();
					this.write(buff, 0, buff.limit());
					
				}
				//ready!
//				lastChange = meWithData.lastModificationTimestamp();
//				parent.setDirty(true);
//				parent.flush();
			}
		}
	}
	
	@Override
	public String toString() {
		return id+" size:"+currentSize+"/"+maxSize;
	}

	
	public void flush() {
		ensureLoaded();
		ByteBuffer buff = ByteBuffer.allocate(FsTableLocal.FS_SECTOR_SIZE);
		//master.loadSector(buff, getId()); //useless, we want to write, not read!
		save(buff); //this one should auto-call master.write

		if(parentFile==null){
			//delete this one
//			this.delete();
			master.removeSector(this.getSector());
		}else{
			//alread saved in save()
//			master.saveSector(buff, this.getSector());
		}
	}
	

	public void load(ByteBuffer buffer){
		int buffInitPos = buffer.position(); //0
		//check if it's a chunk
		byte type = buffer.get(); //1
		if(type != FsTableLocal.CHUNK){
//			Logs.logDb.warning("Error, not a file at "+getId());
			throw new WrongSectorTypeException("Error, not a chunk @"+sector+", type="+type+" != "+FsTableLocal.CHUNK+"=CHUNK");
		}
		
		//check  id
		this.id = buffer.getLong(); //9
//		if(myId != id){
//			Logs.logDb.warning("Error, wrong chunk id : "+id + " <> "+myId);
//			throw new RuntimeException("Error, wrong chunk id : "+id + " <> "+myId);
//		}
		
		//datas
		currentSize = buffer.getInt(); //13
		maxSize = buffer.getInt(); //17
		idx = buffer.getInt(); //21
		lastChange = buffer.getLong(); //29
		lastaccess = buffer.getLong(); //37
		isValid = buffer.get()==1; //38
		
		
		//get filename  (useless... for now)
		int nbServerId = buffer.getInt(); //42
//		int nbBytesFilename = buffer.getInt(); //46
//		ByteBuffer nameBuffer = ByteBuffer.allocate(nbBytesFilename);
		
		buffer.position(buffInitPos + 64); // 64 to have enough space to store all small datas before
		int canRead =  FsTableLocal.FS_SECTOR_SIZE - (buffer.position() + 8);
		
//		int canRead = FsTableLocal.FS_SECTOR_SIZE-64-8;
		ByteBuffer currentBuffer = buffer;

		Ref<Integer> sectorNum = new Ref<>(0);
		//read serverlist
		for(int i=0;i<nbServerId;i++){
			serverIdPresent.add(currentBuffer.getShort());
			canRead-=2;
			if(canRead == 0){
				canRead = goToNextAndLoad(currentBuffer, sectorNum)*8;
			}
		}
		
		//read filename
//		long currentSector = this.getId();
		//read folder
//		for(int i=0;i<nbBytesFilename;i++){
//			nameBuffer.put(currentBuffer.get());
//			canRead--;
//			if(canRead == 0){
//				canRead = goToNextAndLoad(currentBuffer)*8;
//			}
//		}
//		nameBuffer.flip();
		//lol it's not used as we use the id!!!
//		data = new File(FsObjectImplFromFile.CHARSET.decode(nameBuffer).toString());
//		ensureDatafield();
//		Logs.logDb.info("chunk has data in "+data.getPath());
		
		this.loaded = true;
		
	}

	
	public synchronized void save(ByteBuffer buffer){
		if(!loaded) return;
		
		int buffInitPos = buffer.position();
		
		//set "erased" or d"directory"
		buffer.put(parentFile==null?0:FsTableLocal.CHUNK);

		buffer.putLong(getId());
		
		//save data
		buffer.putInt(currentSize);
		buffer.putInt(maxSize);
		buffer.putInt(idx);
		buffer.putLong(lastChange); 
		buffer.putLong(lastaccess); 
		buffer.put((byte)(isValid?1:0)); 

		//get name buffer
//		ensureDataPath(); //it's not useful to save the path name, as it's constructed from id. TODO: remove saving name
//		ByteBuffer buffName = null;
//		if(data!=null){
//			buffName = FsObjectImplFromFile.CHARSET.encode(data.getPath());
//		}else{
//			buffName = FsObjectImplFromFile.CHARSET.encode("");
//		}
		
		//buffName.flip(); already done in encode()
		buffer.putInt(serverIdPresent.size());
//		buffer.putInt(buffName.limit());

		buffer.position(buffInitPos + 64); // 64 to have enough space to store all small datas before
		int canRead =  FsTableLocal.FS_SECTOR_SIZE - (buffer.position() + 8);
//		int canRead = FsTableLocal.FS_SECTOR_SIZE-64-8;
		
		ByteBuffer currentBuffer = buffer;
//		Ref<Long> currentSector = new Ref<>(this.getSector());
		Ref<Integer> sectorNum = new Ref<>(0);
		//save serverlist
		for(int i=0;i<serverIdPresent.size();i++){
			currentBuffer.putShort(serverIdPresent.getShort(i));
			canRead-=2;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, sectorNum)*8;
			}
		}
		//save name
//		for(int i=0;i<buffName.limit();i++){
//			currentBuffer.put(buffName.get(i));
//			canRead--;
//			if(canRead == 0){
//				canRead = goToNextOrCreate(currentBuffer, currentSector)*8;
//			}
//		}

		//write last sector
		flushLastSector(currentBuffer, sectorNum);
//		currentBuffer.rewind();
//		master.saveSector(currentBuffer, currentSector.get());
	}

	@Override
	public long getId() {
		ensureLoaded();
		return id;
	}

	public long getSector() {
		return sector;
	}

	@Override
	public int currentSize() {
		ensureLoaded();
		return currentSize;
	}

	@Override
	public int getMaxSize() {
		ensureLoaded();
		return maxSize;
	}

	@Override
	public void setMaxSize(int newMaxSize) {
		ensureLoaded();
		maxSize = newMaxSize;
	}

	@Override
	public boolean isPresent() {
		ensureLoaded();
		return isValid;
	}

	@Override
	public ShortList serverIdPresent() {
		ensureLoaded();
		return serverIdPresent;
	}

	@Override
	public long getModifyDate() {
		ensureLoaded();
		return lastChange;
	}

	@Override
	public long getModifyUID() {
		ensureLoaded();
		return master.getUserId();
	}

	public void unallocate() {
		ensureLoaded();
		ensureDataPath();
		data.delete();
		ByteBuffer buff = ByteBuffer.allocate(FsTableLocal.FS_SECTOR_SIZE);
		buff.put((byte)0);
		buff.rewind();
		master.saveSector(buff, sector);
		master.removeSector(sector);
	}

	public void setCurrentSize(int newSize) {
		ensureLoaded();
		if(isValid){
			ensureDatafield();
			ensureFileExist();
	
			synchronized (this) {
	
				try(FileChannel dataChannel = FileChannel.open(data.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)){
					
					if( dataChannel.size() < newSize){
						//grow
						dataChannel.write(ByteBuffer.allocate((int) (newSize - dataChannel.size())), dataChannel.size());
					}else{
						//reduce (or let the same)
						dataChannel.truncate(newSize);
					}
					currentSize = (int) dataChannel.size();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}else{
			currentSize = newSize;
		}
	}

	protected void ensureFileExist() {
		if(!data.exists()){
			try {
//				//we now store all on our root folder, no need to check if the dir exist
//				//TODO: create more rep to not make too much files in the same rep.
//				//		for example, a rep for each 1000 sectorid.
//				//create file
				data.createNewFile();
//				Logs.logDb.info("data '"+data.getPath()+"' is now created");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setPresent(boolean isPresentLocally) {
		ensureLoaded();
		Logs.logDb.info("Chunk : setPresent "+isValid+" -> "+isPresentLocally);
		isValid = isPresentLocally;
		//if invalidate, remove local datafile
		if(!isValid){
			ensureDataPath();
			if(data.exists()){
				Logs.logDb.info("Chunk : delete data on disk");
				data.delete();
			}
			data = null;
		}
	}

	@Override
	public void changes() {
		ensureLoaded();
		lastChange = System.currentTimeMillis();
	}

	@Override
	public void delete(){
		//delete file if present
		ensureDataPath();
		if(data.exists()) data.delete();
		
		//delete self
		deleteSectors();
	}
	
	

	public void deleteSectors(){
		if(sector<=0){
			Logs.logDb.warning("Error, trying to del chunk "+id+" with sector id of "+getSector()+" :  impossible!");
			throw new RuntimeException("Error, trying to del chunk "+id+" with sector id of "+getSector()+" :  impossible!");
		}
		//delete entry into fs table
		//get last long in our fs
		ByteBuffer buff = ByteBuffer.allocate(FsTableLocal.FS_SECTOR_SIZE);
		master.loadSector(buff, getSector());
		load(buff);
		buff.position(buff.limit()-8);
		long nextSector = buff.getLong();
		if(nextSector>0){
			LongList sectorsToDel = new LongArrayList();
			while(nextSector>0){
				sectorsToDel.add(nextSector);
				master.loadSector(buff, nextSector);
				long type = buff.getLong();
				if(type != FsTableLocal.EXTENSION){
					Logs.logDb.warning("Warn : deleted object "+id+" has an extedned sector occupied by somethign else. Sector id: "+nextSector+" : "+type);
					break;
				}
				buff.position(buff.limit()-8);
				nextSector = buff.getLong();
			}
			Arrays.fill(buff.array(), (byte)0);
			//del other ones
			for(int i=sectorsToDel.size()-1;i>=0;i--){
				buff.rewind();
				master.saveSector(buff, sectorsToDel.getLong(i));
			}
		}else{
			Arrays.fill(buff.array(), (byte)0);
		}
		
		//del main sector
		buff.rewind();
		master.saveSector(buff, getSector());
	}

	@Override
	public void accept(FsObjectVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public long getLastAccessDate() {
		return lastaccess;
	}

}
