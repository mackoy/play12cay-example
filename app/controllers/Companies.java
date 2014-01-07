package controllers;

import java.util.List;

import org.apache.cayenne.query.SelectQuery;

import models.Company;
import models.Person;

import controllers.play12cay.CayController;

public class Companies extends CayController {

	public static void index() {
		List<Company> companies = oc().performQuery(new SelectQuery(Company.class));
		
		render(companies);
	}
	
    public static void create() {
    	Company company = oc().newObject(Company.class);

    	_edit(company);
    }
    
    public static void edit(Integer companyId) {
    	Company company = Company.findById(companyId);
    	
    	_edit(company);
    }
    
    private static void _edit(Company company) {    	
    	List<Person> people = oc().performQuery(new SelectQuery(Person.class));
    	
    	renderTemplate("Companies/edit.html", company, people);
    }
    
    public static void delete(Integer companyId) {
    	Company company = Company.findById(companyId);
    	
    	oc().deleteObject(company);
    	saveChanges();
    	
    	index();
    }
    
    public static void update(Company company) {
    	if(!saveChanges()) {
    		_edit(company);
    	} else {
    		index();
    	}
    }
}
