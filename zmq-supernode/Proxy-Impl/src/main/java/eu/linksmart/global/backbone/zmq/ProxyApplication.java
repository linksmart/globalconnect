package eu.linksmart.global.backbone.zmq;

import org.apache.log4j.Logger;

/**
 * Created by carlos on 03.12.14.
 */
public class ProxyApplication {

    private static Logger LOG = Logger.getLogger(ProxyApplication.class.getName());

    public static void main (String[] args) throws InterruptedException {

        // initialize proxy
        Proxy proxy = new Proxy();
        // attach shutdown hook
        ShutdownHook sh = new ShutdownHook(proxy);
        Runtime.getRuntime().addShutdownHook(sh);
        LOG.debug("shutdown hook registered.");
        // start proxy
        LOG.info("starting ZMQ backbone...");
        proxy.startProxy();

        while(true){
            Thread.sleep(30000);
            LOG.trace("ZMQ proxy thread alive       : "+proxy.proxyThreadAlive());
            LOG.trace("traffic watch thread alive   : "+proxy.trafficWatchAlive());
            LOG.trace("heartbeat watch thread alive : "+proxy.heartbeatWatchAlive());
        }
    }
    public static class ShutdownHook extends Thread{
            Proxy mProxy;
            public ShutdownHook(Proxy aProxy){
                mProxy = aProxy;
            }
            @Override
            public void run() {
                    LOG.warn("CTRL-C intercepted");
                    mProxy.stopProxy();
                    LOG.info("ZMQ backbone terminated");
            }
    }
}

