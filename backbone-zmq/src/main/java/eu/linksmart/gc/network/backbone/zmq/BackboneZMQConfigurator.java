package eu.linksmart.gc.network.backbone.zmq;

import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import eu.linksmart.utils.Configurator;
import org.osgi.service.cm.ConfigurationAdmin;

public class BackboneZMQConfigurator extends Configurator {

	/* Configuration PID & file path. */
	public static String BACKBONE_PID = "eu.linksmart.gc.network.backbone.zmq";
	public static String CONFIGURATION_FILE = "/zmq.properties";

	public static final String BACKBONE_DESCRIPTION = "backbone.description";
	public static final String BACKBONE_TCP_HOST = "backbone.tcp.host";
	public static final String BACKBONE_TCP_PORT = "backbone.tcp.port";

	private BackboneZMQImpl bbJXTAImpl;

	/**
	 * Log4j logger of this class
	 */
	private static Logger LOGGER = Logger.getLogger(BackboneZMQConfigurator.class.getName());


    /**
     * Initializes the JXTA backbone configurator.
     *
     * @param bbJXTAImpl
     *            instantiation of JXTA backbone
     * @param context
     *            A bundle context
     * @param configurationAdmin configAdmin reference for proper setup*
     */
    public BackboneZMQConfigurator(BackboneZMQImpl bbJXTAImpl,
                                    BundleContext context, ConfigurationAdmin configurationAdmin) {
        super(context, LOGGER, BACKBONE_PID, CONFIGURATION_FILE, configurationAdmin);
        super.init();
        this.bbJXTAImpl = bbJXTAImpl;
    }

	@SuppressWarnings("rawtypes")
	@Override
	/**
	 * Apply the configuration changes
	 * 
	 * @param updates the configuration changes
	 */
	public void applyConfigurations(Hashtable updates) {
		bbJXTAImpl.applyConfigurations(updates);
	}

}
