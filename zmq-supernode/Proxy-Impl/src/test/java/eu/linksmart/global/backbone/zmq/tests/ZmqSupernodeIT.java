package eu.linksmart.global.backbone.zmq.tests;

import eu.linksmart.global.backbone.zmq.Client;
import eu.linksmart.global.backbone.zmq.Constants;
import eu.linksmart.global.backbone.zmq.Message;
import eu.linksmart.global.backbone.zmq.Proxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Observable;
import java.util.Observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by carlos on 05.12.14.
 */
public class ZmqSupernodeIT {


    public class SubscribtionObserver implements Observer{
        public boolean received = false;

        @Override
        public void update(Observable observable, Object o) {
            received = true;
            System.out.println("SUBSCRIPTION notification arrived");
            System.out.println("Object    : "+((Message)o).topic);
            System.out.println("Observable: "+observable.toString());
        }
    }

    private Proxy proxy;

    @Before
    public void initialize(){
        proxy = new Proxy();
        proxy.startProxy();

    }

    @Test
    public void testSubscriptionWorkflow() throws InterruptedException {

        SubscribtionObserver so = new SubscribtionObserver();

        Client c1,c2;
        c1 = new Client();
        c2 = new Client();
        // subscribe client #2 to topics from client #1
        c2.subscribe(c1.getPeerID());
        // in case client #2 received something from client #1 observer will be notified
        c2.addObserver(so);
        Thread.sleep(1000);
        c1.publish("ABC".getBytes());
        c1.publish("BCD".getBytes());
        Thread.sleep(5000);
        assertTrue("no subscribtion received !", so.received);
        c1.stopClient();
        c2.stopClient();
    }

    @Test
    public void testPeerdownWorkflow() throws InterruptedException {

        SubscribtionObserver so = new SubscribtionObserver();

        Client c1,c2;
        c1 = new Client();
        c2 = new Client();
        Thread.sleep(5000);
        assertEquals(2, proxy.getNumberOfPeers());
        c1.publish("ABC".getBytes());
        c1.publish("BCD".getBytes());
        c2.stopClient();
        c1.stopClient();

        Thread.sleep(Constants.HEARTBEAT_TIMEOUT+1000);
        assertEquals(0,proxy.getNumberOfPeers());

    }

    @After
    public void shutdown() throws InterruptedException {
        try {
            proxy.stopProxy();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
