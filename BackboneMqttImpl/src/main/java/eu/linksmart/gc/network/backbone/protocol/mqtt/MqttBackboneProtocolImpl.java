package eu.linksmart.gc.network.backbone.protocol.mqtt;


import eu.linksmart.gc.api.network.*;
import eu.linksmart.gc.network.backbone.protocol.mqtt.conf.MqttBackboneProtocolConfigurator;
import eu.linksmart.gc.utils.mqtt.broker.StaticBroker;
import eu.linksmart.gc.api.types.Configurable;
import eu.linksmart.gc.utils.mqtt.types.MqttMessage;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class MqttBackboneProtocolImpl implements Observer, Configurable {


    private MqttBackboneProtocolConfigurator conf;

    private Logger LOG = Logger.getLogger(MqttBackboneProtocolImpl.class.getName());


    // this objects map how many clients/vad are hearing the same topic
    private Map<String, Set<VirtualAddress>> listeningVirtualAddresses = new HashMap<>();

    // this objects map how many clients/vad are hearing the same topic
    private Map<String, Set<VirtualAddress>> listeningWithWildcardVirtualAddresses = new HashMap<>();


    // controls if one message had been sent already
    private static final Map<String,Boolean> MessageHashControl = new HashMap<>();
    // keep clean the MessageHashControl structure
    private Thread mapCleaner;
    // this is the unique identifier of this Backbone Protocol
    private final UUID MQTTProtocolID = UUID.randomUUID();

    private static final Map<Integer,Map<String, Boolean>> repetitionControl = new HashMap<>();
    private MessageDigest md5 = null;

    private StaticBroker brokerService;
    private MQTTProtocolActivator backbone;



    public MqttBackboneProtocolImpl(MqttBackboneProtocolConfigurator configurator,StaticBroker brokerService, MQTTProtocolActivator backbone) throws Exception {

    	this.conf = configurator;

        startKeepCleanMessageControl();

        try {
            LOG.info("Starting broker main client with name:" +MQTTProtocolID.toString());

            this.backbone = backbone;
            this.brokerService =brokerService;

            if(!brokerService.isConnected())
                brokerService.connect();

            startBroadcastPropagation();

        } catch (Exception e) {
            LOG.error("MQTT-BBP-Impl starting error:"+e.getMessage(),e);

            throw new Exception(e);
        }



    }

    protected void startBroadcastPropagation(){
        try {
            if (!Boolean.valueOf(conf.get(conf.BROKER_AS_SERVICE)))
                throw new UnsupportedOperationException("The  broadcast propagation service is just available in combination with the Broker as a service setting");

            brokerService.addListener(conf.get(conf.BROADCAST_TOPIC),this);

            }catch (Exception e){
                LOG.error("Error while starting Broadcast Propagation service: "+e.getMessage(),e);
        }
    }



    /**
     * TODO add description
     * */
    void startKeepCleanMessageControl(){

        if (Boolean.valueOf(conf.get(MqttBackboneProtocolConfigurator.MESSAGE_REPETITION_CONTROL))) {
            initMessageRepetitionControl();
        }

        mapCleaner = new Thread(){
            private Boolean alive = true;

            public void run(){
                while (alive){
                    synchronized (MessageHashControl) {
                        if (!MessageHashControl.isEmpty()) {
                            // for all messages already sent
                            for (String i : MessageHashControl.keySet()) {

                                // if I is the first time the thread knows about the message then mark to be erase the next cycle
                                if (MessageHashControl.get(i))

                                    MessageHashControl.put(i, false);
                                else // if was mark to be erased then erase it now!

                                    MessageHashControl.remove(i);
                            }
                        }
                    }
                    if (Boolean.valueOf(conf.get(MqttBackboneProtocolConfigurator.MESSAGE_REPETITION_CONTROL)))
                        synchronized (repetitionControl) {
                            if (!repetitionControl.isEmpty()) {
                                // for all messages already sent
                                for (Integer i : repetitionControl.keySet()) {
                                    Map<String, Boolean> ii = (Map<String,Boolean>)repetitionControl.get(i);

                                    for (String j : ii.keySet()) {
                                        // if I is the first time the thread knows about the message then mark to be erase the next cycle
                                        if (ii.get(j))

                                            ii.put(j, false);
                                        else // if was mark to be erased then erase it now!

                                            ii.remove(j);
                                    }

                                }
                            }
                        }
                    try {
                        this.sleep(Integer.valueOf(conf.get(MqttBackboneProtocolConfigurator.MESSAGE_CONTROL_CLEANER_TIMEOUT)));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            public void setStop(){
                alive = false;
            }
        };
        mapCleaner.start();
    }
    void initMessageRepetitionControl(){
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e.getMessage(),e);
            conf.setConfiguration(MqttBackboneProtocolConfigurator.MESSAGE_REPETITION_CONTROL,"false");
        }
    }

	public synchronized void destroy() {

        LOG.info("[Backbone MqttProtocol stopped]");
        try {
            brokerService.destroy();

        }catch (Exception e){
            LOG.error(e.getMessage(),e);
        }

	}



    /**
     * TODO add description
     * */



    public synchronized void subscribe( String topic ) throws Exception {
        // if there is no listener in this topic add one
        brokerService.addListener(topic,this);

    }

    public synchronized void  publish(MqttMessage ms) throws Exception {

            // if the message is not repartition or local,and I successful recovered then is published.
            if(ms != null && uniqueMessagePerTopic(ms) && uniqueMessageControl(ms))
                brokerService.publish(ms.getTopic(),ms.getPayload(),ms.getQoS(),ms.isRetained());
            else
                LOG.warn("Message with topic "+ms.getTopic()+" discarded considered duplicated");

    }
    boolean uniqueMessagePerTopic(MqttMessage ms){
        boolean send = true;

        if(!ms.isGenerated())
            // if the local loop is not allowed, check if this message was sent by this protocol, otherwise don't test if the message is local.
            if(!ms.getOriginProtocol().equals(MQTTProtocolID)  || (Boolean.valueOf(conf.get(MqttBackboneProtocolConfigurator.ALLOWED_LOCAL_MESSAGING_LOOP)))){
                // check if this message was already send
                synchronized (MessageHashControl){
                    // if was sent, mark to no send it again
                    if(MessageHashControl.containsKey(ms.getMessageHash()))
                        send = false;
                    else // if not send yet, add it in the sent messages
                        MessageHashControl.put(ms.getMessageHash(),true);
                }
            }else {
                LOG.info("A message was discarded because was generated by the same MQTT Protocol");
                send =false;
            }

        return send;
    }
 /*   private byte[] concatenateBytes(byte[] a, byte[] b){
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }*/

    private byte[] concatenateBytes(byte[]... bytes){
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

            for( byte[] i : bytes)
                outputStream.write( i );


        return  outputStream.toByteArray( );
        } catch (IOException e) {
            LOG.error(e.getMessage(),e);
        }
        return null;
    }
    private boolean uniqueMessageControl(MqttMessage ms){
        boolean send = true;


            if (Boolean.valueOf(conf.get(MqttBackboneProtocolConfigurator.MESSAGE_REPETITION_CONTROL))){
                String hash = (new BigInteger(1,md5.digest(concatenateBytes(ms.getTopic().getBytes(), ms.getPayload())))).toString();
                synchronized (repetitionControl){

                    if(repetitionControl.containsKey(ms.getPayload().length)) {
                       Map<String,Boolean> selected =(Map<String,Boolean>)repetitionControl.get(ms.getPayload().length);

                        if (selected.containsKey(hash))
                            // if was sent, mark to no send it again
                            send = false;
                        else
                            selected.put(hash,true);


                    }else { // if not send yet, add it in the sent messages
                        repetitionControl.put(ms.getPayload().length, new HashMap<String, Boolean>());
                        repetitionControl.get(ms.getPayload().length).put(hash,true);
                    }
                }
            }

        return send;

    }
  /*  private String list(VirtualAddress senderVAD, String uriEndpoint) throws Exception {
        return (new Gson()).toJson(listeningVirtualAddresses.entrySet());


    }*/
    
    public synchronized void unsubscribe( String topic) throws Exception {


            if (listeningVirtualAddresses.containsKey(topic)) {

                listeningVirtualAddresses.remove(topic);
                if( listeningVirtualAddresses.isEmpty()) {
                    brokerService.removeListener(topic,this);
                }
            }else if (listeningWithWildcardVirtualAddresses.containsKey(topic)) {
                listeningWithWildcardVirtualAddresses.remove(topic);
                if( listeningWithWildcardVirtualAddresses.isEmpty()) {

                    brokerService.removeListener(topic,this);
                }
            } else {
                LOG.info("The unsubscription request cannot be handle because there is no sub with this topic");
            }
    }
    /**
     *
     * When a message is received by a listener is reported to the MQTT Backbone Protocol
     * Then the BBP unpack it and report received data per each VAD hearing this topic
     *
     * @param forwardingListener the listener which catch the MQTT message
     * @param mqttTunnelledMessage the catch message
     *
     *
     * */
    @Override
    public void update(Observable forwardingListener, Object mqttTunnelledMessage) {
        // recovering objects
        MqttMessage data = (MqttMessage)mqttTunnelledMessage;

        // get the VAD which this topic is subscribed
       // VirtualAddress senderVAD =endpointTopicVirtualAddress.get(conf.get(conf.BROKER_URL));

        if(data.getTopic().equals(conf.get(conf.BROADCAST_TOPIC)))
            handleBroadcast(data);
        else if (Boolean.valueOf(conf.get(MqttBackboneProtocolConfigurator.BROKER_AS_SERVICE)))
            backbone.receiveDataBrokerBase(data);
        else
            backbone.receiveDataTopicBase(data);
    }

    private void handleBroadcast(MqttMessage data){


        if(data != null  && uniqueMessageControl(data)) {

            backbone.handleBroadcast(data);
        }


    }
    /**
     * make the necessary steps needed to update the configuration of the broker
     * @param map the updated info
     *
     * */
    public void applyConfigurations(Hashtable map){
        if (Boolean.valueOf(conf.get(MqttBackboneProtocolConfigurator.MESSAGE_REPETITION_CONTROL)))
            initMessageRepetitionControl();


        if (map.containsKey(MqttBackboneProtocolConfigurator.BROKER_NAME)|| map.containsKey(MqttBackboneProtocolConfigurator.BROKER_PORT)) {
            try {

                if (map.containsKey(MqttBackboneProtocolConfigurator.BROKER_NAME))
                    brokerService.setBrokerName(map.get(MqttBackboneProtocolConfigurator.BROKER_NAME).toString());
                if (map.containsKey(MqttBackboneProtocolConfigurator.BROKER_PORT))
                    brokerService.setBrokerPort(map.get(MqttBackboneProtocolConfigurator.BROKER_PORT).toString());
            }catch (Exception e){
                LOG.error("Error while updating broker configuration :"+e.getMessage(),e);
            }
        }





        LOG.info("Configuration changes applied!");
    }


}
