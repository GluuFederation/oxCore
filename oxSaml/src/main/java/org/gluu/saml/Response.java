/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.saml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.xmlsec.encryption.support.ChainingEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.EncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xdi.xml.SimpleNamespaceContext;
import org.xml.sax.SAXException;

import net.shibboleth.utilities.java.support.xml.BasicParserPool;


/**
 * Loads and validates SAML response
 * 
 * @author Yuriy Movchan Date: 24/04/2014
 */
public class Response {
	private final static SimpleNamespaceContext NAMESPACES;
        
        public final static String SAML_RESPONSE_STATUS_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success"; 
        public final static String SAML_RESPONSE_STATUS_RESPONDER = "urn:oasis:names:tc:SAML:2.0:status:Responder";
        public final static String SAML_RESPONSE_STATUS_AUTHNFAILED = "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed";
        
	static {
		HashMap<String, String> preferences = new HashMap<String, String>() {
			{
				put("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
				put("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
			}
		};
		NAMESPACES = new SimpleNamespaceContext(preferences);
	}

	private Document xmlDoc;
	private SamlConfiguration samlSettings;
	
	private org.opensaml.saml.saml2.core.Response encryptedResponse;
	private Assertion assertion;

	public Response(SamlConfiguration samlSettings) throws CertificateException {
		this.samlSettings = samlSettings;
	}

	public void loadXml(String xml) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory fty = DocumentBuilderFactory.newInstance();

		fty.setNamespaceAware(true);

		// Fix XXE vulnerability
		fty.setXIncludeAware(false);
		fty.setExpandEntityReferences(false);
		fty.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		fty.setFeature("http://xml.org/sax/features/external-general-entities", false);
		fty.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

		DocumentBuilder builder = fty.newDocumentBuilder();
		ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
		xmlDoc = builder.parse(bais);
	}

	public void loadXmlFromBase64(String response) throws ParserConfigurationException, SAXException, IOException {
		Base64 base64 = new Base64();
		byte[] decodedResponse = base64.decode(response);
		String decodedS = new String(decodedResponse);
		loadXml(decodedS);
	}

	public boolean isValid() throws Exception {
		NodeList nodes = xmlDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

		if (nodes == null || nodes.getLength() == 0) {
			throw new Exception("Can't find signature in document.");
		}

		if (setIdAttributeExists()) {
			tagIdAttributes(xmlDoc);
		}

		X509Certificate cert = samlSettings.getCertificate();
		DOMValidateContext ctx = new DOMValidateContext(cert.getPublicKey(), nodes.item(0));
		XMLSignatureFactory sigF = XMLSignatureFactory.getInstance("DOM");
		XMLSignature xmlSignature = sigF.unmarshalXMLSignature(ctx);

		return xmlSignature.validate(ctx);
	}

	public boolean isAuthnFailed() throws Exception {
            XPath xPath = XPathFactory.newInstance().newXPath();

            xPath.setNamespaceContext(NAMESPACES);
            XPathExpression query = xPath.compile("/samlp:Response/samlp:Status/samlp:StatusCode");
            NodeList nodes = (NodeList) query.evaluate(xmlDoc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);

                    if (node.getAttributes().getNamedItem("Value") == null)
                        continue;

                    String statusCode = node.getAttributes().getNamedItem("Value").getNodeValue();
                    if (SAML_RESPONSE_STATUS_SUCCESS.equalsIgnoreCase(statusCode))
                        return false;
                    else if (SAML_RESPONSE_STATUS_AUTHNFAILED.equalsIgnoreCase(statusCode))
                        return true;
                    else if (SAML_RESPONSE_STATUS_RESPONDER.equalsIgnoreCase(statusCode))
                        ;// nothing?
            }
            
            return false;
        }
        
