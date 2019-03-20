package com.sap.fsm.applicationconnector.event;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sap.fsm.applicationconnector.ConnectionModel;
import com.sap.fsm.applicationconnector.util.ClientCertRestTemplateBuilder;
import com.sap.fsm.exception.ApplicationConnectorException;

@Service
public class EventGatewayService {
	
private ClientCertRestTemplateBuilder restTemplateBuilder;

	
	@Autowired
	public void setRestTemplateBuilder(ClientCertRestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}
	
	
	public void writeEvent(ConnectionModel connectionModel, EventModel event) {
		
		RestTemplate restTemplate = 
				restTemplateBuilder.applicationConnectorRestTemplate(
						connectionModel.getSslKey(), 
						connectionModel.getKeystorePass());
		
		//Needs to be secured with queue/db or other reliability mechanism
		for (URI eventUrl : connectionModel.getEventsURLs()) {

			try {
				ResponseEntity<String> response = restTemplate.exchange(eventUrl, HttpMethod.POST,
						new HttpEntity<EventModel>(event), String.class);

				if (!response.getStatusCode().is2xxSuccessful()) {
					throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
							response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
				}
			} catch (RestClientException e) {
				throw new ApplicationConnectorException(e.getMessage(), e);
			}
		}
		
	}
	
}
