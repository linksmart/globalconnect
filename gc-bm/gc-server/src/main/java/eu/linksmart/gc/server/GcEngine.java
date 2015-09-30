package eu.linksmart.gc.server;

import eu.linksmart.gc.api.engine.EngineContext;
import eu.linksmart.gc.api.network.backbone.Backbone;
import eu.linksmart.gc.api.network.identity.IdentityManager;
import eu.linksmart.gc.api.network.networkmanager.NetworkManager;
import eu.linksmart.gc.api.network.networkmanager.core.NetworkManagerCore;
import eu.linksmart.gc.api.network.routing.BackboneRouter;
import eu.linksmart.gc.api.sc.client.ServiceCatalogClient;
import eu.linksmart.gc.networkmanager.rest.NetworkManagerRest;

import org.apache.commons.discovery.tools.DiscoverClass;
import org.apache.commons.discovery.tools.DiscoverSingleton;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class GcEngine implements EngineContext {
	
	private static final Logger LOG = Logger.getLogger(GcEngine.class);
	
    private GcEngine engine = null;
    
    private Properties gcConfig = null;
    
    private Properties gcServerConfig = null;
    
    private NetworkManager networkManager = null;
    
    private NetworkManagerCore networkManagerCore = null;
    
    private IdentityManager identityManager = null;
    
    private BackboneRouter backboneRouter = null;
    
    private List<Backbone> backbones = new ArrayList<Backbone>();
    
    private ServiceCatalogClient serviceCatalogClient = null;
    
    private NetworkManagerRest networkManagerRest = null;

    /**
     * Initializes the GC Engine. During the initialization process the GC main configuration (system
     * configuration) is parsed and the referenced GC server configuration(s) are loaded.
     * The GC engine must be initialized before making any calls to the system. By default the
     * GC engine main configuration location is specified by the
     * {@link ConfigConstants#DEFAULT_CONFIGURATION_PATH} constant. This value can be overwritten by the
     * system property {@value ConfigConstants#DEFAULT_CONFIGURATION_PATH_KEY}.
     *
     * @throws Exception
     *             indicates an error during engine initialization
     */
    public GcEngine() throws Exception {
        initialize();
    }
    
    private void initialize() throws Exception {
    	
    	LOG.info("GcEngine -> is initializing");
    	
        loadGcConfiguration();
        
        loadServerConfiguration();
        
        //
        // loading identity manager implementation
        //
        String identityManagerImpl = gcServerConfig.getProperty("eu.linksmart.gc.api.network.identity");
        Object imObject = findImplementation(IdentityManager.class, identityManagerImpl);
        
        if(!(imObject instanceof IdentityManager)) {
        	throw new Exception(MessageFormat.format("Class {0} is not an instance of {1}.", new Object[] { imObject, IdentityManager.class }));
        }
        
        identityManager = (IdentityManager) imObject;
        
        //
        // loading backbone router implementation
        //
        String backboneRouterImpl = gcServerConfig.getProperty("eu.linksmart.gc.api.network.routing.BackboneRouter");
        Object brObject = findImplementation(BackboneRouter.class, backboneRouterImpl);
        
        if(!(brObject instanceof BackboneRouter)) {
        	throw new Exception(MessageFormat.format("Class {0} is not an instance of {1}.", new Object[] { brObject, BackboneRouter.class }));
        }
        
        backboneRouter = (BackboneRouter) brObject;
        
        //
        // loading backbone implementations
        //
        String backboneImpls = gcServerConfig.getProperty("eu.linksmart.gc.api.network.backbone");
        StringTokenizer backboneTokens = new StringTokenizer(backboneImpls, ",");
        while(backboneTokens.hasMoreTokens()) {
        	String backboneImpl = backboneTokens.nextToken();
        	Object backboneObject = findImplementation(Backbone.class, backboneImpl);
            if(!(backboneObject instanceof Backbone)) {
            	throw new Exception(MessageFormat.format("Class {0} is not an instance of {1}.", new Object[] { backboneObject, Backbone.class }));
            }
            backbones.add((Backbone) backboneObject);
        }
        
        //
        // loading network manager implementation
        //
        String nmImpl = gcServerConfig.getProperty("eu.linksmart.gc.api.network.networkmanager.NetworkManager");
        Object nmObject = findImplementation(NetworkManager.class, nmImpl);
        
        if(!(nmObject instanceof NetworkManager)) {
        	throw new Exception(MessageFormat.format("Class {0} is not an instance of {1}.", new Object[] { nmObject, NetworkManager.class }));
        }
        
        networkManager = (NetworkManager) nmObject;
        
        //
        // loading network manager core implementation
        //
        String nmcImpl = gcServerConfig.getProperty("eu.linksmart.gc.api.network.networkmanager.core.NetworkManagerCore");
        Object nmcObject = findImplementation(NetworkManagerCore.class, nmcImpl);
        
        if(!(nmcObject instanceof NetworkManagerCore)) {
        	throw new Exception(MessageFormat.format("Class {0} is not an instance of {1}.", new Object[] { nmcObject, NetworkManagerCore.class }));
        }
        
        networkManagerCore = (NetworkManagerCore) nmcObject;
        
        //
        // loading service catalog client implementation
        //
        String scClientImplName = gcServerConfig.getProperty("eu.linksmart.gc.api.sc.client.ServiceCatalogClient");
        Object scObject = findImplementation(ServiceCatalogClient.class, scClientImplName);
        
        if(!(scObject instanceof ServiceCatalogClient)) {
        	throw new Exception(MessageFormat.format("Class {0} is not an instance of {1}.", new Object[] { scObject, ServiceCatalogClient.class }));
        }
        
        serviceCatalogClient = (ServiceCatalogClient) scObject;
        
        //
        // activate GC components
        //
        serviceCatalogClient.activate(this);
        identityManager.activate(this);
        // backbones are activated first and later backbone router retrieves all available backbones
        for (Backbone backbone : backbones) {
        	backbone.activate(this);
		}
        backboneRouter.activate(this);
        networkManagerCore.activate(this);
        
        // activate network manager rest
        networkManagerRest = new NetworkManagerRest();
        networkManagerRest.activate(this);
        
        //
        // initialize GC components
        //
        backboneRouter.initialize();
        networkManagerCore.initialize();
        serviceCatalogClient.initialize();
        identityManager.initialize();
        for (Backbone backbone : backbones) {
        	backbone.initialize();
		}
        networkManagerRest.initialize();
         
        LOG.info("--------------------------------");
        LOG.info("GcEngine -> is initialized");
        LOG.info("--------------------------------");
    }

    /**
     * Shutdown of the GC engine instance.
     * 
     * @throws Exception
     *             indicates an error during engine shutdown
     */
    public void shutdownEngine() throws Exception {
    	
    	LOG.info("GcEngine -> is shutting down");
    	
    	//
        // deactivate GC components
        //
        identityManager.deactivate();
        serviceCatalogClient.deactivate();
        for (Backbone backbone : backbones) {
        	backbone.deactivate();
		}
        backboneRouter.deactivate();
        networkManagerCore.deactivate();
        
        gcConfig = null;
        gcServerConfig = null;
        engine = null;
        LOG.info("--------------------------------");
        LOG.info("GcEngine is stopped");
        LOG.info("--------------------------------");
    }

    /**
     * Loads the GC main configuration.
     * 
     * @throws Exception
     */
    private void loadGcConfiguration() throws Exception {
    	
    	LOG.info("loading GC main configuration");
    
    	InputStream in = Configurator.findResource(Configurator.getConfigurationFileName());
        
        gcConfig = new Properties();
        gcConfig.load(in);
    }
    
    /**
     * Loads the GC server configuration.
     * 
     * @throws Exception
     */
    private void loadServerConfiguration() throws Exception {
    	
    	LOG.info("loading GC server configuration");
    	
    	String serverConfigFileName = gcConfig.getProperty("eu.linksmart.gc.server.config", ConfigConstants.GC_SERVER_CONFIGURATION_FILE_NAME);
    	
    	InputStream in = Configurator.findResource(serverConfigFileName);
        
    	gcServerConfig = new Properties();
    	gcServerConfig.load(in);
    }
    
    /**
     * Finds an implementation for a given interface.
     * 
     * @param interfaceDef
     *            the interface definition to find an implementation for
     * @param defaultImpl
     *            the default implementation to use
     * @return The instance of the implementation class
     */
    public static Object findImplementation(Class<?> interfaceDef, String defaultImpl) {
        try {
            return (new DiscoverClass().find(interfaceDef, defaultImpl).newInstance());
        } catch(Exception e) {
            LOG.error(MessageFormat.format("Could not instantiate class for interface {0}.", new Object[]{interfaceDef.getName()}));
        }
        return null;
    }

    /**
     * Finds a singleton implementation for a given interface.
     * 
     * @param interfaceDef
     *            the interface definition to find an implementation for
     * @param defaultImpl
     *            the default implementation to use
     * @return The instance of the implementation class
     */
    public static Object findSingeltonImplementation(Class<?> interfaceDef, String defaultImpl) {
        try {
            return DiscoverSingleton.find( interfaceDef, defaultImpl );
        } catch(Exception e) {
        	LOG.error(MessageFormat.format("Could not instantiate class for interface {0}.", new Object[] { interfaceDef.getName() }));
        }
        return null;
    }
    
    public NetworkManager getNetworkManager() {
    	return networkManager;
    }
    
    public NetworkManagerCore getNetworkManagerCore() {
    	return networkManagerCore;
    }
    
    public IdentityManager getIdentityManager() {
    	return identityManager;
    }
    
    public BackboneRouter getBackboneRouter() {
    	return backboneRouter;
    }
    
    public Backbone[] getBackbones() {
    	return new Backbone[backbones.size()];
    }
    
    public ServiceCatalogClient getServiceCatalogClient() {
    	return serviceCatalogClient;
    }
    
    public NetworkManagerRest getNetworkManagerRest() {
    	return networkManagerRest;
    }
    
    /**
     * Reads the value for the given key from the component configuration file. If there is a system property
     * with the same key, the system property will override the value from the configuration file.
     * 
     * @param key
     *            key of the configuration property
     * 
     * @return the configured value
     */
    public String get(String key) {
    	 String defaultValue = gcConfig.getProperty(key);
         return System.getProperty(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
    	return Boolean.parseBoolean(gcConfig.getProperty(key, Boolean.valueOf( defaultValue ).toString()));
    } 
    
    public String getContainerHost() {
        return gcConfig.getProperty("eu.linksmart.gc.server.name");
    }
    
    public int getContainerPort() {
    	return Integer.parseInt(gcConfig.getProperty("eu.linksmart.gc.server.port"));
    }
    
    public String getTunnelingPath() {
    	return gcConfig.getProperty("eu.linksmart.gc.tunneling.path");
    }
}
