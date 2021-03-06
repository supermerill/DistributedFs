package remi.distributedFS.os;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.kenai.jffi.MemoryIO;

import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.types.dev_t;
import jnr.ffi.types.gid_t;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import jnr.ffi.types.u_int32_t;
import jnr.ffi.types.uid_t;
import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import static remi.distributedFS.datastruct.FsDirectory.FsDirectoryMethods.*;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.datastruct.PUGA;
import remi.distributedFS.db.TimeoutException;
import remi.distributedFS.db.UnreachableChunkException;
import remi.distributedFS.db.impl.FsDirectoryFromFile;
import remi.distributedFS.db.impl.WrongSectorTypeException;
import remi.distributedFS.fs.FileSystemManager;
import remi.distributedFS.util.ByteBuff;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.NotImplemented;
import ru.serce.jnrfuse.flags.FuseBufFlags;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.Flock;
import ru.serce.jnrfuse.struct.FuseBuf;
import ru.serce.jnrfuse.struct.FuseBufvec;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import ru.serce.jnrfuse.struct.FusePollhandle;
import ru.serce.jnrfuse.struct.Statvfs;
import ru.serce.jnrfuse.struct.Timespec;

public class JnrfuseImpl extends FuseStubFS {
	
	
	public FileSystemManager manager;

	public JnrfuseImpl(){
		
	}
	public JnrfuseImpl(FileSystemManager manager){
		super();
		this.manager = manager;
	}
	
	public void init(String drivepath){
		
		try {
		    try {
		        String path;
		        switch (Platform.getNativePlatform().getOS()) {
		            case WINDOWS:
		                path = (""+drivepath.charAt(0)).toUpperCase()+":\\";
		                break;
		            default:
		                path = drivepath;//"/tmp/mntm_"+driveletter;
		        }
		        System.out.println("prepare to mount into "+path);
			    try {
			    	mount(Paths.get(path), false, false);
			    }catch(Exception e1){
			    	e1.printStackTrace();
			    	System.out.println(e1.getLocalizedMessage());
			    	throw new RuntimeException(e1);
			    }
		        System.out.println("mounted "+path);
		    }catch(Throwable e2){
		    	e2.printStackTrace();
		    	System.out.println(e2.getLocalizedMessage());
		    } finally {
//		    	umount();
		    }
	    }catch(Throwable e3){
	    	e3.printStackTrace();
	    }
	}

	public static short modeToPUGA(long mode){
		PUGA puga = new PUGA((short)0);
		puga.computerRead = true;
		puga.computerWrite = true;
		puga.userRead = (FileStat.S_IRUSR & mode) != 0;
		puga.userWrite = (FileStat.S_IWUSR & mode) != 0;
		puga.groupRead = (FileStat.S_IRGRP & mode) != 0;
		puga.groupWrite = (FileStat.S_IWGRP & mode) != 0;
		puga.allRead = (FileStat.S_IROTH & mode) != 0;
		puga.allWrite = (FileStat.S_IWOTH & mode) != 0;
		return puga.toShort();
	}

	public static PUGA modeToPUGAObj(long mode){
		PUGA puga = new PUGA((short)0);
		puga.computerRead = true;
		puga.computerWrite = true;
		puga.userRead = (FileStat.S_IRUSR & mode) != 0;
		puga.userWrite = (FileStat.S_IWUSR & mode) != 0;
		puga.groupRead = (FileStat.S_IRGRP & mode) != 0;
		puga.groupWrite = (FileStat.S_IWGRP & mode) != 0;
		puga.allRead = (FileStat.S_IROTH & mode) != 0;
		puga.allWrite = (FileStat.S_IWOTH & mode) != 0;
		return puga;
	}

	public static long PUGAToMode(FsObject obj){
		PUGA puga = new PUGA(obj.getPUGA());
		puga.computerRead = true;
		puga.computerWrite = true;
//		System.out.println("PUGA="+puga);
    	long mode = 0;
    	if(puga.userRead) mode |= FileStat.S_IRUSR;
    	if(puga.userRead && puga.canExec) mode |= FileStat.S_IXUSR;
    	if(puga.userWrite) mode |= FileStat.S_IWUSR;
    	if(puga.groupRead) mode |= FileStat.S_IRGRP;
    	if(puga.groupRead && puga.canExec) mode |= FileStat.S_IXGRP;
    	if(puga.groupWrite) mode |= FileStat.S_IWGRP;
    	if(puga.allRead) mode |= FileStat.S_IROTH;
    	if(puga.allRead && puga.canExec) mode |= FileStat.S_IXOTH;
    	if(puga.allWrite) mode |= FileStat.S_IWOTH;
    	if(obj instanceof FsDirectory){ mode |= FileStat.S_IFDIR; }else{ mode |= FileStat.S_IFREG; }
		return mode;
	}

