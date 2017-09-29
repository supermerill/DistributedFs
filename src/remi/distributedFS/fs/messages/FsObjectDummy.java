package remi.distributedFS.fs.messages;

import remi.distributedFS.datastruct.FsObjectImpl;
import remi.distributedFS.datastruct.FsObjectVisitor;

public class FsObjectDummy extends FsObjectImpl {
	String path;
	
	FsObjectDummy(long id, String path){
		this.id = id;
		this.path = path;
	}
	
	@Override
	public String getPath() {
		return path;
	}

	@Override
	public void setId() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setId(long newId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accept(FsObjectVisitor visitor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changes() {
		// TODO Auto-generated method stub
		
	}

}
