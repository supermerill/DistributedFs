package remi.distributedFS.fs.bigdata;

import remi.distributedFS.util.ByteBuff;

public interface MapAlgo {
	
	public void init(ByteBuff dataInit);
	public void mapAlgo(ByteBuff dataIn, ByteBuff dataOut);

}