    @Override
    public int getattr(String path, FileStat stat) {
    	try{
        	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
//        	System.out.println(">>>>NC getattr for (path) : "+path);
//        	System.out.println(/*">>>>NC */"get attr for (path) : "+path);
    	FsObject obj = getPathObj(manager.getRoot(), path);
    	if(obj ==null){
    		System.err.println(">> getattr : warn: can't find "+path);
    		return -ErrorCodes.ENOENT();
    	}
    	long mode = PUGAToMode(obj);
    	stat.st_mode.set(Long.valueOf(mode));
    	stat.st_ino.set(Long.valueOf(obj.getId()));
    	stat.st_uid.set(Long.valueOf(obj.getUserId()));
    	stat.st_gid.set(Long.valueOf(obj.getGroupId()));
    	if(stat.st_birthtime != null){
	    	stat.st_birthtime.tv_sec.set(Long.valueOf(obj.getCreationDate()/1000));
	    	stat.st_birthtime.tv_nsec.set(Long.valueOf(obj.getCreationDate()*1000));
    	}
    	if(obj.asFile() != null){
        	stat.st_size.set(Long.valueOf(((FsFile)obj).getSize()));
    	}
//		System.out.println(">> getattr : mode "+mode+" == "+modeToPUGAObj(mode)+", uid : "+stat.st_uid);
//    	System.out.println(" bad file perm = "+stat.st_mode.intValue());
//    	mode = 0777;
//    	if(obj instanceof FsDirectory) mode |= FileStat.S_IFDIR;
//        stat.st_mode.set(mode);
//    	System.out.println(" good file perm for "+path+" = "+stat.st_mode.intValue());
    	}catch(Exception e){
    		e.printStackTrace();
    		return -ErrorCodes.ENOENT();
    	}
        return 0;
    }

