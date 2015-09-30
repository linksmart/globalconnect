package eu.linksmart.gc.api.engine;

import eu.linksmart.gc.api.network.backbone.Backbone;
import eu.linksmart.gc.api.network.identity.IdentityManager;
import eu.linksmart.gc.api.network.networkmanager.NetworkManager;
import eu.linksmart.gc.api.network.networkmanager.core.NetworkManagerCore;
import eu.linksmart.gc.api.network.routing.BackboneRouter;
import eu.linksmart.gc.api.sc.client.ServiceCatalogClient;

/**
 * Created by carlos on 01.09.15.
 */

public interface EngineContext {

    /**
     * Reads the value for the given key from the component configuration file. If there is a system property
     * with the same key, the system property will override the value from the configuration file.
     *
     * @param key
     *            key of the configuration property
     *
     * @return the configured value
     */
    public boolean getBoolean(String key, boolean defaultValue);
    public String get(String key);

    /**
     * Container host and port information that is supplied to LinkSmart Components
     *
     */
    public String getContainerHost();
    public int getContainerPort();
    public String getTunnelingPath();
    
    public BackboneRouter getBackboneRouter();
    public Backbone[] getBackbones();

    public IdentityManager getIdentityManager();
    
    public NetworkManager getNetworkManager();
    
    public NetworkManagerCore getNetworkManagerCore();
    
    public ServiceCatalogClient getServiceCatalogClient();
    
    
}

