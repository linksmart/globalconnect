package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;

public class TestZmqHandler {
	
	String p1 = "P1";
	String p2 = "P2";
	
	@Test
	public void testHandler() {
		
		try {
			ZmqHandler peer1 = new ZmqHandler("P1");
			peer1.start();
			
			ZmqHandler peer2 = new ZmqHandler("P2");
			peer2.start();
			
			Thread.sleep(1000);
			
			peer1.publish(peer1.createBroadcastMessage("S1-VAD".getBytes()));
			peer2.publish(peer1.createBroadcastMessage("S2-VAD".getBytes()));
			
			Thread.sleep(3000);
			
			peer1.publish(peer1.createPeerMessage(peer2.getPeerID(), "M1".getBytes()));
			
			Thread.sleep(3000);
			
			peer1.stop();
			peer2.stop();
			
			Thread.sleep(3000);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
}
