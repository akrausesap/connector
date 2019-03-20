package com.sap.fsm.applicationconnector.metadata;

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
import com.sap.fsm.applicationconnector.ConnectionModel;
import com.sap.fsm.applicationconnector.util.ClientCertRestTemplateBuilder;
import com.sap.fsm.exception.ApplicationConnectorException;
import com.sap.fsm.exception.MetadataInvalidException;

import lombok.Data;

@Service
public class MetadataService {
	

	
	private ClientCertRestTemplateBuilder restTemplateBuilder;

	
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
	
	
	public void registerMetadata(ConnectionModel connectionModel, File metaDataFile) {
		
		RestTemplate restTemplate = 
				restTemplateBuilder.applicationConnectorRestTemplate(connectionModel.getSslKey(),
						connectionModel.getKeystorePass());

		Map<String, MetadataResponse> currentMetadata = getMetadata(restTemplate, connectionModel.getMetadataUrl());

		Map<String, Object> metadata = getMetadata(metaDataFile);

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

					if (response.getStatusCode() != HttpStatus.CREATED) {
						throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
								response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
					}
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
	
	private static class MetadataResponseList extends ArrayList<MetadataResponse> {}
}
