package eu.linksmart.global.backbone.zmq;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import java.util.Observable;
import java.util.UUID;
import java.util.Vector;

/**
 * Created by carlos on 28.11.14.
 */
public class Client extends Observable{

    private static Logger LOG = Logger.getLogger(Client.class.getName());

    private final String peerID = UUID.randomUUID().toString();

    ZMQ.Context ctx;
    ZMQ.Socket pubSocket;
    ZMQ.Socket subSocket;

    HeartbeatThread heartbeatThread;
    SubscriberThread subscriberThread;

    Vector<String> subscriptions;


    public Client() {

        subscriptions = new Vector<String>();

        ctx = ZMQ.context(1);

        pubSocket = ctx.socket(ZMQ.PUB);
        pubSocket.connect(Constants.mXSUB);
        LOG.trace("client connected to XSUB trafficSocket : " + Constants.mXSUB);
        subSocket = ctx.socket(ZMQ.SUB);
        subSocket.connect(Constants.mXPUB);
        LOG.trace("client connected to XPUB trafficSocket : " + Constants.mXPUB);

        heartbeatThread = new HeartbeatThread();
        heartbeatThread.start();

        // subscribe to BROADCAST topic
        subscribe(Constants.BROADCAST_TOPIC);
        // subscriber thread, lazy start later
        subscriberThread = new SubscriberThread();

        LOG.info("client "+this.peerID+" initialized.");

    }

    // close thread and resources
    public void finalize() {

        // stop heartbeat
        heartbeatThread.interrupt();

        // unsubscribe topics
        for(String aTopic : subscriptions){
            unsubscribe(aTopic);
        }
        // stop subscriber thread
        subscriberThread.interrupt();

        // clean up the IO
        pubSocket.close();
        LOG.trace("PUB trafficSocket closed.");
        subSocket.close();
        LOG.trace("SUB trafficSocket closed.");
        ctx.term();
        LOG.trace("ZMQ context terminated.");
    }
    public String getPeerID(){
        return this.peerID;
    }
    public void stopClient(){
        // stop heartbeat
        heartbeatThread.interrupt();

        // unsubscribe topics
        for(String aTopic : subscriptions){
            unsubscribe(aTopic);
        }
        // stop subscriber thread
        subscriberThread.interrupt();

        // clean up the IO
        pubSocket.close();
        LOG.trace("PUB trafficSocket closed.");
        subSocket.close();
        LOG.trace("SUB trafficSocket closed.");
        ctx.term();
        LOG.trace("ZMQ context terminated.");
    }

    public void subscribe(String topic){

        // start subscriber thread if not running
        if(!subscriberThread.isAlive()){
            subscriberThread.start();
        }
        subSocket.subscribe(topic.getBytes());
        subscriptions.add(topic);
        LOG.info("subscribed to : " + topic);

    }
    public void unsubscribe(String topic){

        if(subscriptions.contains(topic)){
            subSocket.unsubscribe(topic.getBytes());
            subscriptions.remove(topic);
            LOG.info("un-subscribed : "+topic);
        }
    }


    public void publish(byte[] payload) {
        byte[] serializedUnixTime = Message.serializeTimestamp();
        pubSocket.sendMore(peerID);
        pubSocket.sendMore(new byte[]{Constants.MSG_UNICAST});
        pubSocket.sendMore(serializedUnixTime);
        pubSocket.sendMore(peerID);
        pubSocket.send(payload);
        LOG.debug("message published");
    }

    private void heartbeat() {
        byte[] serializedUnixTime = Message.serializeTimestamp();
        pubSocket.sendMore(Constants.HEARTBEAT_TOPIC);
        pubSocket.sendMore(new byte[]{Constants.MSG_HEARTBEAT});
        pubSocket.sendMore(serializedUnixTime);
        pubSocket.sendMore(peerID);
        pubSocket.send("".getBytes());
        LOG.debug("heartbeat send");
    }
    public void discovery() {

        byte[] serializedUnixTime = Message.serializeTimestamp();
        pubSocket.sendMore(Constants.BROADCAST_TOPIC);
        pubSocket.sendMore(new byte[]{Constants.MSG_DISCOVERY});
        pubSocket.sendMore(serializedUnixTime);
        pubSocket.sendMore(peerID);
        pubSocket.send("".getBytes());
        LOG.debug("discovery send");
    }

    private class HeartbeatThread extends Thread {
        @Override
        public void run() {
            LOG.debug("heartbeat thread started.");
            try {
                while (true) {
                    heartbeat();
                    Thread.sleep(Constants.HEARTBEAT_INTERVAL);
                }
            } catch (InterruptedException ex) {
                LOG.debug("heartbeat thread interrupted.");
            }
        }
    }
    private class SubscriberThread extends Thread {
        @Override
        public void run() {
            Message aMessage = new Message();
            LOG.debug("subscriber thread started.");
            while (!Thread.currentThread ().isInterrupted ()) {
                    aMessage.topic = new String(subSocket.recv());
                    LOG.trace("client subscriber thread received topic : "+aMessage.topic);
                    if(aMessage.topic.equals(Constants.BROADCAST_TOPIC)){
                        LOG.trace("BROADCAST topic received");
                        aMessage.type = subSocket.recv()[0];
                        aMessage.timestamp = Message.deserializeTimestamp(subSocket.recv());
                        aMessage.sender = new String(subSocket.recv());
                        aMessage.payload = subSocket.recv();
                        Message.printMessage(aMessage);
                        // remove subscription on PEER DOWN
                        if(aMessage.type==Constants.MSG_PEERDOWN){
                            LOG.debug("received PEER DOWN for : "+aMessage.sender);
                            if(subscriptions.contains(aMessage.sender)){
                                unsubscribe(aMessage.sender);
                            }
                        }else{
                            LOG.warn("unknown broadcast type : "+aMessage.type);
                        }
                    }
                    else if(Message.isUUID(aMessage.topic)){
                        LOG.trace("UNICAST topic received");
                        // TODO in case the client sends a valid UUID as topic but no proper message, the routine will fail
                        // TODO better handling or format specification required
                        aMessage.type = subSocket.recv()[0];
                        aMessage.timestamp = Message.deserializeTimestamp(subSocket.recv());
                        aMessage.sender = new String(subSocket.recv());
                        aMessage.payload = subSocket.recv();
                        Message.printMessage(aMessage);
                        // notify observers about new message from subscribed topics
                        notifyObservers(aMessage);
                    }
                    else{
                        LOG.warn("unknown topic detected.");
                        // receive crap from unknown topic
                        while(subSocket.hasReceiveMore()){
                            subSocket.recv();
                            LOG.trace("unknown message part ignored.");
                        }
                    }
            }
            LOG.debug("subscriber thread interrupted.");
        }
    }
}
