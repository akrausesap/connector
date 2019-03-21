package io.kyma.project.connector.event;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import io.kyma.project.connector.connection.model.ConnectionModel;
import io.kyma.project.connector.exception.ApplicationConnectorException;
import io.kyma.project.connector.util.ClientCertRestTemplateBuilder;

/**
* Service that forwards events to Kyma/Extension Factory using an existing
* Connection Model and an Event Object.
* 
* 
* @author Andreas Krause
* @see EventModel
* @see ConnectionModel
*/
@Service
public class EventGatewayService {
	
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
	 * Publishes a given event to the connection specified in the ConnectionModel.
	 * @param connectionModel The connection to be used for event forwarding
	 * @param event the event to be published
	 * @throws ApplicationConnectorException if connection fails
	 */
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
