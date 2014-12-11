package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;

import eu.linksmart.network.VirtualAddress;

public class TestZmqHandler {
	
	@Test
	public void testHandler() {
		
		try {
			
			BackboneZMQImpl zmqBackbone = new BackboneZMQImpl();
			
			ZmqHandler ph1 = new ZmqHandler(zmqBackbone);
			ph1.start();
			
			ZmqHandler ph2 = new ZmqHandler(zmqBackbone);
			ph2.start();
			
			Thread.sleep(1000);
			
			VirtualAddress senderVA = new VirtualAddress();
			VirtualAddress receiverVA = new VirtualAddress();
			
			BackboneMessage ph1_b = new BackboneMessage(senderVA, null, "S1".getBytes());
			ph1.broadcast(ph1_b);
			
			BackboneMessage ph2_b = new BackboneMessage(receiverVA, null, "S2".getBytes());
			ph2.broadcast(ph2_b);
			
			Thread.sleep(3000);
			
			BackboneMessage ph1_m = new BackboneMessage(senderVA, receiverVA, "M1".getBytes());
			ph1.send(ph1_m, false);
			
			Thread.sleep(3000);
			
			ph1.stop();
			
			Thread.sleep(10000);
			
			ph2.stop();
			
			Thread.sleep(3000);
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
}