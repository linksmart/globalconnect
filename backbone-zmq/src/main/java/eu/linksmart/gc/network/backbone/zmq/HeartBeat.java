package eu.linksmart.gc.network.backbone.zmq;

import org.zeromq.ZMQ;

public class HeartBeat extends Thread {
	
	private ZMQ.Context context = null;
	private ZMQ.Socket publisher = null;
	private String peerID;
	private static int sleepTime = 5000;
	private boolean isRunning = false;
	
	public HeartBeat(String peerID, String uri) {
		this(peerID, uri, sleepTime);
	}
	
	public HeartBeat(String peerID, String uri, int sleepTimer) {
		this.peerID = peerID;
		sleepTime = sleepTimer;
		isRunning = true;
		context = ZMQ.context(1);
		publisher = context.socket(ZMQ.PUB);
		publisher.connect(uri); 
	}
	
    public void run() {
    	while(isRunning) {
    		//System.out.println("[" + this.peerID + "] is sending hearbeat to proxy");
			publisher.sendMore("HEARTBEAT");
			publisher.sendMore("0x03");
			publisher.sendMore("" + System.currentTimeMillis());
			publisher.sendMore(this.peerID);
			publisher.send("", 0);
			try {Thread.sleep(sleepTime);} catch (InterruptedException e) {}
    	}
    	publisher.setLinger(100);
    	publisher.close();
		context.term();
		System.out.println("[" + peerID + "] hearbeat is stopped");
    }
    
    public void setIsRunning(boolean flag) {
    	this.isRunning = flag;
    }
}
