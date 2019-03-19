package com.sap.fsm;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.sap.fsm.pairing.PairingService;

@SpringBootApplication
public class ScpxfConnectorApplication implements CommandLineRunner {
	
	@Autowired
	private PairingService pairingService;
	
	public static void main(String[] args) {
		SpringApplication.run(ScpxfConnectorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		System.out.println(args[1]);
		URI connectURL = new URI(args[1]);
		
		PairingService.ConnectInfo connectInfo = pairingService.getConnectInfo(connectURL);
		
		pairingService.getCertificate(connectInfo, "test123".toCharArray());
		
		System.out.println("Done");
		
	}

}
