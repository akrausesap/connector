package io.kyma.project.connector.event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import io.kyma.project.connector.connection.model.ConnectionModel;
import io.kyma.project.connector.exception.ApplicationConnectorException;
import io.kyma.project.connector.util.ClientCertRestTemplateBuilder;
import lombok.Data;


/**
* Service that is used to determine active Event Subscriptions on a particular Kyma
* /Extension Factory instance
* 
* @author Andreas Krause
* @see EventSubscriptionModel
*/
@Service
public class EventSubscriptionService {
	
	
	private ClientCertRestTemplateBuilder restTemplateBuilder;

	
	/**
 	* Method used for setter injection
 	* @param restTemplateBuilder the builder used to acquire RestTemplate
 	*/
	@Autowired
	public void setRestTemplateBuilder(ClientCertRestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}
	
	/**
 	* Method that retrieves active event subscriptions
 	* @param connectionModel model containing all details for the current connection 
 	*/
	public EventSubscriptionModel getEventSubscriptions(ConnectionModel connectionModel) {
		
		RestTemplate restTemplate = 
				restTemplateBuilder.applicationConnectorRestTemplate(
						connectionModel.getSslKey(), 
						connectionModel.getKeystorePass());
		
		try {
			ResponseEntity<EventSubscriptionResponse> response = 
					restTemplate.getForEntity(connectionModel.getEventsInfoUrl(), 
							EventSubscriptionResponse.class);
			if (response.getStatusCode() != HttpStatus.OK) {
				throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
						response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
			}
			
			List<EventSubscriptionModel.Event> subscriptions = 
					response.getBody().getEventsInfo().stream()
						.map((e) -> new EventSubscriptionModel.Event(e.getName(), e.getVersion()))
						.collect(Collectors.toList());
			
			return new EventSubscriptionModel(subscriptions);
			
		} catch (RestClientException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}
	
	@Data
	private static class EventSubscriptionResponse {
		
		private List<EventSubscription> eventsInfo = new ArrayList<EventSubscription>();
		
	}
	@Data
	private static class EventSubscription {
		
		private String name;
		private String version;
		
	}


}
