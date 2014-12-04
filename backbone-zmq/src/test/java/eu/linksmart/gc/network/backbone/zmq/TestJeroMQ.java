package eu.linksmart.gc.network.backbone.zmq;

import java.util.UUID;

import org.junit.Test;
import org.zeromq.ZMQ;

public class TestJeroMQ {
	
	//@Test
	public void testHelloServer() {
		try {
			ZMQ.Context context = ZMQ.context(1);
			// Socket to talk to clients
			ZMQ.Socket responder = context.socket(ZMQ.REP);
			responder.bind("tcp://*:5555");
			while (!Thread.currentThread().isInterrupted()) {
			// Wait for next request from the client
			byte[] request = responder.recv(0);
			System.out.println("Received Hello");
			// Do some 'work'
			Thread.sleep(1000);
			// Send reply back to client
			String reply = "World";
			responder.send(reply.getBytes(), 0);
			}
			responder.close();
			context.term();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//@Test
	public void testPub() {
		try {
			// Prepare our context and publisher
			ZMQ.Context context = ZMQ.context(1);
			ZMQ.Socket publisher = context.socket(ZMQ.PUB);
			publisher.bind("tcp://*:5563");
			while (!Thread.currentThread().isInterrupted()) {
			// Write two messages, each with an envelope and content
			publisher.sendMore("A");
			publisher.send ("We don't want to see this");
			publisher.sendMore("B");
			publisher.send("We would like to see this");
			Thread.sleep(5000);
			}
			publisher.close ();
			context.term ();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	//@Test
	public void testJeroMQ() {
		try {
			// Prepare our context and publisher
			ZMQ.Context context = ZMQ.context(1);
			ZMQ.Socket publisher = context.socket(ZMQ.PUB);
			publisher.bind("tcp://*:5556");
			while (!Thread.currentThread ().isInterrupted ()) {
				String update = String.format("%05d %d %d", 10000, 25, 50);
				System.out.println("publishing: " + update);
				publisher.send(update, 0);
				Thread.sleep(5000);
			}
			publisher.close ();
			context.term ();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
