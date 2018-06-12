package remi.distributedFS.fs;

/**
 * This cleaner remove nothing
 * 
 * @author merill
 *
 */
public class CleanerNone implements Cleaner{
	
	public CleanerNone() {
		super();
	}

	@Override
	public boolean checkLiberateSpace(CleanerManager cleanerManager) {
		return false;
	}
	

}
