package eu.linksmart.gc.server;

import eu.linksmart.gc.api.network.networkmanager.core.NetworkManagerCore;
import eu.linksmart.gc.network.networkmanager.core.impl.NetworkManagerCoreImpl;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.*;

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
        in.close();
    }

    /**
     * Test implementation lookup for specified interface.
     */
    public void testFindImplementation() {
    	
        Object instance = GcEngineSingleton.findImplementation(NetworkManagerCore.class, NetworkManagerCoreImpl.class.getName());
        assertNotNull( instance );
        assertTrue(instance instanceof NetworkManagerCore);

        instance = GcEngineSingleton.findImplementation(NetworkManagerCore.class, "eu.linksmart.gc.network.networkmanager.core.impl.NetworkManagerCoreImpl");
        assertNotNull( instance );
        assertTrue(instance instanceof NetworkManagerCore);
    }
    
    /**
     * Test GC engine
     */
    @Ignore
    public void testSingletonEngine() {
    	try {
    		GcEngineSingleton.initializeEngine();
    		GcEngineSingleton.shutdownEngine();
		} catch (Exception e) {
			e.printStackTrace();
			fail("testEngine failed: " + e.getMessage());
		}
    }
    /**
     * Test GC engine
     */
    @Ignore
    public void testEngine() {
        try {
            GcEngine engine = new GcEngine();
            Thread.sleep(500);
            engine.shutdownEngine();
        } catch (Exception e) {
            e.printStackTrace();
            fail("testEngine failed: " + e.getMessage());
        }
    }

    @Test
    public void testTwoEngines() {
        try {
            GcEngine engine = new GcEngine();
            Thread.sleep(250);
            GcEngine engine2 = new GcEngine();
            Thread.sleep(3000);
            System.out.println("engine 1 VM : " + engine.getNetworkManagerCore().getVirtualAddress());
            System.out.println("engine 2 VM : "+engine2.getNetworkManagerCore().getVirtualAddress());


            engine.shutdownEngine();
            Thread.sleep(250);
            engine2.shutdownEngine();
        } catch (Exception e) {
            e.printStackTrace();
            fail("testEngine failed: " + e.getMessage());
        }
    }
}
