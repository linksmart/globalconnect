package eu.linksmart.global.backbone.zmq;

import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by carlos on 28.11.14.
 */
public class Proxy {

    private static Logger LOG = Logger.getLogger(Proxy.class.getName());


    private TrafficWatch trafficWatch;
    private HeartbeatWatch heartbeatWatch;
    private ProxyThread proxyThread;


    ConcurrentHashMap<UUID, Long> heartbeatTimestamps;

    public Proxy() {

        heartbeatTimestamps = new ConcurrentHashMap<UUID, Long>();

        System.out.println(ObjectUtils.identityToString(heartbeatTimestamps));
        //System.out.println(ObjectUtils.i

    }
    public void startProxy(){

        LOG.debug("starting threads...");
        // starting ZMQ proxy
        proxyThread = new ProxyThread();
        proxyThread.start();
        LOG.debug("proxy thread started.");

        // starting traffic watch thread
        trafficWatch = new TrafficWatch(heartbeatTimestamps);
        trafficWatch.start();
        LOG.debug("traffic watch thread started.");

        // starting heart beat thread
        heartbeatWatch = new HeartbeatWatch(heartbeatTimestamps);
        heartbeatWatch.start();
        LOG.debug("heartbeat watch thread started.");

    }

    // close thread and resources
    public void stopProxy() {

        try {
            LOG.debug("stopping proxy threads...");
            heartbeatWatch.interrupt();
            heartbeatWatch.join();
            trafficWatch.stopTrafficWatch();
            trafficWatch.join();
            proxyThread.stopProxyThread();
            //proxyThread.join();
            LOG.debug("all threads stopped.");
        } catch (InterruptedException ex) {
            LOG.debug("Wow !");
        }

    }
    public boolean proxyThreadAlive(){
        return proxyThread.isAlive();
    }
    public boolean heartbeatWatchAlive(){
        return heartbeatWatch.isAlive();
    }
    public boolean trafficWatchAlive(){
        return trafficWatch.isAlive();
    }

    private class ProxyThread extends Thread{

        private ZMQ.Context ctx;
        private ZMQ.Socket xsubSocket;
        private ZMQ.Socket xpubSocket;

        @Override
        public void run() {

            ctx = ZMQ.context(1);
            xsubSocket = ctx.socket(ZMQ.XSUB);
            xsubSocket.bind(Constants.mXSUB);
            LOG.trace("XSUB trafficSocket bound to : " + Constants.mXSUB);
            xpubSocket = ctx.socket(ZMQ.XPUB);
            xpubSocket.bind(Constants.mXPUB);
            LOG.trace("XPUB trafficSocket bound to : " + Constants.mXPUB);

            LOG.debug("starting blocking ZMQ proxy...");
            boolean result = ZMQ.proxy(xsubSocket, xpubSocket, null);

            LOG.info("ProxyThread terminated");
        }
        // TODO current JeroMQ proxy implementation hangs on during context termination. No graceful shutdown of the proxy possible
        public void stopProxyThread(){

            xpubSocket.setLinger(0);
            xpubSocket.unbind(Constants.mXPUB);
            LOG.trace("XPUB unbound..");
            xpubSocket.close();
            LOG.trace("XPUB closed.");

            xsubSocket.setLinger(0);
            xsubSocket.unbind(Constants.mXSUB);
            LOG.trace("XSUB unbound.");
            xsubSocket.close();
            LOG.trace("XSUB closed.");

            //ctx.close();
            //LOG.trace("ZMQ ctx terminated.");

        }
    }

    // analyzes traffic of the proxy
    private class TrafficWatch extends Thread {

        ZMQ.Context ctx;
        ZMQ.Socket trafficSocket;

        // thread safe hash map of heart beat timers
        ConcurrentHashMap<UUID, Long> hartbeatTimestamps = new ConcurrentHashMap<UUID, Long>();

        private Message aMessage;

        ConcurrentHashMap<UUID, Long> mPeers;


        public TrafficWatch(ConcurrentHashMap<UUID, Long> peers) {
            mPeers = peers;
            System.out.println(ObjectUtils.identityToString(peers));
            System.out.println(ObjectUtils.identityToString(mPeers));

            LOG.trace("traffic watch : peers : "+mPeers.size());
            ctx = ZMQ.context(1);
            trafficSocket = ctx.socket(ZMQ.SUB);
            LOG.trace("traffic watch : SUB trafficSocket created");
            trafficSocket.connect(Constants.mXPUB);
            LOG.trace("traffic watch : connected to proxy :" + Constants.mXPUB);
            trafficSocket.subscribe("".getBytes());
            LOG.trace("traffic watch : subscribed to everything");

            aMessage = new Message();

        }
        public void stopTrafficWatch(){
            trafficSocket.unsubscribe("".getBytes());
            trafficSocket.close();
            trafficSocket.setLinger(0);
            ctx.term();
            while(this.isAlive()) {
                LOG.trace("interrupting traffic watch...");
                this.interrupt();
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    LOG.error(e);
                }
            }
        }

