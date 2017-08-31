package remi.distributedFS.datastruct;

import java.util.List;
import java.util.function.BiFunction;

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

	public static class FsDirectoryMethods{
		/**
		 * please, only use '/'
		 * @param path a correct path
		 * @return a file or nul if i can't find.
		 */
		public static FsFile getPathFile(FsDirectory dir, String path){
			return getPath(dir, path, (dir2, filename) -> getFile(dir2, filename));
		}
	
	
		/**
		 * please, only use '/'
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
	//			System.out.println("getroot");
				return func.apply(dir, "..");
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
	//				System.out.println("getMe");
					return getPath(dir, otherPath, func);
				}
				if(name.equals("..")){
	//				System.out.println("getparent");
					return getPath(dir.getParent(), otherPath, func);
				}
	//			System.out.println("getchild "+name);
				FsDirectory dirChild = getDir(dir, name);
				if(dirChild!=null) return getPath(dirChild, otherPath, func);
				else return null;
			}else{
				//find a file
	//			System.out.println("getfile "+path);
				return func.apply(dir, path);
			}
		}
		
		public static FsDirectory getDir(FsDirectory dir, String name) {
			if(name.equals(".")){
	//			System.out.println("getme");
				return dir;
			}
			if(name.equals("..")){
	//			System.out.println("getparent");
				return dir.getParent();
			}
			for(FsDirectory dirChild : dir.getDirs()){
				if(dirChild.getName().equals(name)){
					return dirChild;
				}
			}
	//		System.out.println("erf, '"+name+"'");
			return null;
		}
		
		public static FsFile getFile(FsDirectory dir, String name) {
			for(FsFile file : dir.getFiles()){
				if(file.getName().equals(name)){
					return file;
				}
			}
			return null;
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
	
	}
	
}
