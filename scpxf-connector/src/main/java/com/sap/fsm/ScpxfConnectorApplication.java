package com.sap.fsm;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.sap.fsm.applicationconnector.ConnectionModel;
import com.sap.fsm.applicationconnector.event.EventGatewayService;
import com.sap.fsm.applicationconnector.event.EventModel;
import com.sap.fsm.applicationconnector.metadata.MetadataService;
import com.sap.fsm.applicationconnector.pairing.PairingService;

@SpringBootApplication
public class ScpxfConnectorApplication implements CommandLineRunner {
	
	@Autowired
	private PairingService pairingService;
	
	@Autowired
	private MetadataService metadataService;
	
	@Autowired
	private EventGatewayService eventGatewayService;
	
	public static void main(String[] args) {
		SpringApplication.run(ScpxfConnectorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		System.out.println(args[1]);
		URI connectURL = new URI(args[1]);
		
		ConnectionModel initialModel = pairingService.executeInitialPairing(connectURL, "test123".toCharArray());
		
		ConnectionModel intermediateModel = pairingService.getInfo(initialModel);
		
		ConnectionModel newModel = pairingService.renewCertificate(intermediateModel, "test345".toCharArray());
		
		metadataService.registerMetadata(newModel, new File("registration/registrationfile.json"));
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		
		eventGatewayService.writeEvent(newModel, 
				new EventModel("person.created", "v1", df.format(new Date()), 
						 Collections.singletonMap("personid", 
								"testperson")));
		
		System.out.println(newModel.getApplicationName());
		
	}

}
