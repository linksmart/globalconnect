package eu.linksmart.gc.api.sc.client;

import eu.linksmart.gc.api.network.Registration;

public interface ServiceCatalogClient {

	/**
	 * add registration into service catalog
	 * 
	 * @param service registration
	 * @return true or false
	 */
	public boolean add(Registration registration);
	
	/**
	 * update registration in service catalog
	 * 
	 * @param service registration
	 * @return true or false
	 */
	public boolean update(Registration updatedRegistration);
	
	/**
	 * delete registration from service catalog
	 * 
	 * @param registration
	 * @return true or false
	 */
	public boolean delete(Registration registration);
	
}
