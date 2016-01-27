package eu.linksmart.gc.network.backbone.protocol.mqtt;

import eu.linksmart.gc.api.network.*;
import eu.linksmart.gc.api.network.backbone.Backbone;
import eu.linksmart.gc.api.network.networkmanager.core.NetworkManagerCore;
import eu.linksmart.gc.api.network.routing.BackboneRouter;
import eu.linksmart.gc.api.security.communication.SecurityProperty;
import eu.linksmart.gc.api.types.Configurable;
import eu.linksmart.gc.api.types.MqttTunnelledMessage;
import eu.linksmart.gc.api.types.TunnelRequest;
import eu.linksmart.gc.api.types.TunnelResponse;
import eu.linksmart.gc.api.types.utils.SerializationUtil;
import eu.linksmart.gc.api.utils.Configurator;
import eu.linksmart.gc.network.backbone.protocol.mqtt.conf.MqttBackboneProtocolConfigurator;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.felix.scr.annotations.*;
import org.apache.log4j.Logger;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.util.*;

/**
 * Created by José Ángel Carvajal on 27.01.2016 a researcher of Fraunhofer FIT.
 */
@Component(name="BackboneMQTT", immediate=true)
@Service({Backbone.class})
public class MQTTProtocolActivator implements Configurable, Backbone, MessageProcessor {
    private MqttBackboneProtocolConfigurator conf = null;
    @Reference(name="ConfigurationAdmin",
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            bind="bindConfigAdmin",
            unbind="unbindConfigAdmin",
            policy= ReferencePolicy.STATIC)
    private ConfigurationAdmin mConfigAdmin = null;



    protected void bindConfigAdmin(ConfigurationAdmin configAdmin) {
        this.mConfigAdmin = configAdmin;
    }

    protected void unbindConfigAdmin(ConfigurationAdmin configAdmin) {
        this.mConfigAdmin = null;
    }


    @Reference(name="BackboneRouter",
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            bind="bindBackboneRouter",
            unbind="unbindBackboneRouter",
            policy= ReferencePolicy.STATIC)
    private BackboneRouter bbRouter;


    protected void bindBackboneRouter(BackboneRouter bbRouter) {
        this.bbRouter = bbRouter;
    }

    protected void unbindBackboneRouter(BackboneRouter bbRouter) {
        this.bbRouter = null;
    }
    @Reference(name="NetworkManagerCore",
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            bind="bindNetworkManager",
            unbind="unbindNetworkManager",
            policy= ReferencePolicy.DYNAMIC)
    protected NetworkManagerCore networkManager;

    protected void bindNetworkManager(NetworkManagerCore networkManager) {
        LOG.debug("NetworkManagerRestPort::binding network-manager");
        this.networkManager = networkManager;
    }

    protected void unbindNetworkManager(NetworkManagerCore networkManager) {
        LOG.debug("NetworkManagerRestPort::un-binding network-manager");
        this.networkManager = null;
    }


    protected void unbindMQTTConfigurator(Configurator conf) {
        this.conf = null;
    }


    private Logger LOG = Logger.getLogger(MqttBackboneProtocolImpl.class.getName());
    // this object map Broker with VAD
    //private Map<String, VirtualAddress>  endpointTopicVirtualAddress = new HashMap<String, VirtualAddress>();
    // this object map VAD with Broker
    private BidiMap endpointVirtualAddressTopic = new DualHashBidiMap();
    // this objects map how many clients/vad are hearing the same topic
    private Map<String, Set<VirtualAddress>> listeningVirtualAddresses = new HashMap<>();

    // this objects map how many clients/vad are hearing the same topic
    private Map<String, Set<VirtualAddress>> listeningWithWildcardVirtualAddresses = new HashMap<>();


    // this is the unique identifier of this Backbone Protocol
    private final UUID MQTTProtocolID = UUID.randomUUID();

