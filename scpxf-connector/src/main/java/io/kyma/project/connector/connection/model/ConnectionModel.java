package io.kyma.project.connector.connection.model;


import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

import org.apache.commons.codec.digest.DigestUtils;

import io.kyma.project.connector.exception.ApplicationConnectorException;
import lombok.Data;


/**
 * Model Object that holds all information needed to establish a connection
 * to Kyma/Extension Factory.
 * 
 * 
 * @author Andreas Krause
 */
@Data
public class ConnectionModel {
	
	private String applicationName;
	private URI metadataUrl;
	private URI renewCertUrl;
	private URI revocationCertUrl;
	private URI infoUrl;
	private URI eventsInfoUrl;
	private URI eventsURL;
	
	private String certificateSubject;
	private String certificateAlgorithm;
	
	
	private KeyStore sslKey;
	private char[] keystorePass;
	
	
	/**
	 * Returns the expiry date of the application certificate contained in the keystore 
	 * 
	 * @return {@link Date} NotAfter date of the certificate or null if no alias is found
	 * @throws ApplicationConnectorException if anything fails 
	 */
	public Date getCertificateExpirationDate() {
		try {
			Enumeration<String> aliases = sslKey.aliases();
			
			if (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate cert = (X509Certificate) sslKey.getCertificate(alias);
				
				return cert.getNotAfter();
			} else {
				return null;
			}
			
			
		} catch (KeyStoreException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}
	
	/**
	 * Returns the fingerprint date of the application certificate contained in the keystore 
	 * 
	 * @return fingerprint of the certificate
	 * @throws ApplicationConnectorException if anything fails 
	 */
	public String getCertificateFingerprint() {
		try {
			Enumeration<String> aliases = sslKey.aliases();
			
			if (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate cert = (X509Certificate) sslKey.getCertificate(alias);
				
				return DigestUtils.sha1Hex(cert.getEncoded());
			} else {
				return null;
			}
			
			
		} catch (KeyStoreException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		} catch (CertificateEncodingException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}
	
}
