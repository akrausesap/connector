package io.kyma.project.connector.metadata;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kyma.project.connector.connection.model.ConnectionModel;
import io.kyma.project.connector.exception.ApplicationConnectorException;
import io.kyma.project.connector.exception.MetadataInvalidException;
import io.kyma.project.connector.exception.RestTemplateCustomizerException;
import io.kyma.project.connector.util.ClientCertRestTemplateBuilder;
import lombok.Data;


/**
* Service that registers metadata to Kyma/Extension Factory using an existing
* Connection Model and a reference to a file system. Furthermore Authentication Information
* required for API call-back can be passed.
* 
* 
* @author Andreas Krause
* @see ConnectionModel
*/
@Service
public class MetadataService {
	

	
	private ClientCertRestTemplateBuilder restTemplateBuilder;

	/**
	 * Sets the {@link ClientCertRestTemplateBuilder} to be used by this object
	 * 
	 * @param restTemplateBuilder {@link ClientCertRestTemplateBuilder} to be used by this Object 
	 */		
	@Autowired
	public void setRestTemplateBuilder(ClientCertRestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}


	
	
	private Map<String, MetadataResponse> getMetadata(RestTemplate restTemplate, URI metadataUrl) {
		
		Map<String, MetadataResponse> result = new HashMap<String, MetadataResponse>();
		
		try {
			ResponseEntity<MetadataResponseList> response = 
					restTemplate.getForEntity(metadataUrl, MetadataResponseList.class);

			if (response.getStatusCode() != HttpStatus.OK) {
				throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
						response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
			}
			
			
			for (MetadataResponse currentResponse : response.getBody()) {
				result.put(currentResponse.getIdentifier(), currentResponse);
			}
			
			
			return result;
			
		} catch (RestClientException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getMetadata(File metaDataFile) {
		
		try {
			return new ObjectMapper().readValue(FileUtils.readFileToByteArray(metaDataFile), HashMap.class);
		} catch (JsonParseException e) {
			throw new MetadataInvalidException(e.getMessage(), e);
		} catch (JsonMappingException e) {
			throw new MetadataInvalidException(e.getMessage(), e);
		} catch (IOException e) {
			throw new MetadataInvalidException(e.getMessage(), e);
		}
		

	}
	
	private Map<String, Object> addAuthenticationToMetadata(Map<String, Object> metadata, 
			MetadataAuthenticationInformation authentication) {
		
		//no api contained, hence nothing to do
		if (!metadata.containsKey("api")) {
			return metadata;
		}
		
		
		@SuppressWarnings("unchecked")
		Map<String, Object> api = (Map<String, Object>) metadata.get("api");
		
		api.put("credentials", authentication.getAuthenticationInfo());
		
		return metadata;
		
	}
	
	/**
	 * Registers metadata to the Kyma / Extension Factory Application Registry using a pointer
	 * to a file with the appropriate JSON format
	 * 
	 * @param connectionModel model containing all details for the connection to Kyma/Extension Factory
	 * @param authentication model containing authentication data, can be null 
	 * @throws ApplicationConnectorException if anything fails
	 * @throws RestTemplateCustomizerException if anything fails with acquiring the {@link RestTemplate}
	 * @throws MetadataInvalidException if Metadata is flawed (no syntax check though)
	 */
	public void registerMetadata(ConnectionModel connectionModel, File metaDataFile,
			MetadataAuthenticationInformation authentication) {
		
		RestTemplate restTemplate = 
				restTemplateBuilder.applicationConnectorRestTemplate(connectionModel.getSslKey(),
						connectionModel.getKeystorePass());

		Map<String, MetadataResponse> currentMetadata = getMetadata(restTemplate, connectionModel.getMetadataUrl());

		Map<String, Object> metadata = getMetadata(metaDataFile);
		
		if(authentication != null) {
			metadata = addAuthenticationToMetadata(metadata, authentication);
		}

		try {
			if (metadata.containsKey("identifier")) {

				String metadataIdentifier = (String) metadata.get("identifier");
				if (currentMetadata.containsKey(metadataIdentifier)) {

					String metadataId = currentMetadata.get(metadataIdentifier).getId();
					try {
						URIBuilder builder = new URIBuilder(connectionModel.getMetadataUrl());

						URI metadataUpdateUrl = builder.setPath(builder.getPath() + "/" + metadataId).build()
								.normalize();
						restTemplate.put(metadataUpdateUrl, metadata);

					} catch (URISyntaxException e) {
						throw new ApplicationConnectorException(e.getMessage(), e);
					}

				} else {
					ResponseEntity<String> response = restTemplate.postForEntity(connectionModel.getMetadataUrl(),
							metadata, String.class);

					if (response.getStatusCode() != HttpStatus.OK) {
						throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
								response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
					}
				}

			} else {
				ResponseEntity<String> response = restTemplate.postForEntity(connectionModel.getMetadataUrl(),
						metadata, String.class);

				if (response.getStatusCode() != HttpStatus.OK) {
					throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
							response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
				}
				
			}
		} catch (RestClientException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}
	
	@Data
	private static class MetadataResponse {
		private String id;
		private String provider;
		private String name;
		private String description;
		private String identifier;
	}
	
	@SuppressWarnings("serial")
	private static class MetadataResponseList extends ArrayList<MetadataResponse> {}
}
