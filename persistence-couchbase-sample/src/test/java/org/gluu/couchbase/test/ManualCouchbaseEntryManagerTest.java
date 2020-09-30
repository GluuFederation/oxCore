package org.gluu.couchbase.test;

import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.subdoc.SubdocOptionsBuilder;

import org.gluu.couchbase.model.SimpleClient;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManagerFactory;
import org.gluu.persist.exception.operation.SearchException;
import org.gluu.util.Pair;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ManualCouchbaseEntryManagerTest {

    @Test(enabled = false) // manual
    public void sample() throws IOException {
        CouchbaseEntryManager manager = createCouchbaseEntryManager();

        try {
            List<SimpleClient> attributeList = manager.findEntries("o=gluu", SimpleClient.class, null);
            System.out.println(attributeList);
        } finally {
            manager.destroy();
        }
    }

    @Test(enabled = false) // manual
    public void sampleSessionId() throws IOException, SearchException {
        CouchbaseEntryManager manager = createCouchbaseEntryManager();

        try {
            SessionId sessionId = createSessionId();
            manager.persist(sessionId);

            final String key = "sessions_" + sessionId.getId();
            System.out.println("Key: " + key + ", ttl:" + sessionId.getTtl());

            final JsonDocument lookup = manager.getOperationService().getConnectionProvider().getBucketMapping("sessions").getBucket().get(key);
            System.out.println("expiry: " + lookup.expiry());

            updateSession(sessionId);
            manager.merge(sessionId);

            final JsonDocument lookup2 = manager.getOperationService().getConnectionProvider().getBucketMapping("sessions").getBucket().get(key);
            System.out.println("expiry after update: " + lookup2.expiry());

        } finally {
            manager.destroy();
        }
    }

    @Test(enabled = false) // manual
    public void replaceSessionIdByPersist() throws IOException {
        CouchbaseEntryManager manager = createCouchbaseEntryManager();

        try {
            SessionId sessionId = createSessionId();
            sessionId.setJwt("jwt1");
            manager.persist(sessionId);

            final SessionId fromPersistence1 = manager.find(SessionId.class, sessionId.getDn());
            System.out.println(fromPersistence1.getJwt());

            sessionId.setJwt("jwt2");
            manager.persist(sessionId);

            final SessionId fromPersistence2 = manager.find(SessionId.class, sessionId.getDn());
            System.out.println(fromPersistence2.getJwt());

        } finally {
            manager.destroy();
        }
    }

    private SessionId createSessionId() {
        SessionId sessionId = new SessionId();
        sessionId.setId(UUID.randomUUID().toString());
        sessionId.setDn(String.format("oxId=%s,%s", sessionId.getId(), "ou=sessions,o=gluu"));
        sessionId.setCreationDate(new Date());

        updateSession(sessionId);
        return sessionId;
    }

    private void updateSession(SessionId sessionId) {
        final Pair<Date, Integer> expiration = expirationDate(sessionId.getCreationDate());
        sessionId.setLastUsedAt(new Date());
        sessionId.setExpirationDate(expiration.getFirst());
        sessionId.setTtl(expiration.getSecond());
    }

    private Pair<Date, Integer> expirationDate(Date creationDate) {
        int expirationInSeconds = 120;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.SECOND, expirationInSeconds);
        return new Pair<>(calendar.getTime(), expirationInSeconds);
    }

    // MODIFY ACCORDING TO YOUR SERVER
    public static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.put("couchbase.auth.userPassword", "1234.Gluu");

        try (InputStream is = ManualCouchbaseEntryManagerTest.class.getResourceAsStream("cb-bench-backend.gluu.org.properties")) {
            properties.load(is);
            return properties;
        }
    }

    public static CouchbaseEntryManager createCouchbaseEntryManager() throws IOException {
        CouchbaseEntryManagerFactory couchbaseEntryManagerFactory = new CouchbaseEntryManagerFactory();
        couchbaseEntryManagerFactory.create();

        CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerFactory.createEntryManager(loadProperties());
        System.out.println("Created CouchbaseEntryManager: " + couchbaseEntryManager);

        return couchbaseEntryManager;
    }
}
