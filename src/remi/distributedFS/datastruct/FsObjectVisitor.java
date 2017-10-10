package remi.distributedFS.datastruct;

public interface FsObjectVisitor {

	public void visit(FsDirectory dir);
	public void visit(FsFile file);
	public void visit(FsChunk chunk);

}
