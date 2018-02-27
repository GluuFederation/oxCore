/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap;

import java.io.Serializable;
import java.util.*;

import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.gluu.site.ldap.exception.ConnectionException;
import org.gluu.site.ldap.exception.DuplicateEntryException;
import org.gluu.site.ldap.exception.InvalidSimplePageControlException;
import org.gluu.site.ldap.persistence.BatchOperation;
import org.gluu.site.ldap.persistence.exception.MappingException;
import org.xdi.ldap.model.SearchScope;
import org.xdi.ldap.model.SortOrder;
import org.xdi.ldap.model.VirtualListViewResponse;
import org.xdi.util.ArrayHelper;
import org.xdi.util.Pair;
import org.xdi.util.StringHelper;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;
import com.unboundid.ldif.LDIFChangeRecord;

/**
 * OperationsFacade is the base class that performs all the ldap operations
 * using connectionpool
 *
 * @author Pankaj
 * @author Yuriy Movchan
 */
public class OperationsFacade {

	private static final Logger log = Logger.getLogger(OperationsFacade.class);

	public static final String dn = "dn";
	public static final String uid = "uid";
	public static final String success = "success";
	public static final String userPassword = "userPassword";
	public static final String objectClass = "objectClass";

	private LDAPConnectionProvider connectionProvider;
	private LDAPConnectionProvider bindConnectionProvider;

	private static Map<String, Class<?>> attributeDataTypes=new HashMap<String, Class<?>>();
	private static final Map<String, Class<?>> oidSyntaxClassMapping;

	static {
	    //Populates the mapping of syntaxes that will support comparison of attribute values. Only accounting for the most common and existing in Gluu Schema
        oidSyntaxClassMapping=new HashMap<String, Class<?>>();
        //See RFC4517, section 3.3
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.7", Boolean.class);
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.11", String.class);   //Country String
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.15", String.class);   //Directory String
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.12", String.class);   //DN
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.22", String.class);   //Facsimile
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.24", Date.class);     //Generalized Time
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.26", String.class);   //IA5 String (used in email)
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.27", Integer.class);
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.36", String.class);   //Numeric string
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.41", String.class);   //Postal address
        oidSyntaxClassMapping.put("1.3.6.1.4.1.1466.115.121.1.50", String.class);   //Telephone number
    }

	@SuppressWarnings("unused")
	private OperationsFacade() {
	}

	@Deprecated
	public OperationsFacade(LDAPConnectionProvider connectionProvider) {
		this(connectionProvider, null);
	}

	public OperationsFacade(LDAPConnectionProvider connectionProvider, LDAPConnectionProvider bindConnectionProvider) {
		this.connectionProvider = connectionProvider;
		this.bindConnectionProvider = bindConnectionProvider;
		populateAttributeDataTypesMapping(getSubschemaSubentry());
	}

