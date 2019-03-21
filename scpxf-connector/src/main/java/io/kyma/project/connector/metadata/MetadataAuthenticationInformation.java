package io.kyma.project.connector.metadata;

import java.util.Map;

import io.kyma.project.connector.connection.model.ConnectionModel;
import io.kyma.project.connector.exception.ApplicationConnectorException;


/**
* Interface that is used to generalize dealing with API Authentication.
* 
* @author Andreas Krause
* @see ConnectionModel
*/
public interface MetadataAuthenticationInformation {
	
	/**
	 * Returns AUthentication information in the form of a JSON compliant Map.
	 * 
	 * @return Map containing authentication data in a format understood by Kyma/Extension Factory
	 * @throws ApplicationConnectorException if anything fails
	 */
	public Map<String, Object> getAuthenticationInfo();

}
