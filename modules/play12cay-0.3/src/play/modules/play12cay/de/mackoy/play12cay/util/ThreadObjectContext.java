package de.mackoy.play12cay.util;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.MultiProjectConfiguration;

import de.mackoy.play12cay.exceptions.NoDatabaseConfigurationException;
import de.mackoy.play12cay.util.CayConfigurationDelegate;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClassloader;
import play.mvc.Http.Request;

public class ThreadObjectContext {
    
	private static CayConfigurationDelegate delegate = null;
	
	public static boolean isSingleMode(){
		boolean singleMode = true;
		if (getDelegateClassString()!=null){
			singleMode = false;
		}
		return singleMode;
	}
	
	//Method only returns one ObjectContext for this Thread
	public static ObjectContext get() {
		ObjectContext oc;
		try {
		    oc = DataContext.getThreadObjectContext();
		} catch(IllegalStateException e) {
			if (isSingleMode()){
				oc = DataContext.createDataContext();
			} else {
				try{
					Configuration config = getDelegate().getInitializedConfiguration();
					DataDomain d = config.getDomain();
					oc = d.createDataContext();

					if (getDelegate() == null){
						throw new RuntimeException("The CayConfigurationDelegate-class could not be instantiated");
					}
				} catch (NoDatabaseConfigurationException e2){
					throw new RuntimeException("Could not find DB-configuration for current Domain!");
				}
			}
		    DataContext.bindThreadObjectContext(oc);
		}
		return oc;
    }

	public static CayConfigurationDelegate getDelegate(){
		Logger.info("Trying to get delegate... currently: %s", delegate);
		if (delegate == null){
			Logger.info("Delegate is null");
			String strClass = getDelegateClassString();
			Class clazz;
			try {
				Logger.info("Trying to initiate class '%s'", strClass);
				ApplicationClassloader loader = new ApplicationClassloader();
				clazz = loader.loadApplicationClass(strClass);
				//clazz = Class.forName(strClass, true, loader);
				delegate = (CayConfigurationDelegate) clazz.newInstance();
				Logger.info("Class set as delegate");
			} catch (InstantiationException e) {
				throw new RuntimeException("Could not instantiate class '"+ strClass +"'!");
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Illegal Access while instantiating class: '"+ strClass +"'");
			}
		}
		Logger.info("returning");
		return delegate;
	}
	
	public static String getDelegateClassString(){
		String strClass = Play.configuration.getProperty("play12cay.configurationDelegate");
		return strClass;
	}
	
    public static void set(ObjectContext oc) {
    	DataContext.bindThreadObjectContext(oc);
    }

    public static void forceNew() {
		DataContext.bindThreadObjectContext(null);
    }
}
