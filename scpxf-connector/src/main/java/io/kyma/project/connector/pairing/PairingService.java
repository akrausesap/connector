package io.kyma.project.connector.pairing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.kyma.project.connector.connection.model.ConnectionModel;
import io.kyma.project.connector.exception.ApplicationConnectorException;
import io.kyma.project.connector.exception.RestTemplateCustomizerException;
import io.kyma.project.connector.util.CertificateService;
import io.kyma.project.connector.util.CertificateService.CsrResult;
import io.kyma.project.connector.util.ClientCertRestTemplateBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
* Service that "pairs" the client with Kyma / Extension Factory. It supports the following steps:
* * Initial Connect
* * Certificate Renewal
* * Get Info (Needs to be periodically invoked)
* 
* All methods return a fresh Connection Model.
* 
* @author Andreas Krause
* @see ConnectionModel
*/
@Service
public class PairingService {
	

	
	private RestTemplate pairingTemplate;
	
	private CertificateService certService;
	
	private ClientCertRestTemplateBuilder restTemplateBuilder;

	/**
	 * Sets the {@link ClientCertRestTemplateBuilder} to be used by this object
	 * 
	 * @param restTemplateBuilder {@link ClientCertRestTemplateBuilder} to be used by this Object 
	 */	
	@Autowired
	public void setClientCertRestTemplateBuilder(ClientCertRestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}

	/**
	 * Sets the {@link CertificateService} to be used by this object
	 * 
	 * @param certService {@link CertificateService} to be used by this Object 
	 */	
	@Autowired
	public void setCertService(CertificateService certService) {
		this.certService = certService;
	}

	/**
	 * Sets the {@link RestTemplate} to be used by this object for the initila pairing step (no 2-way-ssl)
	 * 
	 * @param pairingTemplate {@link RestTemplate} to be used by this Object 
	 */
	@Autowired
	@Qualifier("PairingTemplate")
	public void setPairingTemplate(RestTemplate pairingTemplate) {
		this.pairingTemplate = pairingTemplate;
	}
	
	private Encoder base64Encoder = Base64.getEncoder();
	private Decoder base64Decoder = Base64.getDecoder();
	
	
	private ConnectInfo getConnectInfo(URI connectUri) {
		
		try {
			ResponseEntity<ConnectInfo> response = 
					pairingTemplate.getForEntity(connectUri, ConnectInfo.class);
			
			if (response.getStatusCode() != HttpStatus.OK) {
				throw new ApplicationConnectorException(
						String.format("Error Response Received, code: %d (%s)", 
								response.getStatusCode().value(), 
								response.getStatusCode().getReasonPhrase()));
			}
			
			return response.getBody();
		} catch (RestClientException e) {
			throw new ApplicationConnectorException(e.getMessage(),e);
		}
	}
	
