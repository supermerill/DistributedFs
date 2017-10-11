package remi.distributedFS.fs.messages;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsObjectVisitor;
import remi.distributedFS.util.ByteBuff;

public class FsChunkBuffer implements FsChunk{
	
	ByteBuff buffer;
	long timemod;
	long modUID;
	private long id;
	int maxSize = 0;

	
	
	public FsChunkBuffer(ByteBuff buffer, long timemod, long modUID, long id) {
		super();
		this.buffer = buffer;
		this.timemod = timemod;
		this.modUID = modUID;
		this.id = id;
	}

	@Override
	public boolean read(ByteBuff toAppend, int offset, int size) {
//		System.out.println(buffer.array().length+" : ["+(buffer.position()+offset)+" ; "+(buffer.position()+offset+size)+"] "
//				+ " => "+toAppend.array().length+" : ["+(toAppend.position())+" ; "+(toAppend.position()+size)+"] ");
		System.arraycopy(buffer.array(), buffer.position()+offset, toAppend.array(), toAppend.position(), size);
		return true;
	}

	@Override
	public boolean write(ByteBuff toWrite, int offset, int size) {
		return false;
	}

	@Override
	public int currentSize() {
		return buffer.limit()-buffer.position();
	}

	@Override
	public int getMaxSize() {
		return maxSize;
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public void setPresent(boolean isPresentLocally) {
	}

	@Override
	public LongList serverIdPresent() {
		return new LongArrayList();
	}

	@Override
	public long getModifyDate() {
		return timemod;
	}

	@Override
	public long getModifyUID() {
		return modUID;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public void setCurrentSize(int newSize) {
		buffer.expand(newSize);
	}

	@Override
	public void setMaxSize(int newMaxSize) {
		maxSize = newMaxSize;
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changes() {
	}

	@Override
	public void delete() {
		
	}

	@Override
	public void accept(FsObjectVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public long getLastAccessDate() {
		// TODO Auto-generated method stub
		return 0;
	}

}
