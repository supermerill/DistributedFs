package remi.distributedFS.db.impl.bigdata;

import java.util.Iterator;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.util.ByteBuff;

public interface FsChunkStreamable extends Iterable<ByteBuff> , FsChunk{
	
	public boolean append(ByteBuff data);
	public Iterator<ByteBuff> iterator();

}
