package de.mackoy.play12cay.plugins;

import javassist.CtClass;
import javassist.CtMethod;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

public class PlayDataObjectEnhancer extends Enhancer {

	@Override
	public void enhanceThisClass(ApplicationClass appClass)	throws Exception {
		final CtClass ctClass = this.makeClass(appClass);
		
		if(ctClass.subclassOf(this.classPool.get("models.auto.PlayDataObject"))) {
			String entityName = ctClass.getName();

	        final CtMethod findById = CtMethod.make(
	        		"public static models.auto.PlayDataObject findById(int id) {" +
	        		
	        		"  org.apache.cayenne.ObjectContext oc = de.mackoy.play12cay.util.ThreadObjectContext.get();" +
	        		"  Class dataObjectClass = null;" +
	        		
	        		"  try {" +
	        		"    dataObjectClass = Class.forName(\"" + entityName + "\");" +
	        		"  } catch(Exception e) { throw new RuntimeException(e); }" +
	        		
	        		"  return (models.auto.PlayDataObject)org.apache.cayenne.DataObjectUtils.objectForPK(oc, dataObjectClass, id);" +
	        		"}",
	        		ctClass);
	        
	        ctClass.addMethod(findById);
	        
	        appClass.enhancedByteCode = ctClass.toBytecode();
	        ctClass.defrost();
		}
	}
}
