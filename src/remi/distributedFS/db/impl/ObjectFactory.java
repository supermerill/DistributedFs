package remi.distributedFS.db.impl;

public interface ObjectFactory {

	FsChunkFromFile createChunk(FsTableLocal master, long sectorId, FsFileFromFile parent, long id);
	FsFileFromFile createFile(FsTableLocal master, long sectorId, FsDirectoryFromFile parent);
	
	
	public static class StandardFactory implements ObjectFactory{

		@Override
		public FsChunkFromFile createChunk(FsTableLocal master, long sectorId, FsFileFromFile parent, long id) {
			return new FsChunkFromFile(master, sectorId, parent, id);
//			return new FsChunkFromFile(master, currentBuffer.getLong(), this,i);
		}

		@Override
		public FsFileFromFile createFile(FsTableLocal master, long sectorId, FsDirectoryFromFile parent) {
			return new FsFileFromFile(master, sectorId, parent);
		}
		
	}
}
