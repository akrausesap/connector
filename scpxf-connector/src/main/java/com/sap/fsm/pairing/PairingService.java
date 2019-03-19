package com.sap.fsm.pairing;

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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.fsm.exception.ApplicationConnectorException;
import com.sap.fsm.pairing.util.CertificateService;
import com.sap.fsm.pairing.util.CertificateService.CsrResult;

import lombok.AllArgsConstructor;
import lombok.Data;


@Service
//@RequestScope
public class PairingService {
	
	private RestTemplate pairingTemplate;
	
	private RestTemplateBuilder restTemplateBuilder;
	
	private CertificateService certService;
	
	@Autowired
	public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
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
	
	
	public ConnectInfo getConnectInfo(URI connectUri) {
		
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
	
	
	//ToDo build rest template based on old keystore
	/*public KeyStore getCertificate(ConnectInfo connectInfo, char[] keystorePassword, KeyStore existingKeystore) {
		
		RestTemplateBuilder.
		
		
	}*/
	
	public KeyStore getCertificate(ConnectInfo connectInfo, char[] keystorePassword) {
		
		CsrResult csr = certService.createCSR(connectInfo.getCertificate().getSubject(), 
				connectInfo.getCertificate().getKeyAlgorithm());
		
		return getCertificateInternal(pairingTemplate, keystorePassword, connectInfo.getCsrUrl(), 
				csr.getCsr(), csr.getKeypair());
	}
	
	@Data
	public static class ConnectInfo {
		
		private URI csrUrl;
		private Api api;
		private CertificateSpecification certificate;
		
		
	}
	
	@Data
	public static class Api {
		
		private URI metadataUrl;
		private URI certificatesUrl;
		private URI infoUrl;
		
		private CertificateSpecification certificate;
		
		
	}
	
	@Data
	public static class CertificateSpecification {
		
		private String subject;
		private String extensions;
		
		@JsonProperty("key-algorithm")
		private String keyAlgorithm;
		
	}
	
	@Data
	@AllArgsConstructor
	public static class CsrRequest {
		private String csr;
	}
	
	@Data
	@AllArgsConstructor
	public static class CsrResponse {
		private String crt;
		private String clientCrt;
		private String caCrt;
	}
	

}
