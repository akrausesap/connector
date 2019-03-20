package com.sap.fsm.applicationconnector.pairing;

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
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.fsm.applicationconnector.ConnectionModel;
import com.sap.fsm.applicationconnector.util.CertificateService;
import com.sap.fsm.applicationconnector.util.ClientCertRestTemplateBuilder;
import com.sap.fsm.applicationconnector.util.CertificateService.CsrResult;
import com.sap.fsm.exception.ApplicationConnectorException;

import lombok.AllArgsConstructor;
import lombok.Data;


@Service
//@RequestScope
public class PairingService {
	

	
	private RestTemplate pairingTemplate;
	
	private CertificateService certService;
	
	private ClientCertRestTemplateBuilder restTemplateBuilder;

	
	@Autowired
	public void setRestTemplateBuilder(ClientCertRestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}


	@Autowired
	public void setCertService(CertificateService certService) {
		this.certService = certService;
	}
	
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
	
	public ConnectionModel getInfo(ConnectionModel currentConnectionModel) {
		return getInfo(currentConnectionModel.getInfoUrl(), 
				currentConnectionModel.getKeystorePass(), 
				currentConnectionModel.getSslKey(),
				currentConnectionModel.getCertificateAlgorithm(),
				currentConnectionModel.getCertificateSubject());
	}
	
	private ConnectionModel getInfo(URI infoUrl, char[] keystorePassword, KeyStore keyStore,
			String certificateAlgorithm, String certificateSubject) {
		
		RestTemplate restTemplate = restTemplateBuilder.applicationConnectorRestTemplate(keyStore, keystorePassword);
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
			result.setKeystorePass(keystorePassword);
			result.setSslKey(keyStore);
			result.setCertificateAlgorithm(certificateAlgorithm);
			result.setCertificateSubject(certificateSubject);

			if (response.getBody().getUrls().getEventsUrl() != null)
				result.setEventsURLs(Collections.singletonList(response.getBody().getUrls().getEventsUrl()));

			return result;
		} catch (RestClientException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}
	
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
		
	}

}
