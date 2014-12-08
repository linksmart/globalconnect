package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;
import org.zeromq.ZMQ;

public class TestPeer1 {
	
	String peerID = "PEER-1";
	//String xsub_uri = "tcp://gando.fit.fraunhofer.de:6000";
	//String xpub_uri = "tcp://gando.fit.fraunhofer.de:6001";
	String xsub_uri = "tcp://localhost:7000";
	String xpub_uri = "tcp://localhost:7001";
	
	//@Test
	public void testPeer1() {
		
		try {
			
			System.out.println("[" + peerID + "] is going down");
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
