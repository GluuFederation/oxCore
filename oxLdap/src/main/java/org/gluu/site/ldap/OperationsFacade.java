/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.gluu.site.ldap.exception.ConnectionException;
import org.gluu.site.ldap.exception.DuplicateEntryException;
import org.xdi.ldap.model.SortOrder;
import org.xdi.ldap.model.VirtualListViewResponse;
import org.xdi.util.ArrayHelper;

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
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.*;
import com.unboundid.ldif.LDIFChangeRecord;

/**
 * OperationsFacade is the base class that performs all the ldap operations
 * using connectionpool
 *
 * @author Pankaj
 * @author Yuriy Movchan
 */
public class OperationsFacade {

	public static final String dn = "dn";
	public static final String uid = "uid";
	public static final String success = "success";
	public static final String userPassword = "userPassword";
	public static final String objectClass = "objectClass";

	private LDAPConnectionProvider connectionProvider;
	private LDAPConnectionProvider bindConnectionProvider;

	private static final Logger log = Logger.getLogger(OperationsFacade.class);

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
		SearchRequest searchRequest;

		if (attributes == null) {
			searchRequest = new SearchRequest(dn, scope, filter);
		} else {
			searchRequest = new SearchRequest(dn, scope, filter, attributes);
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
		
		if (searchLimit > 0) {
			ASN1OctetString cookie = null;
			do {
				searchRequest.setControls(new Control[] { new SimplePagedResultsControl(searchLimit, cookie) });
				setControls(searchRequest, controls);
				searchResult = getConnectionPool().search(searchRequest);
				searchResultList.add(searchResult);
				searchResultEntries.addAll(searchResult.getSearchEntries());
				searchResultReferences.addAll(searchResult.getSearchReferences());
				cookie = null;
				try {
					SimplePagedResultsControl c = SimplePagedResultsControl.get(searchResult);
					if (c != null) {
						cookie = c.getCookie();
					}
				} catch (LDAPException ex) {
					log.error("Error while accessing cookies" + ex.getMessage());
				}
				
				if (useSizeLimit) {
					break;
				}
			} while ((cookie != null) && (cookie.getValueLength() > 0));
			SearchResult searchResultTemp = searchResultList.get(0);
			searchResult = new SearchResult(searchResultTemp.getMessageID(), searchResultTemp.getResultCode(),
					searchResultTemp.getDiagnosticMessage(), searchResultTemp.getMatchedDN(), searchResultTemp.getReferralURLs(),
					searchResultEntries, searchResultReferences, searchResultEntries.size(), searchResultReferences.size(),
					searchResultTemp.getResponseControls());
		} else {
			setControls(searchRequest, controls);
			searchResult = getConnectionPool().search(searchRequest);
		}
		return searchResult;
	}

	public SearchResult searchVirtualListView(String dn, Filter filter, SearchScope scope, int startIndex, int count, String sortBy, SortOrder sortOrder, VirtualListViewResponse vlvResponse, String... attributes) throws Exception {

		SearchRequest searchRequest;

		if (attributes == null) {
			searchRequest = new SearchRequest(dn, scope, filter);
		} else {
			searchRequest = new SearchRequest(dn, scope, filter, attributes);
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

			throw new ConnectionException("Error adding object to directory. LDAP error number " + errorCode, ex);
		}

		return false;
	}

	/**
	 * This method is used to update set of attributes for an entry
	 *
	 * @param dn
	 * @param attrs
	 * @return
	 * @throws LDAPException
	 */
	public boolean updateEntry(String dn, Collection<Attribute> attrs) throws LDAPException {
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

	public boolean updateEntry(String dn, List<Modification> modifications) throws LDAPException {
		ModifyRequest modifyRequest = new ModifyRequest(dn, modifications);
		return modifyEntry(modifyRequest);
	}

	/**
	 * Use this method to add / replace / delete attribute from entry
	 *
	 * @param modifyRequest
	 * @return true if modification is successful
	 * @throws LDAPException
	 */
	protected boolean modifyEntry(ModifyRequest modifyRequest) throws LDAPException {
		LDAPResult modifyResult = null;
		try {
			modifyResult = getConnectionPool().modify(modifyRequest);
			return ResultCode.SUCCESS.equals(modifyResult.getResultCode());
		} catch (LDAPException e) {
			log.error("Entry can't be modified" , e);
		}

		return false;
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

}
