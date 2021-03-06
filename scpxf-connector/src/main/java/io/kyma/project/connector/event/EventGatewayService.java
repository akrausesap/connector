package io.kyma.project.connector.event;

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
	

		try {
			ResponseEntity<String> response = restTemplate.exchange(connectionModel.getEventsURL(), HttpMethod.POST,
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
