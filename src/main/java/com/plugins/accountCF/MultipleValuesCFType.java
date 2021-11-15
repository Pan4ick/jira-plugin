package com.plugins.accountCF;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.AbstractCustomFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.NotNullPredicate;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.atlassian.jira.issue.customfields.impl.DateCFType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * All the other Multi* classes refer to Users or Options. This class,
 * like VersionCFType, uses a different transport object, a Collection
 * of Carrier objects.
 *
 * The changes for JIRA 5.0 mean that the transport and singular types
 * have to be given as a parameter to AbstractCustomFieldType. Also
 *
 * More information can be found at
 * "https://developer.atlassian.com/display/JIRADEV/Java+API+Changes+in+JIRA+5.0#JavaAPIChangesinJIRA50-CustomFieldTypes
 */
public class MultipleValuesCFType extends AbstractCustomFieldType<Collection<Carrier>, Carrier> {

    public static final Logger log = Logger.getLogger(MultipleValuesCFType.class);

    private final CustomFieldValuePersister persister;
    private final GenericConfigManager genericConfigManager;
    private  SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    // The type of data in the database, one entry per value in this field
    private static final PersistenceFieldType DB_TYPE = PersistenceFieldType.TYPE_UNLIMITED_TEXT;

    /**
     * Used in the database representation of a singular value.
     * Treated as a regex when checking text input.
     */
    public static final String DB_SEP = "###";

    public MultipleValuesCFType( @JiraImport CustomFieldValuePersister customFieldValuePersister,
                                 @JiraImport GenericConfigManager genericConfigManager) {
        this.persister = customFieldValuePersister;
        this.genericConfigManager = genericConfigManager;
    }

