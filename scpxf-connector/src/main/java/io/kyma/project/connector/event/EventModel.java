package io.kyma.project.connector.event;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Model Object that holds all information needed to publish an event to
 * to Kyma/Extension Factory.
 * 
 * Data is supposed to be JSON and captured in a Map<String, Object> object
 * called data.
 * 
 * 
 * @author Andreas Krause
 */
@Data
@AllArgsConstructor
public class EventModel {

	@JsonProperty("event-type")
	private String eventType;

	@JsonProperty("event-type-version")
	private String eventTypeVersion;

	@JsonProperty("event-time")
	private String eventTime;

	private Map<String, Object> data;

}
