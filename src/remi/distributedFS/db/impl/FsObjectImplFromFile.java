package remi.distributedFS.db.impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsObjectImpl;
import remi.distributedFS.util.Ref;

public abstract class FsObjectImplFromFile extends FsObjectImpl {
	
	public static Charset CHARSET = Charset.forName("UTF-8");
	
	FsTableLocal master;
	boolean loaded;
	long sector;

	public FsObjectImplFromFile(FsTableLocal master, long sectorId, FsDirectory parent){
		this.master = master;
		loaded = false;
		id = -1;
		//read all datas //0
		this.sector = sectorId;	
		this.parent = parent;
//		if(parent != null){
			parentId = parent.getId();
//			System.out.println("created object with parent "+this.parent.getId());
//		}else{
//			parentId = 0;
//		}
	}

	//only for root folder
	protected FsObjectImplFromFile(FsTableLocal master, long sectorId){
		System.out.println("ROOT created");
		this.master = master;
		loaded = false;
		id = -1;
		//read all datas //0
		this.sector = sectorId;	
	}
	
	
	
//	public FsObjectImplFromFile(FsTableLocal master, ByteBuffer buffer,long sectorId, FsDirectory parent){
//		this(master, sectorId, parent);
//		load(buffer);
//	}

	@Override
	public void setId() {
		id = ((long)master.getComputerId())<<48 | (this.sector&0xFFFFFFFFFFFFL);
		System.out.println(master.getComputerId()+"$ "+getPath()+" NEW ID : "+id+" from this.sector : "+this.sector+" => "+Long.toHexString(id));
		master.registerNewFile(id, this);
	}

	@Override
	public void setId(long newId) {
		System.out.println(master.getComputerId()+"$ "+getPath()+" SET ID : "+id);
		master.unregisterFile(id, this);
		id = newId;
		master.registerNewFile(id, this);
	}



	public void load(ByteBuffer buffer){


		int pos = buffer.position();
		if(pos != 1){
			System.err.println("error, wrong pos : 1<>"+pos);
		}
		//check if id is ok
		id = buffer.getLong();
//		if( != getId()){
//			buffer.position(pos);
//			System.err.println("error, not my id : "+buffer.getLong()+" <> "+getId());
//		}
		//read all datas //0+8
		parentId = buffer.getLong(); //8
		creationDate = buffer.getLong();//16
		creatorUserId = buffer.getLong();
		modifyDate = buffer.getLong(); //32
		modifyUserId = buffer.getLong();
		deleteDate = buffer.getLong(); //48
		deleteUserId = buffer.getLong();
		computerId = buffer.getLong(); //64
		groupId = buffer.getLong(); // 72
		PUGA = buffer.getShort(); // 74
		int nameLength = buffer.getShort(); // 76
		ByteBuffer nameBuff = ByteBuffer.allocate(256);
		buffer.get(nameBuff.array());
		nameBuff.limit(nameLength);
		name = CHARSET.decode(nameBuff).toString();
		//336
		if(pos+340!=buffer.position()){
			System.err.println("error, buffer at wrong pos! : "+buffer.position()+" != "+(340+pos)+" (posinit = "+pos+")");
		}
		
//		parent = null;
//		System.out.println("LOAD: parent null!");
		loaded = true;
	}

	public void print(){
		ByteBuffer buff = ByteBuffer.allocate(FsTableLocal.FS_SECTOR_SIZE);
		master.loadSector(buff, getSector());
		buff.get();
		this.print(buff);
	}

	public void print(ByteBuffer buffer){


		int pos = buffer.position();
		//read all datas //0
		System.out.println("read @pos "+pos);
		System.out.println("Id = "+buffer.getLong());
		System.out.println("parentId = "+buffer.getLong());
		System.out.println("creationDate = "+buffer.getLong());//16
		System.out.println("creatorUserId = "+buffer.getLong());
		System.out.println("modifyDate = "+buffer.getLong()); //32
		System.out.println("modifyUserId = "+buffer.getLong());
		System.out.println("deleteDate = "+buffer.getLong()); //48
		System.out.println("deleteUserId = "+buffer.getLong());
		System.out.println("computerId = "+buffer.getLong()); //64
		System.out.println("groupId = "+buffer.getLong()); // 72
		System.out.println("PUGA = "+Integer.toHexString(buffer.getShort())); // 74
		int nameLength = buffer.getShort(); // 76
		System.out.println("nameLength = "+nameLength);
		ByteBuffer nameBuff = ByteBuffer.allocate(256);
		buffer.get(nameBuff.array());
		nameBuff.limit(nameLength);
		System.out.println("name = "+CHARSET.decode(nameBuff));
		//336
		if(pos+340!=buffer.position()){
			System.err.println("error, buffer at wrong pos! : "+buffer.position()+" != "+(340+pos)+" (posinit = "+pos+")");
		}
	}
	