	private KeyStore getCertificateInternal(RestTemplate restTemplate, char[] keystorePassword, URI csrUrl, 
			byte[] csr, KeyPair keyPair) {
		
		String encodedCsr = String.format("-----BEGIN CERTIFICATE REQUEST-----\n%s"
				+ "\n-----END CERTIFICATE REQUEST-----", base64Encoder.encodeToString(csr));
				
		String doubleEncodedCsr = base64Encoder.encodeToString(encodedCsr.getBytes());

		CsrRequest request = new CsrRequest(doubleEncodedCsr);
		try {
			ResponseEntity<CsrResponse> response = restTemplate.postForEntity(csrUrl, request, CsrResponse.class);

			if (response.getStatusCode() != HttpStatus.CREATED) {
				throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
						response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
			}
			
			KeyStore ks = KeyStore.getInstance("JKS");
			
			
			Certificate[] certificateChain = new X509Certificate[2];
			
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			
			certificateChain[0] = 
					cf.generateCertificate(
						new ByteArrayInputStream(
								base64Decoder.decode(
												response.getBody().getClientCrt())));
			
			certificateChain[1] = 
					cf.generateCertificate(
						new ByteArrayInputStream(
								base64Decoder.decode(
												response.getBody().getCaCrt())));

			ks.load(null, keystorePassword);
			
			ks.setKeyEntry("extension-factory-key", 
					keyPair.getPrivate(), 
					keystorePassword, 
					certificateChain);
			
			return ks;
			
		} catch (RestClientException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		} catch (KeyStoreException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		} catch (CertificateException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		} catch (IOException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
		

	}
	
	
	/**
	 * Refreshes the current certificate and private key used to communicate with Kyma / Extension Factory 
	 * and returns a new object {@link ConnectionModel} with refreshed key store and password.
	 * 
	 * @param currentConnectionModel model containing all details for the current connection 
	 * @param newKeyStorePassword password to be used for the refreshed keystore
	 * @return {@link ConnectionModel} that contains updated connection details with refreshed keystore
	 * @throws ApplicationConnectorException if anything fails
	 * @throws RestTemplateCustomizerException if anything fails with acquiring the {@link RestTemplate}
	 */
	public ConnectionModel renewCertificate(ConnectionModel currentConnectionModel,
			char[] newKeyStorePassword) {
		
		ConnectionModel result = getInfo(currentConnectionModel);
		
		CsrResult csr = certService.createCSR(currentConnectionModel.getCertificateSubject(), 
				currentConnectionModel.getCertificateAlgorithm());
		
		RestTemplate restTemplate = 
				restTemplateBuilder
				.applicationConnectorRestTemplate(currentConnectionModel.getSslKey(), 
						currentConnectionModel.getKeystorePass());	
			
		KeyStore newKey =  getCertificateInternal(	restTemplate, 
										newKeyStorePassword,
										currentConnectionModel.getRenewCertUrl(), 
										csr.getCsr(), 
										csr.getKeypair());
		
		result.setKeystorePass(newKeyStorePassword);
		result.setSslKey(newKey);
		
		return result;
	}
	
	/**
	 * Refreshes the current connection data to Kyma / Extension Factory 
	 * and returns an object {@link ConnectionModel} with all needed details.
	 * 
	 * @param currentConnectionModel model containing all details for the current connection 
	 * @return {@link ConnectionModel} that contains updated connection details
	 * @throws ApplicationConnectorException if anything fails
	 * @throws RestTemplateCustomizerException if anything fails with acquiring the {@link RestTemplate}
	 */
	public ConnectionModel getInfo(ConnectionModel currentConnectionModel) {
		return getInfo(currentConnectionModel.getInfoUrl(), 
				currentConnectionModel.getKeystorePass(), 
				currentConnectionModel.getSslKey(),
				currentConnectionModel.getCertificateAlgorithm(),
				currentConnectionModel.getCertificateSubject());
	}
	
	private ConnectionModel getInfo(URI infoUrl, char[] keystorePassword, KeyStore keyStore,
			String certificateAlgorithm, String certificateSubject) {
		
		RestTemplate restTemplate = 
				restTemplateBuilder.applicationConnectorRestTemplate(keyStore, keystorePassword);
		try {
			ResponseEntity<InfoResponse> response = restTemplate.getForEntity(infoUrl, InfoResponse.class);

			if (response.getStatusCode() != HttpStatus.OK) {
				throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
						response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
			}

			ConnectionModel result = new ConnectionModel();

			result.setApplicationName(response.getBody().getClientIdentity().getApplication());
			result.setInfoUrl(infoUrl);
			result.setMetadataUrl(response.getBody().getUrls().getMetadataUrl());
			result.setRenewCertUrl(response.getBody().getUrls().getRenewCertUrl());
			result.setRevocationCertUrl(response.getBody().getUrls().getRevocationCertUrl());
			result.setEventsInfoUrl(response.getBody().getUrls().getEventsInfoUrl());
			result.setKeystorePass(keystorePassword);
			result.setSslKey(keyStore);
			result.setCertificateAlgorithm(certificateAlgorithm);
			result.setCertificateSubject(certificateSubject);

			if (response.getBody().getUrls().getEventsUrl() != null)
				result.setEventsURL(response.getBody().getUrls().getEventsUrl());

			return result;
		} catch (RestClientException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}
	
	/**
	 * Establishes the initial connection to Kyma / Extension Factory 
	 * and returns an object {@link ConnectionModel} with all needed details.
	 * 
	 * @param keystorePassword password to be provided for the keystore
	 * @param connectUri with valid one time token from Connector Services
	 * @return {@link ConnectionModel} that contains all info related to the connection
	 * @throws ApplicationConnectorException if anything fails
	 */
	public ConnectionModel executeInitialPairing(URI connectUri, char[] keystorePassword) {
		
		ConnectInfo connectInfo = getConnectInfo(connectUri);
		
		CsrResult csr = certService.createCSR(connectInfo.getCertificate().getSubject(), 
				connectInfo.getCertificate().getKeyAlgorithm());
		
		KeyStore keyStore =  getCertificateInternal(pairingTemplate, keystorePassword, connectInfo.getCsrUrl(), 
				csr.getCsr(), csr.getKeypair());
		
		
		
		return getInfo(connectInfo.getApi().getInfoUrl(), keystorePassword, keyStore, 
				connectInfo.getCertificate().getKeyAlgorithm(),
				connectInfo.getCertificate().getSubject());
	}
	
	@Data
	private static class ConnectInfo {
		
		private URI csrUrl;
		private Api api;
		private CertificateSpecification certificate;
		
		
	}
	
	@Data
	private static class Api {
		
		private URI metadataUrl;
		private URI certificatesUrl;
		private URI infoUrl;
		
		private CertificateSpecification certificate;
		
		
	}
	
	@Data
	private static class CertificateSpecification {
		
		private String subject;
		private String extensions;
		
		@JsonProperty("key-algorithm")
		private String keyAlgorithm;
		
	}
	
	@Data
	@AllArgsConstructor
	private static class CsrRequest {
		private String csr;
	}
	
	@Data
	@AllArgsConstructor
	private static class CsrResponse {
		private String crt;
		private String clientCrt;
		private String caCrt;
	}
	
	@Data
	private static class InfoResponse {
		
		private ClientIdentity clientIdentity;
		private Urls urls;
	}
	
	@Data
	private static class ClientIdentity {
		private String application;
	}
	
	@Data
	private static class Urls {
		
		private URI eventsUrl;
		private URI metadataUrl;
		private URI renewCertUrl;
		private URI revocationCertUrl;
		private URI eventsInfoUrl;
		
	}

}
