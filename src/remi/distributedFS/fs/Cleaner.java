package remi.distributedFS.fs;

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
		nextCleaningOp = System.currentTimeMillis() + 100000;
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
			checkRemove();
			nextRemoveOp = System.currentTimeMillis() + 1000;
		}else if(System.currentTimeMillis() > nextCleaningOp){
			manager.getDb().removeOldDelItem(System.currentTimeMillis() - mstimeBeforeDelete);
//			nextCleaningOp = System.currentTimeMillis() + 1000 * 60 * 60 * 2; //every 2 hour
			nextCleaningOp = System.currentTimeMillis() + 1000 * 20; //testing : every 20 seconds
		}
		
	}

	protected void checkRemove() {
		// TODO Auto-generated method stub
		
	}
	
	
	
	

}
