package io.kyma.project.connector;

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

import io.kyma.project.connector.connection.model.ConnectionModel;
import io.kyma.project.connector.event.EventGatewayService;
import io.kyma.project.connector.event.EventModel;
import io.kyma.project.connector.metadata.MetadataBasicAuthentication;
import io.kyma.project.connector.metadata.MetadataOAuth2ClientCredentialsAuthentication;
import io.kyma.project.connector.metadata.MetadataService;
import io.kyma.project.connector.pairing.PairingService;

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
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		
		if (args.length < 2) {
			System.out.println("You need to provide a Kyma Connection String");
		} else {
			
			System.out.println("Starting processing...\n\n");
			
			System.out.println(String.format("Connect URL: \t%s", args[1]));
			
			URI connectURL = new URI(args[1]);

			ConnectionModel initialModel = pairingService.executeInitialPairing(connectURL, "test123".toCharArray());
			
			System.out.println(String.format("\nSuccessfully paired with application: \t%s", initialModel.getApplicationName()));
			System.out.println(String.format("\nCertificate expires: \t%s", 
					df.format(initialModel.getCertificateExpirationDate())));
			System.out.println(String.format("\nCertificate Fingerprint: \t%s", 
					initialModel.getCertificateFingerprint()));
			
			ConnectionModel intermediateModel = pairingService.getInfo(initialModel);
			
			System.out.println("\nSucessfully refreshed information using Get Info, no change expected");

			ConnectionModel newModel = pairingService.renewCertificate(intermediateModel, "test345".toCharArray());
			
			System.out.println(String.format("\nSucessfully renewed certificate, it now expires: \t%s", 
					df.format(newModel.getCertificateExpirationDate())));
			System.out.println(String.format("\nRenewed Certificate Fingerprint: \t%s", 
					newModel.getCertificateFingerprint()));
			
			metadataService.registerMetadata(newModel, new File("registration/registrationfile.json"),
					null);
			
			System.out.println("\nSucessfully registered metadata from registration/registrationfile.json without credentials");

			metadataService.registerMetadata(newModel, new File("registration/registrationfile.json"),
					new MetadataBasicAuthentication("un", "pw"));
			
			System.out.println("\nSucessfully registered metadata from registration/registrationfile.json with basic auth");

			metadataService.registerMetadata(newModel, new File("registration/registrationfile.json"),
					new MetadataOAuth2ClientCredentialsAuthentication(new URI("https://test.com"), "client", "secret"));
			
			System.out.println("\nSucessfully registered metadata from registration/registrationfile.json with OAuth2 client credentials");

		
			eventGatewayService.writeEvent(newModel, new EventModel("person.created", "v1", df.format(new Date()),
					Collections.singletonMap("personid", "testperson")));
			
			System.out.println("\nSucessfully sent person.created event for testperson");
			
			System.out.println("Processing finished...\n\n");
		
		}
		
	}

}
