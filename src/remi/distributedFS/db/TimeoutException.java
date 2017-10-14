package remi.distributedFS.db;

public class TimeoutException extends UnreachableChunkException {

	public TimeoutException(String msg) {
		super(msg);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}
