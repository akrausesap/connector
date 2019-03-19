package com.sap.fsm.pairing.util;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.stereotype.Service;

import com.sap.fsm.exception.ApplicationConnectorException;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
public class CertificateService {
	
	private KeyPair generateKeyPair(String algorithm) {
		
		if (!algorithm.equals("rsa2048")) {
			throw new ApplicationConnectorException(
					String.format("Key Algorith %s not supported", algorithm));
		}
		
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		
			keyGen.initialize(2048, new SecureRandom());
			return keyGen.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new ApplicationConnectorException(
					String.format("Error generating Keypair: %s", e.getMessage()), 
					e);
		}
		
	}
	
	
	private byte[] createSigningRequest(String subjectString, KeyPair keypair) throws OperatorCreationException, IOException {
		X500Principal subject = new X500Principal(subjectString);
		
	
		
		ContentSigner signGen = new JcaContentSignerBuilder("SHA1withRSA").build(keypair.getPrivate());
		
		PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, keypair.getPublic());
		PKCS10CertificationRequest csr = builder.build(signGen);
		
		return csr.getEncoded();
	}
	
	
	
	public CsrResult createCSR(String subject, String algorithm) {
		KeyPair keypair = generateKeyPair(algorithm);
		try {
			byte[] csr = createSigningRequest(subject, keypair);
			
			return new CsrResult(keypair, csr);
			
		} catch (Exception e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}
	
	@Data
	@AllArgsConstructor
	public static class CsrResult {
		
		private KeyPair keypair;
		private byte[] csr;
		
	}

}