    private BrokerConnectionService brokerService;
    MqttBackboneProtocolImpl MqttBBP;
    @Activate
    protected void activate(ComponentContext context) throws Exception {
        LOG.info("[activating Backbone MQTTProtocol]");
        this.conf = new MqttBackboneProtocolConfigurator(this, context.getBundleContext(), mConfigAdmin);



        try {
            LOG.info("Starting broker main client with name:" +MQTTProtocolID.toString());

            brokerService = new BrokerConnectionService(conf.get(MqttBackboneProtocolConfigurator.BROKER_NAME),conf.get(MqttBackboneProtocolConfigurator.BROKER_PORT), MQTTProtocolID,networkManager,Boolean.valueOf(conf.get(MqttBackboneProtocolConfigurator.BROKER_AS_SERVICE)),conf.get(conf.BACKBONE_DESCRIPTION));

            brokerService.connect();
             MqttBBP = new MqttBackboneProtocolImpl(conf,brokerService, this);
            startBroadcastPropagation(this);

        } catch (Exception e) {
            LOG.error("Activating error:"+e.getMessage(),e);

            throw new Exception(e);
        }



    }
    @Deactivate
    public void deactivate(ComponentContext context) {

        LOG.info("[de-activating Backbone MqttProtocol]");
        try {
            MqttBBP.destroy();
            brokerService.destroy();

        }catch (Exception e){
            LOG.error(e.getMessage(),e);
        }
    }
    protected void startBroadcastPropagation(MessageProcessor observer){
        try {
            if (!Boolean.valueOf(conf.get(conf.BROKER_AS_SERVICE)))
                throw new UnsupportedOperationException("The  broadcast propagation service is just available in combination with the Broker as a service setting");
           else
                ((MessageDistributor)networkManager).subscribe("mqttBroadcast", observer);

        }catch (Exception e){
            LOG.error("Error while starting Broadcast Propagation service: "+e.getMessage(),e);
        }
    }

