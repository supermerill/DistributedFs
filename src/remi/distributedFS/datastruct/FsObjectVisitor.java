package remi.distributedFS.datastruct;

public interface FsObjectVisitor {

	public void visit(FsDirectory obj);
	public void visit(FsFile obj);

}
