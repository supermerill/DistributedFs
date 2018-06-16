package remi.distributedFS.fs;

import java.io.File;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMap;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.log.Logs;

/**
 * This cleaner remove all files that are added/modified/accessed a long time ago to make space
 * 
 * @author merill
 *
 */
public class CleanerKeepWantedFiles implements Cleaner{
	
	//TODO parameter: minSizeFileForDel : minimul size a file should be to be deleted. (Do not delete small files)
	
	public CleanerKeepWantedFiles() {
		super();
	}
	
	public static class ScoreDeletion implements Comparable<ScoreDeletion>{
		long id;
		long last;
		int size;
		int nbCopy;
		
		
		public ScoreDeletion(long id, long last, int size, int nbCopy) {
			super();
			this.id = id;
			this.last = last;
			this.size = size;
			this.nbCopy = nbCopy;
		}

		@Override
		public int hashCode() {
			return Long.hashCode(id);
		}
		
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof ScoreDeletion && ((ScoreDeletion)obj).id == this.id);
		}

		@Override
		public int compareTo(ScoreDeletion o) {
			
			
			long score = (last - o.last) / 100000;
			
			if(score > Integer.MAX_VALUE){
				return Integer.MAX_VALUE;
			}
			if(score < Integer.MIN_VALUE){
				return Integer.MIN_VALUE;
			}
			return (int)score;
		}
	}

	public boolean checkLiberateSpace(CleanerManager manager) {
		Logs.logManager.info("CheckLiberateSpace");
		Pattern patternNumeral = Pattern.compile("^[0-9]+$");
		
		File rootFolder = new File(manager.manager.rootFolder);
		//get space occupied by the fs
		long fsSpace = 0;
		for(File fic : rootFolder.listFiles()){
			if(!fic.isDirectory() && !patternNumeral.matcher(fic.getName()).matches()){
				fsSpace += fic.length();
				Logs.logManager.info("system file : "+fic.getName());
			}
		}
		Logs.logManager.info("FS space used : "+((((int)fsSpace)/1000*1000*1000)%1000)+"go "+((((int)fsSpace)/1000*1000)%1000)+"mo "+((((int)fsSpace)/1000)%1000)+"ko "+((int)fsSpace)%1000+"o");
		
		//get space occupied by the files
		long chunkSpace = 0;
		for(File fic : rootFolder.listFiles()){
			if(!fic.isDirectory() && patternNumeral.matcher(fic.getName()).matches()){
				chunkSpace += fic.length();
				Logs.logManager.info("chunk file : "+fic.getName()+" "+ fic.length());
			}
		}
		Logs.logManager.info("Chunks space used : "+((chunkSpace/(1000*1000*1000))%1000)+"go "+((chunkSpace/(1000*1000))%1000)+"mo "+((chunkSpace/1000)%1000)+"ko "+chunkSpace%1000+"o");
		Logs.logManager.info("idealSize         : "+((manager.idealSize/(1000*1000*1000))%1000)+"go "+((manager.idealSize/(1000*1000))%1000)+"mo "+((manager.idealSize/1000)%1000)+"ko "+manager.idealSize%1000+"o");
		
		//do we need to deleted some things?
		long spaceToLiberate = Math.max(manager.idealSize - chunkSpace, manager.maxSize - fsSpace -chunkSpace);
		if(spaceToLiberate>0){
			
			Long2ObjectSortedMap<FsFile> time2FicToDel = new Long2ObjectAVLTreeMap<>();
			//for each file in the fs
			long spaceToLiberateAfterDel = checkRep(manager.manager.getRoot(), time2FicToDel, spaceToLiberate);
			
			//remove them for local storage
			for(FsFile fileToRemove : time2FicToDel.values()) {
				for(FsChunk chunkToRemove : fileToRemove.getChunks()) {
					chunkToRemove.setPresent(false);
					chunkToRemove.flush();
				}
			}}

		return false;
		
	}

	//TODO derecursify this
	private long checkRep(FsDirectory dir, Long2ObjectSortedMap<FsFile> time2FicToDel, long need2Del) {
		
		for(FsFile fic : dir.getFiles()) {
			long ficdate = fic.getModifyDate();
			long sizeOnDisk = 0;
			for(FsChunk chunk : fic.getChunks()) {
				if(chunk.isPresent()) {
					sizeOnDisk += chunk.currentSize();
					if(ficdate<chunk.getLastAccessDate()) ficdate = chunk.getLastAccessDate();
				}
			}
					
			if(time2FicToDel.isEmpty() || time2FicToDel.lastLongKey() > ficdate) {
				
				if(sizeOnDisk > 0) {
					time2FicToDel.put(ficdate, fic);
					need2Del -= sizeOnDisk;
				}

				while(need2Del<0 && !time2FicToDel.isEmpty()) {
					sizeOnDisk = 0;
					for(FsChunk chunk : time2FicToDel.get(time2FicToDel.lastLongKey()).getChunks()) {
						if(chunk.isPresent()) {
							sizeOnDisk += chunk.currentSize();
						}
					}
					if(sizeOnDisk > -need2Del) break;
					need2Del += sizeOnDisk;
					time2FicToDel.remove(time2FicToDel.lastLongKey());
				}
			}
		}
		
		//then do it recursively for each child folder
		for(FsDirectory dirChild : dir.getDirs()) {
			need2Del = checkRep(dirChild, time2FicToDel, need2Del);
		}
		
		return need2Del;
	}
	
	
	
	

}
