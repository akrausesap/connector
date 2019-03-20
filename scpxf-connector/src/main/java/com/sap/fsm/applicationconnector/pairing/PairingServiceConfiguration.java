package com.sap.fsm.applicationconnector.pairing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PairingServiceConfiguration {
	
	private RestTemplateBuilder restTemplateBuilder;
	
	@Autowired
	public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}
	
	
	@Bean("PairingTemplate")
	public RestTemplate pairingRestTemplate() {
		return restTemplateBuilder.build();
		
	}
	
	

}
