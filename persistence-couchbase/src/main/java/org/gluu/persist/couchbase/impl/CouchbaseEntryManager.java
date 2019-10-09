/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.impl;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.couchbase.model.ConvertedExpression;
import org.gluu.persist.couchbase.model.SearchReturnDataType;
import org.gluu.persist.couchbase.operation.CouchbaseOperationService;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.couchbase.operation.impl.CouchbaseOperationsServiceImpl;
import org.gluu.persist.event.DeleteNotifier;
import org.gluu.persist.exception.AuthenticationException;
import org.gluu.persist.exception.EntryDeleteException;
import org.gluu.persist.exception.EntryPersistenceException;
import org.gluu.persist.exception.MappingException;
import org.gluu.persist.exception.operation.SearchException;
import org.gluu.persist.impl.BaseEntryManager;
import org.gluu.persist.key.impl.GenericKeyConverter;
import org.gluu.persist.key.impl.model.ParsedKey;
import org.gluu.persist.model.AttributeData;
import org.gluu.persist.model.AttributeDataModification;
import org.gluu.persist.model.AttributeDataModification.AttributeModificationType;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.DefaultBatchOperation;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.SortOrder;
import org.gluu.persist.reflect.property.PropertyAnnotation;
import org.gluu.persist.reflect.util.ReflectHelper;
import org.gluu.search.filter.Filter;
import org.gluu.util.ArrayHelper;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.Sort;
import com.couchbase.client.java.subdoc.MutationSpec;

/**
 * Couchbase Entry Manager
 *
 * @author Yuriy Movchan Date: 05/14/2018
 */
public class CouchbaseEntryManager extends BaseEntryManager implements Serializable {

	private static final long serialVersionUID = 2127241817126412574L;

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    @Inject
    private Logger log;

    private final CouchbaseFilterConverter FILTER_CONVERTER;
    private static final GenericKeyConverter KEY_CONVERTER = new GenericKeyConverter();

    private SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private CouchbaseOperationsServiceImpl operationService;
    private List<DeleteNotifier> subscribers;

    protected CouchbaseEntryManager(CouchbaseOperationsServiceImpl operationService) {
        this.operationService = operationService;
        this.FILTER_CONVERTER = new CouchbaseFilterConverter(this);
        subscribers = new LinkedList<DeleteNotifier>();
    }

    @Override
    public boolean destroy() {
        if (this.operationService == null) {
            return true;
        }

        return this.operationService.destroy();
    }

    public CouchbaseOperationsServiceImpl getOperationService() {
        return operationService;
    }

    @Override
    public void addDeleteSubscriber(DeleteNotifier subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void removeDeleteSubscriber(DeleteNotifier subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public <T> T merge(T entry) {
        Class<?> entryClass = entry.getClass();
        checkEntryClass(entryClass, true);
        if (isSchemaEntry(entryClass)) {
            throw new UnsupportedOperationException("Server doesn't support dynamic schema modifications");
        } else {
            return merge(entry, false, null);
        }
    }

    @Override
    protected <T> void updateMergeChanges(String baseDn, T entry, boolean isSchemaUpdate, Class<?> entryClass, Map<String, AttributeData> attributesFromDbMap,
            List<AttributeDataModification> attributeDataModifications) {
        // Update object classes if entry contains custom object classes
        if (!isSchemaUpdate) {
            String[] objectClasses = getObjectClasses(entry, entryClass);
            if (ArrayHelper.isEmpty(objectClasses)) {
                throw new UnsupportedOperationException(String.format("There is no attribute with objectClasses to persist! Entry is invalid: '%s'", entry));
            }

            AttributeData objectClassAttributeData = attributesFromDbMap.get(OBJECT_CLASS.toLowerCase());
            if (objectClassAttributeData == null) {
                throw new UnsupportedOperationException(String.format("There is no attribute with objectClasses in DB! Entry is invalid: '%s'", entry));
            }

            String[] objectClassesFromDb = objectClassAttributeData.getStringValues();
            if (ArrayHelper.isEmpty(objectClassesFromDb)) {
                throw new UnsupportedOperationException(String.format("There is no attribute with objectClasses in DB! Entry is invalid: '%s'", entry));
            }
            
            // We need to check only first element of each array because objectCLass in Couchbase is single value attribute
            if (!StringHelper.equals(objectClassesFromDb[0], objectClasses[0])) {
                attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REPLACE,
                        new AttributeData(OBJECT_CLASS, objectClasses, false), new AttributeData(OBJECT_CLASS, objectClassesFromDb, false)));
            }
        }
    }

