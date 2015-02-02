package eu.linksmart.gc.networkmanager.rest;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import eu.linksmart.it.utils.ITConfiguration;

@RunWith(PaxExam.class)
public class NetworkManagerRestIT {
	
	private static Logger LOG = Logger.getLogger(NetworkManagerRestIT.class.getName());
	
    @Configuration
    public Option[] config() {
        return new Option[] {
        		ITConfiguration.regressionDefaults(),
        		features("mvn:eu.linksmart.gc.features/linksmart-gc-features/0.0.1-SNAPSHOT/xml/features","network-manager-rest-it"),  
        };
    }
    
    @Test
    public void testBackbone() {
    	
    	try {
    		
    		LOG.info("starting networkmanager-rest test");
            		
    		LOG.info("networkmanager-rest test successfully completed");
			
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("unable to access networkmanager-rest service: " + e.getMessage());
		}
    }
    
}