package eu.linksmart.global.backbone.zmq.tests;

import org.zeromq.ZMQ;

import java.util.UUID;

/**
 * Created by carlos on 25.11.14.
 */
public class testingSubscriber {


    public static void main (String[] args) throws InterruptedException {

        SubThread t = new SubThread();
        t.start();
        Thread.sleep(30000);


    }
    private static class SubThread extends Thread{


        private static final String senderID = UUID.randomUUID().toString();

        @Override
        public void run(){

            System.out.println("sub thread started");
            ZMQ.Context ctx = ZMQ.context(1);

            ZMQ.Socket socket = ctx.socket(ZMQ.SUB);
            //socket.su
            socket.connect("tcp://localhost:7001");
            System.out.println("sub thread connected to : tcp://localhost:7001");

            //socket.subscribe("1518016c-5db1-4f69-a405-de40a8c2d0b9".getBytes());
            socket.subscribe("XXXXX".getBytes());
            System.out.println("sub thread subscribed to " + "1518016c-5db1-4f69-a405-de40a8c2d0b9");

            while(!Thread.currentThread ().isInterrupted ()){
                String address = socket.recvStr();
                String content = socket.recvStr();
                System.out.println("sub thread recieved : "+address + " : " + content);

            }
            socket.close();
            ctx.term();



        }

    }


}
