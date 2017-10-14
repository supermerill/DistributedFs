package remi.distributedFS.db;

public class NotFindedException extends UnreachableChunkException {

	public NotFindedException(String msg) {
		super(msg);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}
