package eu.linksmart.gc.server;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.linksmart.gc.api.network.networkmanager.core.NetworkManagerCore;
import eu.linksmart.gc.network.networkmanager.core.impl.NetworkManagerCoreImpl;

import java.io.InputStream;
import java.util.Properties;

public class EngineTest {
	
	 /**
     * Test Configuration files lookup and loading
     */
	@Test
    public void testLoadConfiguration() throws Exception  {
    	
    	InputStream in = Configurator.findResource(Configurator.getConfigurationFileName());
        
        Properties gcConfig = new Properties();
        gcConfig.load(in);
        
        String serverConfigFileName = gcConfig.getProperty("eu.linksmart.gc.server.config", ConfigConstants.GC_SERVER_CONFIGURATION_FILE_NAME);
        assertNotNull(serverConfigFileName);  
    }

    /**
     * Test implementation lookup for specified interface.
     */
    public void testFindImplementation() {
    	
        Object instance = GcEngine.findImplementation(NetworkManagerCore.class,  NetworkManagerCoreImpl.class.getName());
        assertNotNull( instance );
        assertTrue(instance instanceof NetworkManagerCore);

        instance = GcEngine.findImplementation(NetworkManagerCore.class, "eu.linksmart.gc.network.networkmanager.core.impl.NetworkManagerCoreImpl");
        assertNotNull( instance );
        assertTrue(instance instanceof NetworkManagerCore);
    }
    
    /**
     * Test GC engine
     */
    @Test
    public void testFindSingletonImplementation() {
    	try {
    		GcEngine.initializeEngine();
    		//GcEngine.shutdownEngine();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
