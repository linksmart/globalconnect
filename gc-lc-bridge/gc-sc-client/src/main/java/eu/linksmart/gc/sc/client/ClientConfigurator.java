package eu.linksmart.gc.sc.client;

import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import eu.linksmart.gc.api.utils.Configurator;

import org.osgi.service.cm.ConfigurationAdmin;

public class ClientConfigurator extends Configurator {

	private static Logger LOGGER = Logger.getLogger(ClientConfigurator.class.getName());
	
	public static String BACKBONE_PID = "eu.linksmart.gc.sc.client.SCClientImpl";
	
	public static String CONFIGURATION_FILE = "/sc-client.properties";

	public static final String DESCRIPTION = "eu.linksmart.gc.sc.client.description";
	
	public static final String SERVICE_CATALOG_URL = "eu.linksmart.gc.sc.client.service.catalog.url";

	public static final String TUNNELING_BASE_URL = "eu.linksmart.gc.sc.client.tunneling.base.url";
	
	private SCClientImpl scClient;

    public ClientConfigurator(SCClientImpl scClient,
                                    BundleContext context, ConfigurationAdmin configurationAdmin) {
        super(context, LOGGER, BACKBONE_PID, CONFIGURATION_FILE, configurationAdmin);
        super.init();
        this.scClient = scClient;
    }

	@SuppressWarnings("rawtypes")
	@Override
	/**
	 * Apply the configuration changes
	 * 
	 * @param updates the configuration changes
	 */
	public void applyConfigurations(Hashtable updates) {
		this.scClient.applyConfigurations(updates);
	}

}
