package eu.linksmart.gc.network.backbone.zmq;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// checks periodically for peer heartbeat timeouts
    public class HeartbeatWatch extends Thread {
    private static Logger LOG = Logger.getLogger(HeartbeatWatch.class.getName());
    private ZmqHandler zmqHandler = null;
    private Boolean ok = false, alive= true, online = true;


        public HeartbeatWatch(ZmqHandler zmqHandler) {

            this.zmqHandler=zmqHandler;
        }
        @Override
        public void run() {
            boolean aux, wasConnected= false;


            try {
                sleep(ZmqConstants.HEARTBEAT_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int tolerance = 2, n=0, retry=0;

            do{
                do {
                    synchronized (ok){
                        aux = ok;
                        ok =false;
                    }
                    if(aux) {
                        n=0;
                        retry=0;
                        try {
                            wasConnected =false;
                            sleep(ZmqConstants.HEARTBEAT_TIMEOUT);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }while (aux);

                n++;
                if(n>=tolerance) {
                    synchronized (alive) {
                        alive = false;
                    }
                    if (wasConnected) {
                        LOG.error("Super node connection lost!");
                    }else{
                        LOG.error("It is not possible to establish connection with the Supernode!");
                    }

                    if(retry<5){
                        LOG.info("Trying to reconnect...");
                        zmqHandler.restart();
                    }else
                    retry++;
                }else
                    LOG.warn("A heartbeat was missed!");


                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }while (online);


        }

        public synchronized void beat(){
            synchronized (ok){
                ok =true;
            }
        }

    public  boolean isBeating(){

        return alive;
    }

    public void logoff(){
        online=false;
    }


    }