package remi.distributedFS.fs;

public interface Cleaner {

	/**
	 * Clean the fs (erase file from the fs but not from disk)
	 * @param cleanerManager The manager, contain some parameters and it's the thread who call us.
	 *  You can change the stimeBeforeDelete to 0 if you want the deletion to happen on the disk right now.
	 * @return true if we should do an erase pass on the disk.
	 */
	boolean checkLiberateSpace(CleanerManager cleanerManager);

}
