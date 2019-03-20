package com.sap.fsm.applicationconnector.event;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

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