        @Override
        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                LOG.debug("receiving topic...");
                try {
                    byte[] raw = trafficSocket.recv();
                    aMessage.topic = new String(raw);
                    LOG.trace("topic received : "+aMessage.topic);

                    if (aMessage.topic.equals(Constants.HEARTBEAT_TOPIC)) {
                        aMessage.type = trafficSocket.recv()[0];
                        aMessage.timestamp = Message.deserializeTimestamp(trafficSocket.recv());
                        aMessage.sender = new String(trafficSocket.recv());
                        aMessage.payload = trafficSocket.recv();
                        mPeers.put(java.util.UUID.fromString(aMessage.sender), System.currentTimeMillis());
                        LOG.debug("no of peers : " + mPeers.size());
                        Message.printMessage(aMessage);
                    } else if (aMessage.topic.equals(Constants.BROADCAST_TOPIC)) {
                        aMessage.type = trafficSocket.recv()[0];
                        aMessage.timestamp = Message.deserializeTimestamp(trafficSocket.recv());
                        aMessage.sender = new String(trafficSocket.recv());
                        aMessage.payload = trafficSocket.recv();
                        Message.printMessage(aMessage);
                    } else if (Message.isUUID(aMessage.topic)) {
                        // TODO in case the client sends a valid UUID as topic but no proper message, the routine will fail
                        // TODO better handling or format specification required
                        aMessage.type = trafficSocket.recv()[0];
                        aMessage.timestamp = Message.deserializeTimestamp(trafficSocket.recv());
                        aMessage.sender = new String(trafficSocket.recv());
                        aMessage.payload = trafficSocket.recv();
                        Message.printMessage(aMessage);
                    } else {
                        LOG.warn("unknown topic detected.");
                        // receive crap from unknown topic
                        while (trafficSocket.hasReceiveMore()) {
                            trafficSocket.recv();
                            LOG.trace("unknown message part ignored.");
                        }
                    }
                } catch (ZMQException ex) {

                    if (ex.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                        LOG.debug("received termination interrupt");
                        break;
                    }else{
                        LOG.error(ex);
                    }
                }
            }
            LOG.info("traffic watch thread terminated.");
        }
    }


    // checks periodically for peer heartbeat timeouts
    private class HeartbeatWatch extends Thread {

        ZMQ.Context ctx;
        ZMQ.Socket heartbeatSocket;
        ConcurrentHashMap<UUID, Long> mPeers;

        public HeartbeatWatch(ConcurrentHashMap<UUID, Long> peers) {


            mPeers = peers;
            System.out.println(ObjectUtils.identityToString(peers));
            System.out.println(ObjectUtils.identityToString(mPeers));
        }

        @Override
        public void run() {
            LOG.trace("heartbeat watch : peers : "+mPeers.size());

            ctx = ZMQ.context(1);
            heartbeatSocket = ctx.socket(ZMQ.PUB);
            LOG.trace("PUB trafficSocket created");
            heartbeatSocket.connect(Constants.mXSUB);
            LOG.trace("connected to proxy: " + Constants.mXSUB);

            try {
                while (true) {
                    LOG.trace("analyzing "+mPeers.size()+" peer timers...");
                    LOG.trace("peers : "+mPeers.size());
                    for (UUID sender : mPeers.keySet()) {
                        Long tstamp = mPeers.get(sender);
                        LOG.trace("peer : " + sender.toString());
                        if (System.currentTimeMillis() - tstamp > Constants.HEARTBEAT_TIMEOUT) {
                            LOG.debug("detected timeout for peer :" + sender.toString());
                            broadcastPeerdown(sender);
                            mPeers.remove(sender);
                            LOG.debug("no. of peers : " + mPeers.size());
                        }
                    }
                    this.sleep(Constants.HEARTBEAT_INTERVAL/2);
                }
            } catch (InterruptedException ex) {
                LOG.info("heartbeat thread : interrupt signal received.");
            }
            heartbeatSocket.setLinger(0);
            heartbeatSocket.close();
            ctx.close();
            LOG.info("heartbeat watch thread terminated.");
        }

        private void broadcastPeerdown(UUID aSender) {
            byte[] serializedUnixTime = Message.serializeTimestamp();
            heartbeatSocket.sendMore(Constants.BROADCAST_TOPIC);
            heartbeatSocket.sendMore(new byte[]{Constants.MSG_PEERDOWN});
            heartbeatSocket.sendMore(serializedUnixTime);
            heartbeatSocket.sendMore(aSender.toString());
            heartbeatSocket.send("".getBytes());
            LOG.info("peer down broadcasted");
        }
    }



}