	private void tagIdAttributes(Document xmlDoc) {
		NodeList nodeList = xmlDoc.getElementsByTagName("*");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (node.getAttributes().getNamedItem("ID") != null) {
					((Element) node).setIdAttribute("ID", true);
				}
			}
		}
	}

	private boolean setIdAttributeExists() {
		for (Method method : Element.class.getDeclaredMethods()) {
			if (method.getName().equals("setIdAttribute")) {
				return true;
			}
		}
		return false;
	}

	public String getNameId() throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();

		xPath.setNamespaceContext(NAMESPACES);
		XPathExpression query = xPath.compile("/samlp:Response/saml:Assertion/saml:Subject/saml:NameID");
		return query.evaluate(xmlDoc);
	}

	public Map<String, List<String>> getAttributes() throws XPathExpressionException {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		XPath xPath = XPathFactory.newInstance().newXPath();

		xPath.setNamespaceContext(NAMESPACES);
		XPathExpression query = xPath.compile("/samlp:Response/saml:Assertion/saml:AttributeStatement/saml:Attribute");
		NodeList nodes = (NodeList) query.evaluate(xmlDoc, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);

			Node nameNode = node.getAttributes().getNamedItem("Name");
			if (nameNode == null) {
				continue;
			}

			String attributeName = nameNode.getNodeValue();
			List<String> attributeValues = new ArrayList<String>();

			NodeList nameChildNodes = node.getChildNodes();
			for (int j = 0; j < nameChildNodes.getLength(); j++) {
				Node nameChildNode = nameChildNodes.item(j);

				if (nameChildNode.getNamespaceURI().equalsIgnoreCase("urn:oasis:names:tc:SAML:2.0:assertion")
						&& nameChildNode.getLocalName().equals("AttributeValue")) {
					NodeList valueChildNodes = nameChildNode.getChildNodes();
					for (int k = 0; k < valueChildNodes.getLength(); k++) {
						Node valueChildNode = valueChildNodes.item(k);
						attributeValues.add(valueChildNode.getNodeValue());
					}
				}
			}

			result.put(attributeName, attributeValues);
		}

		return result;
	}

	public void printDocument(OutputStream out) throws IOException, TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(new DOMSource(xmlDoc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
	}

	
	public String getDecryptedNameId() {		
		return assertion.getSubject().getNameID().getValue();
	}

	public Map<String, List<String>> getDecryptedAttributes(String keystoreType, String keystoreLocation, String keystorePwd, String keyAlias, String keyPassword) throws Exception {
		Map<String, List<String>> result = new HashMap<String, List<String>>();

		try {
			File loc = new File(keystoreLocation);
			FileInputStream fis = new FileInputStream(loc);
			KeyStore ks = KeyStore.getInstance(keystoreType);
			ks.load(fis, keystorePwd.toCharArray());
			X509Certificate cert = (X509Certificate) ks.getCertificate(keyAlias);
			PrivateKey privateKey = (PrivateKey) ks.getKey(keyAlias, keyPassword.toCharArray());	
            PublicKey pubKey = cert.getPublicKey();

			EncryptedAssertion encryptedAssertion = encryptedResponse.getEncryptedAssertions().get(0);

			BasicCredential basicCredential = new BasicCredential(pubKey, privateKey);
			StaticKeyInfoCredentialResolver keyInfoCredentialResolver = new StaticKeyInfoCredentialResolver(basicCredential);
			
			List<EncryptedKeyResolver> encKeyResolvers = new ArrayList<EncryptedKeyResolver>();
			encKeyResolvers.add(new InlineEncryptedKeyResolver());
			encKeyResolvers.add(new EncryptedElementTypeEncryptedKeyResolver());
			encKeyResolvers.add(new SimpleRetrievalMethodEncryptedKeyResolver());
		    ChainingEncryptedKeyResolver keyResolver = new ChainingEncryptedKeyResolver(encKeyResolvers);
			 
		    Decrypter decrypter = new Decrypter(null, keyInfoCredentialResolver, keyResolver);
			decrypter.setRootInNewDocument(true);
			 
			assertion = decrypter.decrypt(encryptedAssertion);
			
			List<Attribute> la = assertion.getAttributeStatements().get(0).getAttributes();
			for (Attribute aa: la) {							
				String attributeValue = "";
				for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
					List<Attribute> attributes = attributeStatement.getAttributes();
					for (Attribute attribute : attributes) {
						if (aa.getName().equals(attribute.getName())) {
							attributeValue = attribute.getAttributeValues().get(0).getDOM().getTextContent();
							break;
						}
					 }
				}
				result.put(aa.getName(), Arrays.asList(attributeValue));		
			}
	
		} catch (Exception e) {
			//log.error("Error in the decryption of encrypted assertion: "+e);
			throw e;
		}

		return result;
	}
	
	public void loadEncryptedXmlFromBase64(String SAMLResponse) throws Exception {
		Base64 base64 = new Base64();
		byte[] decodedResponse = base64.decode(SAMLResponse);
		String decodedS = new String(decodedResponse);
		loadXml(decodedS);
		encryptedResponse = (org.opensaml.saml.saml2.core.Response) unmarshall(decodedS);
	}
	
    private XMLObject unmarshall(String samlResponse) throws Exception {
        BasicParserPool parser = new BasicParserPool();
        parser.setNamespaceAware(true);
        parser.initialize();

        StringReader reader = new StringReader(samlResponse);

        Document doc = parser.parse(reader);
        Element samlElement = doc.getDocumentElement();
 
        InitializationService.initialize();
        Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory().getUnmarshaller(samlElement);
        if (unmarshaller == null) {
        		//log.error("Error in unmarshall SAML response, unmarshaller is null.");
            throw new Exception("Failed to unmarshall SAML response");
        }

        return unmarshaller.unmarshall(samlElement);
    }
	

}
