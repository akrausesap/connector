package io.kyma.project.connector.metadata;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;


/**
* Model object that holds OAuth2Client Credentials Authentication info, it implements interface 
* {@link MetadataAuthenticationInformation}
* 
* 
* @author Andreas Krause
* @see MetadataAuthenticationInformation
*/
@Data
@AllArgsConstructor
public class MetadataOAuth2ClientCredentialsAuthentication implements MetadataAuthenticationInformation{
	
	private URI url;
	private String clientId;
	private String clientSecret;
	
	
	@Override
	public Map<String, Object> getAuthenticationInfo() {
		
		Map<String, Object> oauth = new HashMap<String, Object>();
		oauth.put("url", url.toString());
		oauth.put("clientId", clientId);
		oauth.put("clientSecret", clientSecret);
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		result.put("oauth", oauth);
		
		return result;
	}
	
	

}
