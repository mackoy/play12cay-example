package controllers.play12cay;

import java.util.List;

import models.auto.PlayDataObject;

import org.apache.cayenne.BaseContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationException;
import org.apache.cayenne.validation.ValidationFailure;

import play.data.validation.Validation;
import play.mvc.After;
import play.mvc.Controller;
import play.mvc.With;

import de.mackoy.play12cay.util.ThreadObjectContext;

public class CayController extends Controller {
	

        // Reset ObjectContext after request is finished. Cannot be done in @Before-Handler, because
        // @Before-Handler is sometimes called before Binding occurs and sometimes after Binding occurs.
        // Which can lead to bound objects, having a differing ObjectContext than the one provided by this
        // Controller.
	@After
	static void resetObjectContext() {
		ThreadObjectContext.forceNew();
	}

	public static ObjectContext oc() {
        return ThreadObjectContext.get();
	}
	
	public static boolean saveChanges() {
		boolean saved = true;
		
		try {
			ObjectContext oc = oc();
			
			oc.commitChanges();
		} catch(ValidationException ve) {
			List<ValidationFailure> failures = ve.getValidationResult().getFailures();

			for(ValidationFailure failure : failures) {
				if(failure instanceof BeanValidationFailure) {
					BeanValidationFailure bvf = (BeanValidationFailure)failure;
					String key = ((PlayDataObject)bvf.getSource()).getBindingPath() + "." + bvf.getProperty();

					Validation.addError(key, bvf.getDescription());
				} else {
					Validation.addError("general", failure.getDescription());
				}
			}
			
			saved = false;
		}
		
		return saved;
	}
}
