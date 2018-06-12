package remi.distributedFS.fs;

import java.io.File;
import java.util.Random;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2LongAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import remi.distributedFS.datastruct.FsChunk;

/**
 * A thread used for :
 * 	<br> - cleaning old deleted files
 *  <br> - removing files locally when the space available is not enough
 * @author Admin
 *
 */
public class CleanerDefault implements Cleaner{
	
	
	public CleanerDefault() {
		super();
	}
	
	public static class ScoreDeletion implements Comparable<ScoreDeletion>{
		long id;
		long lastaccess;
		int size;
		int nbCopy;
		
		
		public ScoreDeletion(long id, long lastaccess, int size, int nbCopy) {
			super();
			this.id = id;
			this.lastaccess = lastaccess;
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
			
			// - 1 day = 864 score (86 400 s in a day)
			// * 2 = 1000 score
			// *2 copy => score * 10;
			
			long score = (lastaccess - o.lastaccess) / 100000;
			if(size>o.size){
				float ratio = size/o.size;
				score += ratio * 1000;
			}
			if(size<o.size){
				float ratio = o.size/size;
				score -= ratio * 1000;
			}
			
			score *= Math.pow(10, nbCopy - o.nbCopy);

			if(score > Integer.MAX_VALUE){
				return Integer.MAX_VALUE;
			}
			if(score < Integer.MIN_VALUE){
				return Integer.MIN_VALUE;
			}
			return (int)score;
		}
	}

	//TODO: this impl is linked strongly with the basic impl from bd (use files as chunk), you HAVE TO change that to make an implentation-independant impl
	public boolean checkLiberateSpace(CleanerManager manager) {
		System.out.println("CheckLiberateSpace");
		Pattern patternNumeral = Pattern.compile("^[0-9]+$");
		
		File rootFolder = new File(manager.manager.rootFolder);
		//get space occupied by the fs
		long fsSpace = 0;
		for(File fic : rootFolder.listFiles()){
			if(!fic.isDirectory() && !patternNumeral.matcher(fic.getName()).matches()){
				fsSpace += fic.length();
				System.out.println("system file : "+fic.getName());
			}
		}
		System.out.println("FS space used : "+((((int)fsSpace)/1000*1000*1000)%1000)+"go "+((((int)fsSpace)/1000*1000)%1000)+"mo "+((((int)fsSpace)/1000)%1000)+"ko "+((int)fsSpace)%1000+"o");
		
		//get space occupied by the files
		long chunkSpace = 0;
		for(File fic : rootFolder.listFiles()){
			if(!fic.isDirectory() && patternNumeral.matcher(fic.getName()).matches()){
				chunkSpace += fic.length();
				System.out.println("chunk file : "+fic.getName()+" "+ fic.length());
			}
		}
		System.out.println("Chunks space used : "+((chunkSpace/(1000*1000*1000))%1000)+"go "+((chunkSpace/(1000*1000))%1000)+"mo "+((chunkSpace/1000)%1000)+"ko "+chunkSpace%1000+"o");
		System.out.println("idealSize         : "+((manager.idealSize/(1000*1000*1000))%1000)+"go "+((manager.idealSize/(1000*1000))%1000)+"mo "+((manager.idealSize/1000)%1000)+"ko "+manager.idealSize%1000+"o");
		
		//do we need to deleted some things?
		if(chunkSpace > manager.idealSize || fsSpace + chunkSpace > manager.maxSize){
			
			//get all files name (only now to avoid getting it when it's not necessary
			LongList chunkNames = new LongArrayList();
			for(File fic : rootFolder.listFiles()){
				if(!fic.isDirectory() && patternNumeral.matcher(fic.getName()).matches()){
					chunkNames.add(Long.parseLong(fic.getName()));
					System.out.println("chunkname : "+fic.getName());
				}
			}

			//order lastcasses list by lowerbefore
//			Long2LongSortedMap lastAccess2id = new Long2LongAVLTreeMap();
			Object2LongSortedMap<ScoreDeletion> lastAccess2id = new Object2LongAVLTreeMap<>();
			Random aleat = new Random();
			//for each:
			for(long chunkId : chunkNames){
				FsChunk chunk = manager.manager.getDb().getChunkDirect(chunkId);
				// if it's not in the fs (bad file) , del it.
				if(chunk == null){
					System.out.println("bad file : "+chunkId);
//					new File(rootFolder.getAbsolutePath()+"/"+chunkId).delete();
				}else if(chunk.serverIdPresent().size()<=manager.minKnownDuplicate){
					// if i'm the only one to have it, do not del
					System.out.println("chunk "+chunkId+" is alone");
				}else{
					// if i'm not the only one to have it, put it in the map (last access -> filename)
					long timeWithFlavor = chunk.getLastAccessDate()*10+aleat.nextInt(1000);
					int stop = 0;
					ScoreDeletion scoreDeletion = new ScoreDeletion(chunkId, chunk.getLastAccessDate(), chunk.currentSize(), chunk.serverIdPresent().size());
//					while(lastAccess2id.containsKey(timeWithFlavor) && stop<1000){
					while(lastAccess2id.containsKey(scoreDeletion) && stop<1000){
						//add some flavour
						timeWithFlavor = chunk.getLastAccessDate()*10+aleat.nextInt(1000);
						stop++;
						System.out.println("chunk "+chunkId+" is not alone (with flavour"+timeWithFlavor+")");
					}
					lastAccess2id.put(scoreDeletion, chunkId);
					System.out.println("chunk "+chunkId+" is not alone");
				}
			}
		
			//remove all files from this list -> map as long as my space isn't low enough (or no more file)
			long sizeToRemove = Math.max(fsSpace+chunkSpace-manager.maxSize, chunkSpace-manager.idealSize);
			System.out.println("fsSpace+chunkSpace= "+(fsSpace+chunkSpace)+" ,maxSize="+manager.maxSize+", chunkSpace="+chunkSpace+" idealSize="+manager.idealSize);
			System.out.println("Need to remove "+sizeToRemove+" bytes ("+(sizeToRemove/1000)+"KB) "+lastAccess2id.size());
			ObjectBidirectionalIterator<it.unimi.dsi.fastutil.objects.Object2LongMap.Entry<ScoreDeletion>> it = lastAccess2id.object2LongEntrySet().iterator();
			while(it.hasNext() && sizeToRemove>0){
				it.unimi.dsi.fastutil.objects.Object2LongMap.Entry<ScoreDeletion> entry = it.next();
				long idDel = entry.getLongValue();
				FsChunk chunk = manager.manager.getDb().getChunkDirect(idDel);
				System.out.print("chunk "+idDel+" is removed, for a size of "+chunk.currentSize()+", access date = "+entry.getKey().lastaccess+" , size: "+entry.getKey().size+", copies: "+entry.getKey().nbCopy);
				sizeToRemove -= chunk.currentSize();
				System.out.println(", reste "+sizeToRemove+", "+it.hasNext());
				chunk.setPresent(false);
				chunk.flush();
			}
			
			//end!
		}

		return false;
		
	}
	
	
	
	

}
