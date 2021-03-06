package remi.distributedFS.datastruct;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import remi.distributedFS.db.impl.WrongSectorTypeException;

public interface FsDirectory extends FsObject {

//	List<FsObject> dirsAndFiles = new ArrayList<>();
//	List<FsDirectory> dirs = new ArrayList<>();
//	List<FsFile> files = new ArrayList<>();

	public void accept(FsObjectVisitor visitor);
	
//	public List<FsObject> getDirsAndFiles() {
//		return dirsAndFiles;
//	}
	public abstract List<FsDirectory> getDirs() ;
	public abstract List<FsFile> getFiles();
	public abstract FsDirectory createSubDir(String name);
	public abstract FsFile createSubFile(String name);

	public void removeFile(FsFile obj);
	public void removeDir(FsDirectory obj);
	public void removeCompletely(FsObject obj); // remove knowledge that is was deleted Completely

	public void moveFile(FsFile obj, FsDirectory newDir);
	public void moveDir(FsDirectory obj, FsDirectory newDir);


	//last cahnge date : when a sub-folder or this folder has a contnet which has changed
	// if lastchange date if newer, it say "something in this tree has changed, you should check it!"
	public long getLastChangeDate();
	public void setLastChangeDate(long timestamp);
	public long getLastChangeUID() ;
	public void setLastChangeUID(long uid) ;
	
	
	/**
	 * Get the map of delete items inside this directory
	 * @return datetime of deletion.
	 */
	public List<FsObject> getDelete();
	public static class FsDirectoryRemover implements FsObjectVisitor{

		@Override
		public void visit(FsDirectory obj) {
			obj.getParent().removeDir(obj);
		}

		@Override
		public void visit(FsFile obj) {
			obj.getParent().removeFile(obj);
		}

		@Override
		public void visit(FsChunk chunk) {
		}
		
	}

	public static class FsDirectoryMethods{
		
		public static final FsDirectoryRemover REMOVER = new FsDirectoryRemover();
		
		/**
		 * please, only use '/'
		 * @param dir root
		 * @param path a correct path
		 * @return a file or nul if i can't find.
		 */
		public static FsFile getPathFile(FsDirectory dir, String path){
			return getPath(dir, path, (dir2, filename) -> getFile(dir2, filename));
		}
	
	
		/**
		 * please, only use '/'
		 * @param dir root
		 * @param path a correct path
		 * @return a dir or null if i can't find.
		 */
		public static FsDirectory getPathDir(FsDirectory dir, String path){
			return getPath(dir, path, (dir2, filename) -> getDir(dir2, filename));
		}
		
	
		public static FsObject getPathObj(FsDirectory directory, String path){
			return getPath(directory, path, (dir, filename) -> {
	//			System.out.println("getPathObj=>"+dir+" / "+filename);
				FsObject o = getDir(dir, filename);
	//			System.out.println("getPathObj gert dir "+o);
				if(o==null) o = getFile(dir, filename);
	//			System.out.println("getPathObj gert file "+o);
				return o;
			});
		}
		
	
		public static FsDirectory getPathParentDir(FsDirectory directory, String path){
			return getPath(directory, path, (dir, filename) -> dir);
		}
		public static String getPathObjectName(FsDirectory directory, String path){
			return getPath(directory, path, (dir, filename) -> filename);
		}
		
		/**
		 * please, only use '/'
		 * @param path a correct path
		 * @return something get by function, or null if it can't find it.
		 */
		public static <N> N getPath(FsDirectory dir, String path, BiFunction<FsDirectory,String,N> func){
			if(path.equals("/") && dir.getParent()==dir){
//				System.out.println("getroot");
				return func.apply(dir, ".");
			}
			while(path.startsWith("/")){
				path = path.substring(1);
			}
			if(path.contains("/")){
				//find a dir
				int slashPos = path.indexOf('/');
				String name = path.substring(0,slashPos);
				String otherPath = path.substring(slashPos+1);
				if(name.equals(".")){
//					System.out.println("getMe");
					return getPath(dir, otherPath, func);
				}
				if(name.equals("..")){
//					System.out.println("getparent");
					return getPath(dir.getParent(), otherPath, func);
				}
//				System.out.println("getchild "+name);
				FsDirectory dirChild = getDir(dir, name);
				if(dirChild!=null) return getPath(dirChild, otherPath, func);
				else{
//					System.out.println("childdir null ");
					return null;
				}
			}else{
				//find a file
//				System.out.println("getfile "+path);
				return func.apply(dir, path);
			}
		}
		
