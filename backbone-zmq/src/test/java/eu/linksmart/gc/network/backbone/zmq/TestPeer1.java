package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;
import org.zeromq.ZMQ;

public class TestPeer1 {
	
	String peerID = "PEER-1";
	String xsub_uri = "tcp://gando.fit.fraunhofer.de:6000";
	String xpub_uri = "tcp://gando.fit.fraunhofer.de:6001";
	
	@Test
	public void testPeer1() {
		
		try {
			
			//
			// sending heartbeat messages
			//
			HeartBeat heartBeat = new HeartBeat(peerID, xsub_uri);
			heartBeat.start();
			
			//
			// prepare context
			//
			ZMQ.Context context = ZMQ.context(1);
			
			//
			// prepare subscriber to receive (just one) broadcast message from PEER-2
			//
			ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
			subscriber.connect(xpub_uri); 
			subscriber.subscribe(ZmqConstants.BROADCAST_TOPIC.getBytes());
			System.out.println("[" + peerID + "] is subscribed to topic: BROADCAST");
			
			Thread.sleep(100);
			
			//
			// read braodcast message & quit the loop
			//
			String otherPeerID = null;
			boolean broadcastMsgReceived = false;
			while (!broadcastMsgReceived) {
				String topic = subscriber.recvStr();
				String type = new String(subscriber.recvStr());
				String time = subscriber.recvStr();
				String sender = subscriber.recvStr();
				String contents = subscriber.recvStr();
				if(topic.equals(ZmqConstants.BROADCAST_TOPIC)) {
					System.out.println("recieved broadcast message from [" + sender + "] - topic: " + topic + " - type: " + type + " - time: " + time + " - contents : " + contents);
					otherPeerID = sender;
					broadcastMsgReceived = true;
				}
			}
			subscriber.close();
			
			//
			// prepare publisher
			//
			ZMQ.Socket publisher = context.socket(ZMQ.PUB);
			publisher.connect(xsub_uri); 
			
			Thread.sleep(100);
			
			//
			// sending broadcast message
			//
			System.out.println("[" + peerID + "] is sending a broadcast message");
			publisher.sendMore(ZmqConstants.BROADCAST_TOPIC);
			publisher.sendMore(ZmqConstants.MESSAGE_TYPE_DISCOVERY);
			publisher.sendMore("" + System.currentTimeMillis());
			publisher.sendMore(peerID);
			publisher.send(peerID + "[S1,S2]", 0);
				
			int message_counter = 1;
	
			while (message_counter <= 5) {
				//
				// sending messages to PEER-2
				//
				System.out.println("sending message to [" + otherPeerID + "] - " + message_counter);
				publisher.sendMore(otherPeerID);
				publisher.sendMore(ZmqConstants.MESSAGE_TYPE_UNICAST);
				publisher.sendMore("" + System.currentTimeMillis());
				publisher.sendMore(peerID);
				publisher.send(peerID + ":M-" + message_counter, 0);
				message_counter++;
			}
			
			publisher.setLinger(100);
			publisher.close();
			context.term();
			
			heartBeat.setIsRunning(false);
			
			System.out.println("[" + peerID + "] is going down");
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
