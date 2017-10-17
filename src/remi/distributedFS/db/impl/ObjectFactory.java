package remi.distributedFS.db.impl;

public interface ObjectFactory {

	FsChunkFromFile createChunk(FsTableLocal master, long sectorId, FsFileFromFile parent, long id);
	
	
	public static class StandardFactory implements ObjectFactory{

		@Override
		public FsChunkFromFile createChunk(FsTableLocal master, long sectorId, FsFileFromFile parent, long id) {
			return new FsChunkFromFile(master, sectorId, parent, id);
//			return new FsChunkFromFile(master, currentBuffer.getLong(), this,i);
		}
		
	}
}