    @Override
    public void remove(Object entry) {
        Class<?> entryClass = entry.getClass();
        checkEntryClass(entryClass, true);
        if (isSchemaEntry(entryClass)) {
            throw new UnsupportedOperationException("Server doesn't support dynamic schema modifications");
        }

        Object dnValue = getDNValue(entry, entryClass);

        LOG.debug("LDAP entry to remove: '{}'", dnValue.toString());

        remove(dnValue.toString());
    }

    @Override
    protected void persist(String dn, List<AttributeData> attributes) {
        JsonObject jsonObject = JsonObject.create();
        for (AttributeData attribute : attributes) {
            String attributeName = attribute.getName();
            Object[] attributeValues = attribute.getValues();
            Boolean multiValued = attribute.getMultiValued();
            

            if (ArrayHelper.isNotEmpty(attributeValues) && (attributeValues[0] != null)) {
            	Object[] realValues = attributeValues;

            	// We need to store only one objectClass value in Couchbase
                if (StringHelper.equals(CouchbaseOperationService.OBJECT_CLASS, attributeName)) {
                	if (!ArrayHelper.isEmpty(realValues)) {
                		realValues = new Object[] { realValues[0] };
                		multiValued = false;
                	}
                }

            	// Process userPassword 
                if (StringHelper.equals(CouchbaseOperationService.USER_PASSWORD, attributeName)) {
                    realValues = operationService.createStoragePassword(StringHelper.toStringArray(attributeValues));
                }

                escapeValues(realValues);

                if ((multiValued == null) || !multiValued) {
                    jsonObject.put(toInternalAttribute(attributeName), realValues[0]);
                } else {
                    jsonObject.put(toInternalAttribute(attributeName), JsonArray.from(realValues));
                }
            }
        }
        jsonObject.put(CouchbaseOperationService.DN, dn);

        // Persist entry
        try {
            boolean result = operationService.addEntry(toCouchbaseKey(dn).getKey(), jsonObject);
            if (!result) {
                throw new EntryPersistenceException(String.format("Failed to persist entry: %s", dn));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to persist entry: %s", dn), ex);
        }
    }

    @Override
    public void merge(String dn, List<AttributeDataModification> attributeDataModifications) {
        // Update entry
        try {
            List<MutationSpec> modifications = new ArrayList<MutationSpec>(attributeDataModifications.size());
            for (AttributeDataModification attributeDataModification : attributeDataModifications) {
                AttributeData attribute = attributeDataModification.getAttribute();
                AttributeData oldAttribute = attributeDataModification.getOldAttribute();

                String attributeName = null;
                Object[] attributeValues = null;
                Boolean multiValued = null;
                if (attribute != null) {
                    attributeName = attribute.getName();
                    attributeValues = attribute.getValues();
                    multiValued = attribute.getMultiValued();
                }

                String oldAttributeName = null;
                Object[] oldAttributeValues = null;
                if (oldAttribute != null) {
                    oldAttributeName = oldAttribute.getName();
                    oldAttributeValues = oldAttribute.getValues();
                }

                MutationSpec modification = null;
                if (AttributeModificationType.ADD.equals(attributeDataModification.getModificationType())) {
                    modification = createModification(Mutation.DICT_ADD, toInternalAttribute(attributeName), multiValued, attributeValues);
                } else {
                    if (AttributeModificationType.REMOVE.equals(attributeDataModification.getModificationType())) {
                        modification = createModification(Mutation.DELETE, toInternalAttribute(oldAttributeName), multiValued, oldAttributeValues);
                    } else if (AttributeModificationType.REPLACE.equals(attributeDataModification.getModificationType())) {
                        modification = createModification(Mutation.REPLACE, toInternalAttribute(attributeName), multiValued, attributeValues);
                    }
                }

                if (modification != null) {
                    modifications.add(modification);
                }
            }

            if (modifications.size() > 0) {
                boolean result = operationService.updateEntry(toCouchbaseKey(dn).getKey(), modifications);
                if (!result) {
                    throw new EntryPersistenceException(String.format("Failed to update entry: %s", dn));
                }
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to update entry: %s", dn), ex);
        }
    }

    @Override
    public void remove(String dn) {
        // Remove entry
        try {
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onBeforeRemove(dn);
            }
            operationService.delete(toCouchbaseKey(dn).getKey());
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onAfterRemove(dn);
            }
        } catch (Exception ex) {
            throw new EntryDeleteException(String.format("Failed to remove entry: %s", dn), ex);
        }
    }