    @Override
    @NotImplemented
    public int readlink(String path, Pointer buf, @size_t long size) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC readlink for (path) : "+path);
        return 0;
    }

    @Override
    @NotImplemented
    public int mknod(String path, @mode_t long mode, @dev_t long rdev) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC mknod for (path) : "+path);
        return create(path, mode, null);
    }

    @Override
    public int create(String path, @mode_t long mode, FuseFileInfo fi) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC create for (path) : "+path);

    	try{
        	FsDirectory dir = getPathParentDir(manager.getRoot(), path);
        	if(dir == null){
        		return -ErrorCodes.ENOENT();
        	}
	    	String name = getPathObjectName(manager.getRoot(), path);
    		//check it doesn't exist
	    	if(getObj(dir, name) != null){
	    		return -ErrorCodes.EEXIST();
	    	}
        	FsFile fic = dir.createSubFile(getPathObjectName(manager.getRoot(), path));
//        	fic.rearangeChunks(128, 1);
        	FsChunk newChunk = fic.createNewChunk(-1);
			newChunk.setMaxSize(FsFile.newMaxSizeChunk);
			newChunk.setCurrentSize(0);
			List<FsChunk> lst = new ArrayList<>(fic.getChunks());
			lst.add(newChunk);
			fic.setChunks(lst);
        	System.out.println(">> set userid to "+getContext().uid.get());
        	fic.setUserId(getContext().uid.get());
        	System.out.println(">> set groupid to "+getContext().gid.get());
        	fic.setGroupId(getContext().gid.get());

			// modification(s) ? -> set timestamp!
	    	dir.setModifyDate(System.currentTimeMillis());
			System.out.println(">> new modifydate for folder '"+dir.getPath()+"' : "+dir.getModifyDate());
			dir.setModifyUID(manager.getUserId());
			
			// set id
        	fic.setId();
        	
        	//flush (should be done here or in db engine?)
        	newChunk.flush();
        	fic.flush();
        	dir.flush();
        	manager.propagateChange(fic);
        	
        	//temp fix because sometimes, it doesn't work
        	//fic.setPUGA(modeToPUGA(mode));
        	
//            return -ErrorCodes.ENOSYS();
            
            return 0;
    	}catch(Exception e){
    		e.printStackTrace();
    		return -ErrorCodes.ENODATA();
    	}
    }

    @Override
    public int mkdir(String path, @mode_t long mode) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC mkdir for (path) : "+path);
    	try{
    		FsDirectory dir = getPathParentDir(manager.getRoot(), path);
	    	if(dir==null){
	    		return -ErrorCodes.ENOENT();
	    	}
	    	String name = getPathObjectName(manager.getRoot(), path);
    		//check it doesn't exist
	    	if(getObj(dir, name) != null){
	    		return -ErrorCodes.EEXIST();
	    	}
	    	//create
	    	FsDirectory dirChild = dir.createSubDir(name);
	    	
	    	//set access right
	    	dirChild.setUserId(getContext().uid.get());
	    	dirChild.setGroupId(getContext().gid.get());
	    	dirChild.setPUGA(modeToPUGA(mode));
	    	
			// modification(s) ? -> set timestamp!
	    	dir.setModifyDate(System.currentTimeMillis());
			System.out.println("new modifydate for folder '"+dir.getPath()+"' : "+dir.getModifyDate());
			dir.setModifyUID(manager.getUserId());
	    	
			// set id
	    	dirChild.setId();
	    	
	    	// save/propagate
	    	dirChild.flush();
	    	dir.flush();
        	manager.propagateChange(dir);
        	
	        return 0;
		}catch(Exception e){
			e.printStackTrace();
			return -ErrorCodes.EIO();
		}
    }

    @Override
    public int unlink(String path) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC unlink for (path) : "+path);
    	try{
	    	FsFile obj = getPathFile(manager.getRoot(), path);
	    	FsDirectory oldDir = getPathParentDir(manager.getRoot(), path);
	    	if(obj==null || oldDir==null){
	    		return -ErrorCodes.ENOENT();
	    	}
	    	//remove
//	    	Iterator<FsFile> it = oldDir.getFiles().iterator();
//	    	while(it.hasNext()){
//	    		if(it.next() == obj){
//	    			it.remove(); //here we request the deletion
//	    			break;
//	    		}
//	    	}
	    	
	    	oldDir.removeFile(obj);
	    	
	    	// 'request deletion' of this entry
	    	//already done by the fs
//	    	obj.setDeleteDate(System.currentTimeMillis());
//	    	obj.setDeleteUID(manager.getComputerId());
//	    	oldDir.getDelete().add(obj);
	    	
	    	// save/propagate
	    	obj.flush();
	    	oldDir.flush();
        	manager.propagateChange(obj);
        	
	        return 0;
		}catch(Exception e){
			e.printStackTrace();
			return -ErrorCodes.EIO();
		}
    }

    @Override
    public int rmdir(String path) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC rmdir for (path) : "+path);
    	try{
    		FsDirectory obj = getPathDir(manager.getRoot(), path);//getPathParentDir(manager.getRoot(), path);
	    	if(obj==null){
	    		return -ErrorCodes.ENOENT();
	    	}
	    	FsDirectory oldDir = obj.getParent();
	    	if(oldDir==null){
	    		return -ErrorCodes.ENOENT();
	    	}
//	    	//remove
//	    	Iterator<FsDirectory> it = oldDir.getDirs().iterator();
//	    	while(it.hasNext()){
//	    		if(it.next() == obj){
//	    			it.remove();
//	    			break;
//	    		}
//	    	}
//
//	    	// request deletion of this entry
//	    	obj.setParent(null);
//	    	obj.setParentId(-1);
//
//	    	// save/propagate
//	    	obj.flush();
//	    	oldDir.flush();

	    	oldDir.removeDir(obj);
	    	
        	manager.propagateChange(obj);
//        	manager.propagateChange(oldDir);
        	
	        return 0;
		}catch(Exception e){
			e.printStackTrace();
			return -ErrorCodes.EIO();
		}
    }

    @Override
    @NotImplemented
    public int symlink(String oldpath, String newpath) {
        return 0;
    }
    

    @Override
    public int rename(String oldpath, String newpath) {
    	if(oldpath.indexOf(0)>0) oldpath = oldpath.substring(0,oldpath.indexOf(0));
    	if(newpath.indexOf(0)>0) newpath = newpath.substring(0,newpath.indexOf(0));
    	System.out.println(">>>>NC rename for (path) : "+oldpath+" => "+newpath);
		try{
	    	FsObject obj = getPathObj(manager.getRoot(), oldpath);
	    	FsDirectory oldDir = getPathParentDir(manager.getRoot(), oldpath);
	    	FsDirectory newDir = getPathParentDir(manager.getRoot(), newpath);
	    	String newName = getPathObjectName(manager.getRoot(), newpath);
	    	if(obj==null || oldDir==null || newDir==null || newName==null || newName.isEmpty()){
	        	System.out.println("rename : path problem : "+obj+", "+oldDir+", "+newDir);
	    		return -ErrorCodes.ENOENT();
	    	}
//	    	String name = getPathObjectName(newDir, newpath);
	    	String name = newpath.substring(newpath.lastIndexOf("/")+1);
    		//check it doesn't exist
			System.out.println("check it doesn't exist : "+name);
	    	if(getObj(newDir, name) != null){
	    		return -ErrorCodes.EEXIST();
	    	}
        	System.out.println("rename : path : "+obj.getPath()+", "+oldDir.getPath()+", "+newDir.getPath());
        	System.out.println("rename : obj : "+obj+", "+oldDir+", "+newDir);
//	    	if(obj instanceof FsDirectory){
//	        	//remove
//		    	Iterator<FsDirectory> it = oldDir.getDirs().iterator();
//		    	while(it.hasNext()){
//		    		if(it.next() == obj){
//		    	    	System.out.println("rename :removedir");
//		    			it.remove();
//		    			break;
//		    		}
//		    	}
//	        	System.out.println("rename : test old location : "+getPathObj(manager.getRoot(), oldpath));
//		    	//add
//    	    	System.out.println("rename : adddir");
//		    	newDir.getDirs().add((FsDirectory) obj);
//		    	obj.setName(newName);
//	        	System.out.println("rename : test new location : "+getPathObj(manager.getRoot(), newpath));
//
//	    	}else{
//	        	//remove
//		    	Iterator<FsFile> it = oldDir.getFiles().iterator();
//		    	while(it.hasNext()){
//		    		if(it.next() == obj){
//		    			it.remove();
//		    			break;
//		    		}
//		    	}
//		    	//add
//		    	newDir.getFiles().add((FsFile) obj);
//		    	obj.setName(newName);
//		    	
//	    	}
//	    	obj.setParent(newDir);
//	    	// save/propagate
//        	oldDir.flush();
//        	newDir.flush();
//        	obj.flush();

        	obj.setName(newName);
        	if(oldDir != newDir){
        		System.out.println("FS : move "+obj.getPath()+" from "+oldDir.getPath()+" to "+newDir.getPath());
	        	if(obj instanceof FsFile){
	    	    	oldDir.moveFile((FsFile)obj, newDir);
	        	}else if(obj instanceof FsDirectory){
	    	    	oldDir.moveDir((FsDirectory)obj, newDir);
	        	}
        	}
        	obj.changes();
        	obj.flush();
        	manager.propagateChange(obj);
	    	
	    	
	        return 0;
		
		}catch(Exception e){
			e.printStackTrace();
			return -ErrorCodes.EIO();
		}
    }

    @Override
    @NotImplemented
    public int link(String oldpath, String newpath) {
        return 0;
    }

    @Override
    public int chmod(String path, @mode_t long mode) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC chmod for (path) : "+path);
    	try{
    		FsDirectory obj = getPathDir(manager.getRoot(), path);
	    	if(obj==null ){
	    		return -ErrorCodes.ENOENT();
	    	}

	    	//change rights
	    	obj.setPUGA(modeToPUGA(mode));

	    	// save/propagate
        	obj.flush();
        	manager.propagateChange(obj);
	    	
	        return 0;
		}catch(Exception e){
			e.printStackTrace();
			return -ErrorCodes.EIO();
		}
    }

    @Override
    public int chown(String path, @uid_t long uid, @gid_t long gid) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC chown for (path) : "+path);
    	FsObject obj = getPathObj(manager.getRoot(), path);
    	if(obj==null){
            return -ErrorCodes.ENOENT();
    	}
    	obj.setUserId(uid);
    	obj.setGroupId(gid);

    	// save/propagate
    	obj.flush();
    	manager.propagateChange(obj);
    	
        return 0;
    }

    @Override
    public int truncate(String path, @off_t long size) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC truncate for (path) : "+path+", for new size : "+size);
    	FsFile fic = getPathFile(manager.getRoot(), path);
    	if(fic==null){
            return -ErrorCodes.ENOENT();
    	}
    	if(size > Integer.MAX_VALUE){
            return -ErrorCodes.EMSGSIZE();
    	}
    	try{
    		
//	    	fic.truncate(size);
    		FsFile.truncate(fic, size);

	    	// save/propagate
	    	fic.flush();
        	manager.propagateChange(fic);
        	
	    	return 0;
    	}catch(Exception e){
    		e.printStackTrace();
    		return -ErrorCodes.EIO();
    	}
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
    	try{
	    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
	    	System.out.println(">>>>NC Open for (path) : "+path+" with flags: "+fi.flags.get());
	        if (getPathFile(manager.getRoot(), path) == null) {
	        	System.err.println("can't open this file");
	            return -ErrorCodes.ENOENT();
	        }
	        Object padseek = fi.nonseekable;
	        jnr.ffi.Pointer point;
	        fi.fh.struct();
	//    	System.out.println("open : ok, no problem");
	//    	System.out.println("nonseekable:"+fi.nonseekable);
	//    	System.out.println("direct_io:"+fi.direct_io);
	//    	System.out.println("padding:"+fi.padding);
	//    	System.out.println("keep_cache:"+fi.keep_cache);
	//    	System.out.println("lock_owner:"+fi.lock_owner);
	//    	System.out.println("flock_release:"+fi.flock_release);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
        return 0;
    }

    @Override
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC Read for (path) : "+path+", for off/size "+offset+" / "+size);
    	FsFile fic = getPathFile(manager.getRoot(), path);
    	if(fic==null){
    		System.err.println("can't read: no file");
            return -ErrorCodes.ENOENT();
    	}
    	if(size > Integer.MAX_VALUE){
    		System.err.println("can't read: size >2gio");
            return -ErrorCodes.EMSGSIZE();
    	}
    	int readsize = (int) size;
    	if(readsize+offset>fic.getSize()){
//            return -ErrorCodes.ENODATA();
    		readsize = (int) (fic.getSize() - offset);
        	System.out.println("read : reduce size read to'"+readsize+"' ("+fic.getSize()+" - "+offset+")");
    		if(readsize<0)  return -ErrorCodes.ENODATA();
    	}
    	try{
    		if(readsize==0){
    	    	return 0;
    		}
	    	ByteBuff buff = new ByteBuff(readsize);
	    	FsFile.read(fic, buff, offset);
	
	    	buf.put(0, buff.array(), 0, readsize);
//	    	System.out.println("read : '"+ Charset.forName("UTF-8").decode(ByteBuffer.wrap(buff.array()))+"'");
//	    	System.out.println("read : size = "+ buff.array().length);
	    	return readsize;
    	}catch(TimeoutException e){
    		return -ErrorCodes.ETIME();
    	}catch(UnreachableChunkException e){
    		return -ErrorCodes.EOWNERDEAD(); //ENODATA? EFAULT? ECOMM?
    	}catch(Exception e){
    		e.printStackTrace();
    		return -ErrorCodes.EIO();
    	}
    }

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC Write for (path) : "+path+", for off/size "+offset+" / "+size);
    	FsFile fic = getPathFile(manager.getRoot(), path);
    	if(fic==null){
            return -ErrorCodes.ENOENT();
    	}
    	if(size > Integer.MAX_VALUE){
            return -ErrorCodes.EMSGSIZE();
    	}
    	int writesize = (int) size;
    	try{
	    	ByteBuff buff = new ByteBuff(writesize);
	    	buf.get(0, buff.array(), 0, writesize);
	    	
	    	FsFile.write(fic, buff, offset);
	
//	    	System.out.println("write : '"+ Charset.forName("UTF-8").decode(ByteBuffer.wrap(buff.array()))+"'");

	    	// save/propagate
	    	fic.flush();
        	manager.propagateChange(fic);
        	
        	System.out.println("write "+writesize+" bytes / "+size);
	    	return writesize;
    	}catch(TimeoutException e){
    		return -ErrorCodes.ETIME();
    	}catch(UnreachableChunkException e){
    		return -ErrorCodes.EOWNERDEAD(); //ENODATA? EFAULT? ECOMM?
    	}catch(Exception e){
    		e.printStackTrace();
    		return -ErrorCodes.EIO();
    	}
    }

    
    @Override
    public int statfs(String path, Statvfs stbuf) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC statfs for (path) : "+path);
        if (Platform.getNativePlatform().getOS() == jnr.ffi.Platform.OS.WINDOWS) {
            // statfs needs to be implemented on Windows in order to allow for copying
            // data from other devices because winfsp calculates the volume size based
            // on the statvfs call.
            // see https://github.com/billziss-gh/winfsp/blob/14e6b402fe3360fdebcc78868de8df27622b565f/src/dll/fuse/fuse_intf.c#L654
            if ("/".equals(path)) {
            	System.out.println(">>>>NC statfs OK");
                stbuf.f_blocks.set(1024 * 1024); // total data blocks in file system
                stbuf.f_frsize.set(1024);        // fs block size
                stbuf.f_bfree.set(1024 * 1024);  // free blocks in fs
            }else{
            	System.err.println(">>>>NC statfs KO");
            }
        }
        return super.statfs(path, stbuf);
    }
    

    @Override
    @NotImplemented
    public int flush(String path, FuseFileInfo fi) {
    	System.out.println(">>>>NC flush '"+path+"'");
        return 0;
    }

    @Override
    @NotImplemented
    public int release(String path, FuseFileInfo fi) {
    	System.out.println(">>>>NC release '"+path+"'");
        return 0;
    }

    @Override
    @NotImplemented
    public int fsync(String path, int isdatasync, FuseFileInfo fi) {
    	System.out.println(">>>>NC fsync "+path);
        return 0;
    }

    @Override
    @NotImplemented
    public int setxattr(String path, String name, Pointer value, @size_t long size, int flags) {
    	System.out.println(">>>>NC setxattr "+path);
        return 0;
    }

    @Override
    @NotImplemented
    public int getxattr(String path, String name, Pointer value, @size_t long size) {
    	System.out.println(">>>>NC getxattr "+path);
        return 0;
    }

    @Override
    @NotImplemented
    public int listxattr(String path, Pointer list, @size_t long size) {
    	System.out.println(">>>>NC listxattr "+path);
        return 0;
    }

    @Override
    @NotImplemented
    public int removexattr(String path, String name) {
    	System.out.println(">>>>NC removexattr "+path);
        return 0;
    }

    @Override
    @NotImplemented
    public int opendir(String path, FuseFileInfo fi) {
    	System.out.println(">>>>NC opendir "+path);
        return 0;
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println(">>>>NC readdir for (path) : "+path);
    	FsDirectory dir = getPathDir(manager.getRoot(), path);
    	if(dir == null && path.equals("/")){
    		dir = manager.getRoot();
    	}
    	if(dir==null){
    		System.err.println("Error, can't read not-existant dir "+path);
    		return -ErrorCodes.ENOENT();
    	}
    	try{
	    	filter.apply(buf, ".",  null, 0);
	    	filter.apply(buf, "..",  null, 0);
	    	for(FsDirectory childDir : dir.getDirs()){
	        	System.out.println(dir.getName()+" have a dir "+childDir.getName());
	        	filter.apply(buf, childDir.getName(), null, 0);
	    	}
	    	for(FsFile childFile : dir.getFiles()){
	        	System.out.println(dir.getName()+" have a file "+childFile.getName());
	        	filter.apply(buf, childFile.getName(), null, 0);
	    	}
    	}catch(WrongSectorTypeException e)
    	{
    		autocorrectProblems(dir);
    		return readdir(path, buf, filter, offset, fi);
    	}
        return 0;
    }

    @Override
    @NotImplemented
    public int releasedir(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    @NotImplemented
    public int fsyncdir(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    @NotImplemented
    public Pointer init(Pointer conn) {
        return null;
    }

    @Override
    @NotImplemented
    public void destroy(Pointer initResult) {
    }

    @Override
    @NotImplemented
    public int access(String path, int mask) {
    	System.out.println(">>>>NC access "+path);
        return 0;
    }

    @Override
    @NotImplemented
    public int ftruncate(String path, @off_t long size, FuseFileInfo fi) {
        return truncate(path, size);
    }

    @Override
    @NotImplemented
    public int fgetattr(String path, FileStat stbuf, FuseFileInfo fi) {
        return getattr(path, stbuf);
    }

    @Override
    @NotImplemented
    public int lock(String path, FuseFileInfo fi, int cmd, Flock flock) {
    	System.out.println(">>>>NC lock "+path);
//        return -ErrorCodes.ENOSYS();
    	return 0;
    }

    @Override
    @NotImplemented
    public int utimens(String path, Timespec[] timespec) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    @NotImplemented
    public int bmap(String path, @size_t long blocksize, long idx) {
        return 0;
    }

    @Override
    @NotImplemented
    public int ioctl(String path, int cmd, Pointer arg, FuseFileInfo fi, @u_int32_t long flags, Pointer data) {
        return -ErrorCodes.ENOSYS();
    }

    //TODO
    @Override
    @NotImplemented
    public int poll(String path, FuseFileInfo fi, FusePollhandle ph, Pointer reventsp) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    @NotImplemented
    public int write_buf(String path, FuseBufvec buf, @off_t long off, FuseFileInfo fi) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println("write_buf "+path);
    	// TODO.
        // Some problem in implementation, but it not enabling by default
        int res;
        int size = (int) libFuse.fuse_buf_size(buf);
        FuseBuf flatbuf;
        FuseBufvec tmp = new FuseBufvec(Runtime.getSystemRuntime());
        long adr = MemoryIO.getInstance().allocateMemory(Struct.size(tmp), false);
        tmp.useMemory(Pointer.wrap(Runtime.getSystemRuntime(), adr));
        FuseBufvec.init(tmp, size);
        long mem = 0;
        if (buf.count.get() == 1 && buf.buf.flags.get() == FuseBufFlags.FUSE_BUF_IS_FD) {
            flatbuf = buf.buf;
        } else {
            res = -ErrorCodes.ENOMEM();
            mem = MemoryIO.getInstance().allocateMemory(size, false);
            if (mem == 0) {
                MemoryIO.getInstance().freeMemory(adr);
                return res;
            }
            tmp.buf.mem.set(mem);
            res = (int) libFuse.fuse_buf_copy(tmp, buf, 0);
            if (res <= 0) {
                MemoryIO.getInstance().freeMemory(adr);
                MemoryIO.getInstance().freeMemory(mem);
                return res;
            }
            tmp.buf.size.set(res);
            flatbuf = tmp.buf;
        }
        res = write(path, flatbuf.mem.get(), flatbuf.size.get(), off, fi);
        if (mem != 0) {
            MemoryIO.getInstance().freeMemory(adr);
            MemoryIO.getInstance().freeMemory(mem);
        }
        return res;
    }

    @Override
    @NotImplemented
    public int read_buf(String path, Pointer bufp, @size_t long size, @off_t long off, FuseFileInfo fi) {
    	if(path.indexOf(0)>0) path = path.substring(0,path.indexOf(0));
    	System.out.println("read_buf "+path);
        // should be implemented or null
        long vecmem = MemoryIO.getInstance().allocateMemory(Struct.size(new FuseBufvec(Runtime.getSystemRuntime())), false);
        if (vecmem == 0) {
            return -ErrorCodes.ENOMEM();
        }
        Pointer src = Pointer.wrap(Runtime.getSystemRuntime(), vecmem);
        long memAdr = MemoryIO.getInstance().allocateMemory(size, false);
        if (memAdr == 0) {
            MemoryIO.getInstance().freeMemory(vecmem);
            return -ErrorCodes.ENOMEM();
        }
        Pointer mem = Pointer.wrap(Runtime.getSystemRuntime(), memAdr);
        FuseBufvec buf = FuseBufvec.of(src);
        FuseBufvec.init(buf, size);
        buf.buf.mem.set(mem);
        bufp.putAddress(0, src.address());
        int res = read(path, mem, size, off, fi);
        if (res >= 0)
            buf.buf.size.set(res);
        return res;
    }

    @Override
    @NotImplemented
    public int flock(String path, FuseFileInfo fi, int op) {
    	System.out.println("flock "+path);
//        return -ErrorCodes.ENOSYS();
    	return 0;
    }

    @Override
    @NotImplemented
    public int fallocate(String path, int mode, @off_t long off, @off_t long length, FuseFileInfo fi) {
    	System.out.println("fallocate "+path);
//        return -ErrorCodes.ENOSYS();
    	return 0;
    }
    
	public void close() {
		try{
			this.umount();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
