package com.sap.fsm.applicationconnector;


import java.net.URI;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ConnectionModel {
	
	private String applicationName;
	private URI metadataUrl;
	private URI renewCertUrl;
	private URI revocationCertUrl;
	private URI infoUrl;
	
	private String certificateSubject;
	private String certificateAlgorithm;
	
	
	private KeyStore sslKey;
	private char[] keystorePass;
	private List<URI> eventsURLs = new ArrayList<URI>();
	

}
