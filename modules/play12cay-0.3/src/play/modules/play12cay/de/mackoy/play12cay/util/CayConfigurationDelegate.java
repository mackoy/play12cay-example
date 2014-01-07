package de.mackoy.play12cay.util;

import org.apache.cayenne.conf.Configuration;

import de.mackoy.play12cay.exceptions.NoDatabaseConfigurationException;

public interface CayConfigurationDelegate {

	public Configuration getInitializedConfiguration() throws NoDatabaseConfigurationException;
	
}
