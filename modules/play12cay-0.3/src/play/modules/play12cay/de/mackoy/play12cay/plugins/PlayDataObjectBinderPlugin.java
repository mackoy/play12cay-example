package de.mackoy.play12cay.plugins;

import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import models.auto.PlayDataObject;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.conf.FileConfiguration;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;

import de.mackoy.play12cay.util.ThreadObjectContext;

public class PlayDataObjectBinderPlugin extends PlayPlugin {

	@Override
	public void onApplicationStart() {
		this.loadCayenneConfig();
	}

	private void loadCayenneConfig() {
		FileConfiguration conf = new FileConfiguration("cayenne.xml");
		final String driver = Play.configuration.getProperty("db.driver");
		final String url = Play.configuration.getProperty("db.url");
		final String username = Play.configuration.getProperty("db.user");
		final String password = Play.configuration.getProperty("db.pass", "");
		final String minConnections = Play.configuration.getProperty("db.minConnections", "1");
		final String maxConnections = Play.configuration.getProperty("db.maxConnections", "1");

		try {
			File base = new File(this.getClass().getResource("/").toURI());

			conf.addFilesystemPath(base.getAbsolutePath() + File.separator + "cayenne");
			conf.addFilesystemPath(File.separator + "cayenne");
			conf.addFilesystemPath("conf" + File.separator + "cayenne");
			conf.addFilesystemPath("application" + File.separator + "conf" + File.separator + "cayenne");
			conf.addFilesystemPath("classes" + File.separator + "cayenne");
		} catch(Exception e) {
			Logger.error(e, "Unable to load Cayenne configuration.");
		}

		try {
			DriverDataSourceFactory ds = new DriverDataSourceFactory () {
				@Override
				protected InputStream getInputStream(String location) {
					String xml = "<driver class=\"" + driver + "\">" + 
					"  <url value=\"" + url + "\"/>" + 
					"  <connectionPool min=\"" + minConnections + "\" max=\"" + maxConnections + "\"/>" +
					"  <login userName=\"" + username + "\" password=\"" + password + "\" />" +
					"</driver>";

					Logger.error("Cayenne-Connection-XML: \r\n%s", xml);
					return new ByteArrayInputStream(xml.getBytes());
				}
			};

			conf.setDataSourceFactory(ds);
			Configuration.initializeSharedConfiguration(conf);
		} catch (Exception e) {
			Logger.error(e, "Unable to load Cayenne configuration.");
		}		
	}

	@Override
	public void enhance(ApplicationClass appClass) throws Exception {
		new PlayDataObjectEnhancer().enhanceThisClass(appClass);
	}


	@Override
	public Object bind(RootParamNode rootParamNode, String name,
			Class<?> clazz, Type type, Annotation[] annotations) {

		Object boundObject = null;

		if(PlayDataObject.class.isAssignableFrom(clazz)) {
			ParamNode entityNode = rootParamNode.getChild(name);
			ObjectContext oc = ThreadObjectContext.get();

			Logger.debug("Binding: RootParamNode %s", rootParamNode);
			Logger.debug("Binding: Name %s", name);
			Logger.debug("Binding: Entity Node %s", entityNode);

			if(entityNode == null) {
				throw new IllegalStateException("No form parameters for binding: " + name);
			}

			boundObject = bindObject(entityNode, clazz, oc, entityNode.getOriginalKey());
		}

		return boundObject;
	}

	private PlayDataObject bindObject(ParamNode entityNode, Class<?> clazz, ObjectContext oc, String path) {
		ParamNode idNode = getIdNode(entityNode);
		PlayDataObject pdo = null;

		Logger.debug("Binding cayenne object (%s). Path: %s. Thread: %s. Object Context: %s", clazz, path, Thread.currentThread().getName(), oc);

		if(idNode != null) {
			String idVal = idNode.getFirstValue(Object.class);

			if(idVal != null && !"".equals(idVal)) {
				int pk = Integer.parseInt(idVal);

				Logger.debug("Binding: Use exising Object with ID: %s", pk);

				pdo = (PlayDataObject)DataObjectUtils.objectForPK(oc, clazz, pk);
				entityNode.removeChild(idNode.getName(), new ArrayList<ParamNode.RemovedNode>());
			}
		} else {
			Logger.debug("Binding: Create new Object.");

			pdo = (PlayDataObject)oc.newObject(clazz);
		}

		if(pdo != null) {
			for(ObjAttribute att : pdo.getObjEntity().getAttributes()) {
				ParamNode attNode = entityNode.getChild(att.getName());

				if(attNode != null) {
					try {
						String rawVal = attNode.getFirstValue(null);
						Class attClass = att.getJavaClass();
						Object value = rawVal.matches("^\\s*$") ? null : Binder.directBind(rawVal, attClass);

						this.setAttribute(pdo, att.getName(), value);
					} catch(Exception e) {
						Logger.error(e, "Error binding attribute: %s", att.getName());
					}
				}
			}

			for(ObjRelationship rel : pdo.getObjEntity().getRelationships()) {
				ParamNode relNode = entityNode.getChild(rel.getName());
				Class<?> destClass = oc.getEntityResolver().getObjEntity(rel.getTargetEntityName()).getJavaClass();

				if(this.clearToMany(rel, relNode)) {
					ArrayList<PlayDataObject> toRem = new ArrayList<PlayDataObject>();

					for(PlayDataObject oldDest : (List<PlayDataObject>)pdo.readProperty(rel.getName())) {
						toRem.add(oldDest);
					}
					for(PlayDataObject oldDest : toRem) {
						this.removeFromToManyRelationship(pdo, rel.getName(), oldDest);
					}
				}

				if(relNode != null) {
					String relPath = path + "." + relNode.getName();

					Logger.debug("path: %s", path);
					Logger.debug("RelNode Orignal Key: " + relNode.getOriginalKey());
					Logger.debug("RelNode Name: " + relNode.getName());
					Logger.debug("Binding relation: %s", relPath);

					if(rel.isToMany()) {
						ArrayList<ParamNode> destNodes = new ArrayList<ParamNode>(relNode.getAllChildren());

						Logger.debug("Binding to-many");

						try {
							Collections.sort(destNodes, new Comparator<ParamNode>() {
								@Override
								public int compare(ParamNode n1, ParamNode n2) {
									Integer n1Ind = Integer.parseInt(n1.getOriginalKey());
									Integer n2Ind = Integer.parseInt(n2.getOriginalKey());

									return n1Ind.compareTo(n2Ind);
								}
							});
						} catch (NumberFormatException nfe) {
							Logger.warn(nfe, "Unable to sort to-many relation, if in binding path non numeric indices are given.");
						}

						for(ParamNode destNode : destNodes) {
							if(destNode.getName().equals("id")) {
								idNode = destNode;
								destNode = relNode;
							} else {
								idNode = this.getIdNode(destNode);
							}
							
							// flatten id node values and add relation destination objects
							if(idNode != null) {
								for(String value : idNode.getValues()) {
									Logger.debug("Flattening idNode value: %s", value);
									
									ParamNode idNodeSingleValue = new ParamNode(idNode.getName());
									          idNodeSingleValue.setValue(new String[] { value }, idNode.getOriginalKey());
									ParamNode node = new ParamNode(destNode.getName());
											  node.addChild(idNodeSingleValue);
									PlayDataObject relDest = bindObject(node, destClass, oc, relPath + "[" + node.getOriginalKey() + "]");
		
									if(relDest != null) {
										this.addToToManyRelationship(pdo, rel.getName(), relDest);
									}
								}
							}
						}
					} else {
						PlayDataObject relDest = bindObject(relNode, destClass, oc, relPath);

						Logger.debug("Binding to-one: %s . %s = %s", pdo, rel.getName(), relDest);

						this.setToOneRelationShip(pdo, rel.getName(), relDest);
					}
				}
			}

			pdo.setBindingPath(path);
		}

		return pdo;
	}

