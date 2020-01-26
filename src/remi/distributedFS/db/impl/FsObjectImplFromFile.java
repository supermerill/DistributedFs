package remi.distributedFS.db.impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsObjectImpl;
import remi.distributedFS.log.Logs;
import remi.distributedFS.util.Ref;

public abstract class FsObjectImplFromFile extends FsObjectImpl {
	
	public static Charset CHARSET = Charset.forName("UTF-8");
	
	protected FsTableLocal master;
	protected boolean loaded;
	protected long sector;
	protected LongList sectors = null;

	public FsObjectImplFromFile(FsTableLocal master, long sectorId, FsDirectory parent){
		this.master = master;
		loaded = false;
		id = -1;
		//read all datas //0
		this.sector = sectorId;	
		this.parent = parent;
//		if(parent != null){
			parentId = parent.getId();
//			Logs.logDb.info("created object with parent "+this.parent.getId());
//		}else{
//			parentId = 0;
//		}
	}

	//only for root folder (or chunk)
	protected FsObjectImplFromFile(FsTableLocal master, long sectorId){
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
		Logs.logDb.info(master.getComputerId()+"$ "+getPath()+" NEW ID : "+id+" from this.sector : "+this.sector+" => "+Long.toHexString(id));
		master.registerNewFile(id, this);
	}

	@Override
	public void setId(long newId) {
		Logs.logDb.info(master.getComputerId()+"$ "+getPath()+" SET ID : "+id);
		master.unregisterFile(id, this);
		id = newId;
		master.registerNewFile(id, this);
	}


	public void load(ByteBuffer buffer){


		int pos = buffer.position();
		if(pos != 1){
			Logs.logDb.warning("error, wrong pos : 1<>"+pos);
		}
		//check if id is ok
		id = buffer.getLong();
//		if( != getId()){
//			buffer.position(pos);
//			Logs.logDb.warning("error, not my id : "+buffer.getLong()+" <> "+getId());
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
			Logs.logDb.warning("error, buffer at wrong pos! : "+buffer.position()+" != "+(340+pos)+" (posinit = "+pos+")");
		}
		
//		parent = null;
//		Logs.logDb.info("LOAD: parent null!");
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
		Logs.logDb.info("read @pos "+pos);
		Logs.logDb.info("Id = "+buffer.getLong());
		Logs.logDb.info("parentId = "+buffer.getLong());
		Logs.logDb.info("creationDate = "+buffer.getLong());//16
		Logs.logDb.info("creatorUserId = "+buffer.getLong());
		Logs.logDb.info("modifyDate = "+buffer.getLong()); //32
		Logs.logDb.info("modifyUserId = "+buffer.getLong());
		Logs.logDb.info("deleteDate = "+buffer.getLong()); //48
		Logs.logDb.info("deleteUserId = "+buffer.getLong());
		Logs.logDb.info("computerId = "+buffer.getLong()); //64
		Logs.logDb.info("groupId = "+buffer.getLong()); // 72
		Logs.logDb.info("PUGA = "+Integer.toHexString(buffer.getShort())); // 74
		int nameLength = buffer.getShort(); // 76
		Logs.logDb.info("nameLength = "+nameLength);
		ByteBuffer nameBuff = ByteBuffer.allocate(256);
		buffer.get(nameBuff.array());
		nameBuff.limit(nameLength);
		Logs.logDb.info("name = "+CHARSET.decode(nameBuff));
		//336
		if(pos+340!=buffer.position()){
			Logs.logDb.warning("error, buffer at wrong pos! : "+buffer.position()+" != "+(340+pos)+" (posinit = "+pos+")");
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
		if(creatorUserId==0){
			Logs.logDb.warning("warn, trying to save a null user id");
//			new IllegalArgumentException("userid for "+getPath()+", "+id+" is =0").printStackTrace();
		}
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
		buffer.position(pos+340);
	}

//	public int goToNext(ByteBuffer buff){
//		return goToNext(buff, false);
//	}
	
	@Override
	public void setDirty(boolean dirty) {
		super.setDirty(dirty);
	}
	
