package models.auto;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObjectUtils;

public class PlayDataObject extends CayenneDataObject {
	
	private String bindingPath;
	
	public void setBindingPath(String bindingPath) {
		this.bindingPath = bindingPath;
	}
	
	public String getBindingPath() {
		return this.bindingPath;
	}
	
	public Integer getId() {
	    if(this.objectId == null || this.objectId.isTemporary()) {
		return null;
	    } else {
		return DataObjectUtils.intPKForObject(this);
	    }
	}
	
	public static <T extends PlayDataObject> T findById(int id) {
		throw new IllegalStateException("To be byte code enhanced.");
	}

}