	private void setAttribute(PlayDataObject pdo, String attributeName, Object value) {
		String setterMethodName = "set" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1);
		
		try {
			Method m = pdo.getClass().getMethod(setterMethodName, value.getClass());

			m.invoke(pdo, value);
		} catch(Exception e) {
			Logger.warn("Error setting attribute '%s' of Object %s via setter. Fallback to CayenneDataObject.writeProperty.", setterMethodName, pdo.getClass().getName());
			pdo.writeProperty(attributeName, value);
		}
	}
	
	private void setToOneRelationShip(PlayDataObject pdo, String relName, PlayDataObject value) {
		String setRelMethodName = "set" + Character.toUpperCase(relName.charAt(0)) + relName.substring(1);
		
		try {
			Method m = pdo.getClass().getMethod(setRelMethodName, value.getClass());

			m.invoke(pdo, value);
		} catch(Exception e) {
			Logger.warn("Error setting relationship '%s' of Object %s via setter. Fallback to CayenneDataObject.setToOneTarget.", setRelMethodName, pdo.getClass().getName());
			pdo.setToOneTarget(relName, value, true);
		} 
	}
	
	private void addToToManyRelationship(PlayDataObject pdo, String relName, PlayDataObject value) {
		String addToRelMethodName = "addTo" + Character.toUpperCase(relName.charAt(0)) + relName.substring(1);
		
		try {
			Method m = pdo.getClass().getMethod(addToRelMethodName, value.getClass());

			m.invoke(pdo, value);
		} catch(Exception e) {
			Logger.warn("Error setting relationship '%s' of Object %s via method. Fallback to CayenneDataObject.addToManyTarget.", addToRelMethodName, pdo.getClass().getName());
			pdo.addToManyTarget(relName, value, true);
		}		
	}

	private void removeFromToManyRelationship(PlayDataObject pdo, String relName, PlayDataObject value) {
		String removeFromRelMethodName = "removeFrom" + Character.toUpperCase(relName.charAt(0)) + relName.substring(1);
		
		try {
			Method m = pdo.getClass().getMethod(removeFromRelMethodName, value.getClass());

			m.invoke(pdo, value);
		} catch(Exception e) {
			Logger.warn("Error removing from relationship '%s' of Object %s via method. Fallback to CayenneDataObject.removeToManyTarget.", removeFromRelMethodName, pdo.getClass().getName());
			pdo.removeToManyTarget(relName, value, true);
		}		
	}
	
	private boolean clearToMany(ObjRelationship rel, ParamNode relNode) {
		boolean clear;
		String legacy = play.Play.configuration.getProperty("play12cay.legacymode", "false");

		if("true".equals(legacy)) {
			// Delete to-many relations if relation is given in the query string and does have
			// children, i.e. (an) array subscript(s). Taken an object customer with the relation
			// clients, if a query string 'customer.clients[0].id=73' is given, the relations will
			// be deleted first. When the relation clients does not appear in a query string the
			// relations will be deleted first. If there is only one query string containing the
			// relation clients and it looks like customer.clients the relations will NOT be deleted.
			clear = rel.isToMany() &&
			(relNode == null || relNode.getAllChildren().size() > 0);

		} else {
			// Delete to-many relations, if relation is given. Leave it untouched otherwise.
			// To delete every item from a relation, give query string like 'customer.clients[]'
			clear = rel.isToMany() && relNode != null;
		}

		return clear;
	}

	private ParamNode getIdNode(ParamNode node) {
		return node.getChild("id");
	}
}