    /**
     * Convert a database representation of a Carrier object into
     * a Carrier object. This method is also used for bulk moves and imports.
     */
    public Carrier getSingularObjectFromString(String dbValue)
            throws FieldValidationException {
        log.debug("getSingularObjectFromString: " + dbValue);
        if (StringUtils.isEmpty(dbValue)) {
            return null;
        }
        String[] parts = dbValue.split(DB_SEP);
        if (parts.length == 0 || parts.length > 3) {
            log.warn("Invalid database value for MultipleValuesCFType ignored: " + dbValue);
            // If this should not be allowed, then throw a
            // FieldValidationException instead
            return null;
        }

        Date d = null;
        try {
            d = sdf.parse(parts[0]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Double p = new Double(parts[1]);
        Double a = new Double(parts[2]);
        return new Carrier(d, p, a);
    }

    public Collection<Carrier> getValueFromIssue(CustomField field,
                                                 Issue issue) {
        // This is also called to display a default value in view.vm
        // in which case the issue is a dummy one with no key
        if (issue == null || issue.getKey() == null) {
            log.debug("getValueFromIssue was called with a dummy issue for default");
            return null;
        }

        // These are the database representation of the singular objects
        final List<Object> values = persister.getValues(field, issue.getId(), DB_TYPE);
        log.debug("getValueFromIssue entered with " + values);
        if ((values != null) && !values.isEmpty()) {
            List<Carrier> result = new ArrayList<Carrier>();
            for (Iterator it = values.iterator(); it.hasNext(); ) {
                String dbValue = (String)it.next();
                Carrier carrier = getSingularObjectFromString(dbValue);
                if (carrier == null) {
                    continue;
                }
                result.add(carrier);
            }
            return result;
        } else {
            return null;
        }
    }

    public void createValue(CustomField field, Issue issue, Collection<Carrier> value) {
        if (value instanceof Collection)
        {
            persister.createValues(field, issue.getId(), DB_TYPE, getDbValueFromCollection(value));
        }
        else
        {
            // With JIRA 5.0 we should no longer need to test for this case
            persister.createValues(field, issue.getId(), DB_TYPE, getDbValueFromCollection(Lists.newArrayList(value)));
        }
    }

    public void updateValue(CustomField field, Issue issue, Collection<Carrier> value) {
        persister.updateValues(field, issue.getId(), DB_TYPE, getDbValueFromCollection(value));
    }

    /**
     * For removing the field, not for removing one value
     */
    public Set<Long> remove(CustomField field) {
        return persister.removeAllValues(field.getId());
    }

    /**
     * Convert a transport object (a Collection of Carrier objects) to
     * its database representation and store it in the database.
     */
    public void setDefaultValue(FieldConfig fieldConfig, Collection<Carrier> value) {
        log.debug("setDefaultValue with object " + value);
        Collection carrierStrings = getDbValueFromCollection(value);
        if (carrierStrings != null) {
            carrierStrings = new ArrayList(carrierStrings);
            genericConfigManager.update(CustomFieldType.DEFAULT_VALUE_TYPE, fieldConfig.getId().toString(), carrierStrings);
        }
    }

    /**
     * Retrieve the stored default value (if any) from the database
     * and convert it to a transport object (a Collection of Carrier
     * objects).
     */
    public Collection<Carrier> getDefaultValue(FieldConfig fieldConfig) {
        final Object o = genericConfigManager.retrieve(CustomFieldType.DEFAULT_VALUE_TYPE, fieldConfig.getId().toString());
        log.debug("getDefaultValue with database value " + o);

        Collection<Carrier> collectionOfCarriers = null;
        if (o instanceof Collection) {
            collectionOfCarriers = (Collection) o;
        } else if (o instanceof Carrier) {
            log.warn("Backwards compatible default value, should not occur");
            collectionOfCarriers = Lists.newArrayList((Collection)o);
        }

        if (collectionOfCarriers == null) {
            return null; // No default value exists
        }

        final Collection collection = CollectionUtils.collect(collectionOfCarriers, new Transformer() {
            // Convert a database value (String) to a singular Object (Carrier)
            public Object transform(final Object input) {
                if (input == null) {
                    return null;
                }
                String dbValue = (String)input;
                return getSingularObjectFromString(dbValue);
            }
        });
        CollectionUtils.filter(collection, NotNullPredicate.getInstance());
        log.debug("getDefaultValue returning " + collection);
        return collection;
    }

    /**
     * Validate the input from the web pages, a Collection of Strings.
     * Exceptions raised later on after this has passed appear as an
     * ugly page.
     */
    public void validateFromParams(CustomFieldParams relevantParams,
                                   ErrorCollection errorCollectionToAddTo,
                                   FieldConfig config) {
        log.debug("validateFromParams: " + relevantParams.getKeysAndValues());
        try {
            getValueFromCustomFieldParams(relevantParams);
        } catch (FieldValidationException fve) {
            errorCollectionToAddTo.addError(config.getCustomField().getId(), fve.getMessage());
        }
    }

    /**
     * Extract a transport object from the string parameters,
     * Clearing an amount removes the row.
     */
    public Collection<Carrier> getValueFromCustomFieldParams(CustomFieldParams parameters)
            throws FieldValidationException{
        log.debug("getValueFromCustomFieldParams: " + parameters.getKeysAndValues());
        // Strings in the order they appeared in the web page
        final Collection values = parameters.getValuesForNullKey();
        if ((values != null) && !values.isEmpty()) {
            Collection<Carrier> value = new ArrayList();
            Iterator it = values.iterator();
            while ( it.hasNext() ) {
                String dStr = (String)it.next();
                // This won't be true if only one parameter is passed in a query
                String pStr = (String)it.next();
                pStr = pStr.replaceAll("%", "");
                // Allow empty text but not empty amounts
                String aStr = ((String)it.next()).replaceAll("\\s+", "");;
                if (dStr == null || dStr.equals("")) {
                    log.debug("Ignoring text " + pStr + " because the amount is empty");
                    // This is used to delete a row so do not throw a
                    // FieldValidationException
                    continue;
                }
                if (pStr == null) {
                    pStr = "";
                }
                // Make sure the value can be stored safely later on
                pStr = pStr.replace(DB_SEP, "");

                Date d = null;
                try {
                    d = sdf.parse(dStr);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                try {
                    Double p =  Double.parseDouble(pStr);
                    Double a =  Double.parseDouble(aStr);
                    value.add(new Carrier(d, p, a));
                } catch (NumberFormatException nfe) {
                    // A value was provided but it was an invalid value
                    throw new FieldValidationException("Введите числа");
                }
            }
            return value;
        } else {
            return null;
        }
    }

    /**
     * This method is used to create the $value object in Velocity templates.
     */
    public Object getStringValueFromCustomFieldParams(CustomFieldParams parameters) {
        log.debug("getStringValueFromCustomFieldParams: " + parameters.getKeysAndValues());
        return parameters.getAllValues();
    }

    public String getStringFromSingularObject(Carrier singularObject) {
        return singularObject.toString();
    }

    public String getChangelogValue(CustomField field, Collection<Carrier> value)  {
        if (value == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        Collection<Carrier> carriers = value;
        for (Carrier carrier: carriers) {
            sb.append(carrier.toString());
            // Newlines are text not HTML here
            sb.append(", ");

        }
        return sb.toString();
    }

    // Helper Methods

    /**
     * Convert the Transport object to a collection of the
     * representation used in the database.
     */
    private Collection getDbValueFromCollection(final Collection<Carrier> value)
    {
        log.debug("getDbValueFromCollection: " + value);
        if (value == null) {
            return Collections.EMPTY_LIST;
        }
        Collection<Carrier> carriers = value;
        List<String> result = new ArrayList<String>();
        for (Carrier carrier : carriers) {
            if (carrier == null) {
                continue;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(carrier.getStringDate());
            sb.append(DB_SEP);
            sb.append(carrier.getPercent().toString());
            sb.append(DB_SEP);
            sb.append(carrier.getAmount());
            result.add(sb.toString());
        }
        return result;
    }

}