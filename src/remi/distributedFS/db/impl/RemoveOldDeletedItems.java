package remi.distributedFS.db.impl;

import java.util.ArrayList;
import java.util.List;

import remi.distributedFS.datastruct.FsDirectory;
import remi.distributedFS.datastruct.FsFile;
import remi.distributedFS.datastruct.FsObject;
import remi.distributedFS.datastruct.FsObjectVisitor;

public class RemoveOldDeletedItems implements FsObjectVisitor{
	
	long dateThreshold;

	@Override
	public void visit(FsDirectory dirParent) {
		System.out.println(" dir "+dirParent.getPath());
		boolean modified = true;
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
				if(objToDel==null) objToDel = new ArrayList<>();
				objToDel.add(obj);
			}
		}
		//del them
		if(objToDel!=null){
			for(FsObject obj : objToDel){
				dirParent.getDelete().remove(obj);
				obj.delete();
			}
		}
		//update if needed
		if(modified){
			dirParent.flush();
		}
	}

	@Override
	public void visit(FsFile fic) {
	}
	
}
