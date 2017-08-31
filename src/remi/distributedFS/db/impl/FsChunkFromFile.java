package remi.distributedFS.db.impl;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.util.ByteBuff;

public class FsChunkFromFile implements FsChunk {
	protected FsTableLocal master;
	protected FsFileFromFile parent;
	protected long id;
	protected boolean loaded = false;

	public FsChunkFromFile(FsTableLocal master, long sectorId, FsFileFromFile parent) {
		this.id = sectorId;
		this.parent = parent;
		this.master = master;
		loaded = false;
	}

	@Override
	public boolean read(ByteBuff toAppend, int offset, int size) {
		String str = "Hello world!";
		toAppend.put( Arrays.copyOf(Charset.forName("UTF-8").encode(str).array(),str.length()));
		return true;
	}

	@Override
	public boolean write(ByteBuff toWrite, int offset, int size) {
		// TODO Auto-generated method stub
		return true;
	}

	public long getId() {
		return id;
	}

	@Override
	public int currentSize() {
		return 12;//TODO
	}

	@Override
	public int maxSize() {
		return 1024;//TODO
	}

	@Override
	public boolean isPresent() {
		return true;//TODO
	}

	@Override
	public List<Long> serverIdPresent() {
		// TODO Auto-generated method stub
		return new ArrayList<Long>();
	}

	@Override
	public long lastModificationTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long lastModificationUID() {
		// TODO Auto-generated method stub
		return 0;
	}

}
