package models.auto;

import models.Company;

/**
 * Class _Person was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Person extends PlayDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String COMPANY_PROPERTY = "company";

    public static final String ID_PK_COLUMN = "ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }

    public void setCompany(Company company) {
        setToOneTarget("company", company, true);
    }

    public Company getCompany() {
        return (Company)readProperty("company");
    }


}