		public static FsDirectory getDir(FsDirectory dir, String name) {
//			System.out.println("getDir : "+dir+" : "+name);
			if(name.equals(".")){
	//			System.out.println("getme");
				return dir;
			}
			if(name.equals("..")){
	//			System.out.println("getparent");
				return dir.getParent();
			}
			FsDirectory bestCandidate = null;
			for(FsDirectory dirChild : dir.getDirs()){
				if(dirChild.getName().equals(name)){
					if(bestCandidate== null){
						bestCandidate = dirChild;
					}else if(bestCandidate.getModifyDate() < dirChild.getModifyDate() && dirChild.getDeleteDate()<=0){
						bestCandidate = dirChild;
						System.out.println("WARN : conflict in "+dirChild.getPath()+" : multiple directory with the same name!");
					}else{
						System.out.println("WARN : conflict in "+dirChild.getPath()+" : multiple directory with the same name!");
					}
				}
			}
	//		System.out.println("erf, '"+name+"'");
			return bestCandidate;
		}
		
		public static FsFile getFile(FsDirectory dir, String name) {
			FsFile bestCandidate = null;
			Iterator<FsFile> it = dir.getFiles().iterator();
			while(it.hasNext()){
				FsFile file = it.next();
				if(file.getName().equals(name)){
					if(bestCandidate== null){
						bestCandidate = file;
					}else if(bestCandidate.getModifyDate() < file.getModifyDate() && file.getDeleteDate()<=0){
						bestCandidate = file;
						System.out.println("WARN : conflict in "+file.getPath()+" : multiple file with the same name!");
					}else{
						System.out.println("WARN : conflict in "+file.getPath()+" : multiple file with the same name!");
					}
				}
			}
			return bestCandidate;
		}

		/**
		 * Check if some "LoadErasedException" are coming, and if so do the erase operation
		 * @param dir directory to test (recursively)
		 */
		public static void autocorrectProblems(FsDirectory dir) {
			Iterator<FsDirectory> itDir = dir.getDirs().iterator();
			while(itDir.hasNext()){
				FsDirectory childDir = itDir.next();
				try{
					//try to load
					childDir.getDirs();
					childDir.getFiles();
					autocorrectProblems(childDir);
				}catch(WrongSectorTypeException e){
					itDir.remove();
					dir.flush();
				}
			}
			Iterator<FsFile> it = dir.getFiles().iterator();
			while(it.hasNext()){
				FsFile file = it.next();
				try{
					//try to load
					file.getName();
					file.getChunks();
					file.getSize();
				}catch(WrongSectorTypeException e){
					it.remove();
					dir.flush();
				}
			}
		}
		
	
		@Deprecated
		public static FsDirectory mkDirs(FsDirectory dir, String path){
			while(path.startsWith("/")){
				path = path.substring(1);
			}
			if(path.contains("/")){
				//find a dir
				int slashPos = path.indexOf('/');
				String name = path.substring(0,slashPos);
				String otherPath = path.substring(slashPos+1);
				FsDirectory childDir = getDir(dir, name);
				if(childDir==null){
					childDir = dir.createSubDir(name);
					if(childDir == null) return null;
				}
				return mkDirs(childDir, otherPath);
			}else{
				//check if dir is created
				FsDirectory childDir = getDir(dir, dir.getName());
				if(childDir==null){
					childDir = dir.createSubDir(dir.getName());
				}
				return childDir;
			}
		}
		
		public static Object getObj(FsDirectory dir, String name) {
			FsObject obj = getDir(dir, name);
			if(obj == null){
				obj = getFile(dir, name);
			}
			return obj;
		}
		
//		public static void deleteAndFlush(FsObject obj){
//			FsDirectory oldDir = obj.getParent();
//	    	//remove
//	    	Iterator<FsDirectory> it = oldDir.getDirs().iterator();
//	    	while(it.hasNext()){
//	    		if(it.next() == obj){
//	    			it.remove(); // this remove the entry and trigger some other deletion job (hopefully)
//	    			break;
//	    		}
//	    	}
//
//	    	// request deletion of this entry
//	    	//don't do that, as this remove the entry completely (even from the fs array) and we want to keep it to keep track of deletions
////	    	obj.setParent(null);
////	    	obj.setParentId(-1);
//
//	    	// save/propagate
//	    	obj.flush();
//	    	oldDir.flush();
//		}
	
	}

	
}
