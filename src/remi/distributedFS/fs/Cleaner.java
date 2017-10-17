package remi.distributedFS.fs;

import java.io.File;
import java.util.Random;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.longs.Long2LongAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongSortedMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import remi.distributedFS.datastruct.FsChunk;

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

	public Cleaner(StandardManager manager) {
		super();
		this.manager = manager;
		
		Parameters params = new Parameters(manager.getRootFolder()+"/cleaner.properties");
		this.idealSize = params.getLongOrDef("idealSize", 1024*1024*10);
		this.maxSize = params.getLongOrDef("maxSize", 1024*1024*1024);
		this.mstimeBeforeDelete = params.getLongOrDef("mstimeBeforeDelete", 1000*60);
		
		//scheduled next remove op in 100sec from creation, to elt time for the fs to load and rest
		nextCleaningOp = System.currentTimeMillis() + 100000 *0;
		nextRemoveOp = System.currentTimeMillis();
	}
	
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

	//TODO: this impl is linked strongly with the basic impl fromm bd (use files as chunk), you HAVE TO change that to make an implen-independant impl
	protected void checkLiberateSpace() {
		System.out.println("CHeckLiberateSpace");
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
		System.out.println("idealSize         : "+((idealSize/(1000*1000*1000))%1000)+"go "+((idealSize/(1000*1000))%1000)+"mo "+((idealSize/1000)%1000)+"ko "+idealSize%1000+"o");
		
		//do we need to deleted some things?
		if(chunkSpace > idealSize || fsSpace + chunkSpace > maxSize){
			
			//get all files name (only now to avoid getting it when it's not necessary
			LongList chunkNames = new LongArrayList();
			for(File fic : rootFolder.listFiles()){
				if(!fic.isDirectory() && patternNumeral.matcher(fic.getName()).matches()){
					chunkNames.add(Long.parseLong(fic.getName()));
					System.out.println("chunkname : "+fic.getName());
				}
			}

			//order lastcasses list by lowerbefore
			Long2LongSortedMap lastAccess2id = new Long2LongAVLTreeMap();
			Random aleat = new Random();
			//for each:
			for(long chunkId : chunkNames){
				FsChunk chunk = manager.getDb().getChunkDirect(chunkId);
				// if it's not in the fs (bad file) , del it.
				if(chunk == null){
					System.out.println("bad file : "+chunkId);
//					new File(rootFolder.getAbsolutePath()+"/"+chunkId).delete();
				}else if(chunk.serverIdPresent().isEmpty()){
					// if i'm the only one to have it, do not del
					System.out.println("chunk "+chunkId+" is alone");
				}else{
					// if i'm not the only one to have it, put it in the map (last access -> filename)
					
					long timeWithFlavor = chunk.getModifyDate()*10+aleat.nextInt(1000);
					int stop = 0;
					while(lastAccess2id.containsKey(timeWithFlavor) && stop<1000){
						//add some flavour
						timeWithFlavor = chunk.getModifyDate()*10+aleat.nextInt(1000);
						stop++;
						System.out.println("chunk "+chunkId+" is not alone (with flavour"+timeWithFlavor+")");
					}
					lastAccess2id.put(timeWithFlavor, chunkId);
					System.out.println("chunk "+chunkId+" is not alone");
				}
			}
		
			//remove all files from this list -> map as long as my space isn't low enough (or no more file)
			long sizeToRemove = Math.max(fsSpace+chunkSpace-maxSize, chunkSpace-idealSize);
			System.out.println("Need to remove "+sizeToRemove+" bytes ("+(sizeToRemove/1000)+"KB) "+lastAccess2id.size());
			ObjectBidirectionalIterator<it.unimi.dsi.fastutil.longs.Long2LongMap.Entry> it = lastAccess2id.long2LongEntrySet().iterator();
			while(it.hasNext() && sizeToRemove>0){
				Entry entry = it.next();
				long idDel = entry.getLongValue();
				FsChunk chunk = manager.getDb().getChunkDirect(idDel);
				System.out.print("chunk "+idDel+" is removed, for a size of "+chunk.currentSize()+", access date = "+entry.getLongKey());
				sizeToRemove -= chunk.currentSize();
				System.out.println(", reste "+sizeToRemove+", "+it.hasNext());
				chunk.setPresent(false);
				chunk.flush();
			}
			
			//end!
		}
		
		
	}
	
	
	
	

}
