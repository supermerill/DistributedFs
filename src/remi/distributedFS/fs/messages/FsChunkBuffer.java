package remi.distributedFS.fs.messages;

import java.util.ArrayList;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.util.ByteBuff;

public class FsChunkBuffer implements FsChunk{
	
	ByteBuff buffer;
	long timemod;
	long modUID;

	
	
	public FsChunkBuffer(ByteBuff buffer, long timemod, long modUID) {
		super();
		this.buffer = buffer;
		this.timemod = timemod;
		this.modUID = modUID;
	}

	@Override
	public boolean read(ByteBuff toAppend, int offset, int size) {
		System.arraycopy(buffer.array(), buffer.position()+offset, toAppend, toAppend.position()+offset, size);
		return true;
	}

	@Override
	public boolean write(ByteBuff toWrite, int offset, int size) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int currentSize() {
		return buffer.limit()-buffer.position();
	}

	@Override
	public int maxSize() {
		return currentSize();
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public void setPresent(boolean isPresentLocally) {
	}

	@Override
	public List<Long> serverIdPresent() {
		return new ArrayList<>();
	}

	@Override
	public long lastModificationTimestamp() {
		return timemod;
	}

	@Override
	public long lastModificationUID() {
		return modUID;
	}

}
