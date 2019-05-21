package io.kyma.project.connector.event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model Object that holds all information regarding events subscriptions
 * that are active in one particular Kyma/Extension Factory Runtime
 * 
 * 
 * @author Andreas Krause
 */
public class EventSubscriptionModel {
	
	private Set<Event> events;
	
	/**
 	* Constructor used to create an EventSubscriptionModel that contains 
 	* no active subscriptions
 	* @param events List of all active events
 	*/
	public EventSubscriptionModel() {
		this.events =  new HashSet<Event>();
	}
	
	/**
 	* Constructor used to create an EventSubscriptionModel that already contains 
 	* all active subscriptions
 	* @param events List of all active events
 	*/
	public EventSubscriptionModel(List<Event> events) {
		this.events = new HashSet<Event>(events);
	}
	
	/**
 	* Method that checks whether an event has an active subscription
 	* @param event to be checked
 	*/
	public boolean isEventActive(Event event) {
		return events.contains(event);
	}
	
	
	
	@Data
	@EqualsAndHashCode
	@AllArgsConstructor
	/**
	 * Model Object that holds all information identifying a particular event
	 * @author Andreas Krause
	 */
	public static class Event {
		
		private String name;
		private String version;
		
	}
}
