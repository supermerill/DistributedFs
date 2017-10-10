package remi.distributedFS.fs;

import java.io.File;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.longs.Long2LongAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongSortedMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.db.impl.FsChunkFromFile;

/**
 * A thread used for :
 * 	<br> - cleaning old deleted files
 *  <br> - removing files locally when the space available is not enough
 * @author Admin
 *
 */
public class Cleaner extends Thread{
	
	long idealSize;
	long maxSize;
	long mstimeBeforeDelete;
	
	int msSleepPerOp = 10;
	long nextCleaningOp = 0;
	long nextRemoveOp = 0;
	
	StandardManager manager;
	
	
	public Cleaner(StandardManager manager, long idealSize, long maxSize, long mstimeBeforeDelete) {
		super();
		this.manager = manager;
		this.idealSize = idealSize;
		this.maxSize = maxSize;
		this.mstimeBeforeDelete = mstimeBeforeDelete;
		
		//scheduled next remove op in 100sec from creation, to elt time for the fs to load and rest
		nextCleaningOp = System.currentTimeMillis() + 100000 *0;
		nextRemoveOp = System.currentTimeMillis();
	}

	@Override
	public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(System.currentTimeMillis() > nextRemoveOp){
			checkLiberateSpace();
			nextRemoveOp = System.currentTimeMillis() + 1000;
		}
		System.out.println(System.currentTimeMillis()+" > "+nextCleaningOp+" == "+(System.currentTimeMillis() > nextCleaningOp));
		if(System.currentTimeMillis() > nextCleaningOp){
			manager.getDb().removeOldDelItem(System.currentTimeMillis() - mstimeBeforeDelete);
//			nextCleaningOp = System.currentTimeMillis() + 1000 * 60 * 60 * 2; //every 2 hour
			nextCleaningOp = System.currentTimeMillis() + 1000 * 20; //testing : every 20 seconds
		}
		
	}

	protected void checkLiberateSpace() {
		Pattern patternNumeral = Pattern.compile("^[0-9]+$");
		
		File rootFolder = new File(manager.rootFolder);
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
		
		//do we need to deleted some things?
		if(chunkSpace > idealSize || fsSpace + chunkSpace > maxSize){
			
			//get all files name (only now to avoid getting it when it's not necessary
			LongList chunkNames = new LongArrayList();
			for(File fic : rootFolder.listFiles()){
				if(!fic.isDirectory() && patternNumeral.matcher(fic.getName()).matches()){
					chunkNames.add(Long.parseLong(fic.getName()));
				}
			}

			//order lastcasses list by lowerbefore
			Long2LongSortedMap lastAccess2id = new Long2LongAVLTreeMap();
			
			//for each:
			for(long chunkId : chunkNames){
				FsChunk chunk = manager.getDb().getChunkDirect(chunkId);
				// if it's not in the fs (bad file) , del it.
				if(chunk == null){
					new File(rootFolder.getAbsolutePath()+"/"+chunkId).delete();
				}else if(chunk.serverIdPresent().isEmpty()){
					// if i'm the only one to have it, do not del
						
				}else{
					// if i'm not the only one to have it, put it in the map (last access -> filename)
					lastAccess2id.put(chunk.getModifyDate(), chunkId);
				}
			}
		
			//remove all files from this list -> map as long as my space isn't low enough (or no more file)
			ObjectBidirectionalIterator<it.unimi.dsi.fastutil.longs.Long2LongMap.Entry> it = lastAccess2id.long2LongEntrySet().iterator();
			while(it.hasNext()){
				long idDel = it.next().getLongValue();
				FsChunk chunk = manager.getDb().getChunkDirect(idDel);
				chunk.setPresent(false);
			}
			
			//end!
		}
		
		
	}
	
	
	
	

}
