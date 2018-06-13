package remi.distributedFS.fs;

public class CleanerManager extends Thread{
	
	public long idealSize;
	public long maxSize;
	public long stimeBeforeDelete;
	boolean canDelete;
	boolean canElage;
	int minKnownDuplicate;
	
	public long stimeBetweenDelete = 60 * 60 * 2;
	
	
	int msSleepPerOp = 10;
	long nextCleaningOp = 0;
	long nextRemoveOp = 0;
	
	final StandardManager manager;
	final Cleaner cleaner;

	public CleanerManager(StandardManager manager) {
		super();
		this.manager = manager;
		
		Parameters params = new Parameters(manager.getRootFolder()+"/cleaner.properties");
		this.idealSize = 1024*params.getLongOrDef("IdealSizeKB", 1024*10);
		this.maxSize = 1024*params.getLongOrDef("MaxSizeKB", 1024*1024);
		this.stimeBeforeDelete = params.getLongOrDef("SecTimeBeforeDelete", 1000*60);
		this.canDelete = params.getBoolOrDef("CanDelete", true);
		this.canElage = params.getBoolOrDef("CanElage", true);
		this.minKnownDuplicate = params.getIntOrDef("MinKnownDuplicate", 1);
		
		//scheduled next remove op in 100sec from creation, to elt time for the fs to load and rest
		nextCleaningOp = System.currentTimeMillis() + 100000 *0;
		nextRemoveOp = System.currentTimeMillis();
		

		switch(params.getStringOrDef("Type", "CleanerDefault")) {
			case "CleanerKeepNewFiles" : this.cleaner = new CleanerKeepNewFiles(); break;
			case "CleanerNone" : this.cleaner = new CleanerNone(); break;
			case "CleanerDefault" :
			default : this.cleaner = new CleanerKeepWantedFiles();
		}
		
	}
	

	@Override
	public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//TODO: quick path if the max size est atteint (do removeOldDelItem with 0 stimebeforedelete, then cleaner & other remove if not enough)
		if(System.currentTimeMillis() > nextRemoveOp){
			if(cleaner.checkLiberateSpace(this)) {
				manager.getDb().removeOldDelItem(System.currentTimeMillis() - 1000L*stimeBeforeDelete);
				nextCleaningOp = System.currentTimeMillis() + 1000 * stimeBetweenDelete; //every 2 hour
			}
			nextRemoveOp = System.currentTimeMillis() + 1000;
		}
		System.out.println(System.currentTimeMillis()+" > "+nextCleaningOp+" == "+(System.currentTimeMillis() > nextCleaningOp));
		if(System.currentTimeMillis() > nextCleaningOp){
			manager.getDb().removeOldDelItem(System.currentTimeMillis() - 1000L*stimeBeforeDelete);
//			nextCleaningOp = System.currentTimeMillis() + 1000 * 60 * 60 * 2; //every 2 hour
			nextCleaningOp = System.currentTimeMillis() + 1000 * 20; //testing : every 20 seconds
		}
		
	}

}
