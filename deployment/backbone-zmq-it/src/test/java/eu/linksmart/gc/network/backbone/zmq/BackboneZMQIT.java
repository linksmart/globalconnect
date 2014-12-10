package eu.linksmart.gc.network.backbone.zmq;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import eu.linksmart.it.utils.ITConfiguration;
import eu.linksmart.network.NMResponse;
import eu.linksmart.network.VirtualAddress;
import eu.linksmart.network.backbone.Backbone;

@RunWith(PaxExam.class)
public class BackboneZMQIT {
	
	@Inject
    private Backbone backbone;

    @Configuration
    public Option[] config() {
        return new Option[] {
        		ITConfiguration.regressionDefaults(),
        		features("mvn:eu.linksmart.gc.features/linksmart-gc-features/0.0.1-SNAPSHOT/xml/features","backbone-zmq"),  
        };
    }
    
    @Test
    public void testBackbone() {
    	
    	try {
    		
    		System.out.println("starting backbone-zmq test");
            
    		Assert.assertEquals("eu.linksmart.gc.network.backbone.zmq.BackboneZMQImpl",backbone.getClass().getName());
            
            VirtualAddress senderVA = new VirtualAddress();
			VirtualAddress receiverVA = new VirtualAddress();
			
			backbone.broadcastData(senderVA, "S1".getBytes());
			NMResponse response = backbone.sendDataAsynch(senderVA, receiverVA, "M1".getBytes());
			System.out.println("response: " + response.getMessage());
			
			System.out.println("backbone-zmq test successfully completed");
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("unable to access backboneimpl service");
		}
    }
}