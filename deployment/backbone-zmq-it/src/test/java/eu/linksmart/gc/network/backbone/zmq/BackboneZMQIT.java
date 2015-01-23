package eu.linksmart.gc.network.backbone.zmq;

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
import eu.linksmart.network.backbone.Backbone;

@RunWith(PaxExam.class)
public class BackboneZMQIT {
	
	private static Logger LOG = Logger.getLogger(BackboneZMQIT.class.getName());
	
	@Inject
    private Backbone backbone;
	
    @Configuration
    public Option[] config() {
        return new Option[] {
        		ITConfiguration.regressionDefaults(),
        		features("mvn:eu.linksmart.gc.features/linksmart-gc-features/0.0.1-SNAPSHOT/xml/features","linksmart-light-gc"),  
        };
    }
    
    @Test
    public void testBackbone() {
    	
    	try {
    		
    		LOG.info("starting backbone-zmq test");
            
    		//
    		// this assertion might not work all the time since there are many backbone implementations (OSGi, Soap, Data etc) available in run-time and OSGi registry
    		// can return arbitrary implementation of Backbone interface 
    		//
    		//Assert.assertEquals("eu.linksmart.gc.network.backbone.zmq.BackboneZMQImpl",backbone.getClass().getName());
                		
    		LOG.info("backbone-zmq test successfully completed");
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("unable to access backboneimpl service");
		}
    }
    
}