    @Override
    public NMResponse sendDataSynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data) {
        try {

            TunnelRequest tunnelRequest = recoverRequest(data,senderVirtualAddress);

            LOG.debug("method: " + tunnelRequest.getMethod());
            LOG.debug("path: " + tunnelRequest.getPath());
            LOG.debug("body: " + new String(tunnelRequest.getBody()));

            // check if service endpoint is available
            String uriEndpoint = tunnelRequest.getPath().replace("*","#");

            if (uriEndpoint == null) {
                String message = "cannot send tunneled data to service at virtualAddress: " + receiverVirtualAddress.toString() + ", unknown endpoint";
                LOG.error(message);
                return createErrorMessage(404,message);
            }

           /*if(tunnelRequest.getMethod().equals(conf.get(MqttBackboneProtocolConfigurator.LIST))) {
                String body = list(senderVirtualAddress, uriEndpoint);
                return packResponse(createListResponse(body), senderVirtualAddress, receiverVirtualAddress);
            }else {*/
            // determine HTTP method
            if (tunnelRequest.getMethod().equals(conf.get(MqttBackboneProtocolConfigurator.SUBSCRIBE_TO))) {
                if (Boolean.valueOf(conf.get(MqttBackboneProtocolConfigurator.BROKER_AS_SERVICE)))
                    subscribe(senderVirtualAddress, uriEndpoint);
                else
                    subscribe(senderVirtualAddress, receiverVirtualAddress);
            } else if (tunnelRequest.getMethod().equals(conf.get(MqttBackboneProtocolConfigurator.PUBLISH_TO))) {
                MqttBBP.publish(getSyncMessage(uriEndpoint, tunnelRequest.getBody()));
            } else if (tunnelRequest.getMethod().equals(conf.get(MqttBackboneProtocolConfigurator.UNSUBSCRIBE_TO))) {
                MqttBBP.unsubscribe(uriEndpoint);
            } else {
                throw new Exception("unsupported MQTT method for endpoint:" + uriEndpoint);
            }

            // creating & encoding LinkSmart Message Object
            return packResponse(createAcceptResponse(), senderVirtualAddress, receiverVirtualAddress);

        } catch (Exception e) {

            try {
                return  packResponse(createErrorMessage(500, e.getMessage()),senderVirtualAddress,receiverVirtualAddress);
            } catch (Exception ignored) {

            }
            finally {
                LOG.error(e.getMessage(),e);
            }
        }
        return null;
    }
    public synchronized void subscribe( VirtualAddress senderVAD, String topic) throws Exception {

        if (topic.contains("#") || topic.contains("+")) {
            addVadListener(listeningWithWildcardVirtualAddresses,senderVAD, topic);
        } else {
            addVadListener(listeningVirtualAddresses,senderVAD,topic);
        }

        MqttBBP.subscribe(topic);

    }
    public synchronized void subscribe( VirtualAddress senderVAD,VirtualAddress receiverVAD) throws Exception {

        if(endpointVirtualAddressTopic.containsKey(receiverVAD)) {
            String topic = endpointVirtualAddressTopic.get(receiverVAD).toString();

            addVadListener(listeningVirtualAddresses,senderVAD,topic);

            MqttBBP.subscribe(topic);

        }

    }
    public static void addVadListener(Map<String, Set<VirtualAddress>> listeningVirtualAddresses,VirtualAddress vad, String topic){
        Set<VirtualAddress> topicSet = listeningVirtualAddresses.get(topic);
        // create container structure if is needed
        if (topicSet == null)
            topicSet = new HashSet<VirtualAddress>();
        listeningVirtualAddresses.put(topic, topicSet);

        // add a virtual address to the listeners of this topic
        topicSet.add(vad);

    }
    /**
     * TODO add description
     * */
    public MqttTunnelledMessage getAsyncMessage( byte[] data){
        MqttTunnelledMessage ms =null;
        try {
            ms = MqttTunnelledMessage.deserialize(data);
        }catch (Exception e){
            e.printStackTrace();

        }
        return ms;

    }
    /**
     * TODO add description
     * */
    private MqttTunnelledMessage getSyncMessage(String topic, byte[] data){
        MqttTunnelledMessage ms =null;

        ms = new MqttTunnelledMessage(
                topic,
                data,Integer.valueOf(conf.get(MqttBackboneProtocolConfigurator.QoS)),
                Boolean.valueOf(conf.get(MqttBackboneProtocolConfigurator.PERSISTENCE)),
                -1,
                MQTTProtocolID
        );



        return ms;

    }
    NMResponse packResponse(NMResponse response, VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress) throws Exception {
        Message r_message = new Message("applicationData", senderVirtualAddress, receiverVirtualAddress, response.getMessageBytes());
        response.setMessageBytes(SerializationUtil.serializeMessage(r_message, true));

        return response;
    }
    /**
     * TODO add description
     * */
    private static NMResponse createErrorMessage(int errorCode, String message) {

        TunnelResponse tunnel_response = new TunnelResponse();
        tunnel_response.setStatusCode(errorCode);
        tunnel_response.setBody(message.getBytes());
        NMResponse nm_response = new NMResponse();
        nm_response.setStatus(NMResponse.STATUS_ERROR);
        nm_response.setBytesPrimary(true);
        try { nm_response.setMessageBytes(SerializationUtil.serialize(tunnel_response)); } catch (IOException e1) {	e1.printStackTrace(); }
        return nm_response;

    }

    private static NMResponse createListResponse(String body) throws IOException {


        TunnelResponse tunnel_response = new TunnelResponse();
        tunnel_response.setStatusCode(200);
        tunnel_response.setBody(body.getBytes());
        NMResponse nm_response = new NMResponse();
        nm_response.setStatus(NMResponse.STATUS_SUCCESS);
        nm_response.setBytesPrimary(true);
        try { nm_response.setMessageBytes(SerializationUtil.serialize(tunnel_response)); } catch (IOException e1) {	e1.printStackTrace(); }

        return nm_response;

    }
    /**
     * TODO add description
     * */
    private static NMResponse createAcceptResponse() throws IOException {


        TunnelResponse tunnel_response = new TunnelResponse();
        tunnel_response.setStatusCode(202);
        tunnel_response.setBody("202 Accepted".getBytes());
        NMResponse nm_response = new NMResponse();
        nm_response.setStatus(NMResponse.STATUS_SUCCESS);
        nm_response.setBytesPrimary(true);
        try { nm_response.setMessageBytes(SerializationUtil.serialize(tunnel_response)); } catch (IOException e1) {	e1.printStackTrace(); }

        return nm_response;

    }
    /**
     * TODO add description
     * */
    private  TunnelRequest recoverRequest(byte[] rawRequest, VirtualAddress senderVirtualAddress) throws Exception {
        TunnelRequest tunnelRequest = null;

        // decoding LinkSmart Message Object
        byte[] tunnel_data = SerializationUtil.deserializeMessage(rawRequest, senderVirtualAddress).getData();


        // deserialize tunnel request
        // FixME this serialization should be done not with Gson
        tunnelRequest = (TunnelRequest) SerializationUtil.deserialize(tunnel_data, TunnelRequest.class);

        //  tunnelRequest = (TunnelRequest)obj;


        return tunnelRequest;


    }
    @Override
    public NMResponse sendDataAsynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data) {
        try {

            MqttBBP.publish(getAsyncMessage(data));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new NMResponse(NMResponse.STATUS_SUCCESS);

    }

    @Override
    public NMResponse receiveDataSynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data) {

        throw new UnsupportedOperationException("MQTT Protocol do not support receive-Data-Synchronize operation!");
    }

    @Override
    public NMResponse receiveDataAsynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] receivedData) {

        byte[] data;
        try {
            data = SerializationUtil.serializeMessage(new Message("applicationData", senderVirtualAddress, receiverVirtualAddress, receivedData), true);
        } catch (Exception e) {
            LOG.error(e.getMessage(),e);
            return null;
        }

        return bbRouter.receiveDataAsynch(senderVirtualAddress,receiverVirtualAddress,data,this);


    }

    @Override
    public NMResponse broadcastData(VirtualAddress senderVirtualAddress, byte[] data) {
        /*if(senderVirtualAddress.equals(brokerService.getVirtualAddress()))
            return new NMResponse(NMResponse.STATUS_SUCCESS);

        LOG.info("Making broadcast in the MQTT Protocol");
        try {

            brokerService.publish(
                    conf.get(MqttBackboneProtocolConfigurator.BROADCAST_TOPIC),
                    data,
                    Integer.valueOf(conf.get(MqttBackboneProtocolConfigurator.QoS)),
                    Boolean.valueOf(conf.get(MqttBackboneProtocolConfigurator.PERSISTENCE))

            );

        }catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
*/
        return new NMResponse(NMResponse.STATUS_SUCCESS);

    }
    @Override
    public String getEndpoint(VirtualAddress virtualAddress) {
        if (!endpointVirtualAddressTopic.containsKey(virtualAddress)) {
            return null;
        }
        return endpointVirtualAddressTopic.get(virtualAddress).toString();
    }

    @Override
    public boolean addEndpoint(VirtualAddress virtualAddress, String endpoint) {
        if (this.endpointVirtualAddressTopic.containsKey(virtualAddress) || endpointVirtualAddressTopic.containsValue(endpoint)) {
            LOG.info("virtual-address is already store for endpoint: " + endpoint);
            return false;
        }

        this.endpointVirtualAddressTopic.put(virtualAddress, endpoint);

        LOG.info("virtual-address is added for endpoint: " + endpoint);
        return true;

    }

    @Override
    public boolean removeEndpoint(VirtualAddress virtualAddress) {

        this.endpointVirtualAddressTopic.remove(virtualAddress);
        return true;
    }

    @Override
    public void addEndpointForRemoteService(VirtualAddress senderVirtualAddress, VirtualAddress remoteVirtualAddress) {

        LOG.info("Mqtt Protocol do not keep track of remote services");
    }
    /**
     *
     * Reimplemented the getName function so the name of the broker for the BackboneRouter is the name of the Broker this Backbone Protocol is connect to.
     * @return name of the backone protocol this case the broker is connected to.
     *
     * */
    @Override
    public  String getName() {

        return MqttBackboneProtocolImpl.class.getName();
    }

    public Dictionary getConfiguration() {
        return this.conf.getConfiguration();
    }



    @Override
    public List<SecurityProperty> getSecurityTypesRequired() {
        String configuredSecurity = this.conf.get(MqttBackboneProtocolConfigurator.SECURITY_PARAMETERS);
        String[] securityTypes = configuredSecurity.split("\\|");
        SecurityProperty oneProperty;
        List<SecurityProperty> answer = new ArrayList<SecurityProperty>();
        for (String s : securityTypes) {
            try {
                oneProperty = SecurityProperty.valueOf(s);
                answer.add(oneProperty);
            } catch (Exception e) {
                LOG.error("Security property value from configuration is not recognized: " + s + ": " + e);
            }
        }
        return answer;
    }

    @Override
    public void applyConfigurations(Hashtable updates) {
        if(updates.containsKey(MqttBackboneProtocolConfigurator.BROKER_AS_SERVICE))
            try {
                brokerService.setService(Boolean.valueOf(updates.get(MqttBackboneProtocolConfigurator.BROKER_AS_SERVICE).toString()));
            }catch (Exception e) {
                LOG.error("Error applying configuration: " + e.getMessage(), e);
            }

        // TODO: Apply configuration changes regarding broadcast settings

        if(updates.containsKey(conf.BACKBONE_DESCRIPTION))
            this.brokerService.setServiceDescription(updates.get(conf.BACKBONE_DESCRIPTION).toString());

        MqttBBP.applyConfigurations(updates);
    }
    public void handleBroadcast(MqttTunnelledMessage data){




            BroadcastMessage msg = new BroadcastMessage(
                    "mqttBroadcast",
                    brokerService.getVirtualAddress(),
                    data.toBytes());


            networkManager.broadcastMessage(msg);


    }
    public void receiveDataBrokerBase(MqttTunnelledMessage data){

        if(!listeningWithWildcardVirtualAddresses.isEmpty()) {
            boolean send = true;
            String[] obtainTopicTokens = data.getTopic().split("/");
            // send to all VAD who wants to receive this message
            for (String topic : listeningWithWildcardVirtualAddresses.keySet()) {
                String[] orgTopicTokens = topic.replace("#","").split("/");

                if (obtainTopicTokens.length >= orgTopicTokens.length){

                    for (int i= 0; i< orgTopicTokens.length;i++)
                        if (!obtainTopicTokens[i].equals(obtainTopicTokens[i])) {
                            send = false;
                            break;
                        }

                }

                if(send)
                    for (VirtualAddress vad : listeningWithWildcardVirtualAddresses.get(topic))
                        receiveDataAsynch(brokerService.getVirtualAddress(), vad, data.toBytes());

            }
        }
        for (VirtualAddress vad : listeningVirtualAddresses.get(data.getTopic()))
            receiveDataAsynch(brokerService.getVirtualAddress(), vad, data.toBytes());
    }
    public void receiveDataTopicBase(MqttTunnelledMessage data){
        for (VirtualAddress vad : listeningVirtualAddresses.get(data.getTopic()))
            receiveDataAsynch((VirtualAddress) endpointVirtualAddressTopic.getKey(data.getTopic()), vad, data.toBytes());
    }
    @Override
    public Message processMessage(Message msg) {
        if(!msg.getSenderVirtualAddress().equals(brokerService.getVirtualAddress())) {
            MqttTunnelledMessage mqttmsg = getAsyncMessage(msg.getData());
            try {
                MqttBBP.publish(mqttmsg);
            } catch (Exception e) {
                LOG.error("While makig a broadcast: " + e.getMessage(), e);
            }
        }
        return null;
    }
}
