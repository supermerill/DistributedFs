package remi.distributedFS.net;

public enum ChangeReason {

	ADDED, // i added this
	MODIFIED, // i changed this
	DELETED, // i removed this
	INFORMATION;  // you want me to say this to you
}