	public LDAPConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	public void setConnectionProvider(LDAPConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	public LDAPConnectionProvider getBindConnectionProvider() {
		return bindConnectionProvider;
	}

	public void setBindConnectionProvider(LDAPConnectionProvider bindConnectionProvider) {
		this.bindConnectionProvider = bindConnectionProvider;
	}

	public LDAPConnectionPool getConnectionPool() {
		return connectionProvider.getConnectionPool();
	}

	public LDAPConnection getConnection() throws LDAPException {
		return connectionProvider.getConnection();
	}

	public void releaseConnection(LDAPConnection connection) {
		connectionProvider.releaseConnection(connection);
	}

	/**
	 *
	 * @param userName
	 * @param password
	 * @return
	 * @throws ConnectionException
	 * @throws ConnectionException
	 * @throws LDAPException
	 */
	public boolean authenticate(final String userName, final String password, final String baseDN) throws ConnectionException {
		try {
			return authenticateImpl(userName, password, baseDN);
		} catch (LDAPException ex) {
			throw new ConnectionException("Failed to authenticate user", ex);
		}
	}

	public boolean authenticate(final String bindDn, final String password) throws ConnectionException {
		try {
			return authenticateImpl(bindDn, password);
		} catch (LDAPException ex) {
			throw new ConnectionException("Failed to authenticate dn", ex);
		}
	}

	private boolean authenticateImpl(final String userName, final String password, final String baseDN) throws LDAPException, ConnectionException {
		return authenticateImpl(lookupDnByUid(userName, baseDN), password);
	}

	private boolean authenticateImpl(final String bindDn, final String password) throws LDAPException, ConnectionException {
		if (this.bindConnectionProvider == null) {
			return authenticateConnectionPoolImpl(bindDn, password);
		} else {
			return authenticateBindConnectionPoolImpl(bindDn, password);
		}
	}

	private boolean authenticateConnectionPoolImpl(final String bindDn, final String password) throws LDAPException, ConnectionException {
		boolean loggedIn = false;

		if (bindDn == null) {
			return loggedIn;
		}

		boolean closeConnection = false;
		LDAPConnection connection = connectionProvider.getConnection();
		try {
			closeConnection = true;
			BindResult r = connection.bind(bindDn, password);
			if (r.getResultCode() == ResultCode.SUCCESS) {
				loggedIn = true;
			}
		} finally {
			connectionProvider.releaseConnection(connection);
			// We can't use connection which binded as ordinary user
			if (closeConnection) {
				connectionProvider.closeDefunctConnection(connection);
			}
		}

		return loggedIn;
	}

	private boolean authenticateBindConnectionPoolImpl(final String bindDn, final String password) throws LDAPException, ConnectionException {
		if (bindDn == null) {
			return false;
		}

		LDAPConnection connection = bindConnectionProvider.getConnection();
		try {
			BindResult r = connection.bind(bindDn, password);
			return r.getResultCode() == ResultCode.SUCCESS;
		} finally {
			bindConnectionProvider.releaseConnection(connection);
		}
	}

	/**
	 * Looks the uid in ldap and return the DN
	 */
	protected String lookupDnByUid(String uid, String baseDN) throws LDAPSearchException {
		Filter filter = Filter.createEqualityFilter(OperationsFacade.uid, uid);
		SearchResult searchResult = search(baseDN, filter, 1, 1);
		if ((searchResult != null) && searchResult.getEntryCount() > 0) {
			return searchResult.getSearchEntries().get(0).getDN();
		}

		return null;
	}

	public SearchResult search(String dn, Filter filter, int searchLimit, int sizeLimit) throws LDAPSearchException {
		return search(dn, filter, searchLimit, sizeLimit, null, (String[]) null);
	}

	public SearchResult search(String dn, Filter filter, int searchLimit, int sizeLimit, Control[] controls, String... attributes)
			throws LDAPSearchException {
		return search(dn, filter, SearchScope.SUB, searchLimit, sizeLimit, controls, attributes);
	}

	public SearchResult search(String dn, Filter filter, SearchScope scope, int searchLimit, int sizeLimit, Control[] controls, String... attributes)
		throws LDAPSearchException {
		return search(dn, filter, scope, null, 0, searchLimit, sizeLimit, controls, attributes);
	}

	public SearchResult search(String dn, Filter filter, SearchScope scope, BatchOperation<?> batchOperation, int startIndex, int searchLimit, int sizeLimit, Control[] controls, String... attributes)
			throws LDAPSearchException {
		SearchRequest searchRequest;

		if (log.isTraceEnabled()) {
			// Find whole tree search
			if (StringHelper.equalsIgnoreCase(dn, "o=gluu")) {
				log.trace("Search in whole LDAP tree", new Exception());
			}
		}

		if (attributes == null) {
			searchRequest = new SearchRequest(dn, scope.getLdapSearchScope(), filter);
		} else {
			searchRequest = new SearchRequest(dn, scope.getLdapSearchScope(), filter, attributes);
		}

		boolean useSizeLimit = sizeLimit > 0;

		if (useSizeLimit) {
			// Use paged result to limit search
			searchLimit = sizeLimit;
		}

		SearchResult searchResult = null;
		List<SearchResult> searchResultList = new ArrayList<SearchResult>();
		List<SearchResultEntry> searchResultEntries = new ArrayList<SearchResultEntry>();
		List<SearchResultReference> searchResultReferences = new ArrayList<SearchResultReference>();

		if ((searchLimit > 0) || (startIndex > 0)) {
			if (searchLimit == 0) {
				// Default page size
				searchLimit = 100;
			}
			LDAPConnection ldapConnection = null;
			try {
				if (batchOperation != null) {
					ldapConnection = batchOperation.getLdapConnection();
				} else {
					ldapConnection = getConnectionPool().getConnection();
				}
				ASN1OctetString cookie = null;
				if (startIndex > 0) {
					try {
						cookie = scrollSimplePagedResultsControl(ldapConnection, dn, filter, scope, controls, startIndex);
					} catch (InvalidSimplePageControlException ex) {
						throw new LDAPSearchException(ex.getResultCode(), "Failed to scroll to specified startIndex", ex);
					} catch (LDAPException ex) {
						throw new LDAPSearchException(ex.getResultCode(), "Failed to scroll to specified startIndex", ex);
					}
				}

				if (batchOperation != null) {
					cookie = batchOperation.getCookie();
				}

				do {
					searchRequest.setControls(new Control[]{new SimplePagedResultsControl(searchLimit, cookie)});
					setControls(searchRequest, controls);
					searchResult = ldapConnection.search(searchRequest);
					if ((batchOperation == null) || batchOperation.collectSearchResult(searchResult)) {
						searchResultList.add(searchResult);
						searchResultEntries.addAll(searchResult.getSearchEntries());
						searchResultReferences.addAll(searchResult.getSearchReferences());
					}

					if (batchOperation != null) {
						batchOperation.processSearchResult(searchResult);
					}
					cookie = null;
					try {
						SimplePagedResultsControl c = SimplePagedResultsControl.get(searchResult);
						if (c != null) {
							cookie = c.getCookie();
							if (batchOperation != null) {
								batchOperation.setCookie(cookie);
								batchOperation.setMoreResultsToReturn(c.moreResultsToReturn());
							}
						}
					} catch (LDAPException ex) {
						log.error("Error while accessing cookies" + ex.getMessage());
					}

					if (useSizeLimit) {
						break;
					}
				} while ((cookie != null) && (cookie.getValueLength() > 0));
			} catch (LDAPException e) {
				throw new LDAPSearchException(e.getResultCode(), "Failed to scroll to specified startIndex", e);
			} finally {
				if (ldapConnection != null) {
					if (batchOperation != null) {
						batchOperation.releaseConnection();
					} else {
						getConnectionPool().releaseConnection(ldapConnection);
					}
				}
			}

			if (!searchResultList.isEmpty()) {
				SearchResult searchResultTemp = searchResultList.get(0);
				searchResult = new SearchResult(searchResultTemp.getMessageID(), searchResultTemp.getResultCode(),
						searchResultTemp.getDiagnosticMessage(), searchResultTemp.getMatchedDN(), searchResultTemp.getReferralURLs(),
						searchResultEntries, searchResultReferences, searchResultEntries.size(), searchResultReferences.size(),
						searchResultTemp.getResponseControls());
			}
		} else {
			setControls(searchRequest, controls);
			searchResult = getConnectionPool().search(searchRequest);
		}

		return searchResult;
	}

	private ASN1OctetString scrollSimplePagedResultsControl(LDAPConnection ldapConnection, String dn, Filter filter, SearchScope scope, Control[] controls, int startIndex) throws LDAPException, InvalidSimplePageControlException {
		SearchRequest searchRequest = new SearchRequest(dn, scope.getLdapSearchScope(), filter, "dn");

		int currentStartIndex = startIndex;
		ASN1OctetString cookie = null;
		do {
			int pageSize = Math.min(currentStartIndex, 100);
			searchRequest.setControls(new Control[]{new SimplePagedResultsControl(pageSize, cookie, true)});
			setControls(searchRequest, controls);
			SearchResult searchResult = ldapConnection.search(searchRequest);

			currentStartIndex -= searchResult.getEntryCount();
			try {
				SimplePagedResultsControl c = SimplePagedResultsControl.get(searchResult);
				if (c != null) {
					cookie = c.getCookie();
				}
			} catch (LDAPException ex) {
				log.error("Error while accessing cookie", ex);
				throw new InvalidSimplePageControlException(ex.getResultCode(), "Error while accessing cookie");
			}
		} while ((cookie != null) && (cookie.getValueLength() > 0) && (currentStartIndex > 0));

		return cookie;
	}

	public SearchResult searchSearchResult(String dn, Filter filter, SearchScope scope, int startIndex, int count, int searchLimit, String sortBy, SortOrder sortOrder, VirtualListViewResponse vlvResponse, String... attributes) throws Exception {

		if (StringHelper.equalsIgnoreCase(dn, "o=gluu")) {
			(new Exception()).printStackTrace();
		}

		SearchRequest searchRequest;
		if (attributes == null) {
			searchRequest = new SearchRequest(dn, scope.getLdapSearchScope(), filter);
		} else {
			searchRequest = new SearchRequest(dn, scope.getLdapSearchScope(), filter, attributes);
		}

		List<SearchResult> searchResultList = new ArrayList<SearchResult>();
		List<SearchResultEntry> searchResultEntries = new ArrayList<SearchResultEntry>();
		List<SearchResultReference> searchResultReferences = new ArrayList<SearchResultReference>();

		searchRequest.setControls(new SimplePagedResultsControl(searchLimit));
		SearchResult searchResult = getConnectionPool().search(searchRequest);
		List<SearchResultEntry> resultSearchResultEntries = searchResult.getSearchEntries();
		int totalResults = resultSearchResultEntries.size();

		if (StringUtils.isNotEmpty(sortBy)) {
            boolean ascending = sortOrder == null || sortOrder.equals(SortOrder.ASCENDING);

            resultSearchResultEntries = sortListByAttributes(resultSearchResultEntries, SearchResultEntry.class, false, ascending, sortBy);
        }

		List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();

		if (startIndex <= totalResults) {

			int diff = (totalResults - startIndex);
			if (diff <= count) {
				count = (diff + 1) >= count ? count : (diff + 1);
			}

			int startZeroIndex = startIndex - 1;
			searchResultEntryList = resultSearchResultEntries.subList(startZeroIndex, startZeroIndex + count);
		}

		searchResultList.add(searchResult);
		searchResultEntries.addAll(searchResultEntryList);
		searchResultReferences.addAll(searchResult.getSearchReferences());

		SearchResult searchResultTemp = searchResultList.get(0);
		searchResult = new SearchResult(searchResultTemp.getMessageID(), searchResultTemp.getResultCode(), searchResultTemp.getDiagnosticMessage(),
				                        searchResultTemp.getMatchedDN(), searchResultTemp.getReferralURLs(), searchResultEntries, searchResultReferences,
				                        searchResultEntries.size(), searchResultReferences.size(), searchResultTemp.getResponseControls());

		// Get results info
		vlvResponse.setItemsPerPage(count);
		vlvResponse.setTotalResults(totalResults);
		vlvResponse.setStartIndex(startIndex);

		return searchResult;
	}

	public SearchResult searchVirtualListView(String dn, Filter filter, SearchScope scope, int startIndex, int count, String sortBy, SortOrder sortOrder, VirtualListViewResponse vlvResponse, String... attributes) throws Exception {

		if (StringHelper.equalsIgnoreCase(dn, "o=gluu")) {
			(new Exception()).printStackTrace();
		}

		SearchRequest searchRequest;

		if (attributes == null) {
			searchRequest = new SearchRequest(dn, scope.getLdapSearchScope(), filter);
		} else {
			searchRequest = new SearchRequest(dn, scope.getLdapSearchScope(), filter, attributes);
		}

		// startIndex and count should be "cleansed" before arriving here
		int targetOffset = startIndex;
		int beforeCount = 0;
		int afterCount = (count > 0) ? (count - 1) : 0;
		int contentCount = 0;

		boolean reverseOrder = false;
		if (sortOrder != null) {
			reverseOrder = sortOrder.equals(SortOrder.DESCENDING) ? true : false;
		}

		// Note that the VLV control always requires the server-side sort control.
		searchRequest.setControls(
			new ServerSideSortRequestControl(new SortKey(sortBy, reverseOrder)),
			new VirtualListViewRequestControl(targetOffset, beforeCount, afterCount, contentCount, null)
		);

		SearchResult searchResult = getConnectionPool().search(searchRequest);

		/*
		for (SearchResultEntry searchResultEntry : searchResult.getSearchEntries()) {
			log.info("##### searchResultEntry = " + searchResultEntry.toString());
		}
		*/

		// LDAPTestUtils.assertHasControl(searchResult, VirtualListViewResponseControl.VIRTUAL_LIST_VIEW_RESPONSE_OID);

		VirtualListViewResponseControl vlvResponseControl = VirtualListViewResponseControl.get(searchResult);

		// Get results info
		vlvResponse.setItemsPerPage(searchResult.getEntryCount());
		vlvResponse.setTotalResults(vlvResponseControl.getContentCount());
		vlvResponse.setStartIndex(vlvResponseControl.getTargetPosition());

		return searchResult;
	}

	private void setControls(SearchRequest searchRequest, Control... controls) {
		if (!ArrayHelper.isEmpty(controls)) {
			Control[] newControls;
			if (ArrayHelper.isEmpty(searchRequest.getControls())) {
				newControls = controls;
			} else {
				newControls = ArrayHelper.arrayMerge(searchRequest.getControls(), controls);
			}

			searchRequest.setControls(newControls);
		}
	}

	/**
	 * Lookup entry in the directory
	 *
	 * @param dn
	 * @return SearchResultEntry
	 * @throws ConnectionException
	 */
	public SearchResultEntry lookup(String dn) throws ConnectionException {
		return lookup(dn, (String[]) null);
	}

	/**
	 * Lookup entry in the directory
	 *
	 * @param dn
	 * @param attributes
	 * @return SearchResultEntry
	 * @throws ConnectionException
	 */
	public SearchResultEntry lookup(String dn, String... attributes) throws ConnectionException {
		if (StringHelper.equalsIgnoreCase(dn, "o=gluu")) {
			(new Exception()).printStackTrace();
		}

		try {
			if (attributes == null) {
				return getConnectionPool().getEntry(dn);
			} else {
				return getConnectionPool().getEntry(dn, attributes);
			}
		} catch (Exception ex) {
			throw new ConnectionException("Failed to lookup entry", ex);
		}
	}

	/**
	 * Use this method to add new entry
	 *
	 * @param dn
	 *            for entry
	 * @param atts
	 *            attributes for entry
	 * @return true if successfully added
	 * @throws DuplicateEntryException
	 * @throws ConnectionException
	 * @throws DuplicateEntryException
	 * @throws ConnectionException
	 * @throws LDAPException
	 */
	public boolean addEntry(String dn, Collection<Attribute> atts) throws DuplicateEntryException, ConnectionException {
		try {
			LDAPResult result = getConnectionPool().add(dn, atts);
			if (result.getResultCode().getName().equalsIgnoreCase(OperationsFacade.success))
				return true;
		} catch (final LDAPException ex) {
			int errorCode = ex.getResultCode().intValue();
			if (errorCode == ResultCode.ENTRY_ALREADY_EXISTS_INT_VALUE) {
				throw new DuplicateEntryException();
			}
			if (errorCode == ResultCode.INSUFFICIENT_ACCESS_RIGHTS_INT_VALUE) {
				throw new ConnectionException("LDAP config error: insufficient access rights.", ex);
			}
			if (errorCode == ResultCode.TIME_LIMIT_EXCEEDED_INT_VALUE) {
				throw new ConnectionException("LDAP Error: time limit exceeded", ex);
			}
			if (errorCode == ResultCode.OBJECT_CLASS_VIOLATION_INT_VALUE) {
				throw new ConnectionException("LDAP config error: schema violation contact LDAP admin.", ex);
			}

			throw new ConnectionException("Error adding entry to directory. LDAP error number " + errorCode, ex);
		}

		return false;
	}

	/**
	 * This method is used to update set of attributes for an entry
	 *
	 * @param dn
	 * @param attrs
	 * @return
	 * @throws ConnectionException 
	 * @throws DuplicateEntryException 
	 */
	public boolean updateEntry(String dn, Collection<Attribute> attrs) throws DuplicateEntryException, ConnectionException {
		List<Modification> mods = new ArrayList<Modification>();

		for (Attribute attribute : attrs) {

			if (attribute.getName().equalsIgnoreCase(OperationsFacade.objectClass)
					|| attribute.getName().equalsIgnoreCase(OperationsFacade.dn)
					|| attribute.getName().equalsIgnoreCase(OperationsFacade.userPassword)) {
				continue;
			}

			else {
				if (attribute.getName() != null && attribute.getValue() != null)
					mods.add(new Modification(ModificationType.REPLACE, attribute.getName(), attribute.getValue()));
			}
		}

		return updateEntry(dn, mods);
	}

	public boolean updateEntry(String dn, List<Modification> modifications) throws DuplicateEntryException, ConnectionException {
		ModifyRequest modifyRequest = new ModifyRequest(dn, modifications);
		return modifyEntry(modifyRequest);
	}

	/**
	 * Use this method to add / replace / delete attribute from entry
	 *
	 * @param modifyRequest
	 * @return true if modification is successful
	 * @throws DuplicateEntryException 
	 * @throws ConnectionException 
	 */
	protected boolean modifyEntry(ModifyRequest modifyRequest) throws DuplicateEntryException, ConnectionException {
		LDAPResult modifyResult = null;
		try {
			modifyResult = getConnectionPool().modify(modifyRequest);
			return ResultCode.SUCCESS.equals(modifyResult.getResultCode());
		} catch (final LDAPException ex) {
			int errorCode = ex.getResultCode().intValue();
			if (errorCode == ResultCode.INSUFFICIENT_ACCESS_RIGHTS_INT_VALUE) {
				throw new ConnectionException("LDAP config error: insufficient access rights.", ex);
			}
			if (errorCode == ResultCode.TIME_LIMIT_EXCEEDED_INT_VALUE) {
				throw new ConnectionException("LDAP Error: time limit exceeded", ex);
			}
			if (errorCode == ResultCode.OBJECT_CLASS_VIOLATION_INT_VALUE) {
				throw new ConnectionException("LDAP config error: schema violation contact LDAP admin.", ex);
			}

			throw new ConnectionException("Error updating entry in directory. LDAP error number " + errorCode, ex);
		}
	}

	/**
	 * Delete entry from the directory
	 *
	 * @param dn
	 * @throws ConnectionException
	 */
	public void delete(String dn) throws ConnectionException {
		try {
			getConnectionPool().delete(dn);
		} catch (Exception ex) {
			throw new ConnectionException("Failed to delete entry", ex);
		}
	}

    /**
   	 * Delete entry from the directory
   	 *
   	 * @param dn
   	 * @throws ConnectionException
   	 */
    public void deleteWithSubtree(String dn) throws ConnectionException {
        try {
            final DeleteRequest deleteRequest = new DeleteRequest(dn);
            deleteRequest.addControl(new SubtreeDeleteRequestControl());
            getConnectionPool().delete(deleteRequest);
        } catch (Exception ex) {
            throw new ConnectionException("Failed to delete entry", ex);
        }
    }

	public boolean processChange(LDIFChangeRecord ldifRecord) throws LDAPException {
		LDAPConnection connection = getConnection();
		try {
			LDAPResult ldapResult = ldifRecord.processChange(connection);

			return ResultCode.SUCCESS.equals(ldapResult.getResultCode());
		} finally {
			releaseConnection(connection);
		}
	}

	public int getSupportedLDAPVersion() {
		return this.connectionProvider.getSupportedLDAPVersion();
	}

	public String getSubschemaSubentry() {
		return this.connectionProvider.getSubschemaSubentry();
	}

	public boolean destroy() {
		boolean result = true;

		try {
        	connectionProvider.closeConnectionPool();
        } catch (Exception ex) {
        	log.error("Failed to close connection pool correctly");
        	result = false;
        }

		if (bindConnectionProvider != null) {
	        try {
	        	bindConnectionProvider.closeConnectionPool();
	        } catch (Exception ex) {
	        	log.error("Failed to close bind connection pool correctly");
	        	result = false;
	        }
		}

		return result;
	}

	public boolean isBinaryAttribute(String attributeName) {
		return this.connectionProvider.isBinaryAttribute(attributeName);
	}

	public boolean isCertificateAttribute(String attributeName) {
		return this.connectionProvider.isCertificateAttribute(attributeName);
	}

	public String getCertificateAttributeName(String attributeName) {
		return this.connectionProvider.getCertificateAttributeName(attributeName);
	}

	public <T> List<T> sortListByAttributes(List<T> searchResultEntries, Class<T> cls, boolean caseSensitive, boolean ascending, String... sortByAttributes) {

		// Check input parameters
		if (searchResultEntries == null) {
			throw new MappingException("Entries list to sort is null");
		}

		if (searchResultEntries.size() == 0) {
			return searchResultEntries;
		}

		SearchResultEntryComparator<T> comparator = new SearchResultEntryComparator<T>(sortByAttributes, caseSensitive, ascending);

		//The following line does not work because of type erasure
		//T array[]=(T[])searchResultEntries.toArray();

        //Converting the list to an array gets rid of unmodifiable list problem, see issue #68
        T dummyArr[]=(T[]) java.lang.reflect.Array.newInstance(cls, 0);
        T array[]=searchResultEntries.toArray(dummyArr);
		Arrays.sort(array, comparator);
		return Arrays.asList(array);

	}

	private void populateAttributeDataTypesMapping(String schemaEntryDn) {

        try {
            if (attributeDataTypes.size()==0){
                //schemaEntryDn="ou=schema";
                SearchResultEntry entry = lookup(schemaEntryDn, "attributeTypes");
                Attribute attrAttributeTypes = entry.getAttribute("attributeTypes");

                Map<String, Pair<String, String>> tmpMap =new HashMap<String, Pair<String, String>>();

                for (String strAttributeType : attrAttributeTypes.getValues()){
                    AttributeTypeDefinition attrTypeDef=new AttributeTypeDefinition(strAttributeType);
                    String names[]=attrTypeDef.getNames();

                    if (names!=null){
                        for (String name : names)
                            tmpMap.put(name, new Pair<String, String>(attrTypeDef.getBaseSyntaxOID(), attrTypeDef.getSuperiorType()));
                    }
                }

                //Fill missing values
                for (String name : tmpMap.keySet()) {
                    Pair<String, String> currPair=tmpMap.get(name);
                    String sup=currPair.getSecond();

                    if (currPair.getFirst()==null && sup!=null) {     //No OID syntax?
                        //Try to lookup superior type
                        Pair<String, String> pair = tmpMap.get(sup);
                        if (pair != null)
                            currPair.setFirst(pair.getFirst());
                    }
                }

                //Populate map of attribute names vs. Java classes
                for (String name : tmpMap.keySet()) {
                    String syntaxOID=tmpMap.get(name).getFirst();

                    if (syntaxOID!=null){
                        Class<?> cls=oidSyntaxClassMapping.get(syntaxOID);
                        if (cls!=null) {
                            attributeDataTypes.put(name, cls);
                        }
                    }
                }
            }
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
        }

    }

	private static final class SearchResultEntryComparator<T> implements Comparator<T>, Serializable {

		private static final long serialVersionUID = 574848841116711467L;
		private String[] sortByAttributes;
		private boolean caseSensitive;
		private boolean ascending;

		private SearchResultEntryComparator(String[] sortByAttributes, boolean caseSensitive, boolean ascending) {
			this.sortByAttributes = sortByAttributes;
			this.caseSensitive = caseSensitive;
			this.ascending=ascending;
		}

		public int compare(T entry1, T entry2) {

		    int result=0;

            if (entry1 == null){
                if (entry2 == null)
                    result=0;
                else
                    result=-1;
            }
            else{
                if (entry2 == null)
                    result=1;
                else {
                    for (String currSortByAttribute : sortByAttributes) {
                        result = compare(entry1, entry2, currSortByAttribute);
                        if (result != 0) {
                            break;
                        }
                    }
                }
            }

            if (!ascending)
                result*=-1;

			return result;

		}

		//This comparison assumes a default sort order of "ascending"
		public int compare(T entry1, T entry2, String attributeName) {

            int result=0;
            try{

                if (entry1 instanceof SearchResultEntry){

                    SearchResultEntry resultEntry1=(SearchResultEntry) entry1;
                    SearchResultEntry resultEntry2=(SearchResultEntry) entry2;

                    //Obtain a string representation first and do nulls treatments
                    String value1 = resultEntry1.getAttributeValue(attributeName);
                    String value2 = resultEntry2.getAttributeValue(attributeName);

                    if (value1 == null){
                        if (value2 == null)
                            result=0;
                        else
                            result=-1;
                    }
                    else {
                        if (value2 == null)
                            result=1;
                        else {
                            Class<?> cls=attributeDataTypes.get(attributeName);

                            if (cls!=null){
                                if (cls.equals(String.class)){
                                    if (caseSensitive)
                                        result=value1.compareTo(value2);
                                    else
                                        result=value1.toLowerCase().compareTo(value2.toLowerCase());
                                }
                                else
                                if (cls.equals(Integer.class))
                                    result=resultEntry1.getAttributeValueAsInteger(attributeName).compareTo(resultEntry2.getAttributeValueAsInteger(attributeName));
                                else
                                if (cls.equals(Boolean.class))
                                    result=resultEntry1.getAttributeValueAsBoolean(attributeName).compareTo(resultEntry2.getAttributeValueAsBoolean(attributeName));
                                else
                                if (cls.equals(Date.class))
                                    result=resultEntry1.getAttributeValueAsDate(attributeName).compareTo(resultEntry2.getAttributeValueAsDate(attributeName));
                            }
                        }
                    }
                }
            }
            catch (Exception e){
                log.error("Error occurred when comparing entries with SearchResultEntryComparator");
                log.error(e.getMessage(), e);
            }
			return result;

		}

	}

}
