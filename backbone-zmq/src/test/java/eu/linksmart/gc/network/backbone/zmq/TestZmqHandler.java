package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;

public class TestZmqHandler {
	
	@Test
	public void testHandler() {
		
		try {
			ZmqHandler ph1 = new ZmqHandler();
			ph1.start();
			
			ZmqHandler ph2 = new ZmqHandler();
			ph2.start();
			
			Thread.sleep(1000);
			
			ph1.publish(ph1.createBroadcastMessage("S1".getBytes()));
			ph2.publish(ph2.createBroadcastMessage("S2".getBytes()));
			
			Thread.sleep(3000);
			
			ph1.publish(ph1.createPeerMessage(ph2.getPeerID(), "M1".getBytes()));
			
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