	public int goToNextOrCreate(ByteBuffer buff, Ref<Integer> sectorNum){//Ref<Long> currentSector){
		assert buff.position() == FsTableLocal.FS_SECTOR_SIZE-8 : "Error: try to goToNextOrCreate next sector id at wrong pos ("+buff.position()+" != "+(FsTableLocal.FS_SECTOR_SIZE-8);
		final long nextSector = (sectors==null || sectors.size()<sectorNum.get())? -1 : sectors.getLong(sectorNum.get());
		Logs.logDb.info("--get (save) sector "+nextSector+" "+(nextSector <= 0)+" @"+(buff.position())+" ? "+(FsTableLocal.FS_SECTOR_SIZE-8));
		if(nextSector <= 0){
			long newSectorId = master.requestNewSector();
			Logs.logDb.info("--create (read) sector "+newSectorId);
			buff.putLong(newSectorId);
			buff.rewind();
			long currentSector = sectorNum.get()==0? sector : sectors.getLong(sectorNum.get()-1);
			Logs.logDb.info("--save my sector "+currentSector);
			master.saveSector(buff, currentSector);
			if(sectors==null) sectors = new LongArrayList(2);
			assert sectors.size()<=sectorNum.get() :"Error, trying to add sector n°"+sectorNum.get()+" : "+newSectorId+" when my sector array is "+sectors;
			sectors.add(newSectorId);
			sectorNum.set(sectorNum.get()+1);
			
			//master.loadSector(buff, newSectorId); //useless, it's not created yet!
			buff.rewind();
			buff.put(FsTableLocal.EXTENSION);
			buff.putLong(this.getId());
			buff.position(FsTableLocal.FS_SECTOR_SIZE-8);
			buff.putLong(-1);
			buff.position(16);
			return (FsTableLocal.FS_SECTOR_SIZE / 8) - 3;
		}else{
			//save
			long newSectorId = sectors.getLong(sectorNum.get());
			Logs.logDb.info("--infer (read) sector "+newSectorId+" from array "+sectors);
			buff.putLong(newSectorId);
			buff.rewind();
			long currentSector = sectorNum.get()==0? sector : sectors.getLong(sectorNum.get()-1);
			master.saveSector(buff, currentSector);
			//increment
			sectorNum.set(sectorNum.get()+1);
			//format sector header
			buff.rewind();
			buff.put(FsTableLocal.EXTENSION);
			buff.putLong(this.getId());
			buff.position(FsTableLocal.FS_SECTOR_SIZE-8);
			buff.putLong(-1);
			buff.position(16);
			return (FsTableLocal.FS_SECTOR_SIZE / 8) - 3;
		}
	}


	public void flushLastSector(ByteBuffer buff, Ref<Integer> sectorNum){
		Logs.logDb.info("TEST: flushLastSector "+(sectorNum.get()==0?sector:sectors.getLong(sectorNum.get()-1))+" @"+sectorNum.get()+" is my last sector, flushed");
		//fill remaining with zeros
		Arrays.fill(buff.array(), buff.position(), buff.limit(), (byte)0);
		//save it
		buff.rewind();
		master.saveSector(buff, (sectorNum.get()==0?sector:sectors.getLong(sectorNum.get()-1)));
		if(sectors!=null)
		for(int i=sectorNum.get();i<sectors.size();i++){
			long releasedSect = sectors.removeLong(i);
			master.removeSector(releasedSect);
			Logs.logDb.info("TEST: sector "+releasedSect+" has ben released because i don't need it anymore");
		}
	}
	
	//TODO: maybe use the sectors field, now?
	public LongList getSectorsUsed(){
		ByteBuffer buff = ByteBuffer.allocate(FsTableLocal.FS_SECTOR_SIZE);
		long lastSec = getSector();
		LongArrayList lstLng = new LongArrayList();
		while(lastSec>0){
			lstLng.add(lastSec);
			buff.rewind();
			master.loadSector(buff, lastSec);
			buff.position(buff.limit()-8);
			lastSec = buff.getLong();
		}
		return lstLng;
	}

