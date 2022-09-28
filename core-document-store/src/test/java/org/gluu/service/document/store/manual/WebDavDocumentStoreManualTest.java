package org.gluu.service.document.store.manual;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gluu.service.document.store.StandaloneDocumentStoreProviderFactory;
import org.gluu.service.document.store.conf.DocumentStoreConfiguration;
import org.gluu.service.document.store.conf.DocumentStoreType;
import org.gluu.service.document.store.conf.WebDavDocumentStoreConfiguration;
import org.gluu.service.document.store.conf.LocalDocumentStoreConfiguration;
import org.gluu.service.document.store.provider.DocumentStoreProvider;
import org.gluu.util.security.StringEncrypter;
import org.gluu.util.security.StringEncrypter.EncryptionException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WebDavDocumentStoreManualTest {

	public WebDavDocumentStoreManualTest() {
		
	}

	public static void main(String[] args) throws RepositoryException, IOException, EncryptionException {
		StringEncrypter se = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME, "Zqvw62DEFdhxoL4csi9hpVI4");
		DocumentStoreConfiguration dsc = new DocumentStoreConfiguration();
		dsc.setDocumentStoreType(DocumentStoreType.WEB_DAV);
		
		WebDavDocumentStoreConfiguration jca = new WebDavDocumentStoreConfiguration();
		jca.setServerUrl("http://localhost:8080/repository");
		jca.setWorkspaceName("default");
		jca.setUserId("admin");
		jca.setPassword(se.encrypt("admin"));
		jca.setConnectionTimeout(15);
		
		dsc.setWebDavConfiguration(jca);

		LocalDocumentStoreConfiguration lca = new LocalDocumentStoreConfiguration();

		dsc.setLocalConfiguration(lca);
		
		ObjectMapper om = new ObjectMapper();
		System.out.println(om.writeValueAsString(dsc));

		StandaloneDocumentStoreProviderFactory pf = new StandaloneDocumentStoreProviderFactory(se);
		DocumentStoreProvider dsp = pf.getDocumentStoreProvider(dsc);
		
		String doc1 = FileUtils.readFileToString(new File("V:/test.log"), "UTF-8");
		byte[] doc2 = FileUtils.readFileToByteArray(new File("V:/test.zip"));
/*		
		System.out.print("Has document: " + "/test2/test2/test.jmx: ");
		System.out.println(dsp.hasDocument("/test2/test2/test.jmx"));
*/
		System.out.print("Has document: " + "/test2/test3/test3.jmx: ");
		System.out.println(dsp.hasDocument("/test2/test3/test3.jmx"));

		System.out.print("Write document: " + "/test2/test3/test4/test5.jmx: ");
		System.out.println(dsp.saveDocumentStream("/test-1.jmx", new ByteArrayInputStream(doc2), List.of("oxtrust-server","Shibboleth")));
/*
		System.out.print("Has document: " + "/test2/test3/test4/test5.jmx: ");
		System.out.println(dsp.hasDocument("/test2/test3/test4/test5.jmx"));
*/
		System.out.print("Write document: " + "/test2/test3/test4/test5.jmx: ");
		System.out.println(dsp.saveDocument("/test7.jmx", doc1, StandardCharsets.UTF_8, List.of("oxtrust-server","Shibboleth")));
/*
		System.out.print("Has document: " + "/test2/test3/test4/test5.jmx: ");
		System.out.println(dsp.hasDocument("/test2/test3/test4/test5.jmx"));

		System.out.print("Read document: " + "/test2/test3/test4/test5.jmx: ");
		System.out.println(dsp.readDocument("/test2/test3/test4/test5.jmx", StandardCharsets.UTF_8));

		System.out.print("Read document: " + "/test2/test3/test4/test5.jmx: ");
		System.out.println(IOUtils.toString(dsp.readDocumentAsStream("/test2/test3/test4/test5.jmx"), StandardCharsets.UTF_8));

		System.out.print("Rename document: " + "/test2/test3/test4/test5.jmx: ");
		System.out.println(dsp.renameDocument("/test2/test3/test4/test5.jmx", "/test2/test4/test5.jmx"));

		System.out.print("Has document: " + "/test2/test3/test4/test5.jmx: ");
		System.out.println(dsp.hasDocument("/test2/test3/test4/test5.jmx"));

		System.out.print("Has document: " + "/test2/test4/test5.jmx: ");
		System.out.println(dsp.hasDocument("/test2/test4/test5.jmx"));

		System.out.print("Remove document: " + "test2/test4/test5.jmx: ");
		System.out.println(dsp.removeDocument("/test2/test4/test5.jmx"));

		System.out.print("Has document: " + "/test2/test4/test5.jmx: ");
		System.out.println(dsp.hasDocument("/test2/test4/test5.jmx"));
*/
	}

}
