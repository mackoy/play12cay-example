package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.query.SelectQuery;

import controllers.play12cay.CayController;

import models.*;

public class People extends CayController {

    public static void index() {
    	List<Person> people = oc().performQuery(new SelectQuery(Person.class));

        render(people);
    }
    
    public static void create() {
    	Person person = oc().newObject(Person.class);
    	
    	_edit(person);
    }
    
    public static void edit(Integer personId) {
    	Person person = Person.findById(personId);
    	
    	_edit(person);
    }
    
    private static void _edit(Person person) {    	
    	List<Company> companies = oc().performQuery(new SelectQuery(Company.class));
    	
    	renderTemplate("People/edit.html", person, companies);
    }
    
    public static void delete(Integer personId) {
    	Person person = Person.findById(personId);
    	
    	oc().deleteObject(person);
    	saveChanges();
    	
    	index();
    }
    
    public static void update(Person person) {
    	if(!saveChanges()) {
    		_edit(person);
    	} else {
    		index();
    	}
    }

    
}