	/**
	 * this method must be overloaded. When you hae finished, please call master.write() for all changed sector.
	 * <br> the content of this method write  FsObjectImplFromFile inside the 336 first bytes of my sector.
	 * <br> You should set dirty to false after flushing everything to disk.
	 * @param buffer
	 */
	protected void save(ByteBuffer buffer){
		int pos = buffer.position();
		buffer.putLong(id);
		buffer.putLong(parentId);
		buffer.putLong(creationDate);
		buffer.putLong(creatorUserId);
		buffer.putLong(modifyDate);
		buffer.putLong(modifyUserId);
		buffer.putLong(deleteDate);
		buffer.putLong(deleteUserId);
		buffer.putLong(computerId);
		buffer.putLong(groupId);
		buffer.putShort(PUGA);
		ByteBuffer nameBuff = CHARSET.encode(name);
		short size = (short) Math.min(256,nameBuff.limit());
		buffer.putShort(size);
		buffer.put(nameBuff.array(), 0, size);
		buffer.position(pos+336);
	}

//	public int goToNext(ByteBuffer buff){
//		return goToNext(buff, false);
//	}
	
	@Override
	public void setDirty(boolean dirty) {
		super.setDirty(dirty);
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
				System.err.println("Error, no more sector to parse for obj "+this.getPath());
				throw new RuntimeException("Error, no more sector to parse for obj "+this.getPath());
			
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

	
	@Override
	public void flush() {
		if(isDirty()){
			checkLoaded();
			ByteBuffer buff = ByteBuffer.allocate(FsTableLocal.FS_SECTOR_SIZE);
			//master.loadSector(buff, getId()); //useless, we want to write, not read!
			save(buff); //this one should auto-call master.write
			
			if(parentId<0){
				//delete this one
	//			this.delete();
				master.releaseSector(this.getSector());
			}
			
			//flush parent if useful (if i have changed)
			getParent().flush();
		}
	}
	
//	protected abstract void delete();

	public void checkLoaded(){
		if(!loaded){
			ByteBuffer buff = ByteBuffer.allocate(FsTableLocal.FS_SECTOR_SIZE);
			master.loadSector(buff, getSector());
			load(buff);
		}
	}
	

	public long getDeleteDate() {
		checkLoaded();
		return deleteDate;
	}
	public void setDeleteDate(long deleteDate) {
		checkLoaded();
		this.deleteDate = deleteDate;
	}
	public short getPUGA() {
		checkLoaded();
		return PUGA;
	}
	public void setPUGA(short pugaData) {
		checkLoaded();
		PUGA = pugaData;
	}
	public long getModifyDate() {
		checkLoaded();
		return modifyDate;
	}
	public long getCreationDate() {
		checkLoaded();
		return creationDate;
	}
	public String getName() {
		checkLoaded();
		return name;
	}
	public long getId() {
		checkLoaded();
		return id;
	}
	public long getSector() {
		return sector;
	}
	public long getGroupId() {
		checkLoaded();
		return groupId;
	}
	public long getParentId() {
		checkLoaded();
		if(parent==null){
			return parentId; //TODO : it's not refreshed when necessary.
		}
		return parent.getId();
	}
	public FsDirectory getParent() {
		checkLoaded();
		return parent;
	}
	public void setParent(FsDirectory newDir) {
		checkLoaded();
		super.setParent(newDir);
	}
	public String getPath() {
		checkLoaded();
		return super.getPath();
//		if(parent == this || parent == null){
//			return getName();
//		}
//		return "'"+parent.getPath()+"'"+"/#"+getName()+"#";
	}
	public void setName(String newName) {
		checkLoaded();
		super.setName(newName);
	}

	public long getModifyUID() {
		checkLoaded();
		return modifyUserId;
	}
	
	public long getDeleteUID() {
		checkLoaded();
		return deleteUserId;
	}
	public long getCreatorUID() {
		checkLoaded();
		return creatorUserId;
	}
	public void setCreatorUID(long creatorUserId) {
		checkLoaded();
		this.creatorUserId = creatorUserId;
	}
	public void setModifyUID(long modifyUserId) {
		checkLoaded();
		this.modifyUserId = modifyUserId;
	}
	public void setDeleteUID(long deleteUserId) {
		checkLoaded();
		this.deleteUserId = deleteUserId;
	}
	public long getComputerId() {
		checkLoaded();
		return computerId;
	}
	public void setComputerId(long computerId) {
		checkLoaded();
		this.computerId = computerId;
	}
	public void setParentId(long parentId) {
		checkLoaded();
		this.parentId = parentId;
	}
	public void setCreationDate(long creationDate) {
		checkLoaded();
		this.creationDate = creationDate;
	}
	public void setModifyDate(long modifyDate) {
		checkLoaded();
		this.modifyDate = modifyDate;
	}
	public void setGroupId(long groupId) {
		checkLoaded();
		this.groupId = groupId;
	}
	
	@Override
	public void changes() {
		checkLoaded();
		long modDate = System.currentTimeMillis();
		System.out.println("RENAME OBJ "+getModifyDate()+" -> "+modDate);
		setModifyDate(modDate); 
		setModifyUID(master.getUserId());
		getParent().setLastChangeDate(modDate); 
		getParent().setLastChangeUID(master.getUserId());
		setDirty(true);
		flush();
	}
}
