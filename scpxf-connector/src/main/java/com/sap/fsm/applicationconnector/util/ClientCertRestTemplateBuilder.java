package com.sap.fsm.applicationconnector.util;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sap.fsm.exception.RestTemplateCustomizerException;



@Service
public class ClientCertRestTemplateBuilder {
	
	private RestTemplateBuilder restTemplateBuilder;
	
	@Autowired
	public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}
	
	public RestTemplate applicationConnectorRestTemplate(KeyStore clientCertificate, char[] keystorePassword) {

		try {
				SSLContext sslContext = SSLContextBuilder
						.create()
						//Enable only for testing purposes
						//.loadTrustMaterial(null, new TrustSelfSignedStrategy())
						.loadKeyMaterial(clientCertificate, keystorePassword)
						.build();
			
			SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

			HttpClient client = HttpClients.custom()
					.setSSLSocketFactory(socketFactory)
					.build();	

			return restTemplateBuilder
							.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
							.build();
		} catch (KeyManagementException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (UnrecoverableKeyException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (KeyStoreException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		}
	
	}

}
