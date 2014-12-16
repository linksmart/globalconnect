package eu.linksmart.gc.network.backbone.zmq;

import java.util.HashSet;
import java.util.Set;

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
import eu.linksmart.network.NMResponse;
import eu.linksmart.network.Registration;
import eu.linksmart.network.ServiceAttribute;
import eu.linksmart.network.VirtualAddress;
import eu.linksmart.network.backbone.Backbone;
import eu.linksmart.network.networkmanager.NetworkManager;
import eu.linksmart.utils.Part;

@RunWith(PaxExam.class)
public class BackboneZMQIT {
	
	private static Logger LOG = Logger.getLogger(BackboneZMQIT.class.getName());
	
	@Inject
	NetworkManager networkManager;
	
	@Inject
    private Backbone backbone;
	
	private boolean isMessageSent = false;

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
    		
    		System.out.println("starting backbone-zmq test");
            
//    		Assert.assertEquals("eu.linksmart.gc.network.backbone.zmq.BackboneZMQImpl",backbone.getClass().getName());
//            
//            VirtualAddress senderVA = new VirtualAddress();
//			VirtualAddress receiverVA = new VirtualAddress();
//			
//			backbone.broadcastData(senderVA, "S1".getBytes());
//			NMResponse response = backbone.sendDataAsynch(senderVA, receiverVA, "M1".getBytes());
//			System.out.println("response: " + response.getMessage());
    		
    		while (!(isMessageSent)) {
    			sendMessageoverNM();
    			Thread.sleep(10000);
			}
			
			System.out.println("backbone-zmq test successfully completed");
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("unable to access backboneimpl service");
		}
    }
    
    private void sendMessageoverNM() {
    	
    	VirtualAddress nm_virtualAddress = null;
		try {
			
			Registration[] services = networkManager.getServiceByAttributes(new Part[]{new Part(ServiceAttribute.DESCRIPTION.name(), "NetworkManager:LinkSmartUser")});
			if (services == null || services.length == 0) {
				return;
			}
			if(services.length == 1 && services[0].getVirtualAddress().equals(networkManager.getVirtualAddress())) {
				return;
			}
			for (int i = 0; i < services.length; i++) {
				Registration service = services[i];
				if(!(service.getVirtualAddress().equals(networkManager.getVirtualAddress()))) {
					nm_virtualAddress = service.getVirtualAddress();
					break;
				}
			}
			if(nm_virtualAddress == null) {
				return;
			}
				
		} catch (Exception e1) {
			LOG.error("unable to get NetworkManager virtual address by attributes", e1);
			return;
		}
		
		Registration[] services = null;
		try {
			services = networkManager.getServiceByAttributes(new Part[]{new Part(ServiceAttribute.DESCRIPTION.name(), "CalculatorForBeginners")});
			if (services == null || services.length == 0) {
				LOG.info("calculator service was not found in network-manager registrations.");
				return;
			}
		} catch (Exception e1) {
			LOG.error("unable to get calculator service by attributes from NetworkManager", e1);
			return;
		}
		
		LOG.info("calculator service found with virtual address: " + services[0].getVirtualAddressAsString());
		
		try {
			Set<Registration> rservices = new HashSet<Registration>();
			//Registration registration = new Registration(new VirtualAddress(), new Part[]{new Part(ServiceAttribute.DESCRIPTION.name(),"desc")});
			rservices.add(services[0]);
			byte[] servicesBytes = eu.linksmart.utils.ByteArrayCodec.encodeObjectToBytes(rservices);
			//eu.linksmart.network.Message message = new eu.linksmart.network.Message(eu.linksmart.network.Message.TOPIC_APPLICATION, new VirtualAddress(), new VirtualAddress(), servicesBytes);
			isMessageSent = true;
			NMResponse response = networkManager.sendData(networkManager.getVirtualAddress(),nm_virtualAddress, servicesBytes, true);
			LOG.info("status: " + response.getStatus() + " - message: " + response.getMessage());
		} catch (Exception e) {
			LOG.error("error in sending message over network manager", e);
		}
    }
}