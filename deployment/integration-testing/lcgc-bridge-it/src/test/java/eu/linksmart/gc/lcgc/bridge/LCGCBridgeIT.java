package eu.linksmart.gc.lcgc.bridge;

import java.util.Scanner;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import eu.linksmart.gc.api.network.ServiceAttribute;
import eu.linksmart.gc.api.network.VirtualAddress;
import eu.linksmart.gc.api.sc.client.ServiceCatalogClient;
import eu.linksmart.gc.api.utils.Part;
import eu.linksmart.it.utils.ITConfiguration;

@RunWith(PaxExam.class)
public class LCGCBridgeIT {
	
	private static Logger LOG = Logger.getLogger(LCGCBridgeIT.class.getName());
	
	@Inject
    private ServiceCatalogClient client;
	
    @Configuration
    public Option[] config() {
        return new Option[] {
        		ITConfiguration.regressionDefaults(),
        		features(ITConfiguration.getTestingFeaturesRepoURL(),"gc-lc-bridge-it"),  
        };
    }
    
    @Test
    public void testBridge() {
    	
    	try {
    		
    		LOG.info("starting lcgc-bridge test");
            
    		Assert.assertEquals("eu.linksmart.gc.sc.client.SCClientImpl", client.getClass().getName());
    		
    		//
    		// add service catalog registration
    		// service catalog's registration.ID is mapped to networkmanager's registration.Description
    		//
    		String cregistrationJson = readFileContents("/registration.json");
    		
    		Part[] attributes = { new Part(ServiceAttribute.DESCRIPTION.name(), "testserver/ServiceA"), 
    				new Part("SC", cregistrationJson) };
    		eu.linksmart.gc.api.network.Registration sregistration = new eu.linksmart.gc.api.network.Registration(new VirtualAddress(), attributes);
    		
    		org.junit.Assert.assertTrue(client.add(sregistration));
    		
    		//
    		// update service catalog registration
    		//
    		String ucregistrationJson = readFileContents("/update-registration.json");
    		
    		Part[] uattributes = { new Part(ServiceAttribute.DESCRIPTION.name(), "testserver/ServiceB"), 
    				new Part("SC", ucregistrationJson) };
    		eu.linksmart.gc.api.network.Registration usregistration = new eu.linksmart.gc.api.network.Registration(new VirtualAddress(), uattributes);
    		
    		org.junit.Assert.assertTrue(client.update(usregistration));
    		
    		//
    		// delete service catalog registration
    		//
    		org.junit.Assert.assertTrue(client.delete(usregistration));
    		
    		LOG.info("lcgc-bridge test successfully completed");
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("unable to access the service");
		}
    }
    
    private String readFileContents(String fileName) {
		StringBuilder builder = null;
		Scanner scanner = null;
		try {
			builder = new StringBuilder();
			scanner = new Scanner(this.getClass().getResourceAsStream(fileName));
			String lineSeparator = System.getProperty("line.separator");
	        while(scanner.hasNextLine()) {        
	            builder.append(scanner.nextLine() + lineSeparator);
	        }
	    } catch(Exception e) {
	    	e.printStackTrace();
	    } finally {
	        scanner.close();
	    }
		return builder.toString();
	}
    
}