    @Override
    public void removeRecursively(String dn) {
        try {
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onBeforeRemove(dn);
            }
            operationService.deleteRecursively(toCouchbaseKey(dn).getKey());
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onAfterRemove(dn);
            }
        } catch (Exception ex) {
            throw new EntryDeleteException(String.format("Failed to remove entry: %s", dn), ex);
        }
    }

    @Override
	public <T> int remove(String dn, Class<T> entryClass, Filter filter, int count) {
		if (StringHelper.isEmptyString(dn)) {
			throw new MappingException("Base DN to delete entries is null");
		}

		// Remove entries by filter
		return removeImpl(dn, entryClass, filter, count);
	}

    protected <T> int removeImpl(String dn, Class<T> entryClass, Filter filter, int count) {
        // Check entry class
        checkEntryClass(entryClass, false);

        String[] objectClasses = getTypeObjectClasses(entryClass);

        Filter searchFilter;
        if (objectClasses.length > 0) {
        	 LOG.trace("Filter: {}", filter);
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        // Find entries
        LOG.trace("-------------------------------------------------------");
        LOG.trace("Filter: {}", filter);
        LOG.trace("objectClasses count: {} ", objectClasses.length);
        LOG.trace("objectClasses: {}", objectClasses.toString());
        LOG.trace("Search filter: {}", searchFilter);

		// Prepare properties types to allow build filter properly
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

        ParsedKey keyWithInum = toCouchbaseKey(dn);
        ConvertedExpression convertedExpression;
		try {
			convertedExpression = toCouchbaseFilter(searchFilter, propertiesAnnotationsMap);
		} catch (SearchException ex) {
            throw new EntryDeleteException(String.format("Failed to convert filter %s to expression", searchFilter));
		}
        
        try {
        	int processed = operationService.delete(keyWithInum.getKey(), getScanConsistency(convertedExpression), convertedExpression.expression(), count);
        	
        	return processed;
        } catch (Exception ex) {
            throw new EntryDeleteException(String.format("Failed to delete entries with key: %s, expression: %s", keyWithInum.getKey(), convertedExpression), ex);
        }
    }

	@Override
    protected List<AttributeData> find(String dn, Map<String, PropertyAnnotation> propertiesAnnotationsMap, String... ldapReturnAttributes) {
        try {
            // Load entry
            ParsedKey keyWithInum = toCouchbaseKey(dn);
            ScanConsistency scanConsistency = getScanConsistency(keyWithInum.getName(), propertiesAnnotationsMap);
            JsonObject entry = operationService.lookup(keyWithInum.getKey(), scanConsistency, toInternalAttributes(ldapReturnAttributes));
            List<AttributeData> result = getAttributeDataList(entry);
            if (result != null) {
                return result;
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry: %s", dn), ex);
        }

        throw new EntryPersistenceException(String.format("Failed to find entry: %s", dn));
    }

    @Override
    public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
            BatchOperation<T> batchOperation, int start, int count, int chunkSize) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        PagedResult<JsonObject> searchResult = findEntriesImpl(baseDN, entryClass, filter, scope, ldapReturnAttributes, null, null, batchOperation,
        		SearchReturnDataType.SEARCH, start, count, chunkSize);
        if (searchResult.getEntriesCount() == 0) {
            return new ArrayList<T>(0);
        }

        List<T> entries = createEntities(baseDN, entryClass, searchResult);

        return entries;
    }

    @Override
    public <T> PagedResult<T> findPagedEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, String sortBy,
            SortOrder sortOrder, int start, int count, int chunkSize) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        PagedResult<JsonObject> searchResult = findEntriesImpl(baseDN, entryClass, filter, SearchScope.SUB, ldapReturnAttributes, sortBy, sortOrder,
                null, SearchReturnDataType.SEARCH_COUNT, start, count, chunkSize);

        PagedResult<T> result = new PagedResult<T>();
        result.setEntriesCount(searchResult.getEntriesCount());
        result.setStart(searchResult.getStart());
        result.setTotalEntriesCount(searchResult.getTotalEntriesCount());

        if (searchResult.getEntriesCount() == 0) {
            result.setEntries(new ArrayList<T>(0));
            return result;
        }

        List<T> entries = createEntities(baseDN, entryClass, searchResult);
        result.setEntries(entries);

        return result;
    }

    protected <T> PagedResult<JsonObject> findEntriesImpl(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope,
            String[] ldapReturnAttributes, String sortBy, SortOrder sortOrder, BatchOperation<T> batchOperation, SearchReturnDataType returnDataType, int start,
            int count, int chunkSize) {
        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);

        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        String[] currentLdapReturnAttributes = ldapReturnAttributes;
        if (ArrayHelper.isEmpty(currentLdapReturnAttributes)) {
            currentLdapReturnAttributes = getAttributes(null, propertiesAnnotations, false);
        }

        Filter searchFilter;
        if (objectClasses.length > 0) {
        	 LOG.trace("Filter: {}", filter);
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        // Find entries
        LOG.trace("-------------------------------------------------------");
        LOG.trace("Filter: {}", filter);
        LOG.trace("objectClasses count: {} ", objectClasses.length);
        LOG.trace("objectClasses: {}", ArrayHelper.toString(objectClasses));
        LOG.trace("Search filter: {}", searchFilter);

        // Prepare default sort
        Sort[] defaultSort = getDefaultSort(entryClass);

        if (StringHelper.isNotEmpty(sortBy)) {
            Sort requestedSort = buildSort(sortBy, sortOrder);

            if (ArrayHelper.isEmpty(defaultSort)) {
                defaultSort = new Sort[] { requestedSort };
            } else {
                defaultSort = ArrayHelper.arrayMerge(new Sort[] { requestedSort }, defaultSort);
            }
        }

		// Prepare properties types to allow build filter properly
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);
        ParsedKey keyWithInum = toCouchbaseKey(baseDN);
        ConvertedExpression convertedExpression;
		try {
			convertedExpression = toCouchbaseFilter(searchFilter, propertiesAnnotationsMap);
		} catch (SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to convert filter %s to expression", searchFilter));
		}

        PagedResult<JsonObject> searchResult = null;
        try {
            CouchbaseBatchOperationWraper<T> batchOperationWraper = null;
            if (batchOperation != null) {
                batchOperationWraper = new CouchbaseBatchOperationWraper<T>(batchOperation, this, entryClass, propertiesAnnotations);
            }
            searchResult = searchImpl(keyWithInum.getKey(), getScanConsistency(convertedExpression), convertedExpression.expression(), scope, currentLdapReturnAttributes,
                    defaultSort, batchOperationWraper, returnDataType, start, count, chunkSize);

            if (searchResult == null) {
                throw new EntryPersistenceException(String.format("Failed to find entries with key: %s, expression: %s", keyWithInum.getKey(), convertedExpression));
            }

            return searchResult;
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with key: %s, expression: %s", keyWithInum.getKey(), convertedExpression), ex);
        }
    }

	@Override
    protected <T> boolean contains(String baseDN, Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations, Filter filter, String[] objectClasses, String[] ldapReturnAttributes) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to check contain entries is null");
        }

        // Create filter
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

		// Prepare properties types to allow build filter properly
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

        ConvertedExpression convertedExpression;
		try {
			convertedExpression = toCouchbaseFilter(searchFilter, propertiesAnnotationsMap);
		} catch (SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to convert filter %s to expression", searchFilter));
		}

        PagedResult<JsonObject> searchResult = null;
        try {
            ParsedKey keyWithInum = toCouchbaseKey(baseDN);
            searchResult = searchImpl(keyWithInum.getKey(), getScanConsistency(convertedExpression), convertedExpression.expression(), SearchScope.SUB, ldapReturnAttributes, null,
                    null, SearchReturnDataType.SEARCH, 1, 1, 0);
            if (searchResult == null) {
                throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        return (searchResult != null) && (searchResult.getEntriesCount() > 0);
    }

	private <O> PagedResult<JsonObject> searchImpl(String key, ScanConsistency scanConsistency, Expression expression, SearchScope scope, String[] attributes, Sort[] orderBy,
            CouchbaseBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
		return operationService.search(key, scanConsistency, expression, scope, toInternalAttributes(attributes), orderBy, batchOperationWraper, returnDataType, start, count, pageSize);
	}

    protected <T> List<T> createEntities(String baseDN, Class<T> entryClass, PagedResult<JsonObject> searchResult) {
        ParsedKey keyWithInum = toCouchbaseKey(baseDN);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        List<T> entries = createEntities(entryClass, propertiesAnnotations, keyWithInum,
                searchResult.getEntries().toArray(new JsonObject[searchResult.getEntriesCount()]));

        return entries;
    }

    protected <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations, ParsedKey baseDn,
            JsonObject... searchResultEntries) {
        List<T> result = new ArrayList<T>(searchResultEntries.length);
        Map<String, List<AttributeData>> entriesAttributes = new LinkedHashMap<String, List<AttributeData>>(100);

        int count = 0;
        for (int i = 0; i < searchResultEntries.length; i++) {
            count++;
            JsonObject entry = searchResultEntries[i];
            // String key = entry.getString(CouchbaseOperationService.META_DOC_ID);
            String dn = entry.getString(fromInternalAttribute(CouchbaseOperationService.DN));
            entriesAttributes.put(dn, getAttributeDataList(entry));

            // Remove reference to allow java clean up object
            searchResultEntries[i] = null;

            // Allow java to clean up temporary objects
            if (count >= 100) {
                List<T> currentResult = createEntities(entryClass, propertiesAnnotations, entriesAttributes);
                result.addAll(currentResult);

                entriesAttributes = new LinkedHashMap<String, List<AttributeData>>(100);
                count = 0;
            }
        }

        List<T> currentResult = createEntities(entryClass, propertiesAnnotations, entriesAttributes);
        result.addAll(currentResult);

        return result;
    }

    private List<AttributeData> getAttributeDataList(JsonObject entry) {
        if (entry == null) {
            return null;
        }

        List<AttributeData> result = new ArrayList<AttributeData>();
        for (String shortAttributeName : entry.getNames()) {
        	Object attributeObject = entry.get(shortAttributeName);

        	String attributeName = fromInternalAttribute(shortAttributeName);

        	Boolean multiValued = Boolean.FALSE;
            Object[] attributeValueObjects;
            if (attributeObject == null) {
                attributeValueObjects = NO_OBJECTS;
            }
            if (attributeObject instanceof JsonArray) {
            	JsonArray jsonArray = (JsonArray) attributeObject;
            	ArrayList<Object> resultList = new ArrayList<Object>(jsonArray.size());
            	for (Iterator<Object> it = jsonArray.iterator(); it.hasNext();) {
            		resultList.add(it.next());
				}
                attributeValueObjects = resultList.toArray(NO_OBJECTS);
                multiValued = Boolean.TRUE;
            } else {
            	if ((attributeObject instanceof Boolean) || (attributeObject instanceof Integer) || (attributeObject instanceof Long) ||
            		(attributeObject instanceof JsonObject)) {
                    attributeValueObjects = new Object[] { attributeObject };
            	} else if (attributeObject instanceof String) {
               		Object value = attributeObject.toString();
                    try {
                    	value = jsonDateFormat.parse(attributeObject.toString());
                    } catch (Exception ex) {}
            		attributeValueObjects = new Object[] { value };
            	} else {
               		Object value = attributeObject.toString();
            		attributeValueObjects = new Object[] { value };
            	}
            }
            
            unescapeValues(attributeValueObjects);

            AttributeData tmpAttribute = new AttributeData(attributeName, attributeValueObjects);
            if (multiValued != null) {
            	tmpAttribute.setMultiValued(multiValued);
            }
            result.add(tmpAttribute);
        }

        return result;
    }

    @Override
    public <T> boolean authenticate(String baseDN, Class<T> entryClass, String userName, String password) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        
        // Find entries
        Filter searchFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter(CouchbaseOperationService.UID), userName);
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(searchFilter, objectClasses);
        }

        // Prepare properties types to allow build filter properly
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

        ConvertedExpression convertedExpression;
		try {
			convertedExpression = toCouchbaseFilter(searchFilter, propertiesAnnotationsMap);
		} catch (SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to convert filter %s to expression", searchFilter));
		}

		try {
            PagedResult<JsonObject> searchResult = searchImpl(toCouchbaseKey(baseDN).getKey(), getScanConsistency(convertedExpression), convertedExpression.expression(),
                    SearchScope.SUB, null, null, null, SearchReturnDataType.SEARCH, 0, 1, 1);
            if ((searchResult == null) || (searchResult.getEntriesCount() != 1)) {
                return false;
            }

            String bindDn = searchResult.getEntries().get(0).getString(CouchbaseOperationService.DN);

            return authenticate(bindDn, password);
        } catch (SearchException ex) {
            throw new AuthenticationException(String.format("Failed to find user DN: %s", userName), ex);
        } catch (Exception ex) {
            throw new AuthenticationException(String.format("Failed to authenticate user: %s", userName), ex);
        }
    }

    @Override
    public boolean authenticate(String bindDn, String password) {
        try {
            return operationService.authenticate(toCouchbaseKey(bindDn).getKey(), escapeValue(password));
        } catch (Exception ex) {
            throw new AuthenticationException(String.format("Failed to authenticate DN: %s", bindDn), ex);
        }
    }

    @Override
    public <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter) {
        return countEntries(baseDN, entryClass, filter, SearchScope.SUB);
    }

    @Override
    public <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        
        // Find entries
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

		// Prepare properties types to allow build filter properly
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

        ConvertedExpression convertedExpression;
		try {
			convertedExpression = toCouchbaseFilter(searchFilter, propertiesAnnotationsMap);
		} catch (SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to convert filter %s to expression", searchFilter));
		}

        PagedResult<JsonObject> searchResult;
        try {
            searchResult = searchImpl(toCouchbaseKey(baseDN).getKey(), getScanConsistency(convertedExpression), convertedExpression.expression(), scope, null, null,
                    null, SearchReturnDataType.COUNT, 0, 0, 0);
        } catch (Exception ex) {
            throw new EntryPersistenceException(
                    String.format("Failed to calculate the number of entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        return searchResult.getTotalEntriesCount();
    }

    private MutationSpec createModification(final Mutation type, final String attributeName, final Boolean multiValued, final Object... attributeValues) {
        String realAttributeName = attributeName;

        Object[] realValues = attributeValues;
        if (StringHelper.equals(CouchbaseOperationService.USER_PASSWORD, realAttributeName)) {
            realValues = operationService.createStoragePassword(StringHelper.toStringArray(attributeValues));
        }

        escapeValues(realValues);
        
        if ((multiValued == null) || !multiValued) {
            return new MutationSpec(type, realAttributeName, realValues[0]);
        } else {
            return new MutationSpec(type, realAttributeName, realValues);
        }
    }

    protected Sort buildSort(String sortBy, SortOrder sortOrder) {
        Sort requestedSort = null;
        if (SortOrder.DESCENDING == sortOrder) {
            requestedSort = Sort.desc(Expression.path(sortBy));
        } else if (SortOrder.ASCENDING == sortOrder) {
            requestedSort = Sort.asc(Expression.path(sortBy));
        } else {
            requestedSort = Sort.def(Expression.path(sortBy));
        }
        return requestedSort;
    }

    protected <T> Sort[] getDefaultSort(Class<T> entryClass) {
        String[] sortByProperties = getEntrySortBy(entryClass);

        if (ArrayHelper.isEmpty(sortByProperties)) {
            return null;
        }

        Sort[] sort = new Sort[sortByProperties.length];
        for (int i = 0; i < sortByProperties.length; i++) {
            sort[i] = Sort.def(Expression.path(sortByProperties[i]));
        }

        return sort;
    }

    @Override
    public List<AttributeData> exportEntry(String dn) {
        try {
            // Load entry
            ParsedKey keyWithInum = toCouchbaseKey(dn);
            JsonObject entry = operationService.lookup(keyWithInum.getKey(), null);

            List<AttributeData> result = getAttributeDataList(entry);
            if (result != null) {
                return result;
            }
            
            return null;
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry: %s", dn), ex);
        }
    }
    @Override
    public void importEntry(String dn, List<AttributeData> attributes) {
    	persist(dn, attributes);
    }

    private ConvertedExpression toCouchbaseFilter(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) throws SearchException {
        return FILTER_CONVERTER.convertToCouchbaseFilter(genericFilter, propertiesAnnotationsMap);
    }

    private ConvertedExpression toCouchbaseFilter(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, Function<? super Filter, Boolean> processor) throws SearchException {
        return FILTER_CONVERTER.convertToCouchbaseFilter(genericFilter, propertiesAnnotationsMap, processor);
    }

    private ParsedKey toCouchbaseKey(String dn) {
        return KEY_CONVERTER.convertToKey(dn);
    }

    @Override
	protected Filter addObjectClassFilter(Filter filter, String[] objectClasses) {
		if (objectClasses.length == 0) {
			return filter;
		}
		
		// In Couchbase implementation we need to use first one as entry type
		Filter searchFilter = Filter.createEqualityFilter(OBJECT_CLASS, objectClasses[0]);
		if (filter != null) {
			searchFilter = Filter.createANDFilter(Filter.createANDFilter(searchFilter), filter);
		}

		return searchFilter;
	}

    private static final class CountBatchOperation<T> extends DefaultBatchOperation<T> {

        private int countEntries = 0;

        @Override
        public void performAction(List<T> entries) {
        }

        @Override
        public boolean collectSearchResult(int size) {
            countEntries += size;
            return false;
        }

        public int getCountEntries() {
            return countEntries;
        }
    }

    @Override
    public String encodeTime(String baseDN, Date date) {
        if (date == null) {
            return null;
        }

        return jsonDateFormat.format(date);
    }

    @Override
	protected String encodeTime(Date date) {
		return encodeTime(null, date);
	}

    @Override
    public Date decodeTime(String baseDN, String date) {
        if (StringHelper.isEmpty(date)) {
            return null;
        }

        Date decodedDate;
        try {
            decodedDate = jsonDateFormat.parse(date);
        } catch (ParseException ex) {
            LOG.error("Failed to decode generalized time '{}'", date, ex);

            return null;
        }

        return decodedDate;
    }

    @Override
    public Date decodeTime(String date) {
		return decodeTime(null, date);
	}

	@Override
	public boolean hasBranchesSupport(String dn) {
		return false;
	}

    @Override
	public String getPersistenceType(String primaryKey) {
		return CouchbaseEntryManagerFactory.PERSISTANCE_TYPE;
	}

    @Override
	protected Object convertValueToJson(Object propertyValue) {
    	String jsonStringPropertyValue = (String) super.convertValueToJson(propertyValue);
    	return JsonObject.fromJson(jsonStringPropertyValue);
	}

	@Override
	protected Object convertJsonToValue(Class<?> parameterType, Object propertyValue) {
    	Object jsonStringPropertyValue = propertyValue;
    	if (propertyValue instanceof JsonObject) {
    		JsonObject jsonObject = (JsonObject) propertyValue;
    		jsonStringPropertyValue = jsonObject.toString();
    	}

    	return super.convertJsonToValue(parameterType, jsonStringPropertyValue);
	}

	private ScanConsistency getScanConsistency(ConvertedExpression convertedExpression) {
		if (convertedExpression.consistency()) {
			return ScanConsistency.REQUEST_PLUS;
		}

		return null;
	}

	private ScanConsistency getScanConsistency(String attributeName, Map<String, PropertyAnnotation> propertiesAnnotationsMap) {
		if (StringHelper.isEmpty(attributeName)) {
			return null;
		}
		
    	PropertyAnnotation propertyAnnotation = propertiesAnnotationsMap.get(attributeName);
		if ((propertyAnnotation == null) || (propertyAnnotation.getParameterType() == null)) {
			return null;
		}
		AttributeName attributeNameAnnotation = (AttributeName) ReflectHelper.getAnnotationByType(propertyAnnotation.getAnnotations(),
				AttributeName.class);
		
		if (attributeNameAnnotation.consistency()) {
			return ScanConsistency.REQUEST_PLUS;
		}

		return null;
	}

	private String escapeValue(String value) {
		// Couchbade SDK do this automatically 
//		return StringHelper.escapeJson(value);
		return value;
	}

	private String unescapeValue(String value) {
		// Couchbade SDK do this automatically 
//		return StringHelper.unescapeJson(value);
		return value;
	}

	private void escapeValues(Object[] realValues) {
		// Couchbade SDK do this automatically 
//		for (int i = 0; i < realValues.length; i++) {
//        	if (realValues[i] instanceof String) {
//        		realValues[i] = StringHelper.escapeJson(realValues[i]);
//        	}
//        }
	}

	private void unescapeValues(Object[] realValues) {
		// Couchbade SDK do this automatically 
//		for (int i = 0; i < realValues.length; i++) {
//        	if (realValues[i] instanceof String) {
//        		realValues[i] = StringHelper.unescapeJson(realValues[i]);
//        	}
//        }
	}

	public String toInternalAttribute(String attributeName) {
		return attributeName;
//		if (operationService.isDisableAttributeMapping()) {
//			return attributeName;
//		}
//
//		return KeyShortcuter.shortcut(attributeName);
	}

	public String[] toInternalAttributes(String[] attributeNames) {
		return attributeNames;
//		if (operationService.isDisableAttributeMapping() || ArrayHelper.isEmpty(attributeNames)) {
//			return attributeNames;
//		}
//		
//		String[] resultAttributeNames = new String[attributeNames.length];
//		
//		for (int i = 0; i < attributeNames.length; i++) {
//			resultAttributeNames[i] = KeyShortcuter.shortcut(attributeNames[i]);
//		}
//		
//		return resultAttributeNames;
	}

	public String fromInternalAttribute(String internalAttributeName) {
		return internalAttributeName;
//		if (operationService.isDisableAttributeMapping()) {
//			return internalAttributeName;
//		}
//
//		return KeyShortcuter.fromShortcut(internalAttributeName);
	}

	public String[] fromInternalAttributes(String[] internalAttributeNames) {
		return internalAttributeNames;
//		if (operationService.isDisableAttributeMapping() || ArrayHelper.isEmpty(internalAttributeNames)) {
//			return internalAttributeNames;
//		}
//		
//		String[] resultAttributeNames = new String[internalAttributeNames.length];
//		
//		for (int i = 0; i < internalAttributeNames.length; i++) {
//			resultAttributeNames[i] = KeyShortcuter.fromShortcut(internalAttributeNames[i]);
//		}
//		
//		return resultAttributeNames;
	}

}
