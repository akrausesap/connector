# A small "SDK" to connect your java Application to Kyma

For further details see https://kyma-project.io/docs/components/application-connector/

All services are used in io.kyma.project.connector.ScpxfConnectorApplication

Javadoc is here: [Javadoc](https://raw.githubusercontent.com/akrausesap/connector/master/scpxf-connector/doc/index.html)

Sample code is here:

```
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
					Collections.singletonMap("personid","testperson")));
			
			System.out.println("\nSucessfully sent person.created event for testperson");
			
			System.out.println("Processing finished...\n\n");
```