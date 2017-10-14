package remi.distributedFS.db.impl;

import java.util.ArrayList;
import java.util.List;

import remi.distributedFS.datastruct.FsChunk;
import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.datastruct.FsObjectVisitor;

public class RemoveOldDeletedItems implements FsObjectVisitor{
	
	long dateThreshold;

	@Override
	public void visit(FsDirectory dirParent) {
		System.out.println("RemoveOldDeletedItems dir "+dirParent.getPath());
		boolean modified = false;
		try{
			//profondeur d'abords
			for(FsDirectory dir : new ArrayList<>(dirParent.getDirs())){
				try{
					visit(dir);
				}catch(WrongSectorTypeException ex){
					ex.printStackTrace();
					//recover : del this
					dirParent.getDirs().remove(dir);
					((FsDirectoryFromFile)dirParent).setDirty(true);
					modified = true;
	//				dirParent.flush();
				}
				System.out.println(dir.getPath() + " "+dir.getId());
			}
			
			//get all thing to del
			List<FsObject> objToDel = null;
			for(FsObject obj : dirParent.getDelete()){
				if(obj.getDeleteDate() < dateThreshold){
					System.out.println("find to del : "+obj.getPath()+" , diff of date : "+(obj.getDeleteDate() - dateThreshold));
					if(objToDel==null) objToDel = new ArrayList<>();
					objToDel.add(obj);
				}else{
					System.out.println("find to keep : "+obj.getPath()+" , diff of date : "+(obj.getDeleteDate() - dateThreshold));
				}
			}
			//del them
			if(objToDel!=null){
				for(FsObject obj : objToDel){
					System.out.println("Delete definitively  : "+obj.getPath()+" , diff of date : "+(obj.getDeleteDate() - dateThreshold));
					dirParent.getDelete().remove(obj);
					obj.delete();
					modified = true;
				}
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}finally {

			//update if needed
			if(modified){
				dirParent.flush();
			}
		}
	}

	@Override
	public void visit(FsFile fic) {
	}

	@Override
	public void visit(FsChunk chunk) {
		
	}
	
}