	/**
	 * return the number of long that can be read.
	 * @param buff
	 * @return nb long to read (max)
	 */
	public int goToNextAndLoad(ByteBuffer buff, Ref<Integer> sectNumBefore){
		assert buff.position() == FsTableLocal.FS_SECTOR_SIZE-8 : "Error: try to get next sector id at wrong pos ("+buff.position()+" != "+(FsTableLocal.FS_SECTOR_SIZE-8);
		long nextsector = buff.getLong();
//		System.out.print(" <nxtSec:"+nextsector+"> ");
		if(nextsector<=0 && sectors!=null && sectors.size()>sectNumBefore.get()){
//			System.out.print(" <STnxtSec:"+nextsector+"> ");
			nextsector = sectors.getLong(sectNumBefore.get());
		}
		if(nextsector<=0){
				Logs.logDb.warning("--Error, no more sector to parse for obj "+this.getPath()+" : "+nextsector);
				throw new RuntimeException("Error, no more sector to parse for obj "+this.getPath());
			
		}
		buff.rewind();
		master.loadSector(buff, nextsector);
		byte type = buff.get();
		if(type==FsTableLocal.EXTENSION){
			//ok
			//verify it's mine
			long storedSec = buff.getLong();
			if(storedSec != this.getId()){
				Logs.logDb.warning("Error, my next sector is not picked for me !!! : "+storedSec+" != "+sector);
				throw new RuntimeException("Error, my next sector is not picked for me !!! : "+storedSec+" != "+sector);
			}
			
			//add it to storage array
			if(sectors==null){
				sectors = new LongArrayList(2);
			}
			if(sectors.size()<=sectNumBefore.get()){
				if(sectors.size()<=sectNumBefore.get()-1){
					throw new WrongSectorTypeException("Error, i have a sector id but not the previous one in my arrray, it's imposssible and i'm lost! size: "+sectors.size()+", my pos : "+sectNumBefore);
				}
				sectors.add(nextsector);
			}else{
				sectors.set(sectNumBefore.get().intValue(), nextsector);
			}
			sectNumBefore.set(sectNumBefore.get()+1);
			
			//now i should be at pos 9
			//go to ok pos
			buff.position(16);
			return (FsTableLocal.FS_SECTOR_SIZE / 8) - 3; //128 -2(prefix) -1(suffix)
		}else{
			Logs.logDb.warning("Error, my next sector is not picked for me !!! sector id: "+nextsector+" has a type of "+type);
			throw new WrongSectorTypeException("Error, my next sector is not picked for me !!!");
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
				this.delete();
//				master.releaseSector(this.getSector());
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
		this.setModifyUID(master.getUserId());
		super.setModifyDate(modifyDate);
	}
	public void setGroupId(long groupId) {
		checkLoaded();
		this.groupId = groupId;
	}
	
	@Override
	public void changes() {
		checkLoaded();
		long modDate = System.currentTimeMillis();
		Logs.logDb.info("RENAME OBJ "+getModifyDate()+" -> "+modDate);
		setModifyDate(modDate); 
		setModifyUID(master.getUserId());
		getParent().setLastChangeDate(modDate); 
		getParent().setLastChangeUID(master.getUserId());
		setDirty(true);
		flush();
	}
	

	@Override
	public void delete(){
		if(sector<=0){
			Logs.logDb.warning("Error, trying to del object "+getPath()+" with sector id of "+getSector()+" :  impossible!");
			throw new RuntimeException("Error, trying to del object "+getPath()+" with sector id of "+getSector()+" :  impossible!");
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
					Logs.logDb.warning("Warn : deleted object "+getPath()+" has an extedned sector occupied by somethign else. Sector id: "+nextSector+" : "+type);
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
				master.removeSector(sectorsToDel.getLong(i));
			}
		}else{
			Arrays.fill(buff.array(), (byte)0);
		}
		
		//del main sector
		buff.rewind();
		master.saveSector(buff, getSector());
		master.removeSector(getSector());
	}
	
}
