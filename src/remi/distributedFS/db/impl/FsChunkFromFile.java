package remi.distributedFS.db.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.util.ByteBuff;
import remi.distributedFS.util.Ref;

public class FsChunkFromFile implements FsChunk {
	protected FsTableLocal master;
	protected FsFileFromFile parent;
	protected long id;
	protected long sector;
	protected int idx;
	protected int currentSize;
	protected int maxSize;
	protected boolean isValid; // false if data isn't available locally
	protected long lastChange = 0;

	protected boolean loaded = false;
	protected File data;

	public FsChunkFromFile(FsTableLocal master, long sectorId, FsFileFromFile parent, int idx) {
		this.sector = sectorId;
		this.parent = parent;
		this.master = master;
		this.idx = idx;
		this.lastChange = 0;
		this.isValid = false;
		this.loaded = false;
		this.id = master.getComputerId()<<48 | ( sectorId&0xFFFFFFFFFFFFL);
	}

	@Override
	public boolean read(ByteBuff toAppend, int offset, int size) {
		ensureLoaded();
		ensureDatafield();
		if(!data.exists()){
			System.err.println("data '"+data.getPath()+"' doesn't exist!");
			return false;
		}
//		String str = "Hello world!";
//		toAppend.put( Arrays.copyOf(Charset.forName("UTF-8").encode(str).array(),str.length()));
		

		synchronized (this) {

			try {
				FileChannel dataChannel = FileChannel.open(data.toPath(), StandardOpenOption.READ);
				
				System.out.println("pos before = "+toAppend.position());
				ByteBuffer buff = toAppend.toByteBuffer();
				buff.limit(buff.position()+size);
				dataChannel.read(buff, offset);
				dataChannel.close();

				toAppend.position(toAppend.position()+size);
				System.out.println("pos after = "+toAppend.position());
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
		//create folder path
		if(!data.exists()){
			try {
				//ensure dirs exists
//				File myDir = new File(master.getRootRep()+parent.getParent().getPath());
//				if(!myDir.exists()) myDir.mkdirs();
				//useless, we now store all on our root folder
				
				
				//create file
				data.createNewFile();
				System.err.println("data '"+data.getPath()+"'is now created");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		synchronized (this) {

			try {
				FileChannel dataChannel = FileChannel.open(data.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
				
				ByteBuffer buff = toWrite.toByteBuffer();
				buff.limit(buff.position()+size);
				toWrite.rewind();
				dataChannel.write(buff, offset);
				//sometimes it's not correct (if we re-use a file we didn't delete)
//				currentSize = (int) dataChannel.size();
				currentSize = Math.max(currentSize, offset+size);
				if((int) dataChannel.size() != currentSize){
					System.err.println("Error: the chunk "+idx+" of file "+parent.getPath()+" has a file size of "+currentSize+" and the read fileahs a size of "+dataChannel.size());
				}
				dataChannel.close();

				toWrite.position(toWrite.position()+size);
				lastChange = System.currentTimeMillis();
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
	

	public void load(ByteBuffer buffer){
		int buffInitPos = buffer.position();
		//check if it's a chunk
		byte type = buffer.get(); //1
		if(type != FsTableLocal.CHUNK){
			System.err.println("Error, not a file at "+getId());
			throw new RuntimeException("Error, not a file at "+getId());
		}
		
		//check  id
		this.id = buffer.getLong(); //9
//		if(myId != id){
//			System.err.println("Error, wrong chunk id : "+id + " <> "+myId);
//			throw new RuntimeException("Error, wrong chunk id : "+id + " <> "+myId);
//		}
		
		//datas
		currentSize = buffer.getInt(); //13
		maxSize = buffer.getInt(); //17
		idx = buffer.getInt(); //21
		isValid = buffer.get()==1; //22
		
		
		//get filename  (useless... for now)
		int nbBytes = buffer.getInt(); //26
		ByteBuffer nameBuffer = ByteBuffer.allocate(nbBytes);
		
		buffer.position(buffInitPos + 32);
		
		int canRead = FsTableLocal.FS_SECTOR_SIZE-32-8;
		ByteBuffer currentBuffer = buffer;
//		long currentSector = this.getId();
		//read folder
		for(int i=0;i<nbBytes;i++){
			nameBuffer.put(currentBuffer.get());
			canRead--;
			if(canRead == 0){
				canRead = goToNext(currentBuffer);
			}
		}
		nameBuffer.flip();
//		data = new File(FsObjectImplFromFile.CHARSET.decode(nameBuffer).toString());
		ensureDatafield();
		System.out.println("chunk has data in "+data.getPath());
		
		this.loaded = true;
		
	}
	
	protected void ensureDatafield(){
		if(data==null){
			//it never change & is unique (because the option to "defragment"/move the descriptors isn't implemented yet)
			data=new File(master.getRootRep()+"/"+getId());
			if(!isValid){
				//request it
				FsChunk meWithData = master.getManager().requestChunk(this.parent, idx, serverIdPresent());
				this.currentSize = meWithData.currentSize();
				//create file & copy data
				if(!data.exists()){
					try {
						data.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				//copy
				ByteBuff buff = new ByteBuff(256*256);
				for(int i=0;i<meWithData.currentSize();i+=buff.limit()){
					buff.rewind();
					meWithData.read(buff, i*buff.limit(), buff.limit());
					this.write(buff, i*buff.limit(), buff.limit());
				}
				//ready!
				lastChange = meWithData.lastModificationTimestamp();
				isValid = true;
				parent.setDirty(true);
				parent.flush();
			}
		}
	}
	

	
	public void flush() {
		ensureLoaded();
		ByteBuffer buff = ByteBuffer.allocate(FsTableLocal.FS_SECTOR_SIZE);
		//master.loadSector(buff, getId()); //useless, we want to write, not read!
		save(buff); //this one should auto-call master.write
		
		if(parent==null){
			//delete this one
//			this.delete();
			master.releaseSector(this.getSector());
		}else{
			master.saveSector(buff, this.getSector());
		}
	}

	
	public synchronized void save(ByteBuffer buffer){
		if(!loaded) return;
		
		int buffInitPos = buffer.position();
		
		//set "erased" or d"directory"
		buffer.put(parent==null?0:FsTableLocal.CHUNK);

		buffer.putLong(getId());
		
		//save data
		buffer.putInt(currentSize);
		buffer.putInt(maxSize);
		buffer.putInt(idx);
		buffer.put((byte)(isValid?1:0));
		
		//get name buffer
		ensureDatafield();
		ByteBuffer buffName = FsObjectImplFromFile.CHARSET.encode(data.getPath());
		//buffName.flip(); already done in encode()
		buffer.putInt(buffName.limit());

		buffer.position(buffInitPos + 32);
		int canRead = FsTableLocal.FS_SECTOR_SIZE-32-8;
		
		ByteBuffer currentBuffer = buffer;
		Ref<Long> currentSector = new Ref<>(this.getSector());
		//save folder
		for(int i=0;i<buffName.limit();i++){
			currentBuffer.put(buffName.get(i));
			canRead--;
			if(canRead == 0){
				canRead = goToNextOrCreate(currentBuffer, currentSector);
			}
		}

		//write last sector
		currentBuffer.rewind();
		master.saveSector(currentBuffer, currentSector.get());
	}
	

	public int goToNextOrCreate(ByteBuffer buff, Ref<Long> currentSector){
		int pos = buff.position();
		long nextsector = buff.getLong();
		buff.position(pos);
		if(nextsector<=0){
			long newSectorId = master.requestNewSector();
			buff.putLong(newSectorId);
			buff.rewind();
			master.saveSector(buff, currentSector.get());
			currentSector.set(newSectorId);
			//master.loadSector(buff, newSectorId); //useless, it's not created yet!
			buff.rewind();
			buff.put(FsTableLocal.EXTENSION);
			buff.putLong(this.getSector());
			buff.position(FsTableLocal.FS_SECTOR_SIZE-8);
			buff.putLong(-1);
			buff.position(16);
			return (FsTableLocal.FS_SECTOR_SIZE / 8) - 3;
		}else{
			return goToNext(buff);
		}
	}
	

	/**
	 * return the number of long that can be read.
	 * @param buff
	 * @return nb long to read (max)
	 */
	public int goToNext(ByteBuffer buff){
		long nextsector = buff.getLong();
		if(nextsector<=0){
				System.err.println("Error, no more sector to parse for chunk "+this.getSector());
				throw new RuntimeException("Error, no more sector to parse for chunk "+this.getSector());
			
		}
		buff.rewind();
		master.loadSector(buff, nextsector);
		if(buff.get()==FsTableLocal.EXTENSION){
			//ok
			//verify it's mine
			long storedSec = buff.getLong();
			if(storedSec != this.getSector()){
				System.err.println("Error, my next sector is not picked for me !!! : "+storedSec+" != "+sector);
				throw new RuntimeException("Error, my next sector is not picked for me !!! : "+storedSec+" != "+sector);
			}
			//now i should be at pos 9
			//go to ok pos
			buff.position(16);
			return (FsTableLocal.FS_SECTOR_SIZE / 8) - 3; //128 -2(prefix) -1(suffix)
		}else{
			System.err.println("Error, my next sector is not picked for me !!!");
			throw new RuntimeException("Error, my next sector is not picked for me !!!");
		}
	}

	public long getId() {
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
	public int maxSize() {
		ensureLoaded();
		return maxSize;
	}

	@Override
	public boolean isPresent() {
		ensureLoaded();
		return isValid;
	}

	@Override
	public List<Long> serverIdPresent() {
		ensureLoaded();
		// TODO Auto-generated method stub
		return new ArrayList<Long>();
	}

	@Override
	public long lastModificationTimestamp() {
		ensureLoaded();
		return lastChange;
	}

	@Override
	public long lastModificationUID() {
		ensureLoaded();
		return master.getUserId();
	}

	public void unallocate() {
		ensureLoaded();
		ensureDatafield();
		data.delete();
		ByteBuffer buff = ByteBuffer.allocate(FsTableLocal.FS_SECTOR_SIZE);
		buff.put((byte)0);
		buff.rewind();
		master.saveSector(buff, sector);
		master.releaseSector(sector);
	}

	public void truncate(long newSize) {
		ensureLoaded();
		ensureDatafield();

		synchronized (this) {

			try {
				FileChannel dataChannel = FileChannel.open(data.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
				
				dataChannel.truncate(newSize);
				currentSize = (int) dataChannel.size();
				dataChannel.close();
				
				lastChange = System.currentTimeMillis();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setPresent(boolean isPresentLocally) {
		ensureLoaded();
		isValid = isPresentLocally;
		//if invalidate, remove local datafile
		if(!isValid){
			if(data!=null && data.exists()){
				data.delete();
			}
			data = null;
		}
	}

}
