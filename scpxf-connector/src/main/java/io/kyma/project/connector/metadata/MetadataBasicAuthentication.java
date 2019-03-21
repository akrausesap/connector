package io.kyma.project.connector.metadata;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;


/**
* Model object that holds Basic Authentication Credentials, it implements interface 
* {@link MetadataAuthenticationInformation}
* 
* @author Andreas Krause
* @see MetadataAuthenticationInformation
*/
@Data
@AllArgsConstructor
public class MetadataBasicAuthentication implements MetadataAuthenticationInformation{
	
	private String username;
	private String password;
	
	
	@Override
	public Map<String, Object> getAuthenticationInfo() {
		
		
		
		Map<String, Object> basic = new HashMap<String, Object>();
		basic.put("username", username);
		basic.put("password", password);
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		result.put("basic", basic);
		
		return result;
	}
	
	

}
