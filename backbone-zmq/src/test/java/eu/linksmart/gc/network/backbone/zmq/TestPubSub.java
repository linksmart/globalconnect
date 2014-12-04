package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;
import org.zeromq.ZMQ;

public class TestPubSub {
	
	//@Test
	public void testHelloClient() {
		try {
			ZMQ.Context context = ZMQ.context(1);
			// Socket to talk to server
			System.out.println("Connecting to hello world server...");
			ZMQ.Socket requester = context.socket(ZMQ.REQ);
			requester.connect("tcp://localhost:5555");
			for (int requestNbr = 0; requestNbr != 10; requestNbr++) {
			String request = "Hello";
			System.out.println("Sending Hello " + requestNbr);
			requester.send(request.getBytes(), 0);
			byte[] reply = requester.recv(0);
			System.out.println("Received " + new String(reply) + " " + requestNbr);
			}
			requester.close();
			context.term();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//@Test
	public void testSub() {
		try {
			// Prepare our context and subscriber
			ZMQ.Context context = ZMQ.context(1);
			ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
			subscriber.connect("tcp://localhost:5563");
			subscriber.subscribe("B".getBytes());
			while (!Thread.currentThread ().isInterrupted ()) {
			// Read envelope with address
			String address = subscriber.recvStr();
			// Read message contents
			String contents = subscriber.recvStr();
			System.out.println(address + " : " + contents);
			}
			subscriber.close ();
			context.term ();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	//@Test
	public void testPubSub() {
		
		ZMQ.Context scontext = ZMQ.context(1);
		// Socket to talk to server
		System.out.println("Collecting updates from publisher");
		ZMQ.Socket subscriber = scontext.socket(ZMQ.SUB);
		subscriber.connect("tcp://localhost:5556");
		String filter = "10000";
		subscriber.subscribe(filter.getBytes(ZMQ.CHARSET));
		while (!Thread.currentThread ().isInterrupted ()) {
			String string = subscriber.recvStr(0).trim();
			System.out.println("received update: " + string);
		}
		subscriber.close();
		scontext.term();
		
	}
}
