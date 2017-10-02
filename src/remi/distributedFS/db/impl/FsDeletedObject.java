package remi.distributedFS.db.impl;

import java.nio.ByteBuffer;

import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObjectVisitor;

public class FsDeletedObject extends FsObjectImplFromFile{
	
	public FsDeletedObject(FsTableLocal master, long sectorId, FsDirectory parent) {
		super(master, sectorId, parent);
	}

	public void load(ByteBuffer buffer){
		//check if it's a file
		byte type = buffer.get();
		if(type != FsTableLocal.DELETED){
			System.err.println("Error, not a deleted object at "+getSector());
			throw new WrongSectorTypeException("Error, not a deleted object at "+getSector());
		}
		super.load(buffer);
	}

	public synchronized void save(ByteBuffer buffer){
		//set "erased" or d"directory"
		if(parentId<0){
			buffer.put(FsTableLocal.ERASED);
		}else{
			buffer.put(FsTableLocal.DELETED);
		}
		super.save(buffer);
		
	}

	@Override
	public void accept(FsObjectVisitor visitor) {
	}
}
