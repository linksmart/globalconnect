package eu.linksmart.gc.network.backbone.protocol.mqtt;

import eu.linksmart.gc.api.network.Registration;
import eu.linksmart.gc.api.network.VirtualAddress;
import eu.linksmart.gc.api.network.networkmanager.NetworkManager;
import eu.linksmart.gc.api.utils.Part;
import eu.linksmart.gc.utils.mqtt.broker.BrokerService;
import eu.linksmart.gc.utils.mqtt.broker.StaticBroker;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created by Caravajal on 22.04.2015.
 */
public class BrokerConnectionService extends StaticBroker {
    private Logger LOG = Logger.getLogger(MqttBackboneProtocolImpl.class.getName());
    // this is the MQTT client to publish in the local broker

    private NetworkManager networkManager;

    private String serviceDescription;

    private boolean isService;

    private Registration brokerRegistrationInfo = null;
    BrokerConnectionService(  String brokerName, String brokerPort , UUID ID, NetworkManager netMngr, boolean isService, String serviceDescription) throws Exception {
        super(brokerName, brokerPort);
        this.networkManager = netMngr;
        this.isService = isService;
        this.serviceDescription = serviceDescription;
       createClient();

    }

    public VirtualAddress getVirtualAddress(){
        return brokerRegistrationInfo.getVirtualAddress();
    }





    private void registerBroker() throws RemoteException {

        String description = "Broker:"+getBrokerURL();


        Registration[] registration = networkManager.getServiceByDescription(serviceDescription);
        if(registration != null && registration.length > 0){
            throw new RemoteException("There is already a service with this description");

        }else {
            Part[] attributes = {
                    new Part("DESCRIPTION",description),
                    new Part("UUID",this.clientID.toString())

            };

            brokerRegistrationInfo = networkManager.registerService(attributes,getBrokerName(),MqttBackboneProtocolImpl.class.getName());
            LOG.info("MQTT broker service is registered");
        }
    }
    private void deregisterBroker() throws RemoteException {
        Registration[] registration = networkManager.getServiceByDescription(brokerRegistrationInfo.getDescription());
        if(registration != null && registration.length > 0) {
            networkManager.removeService(brokerRegistrationInfo.getVirtualAddress());
            LOG.info("MQTT broker service is deregistered");
        }


    }

    private String getHostName(){
        String hostname = "localhost";

        try
        {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        }
        catch (UnknownHostException ex)
        {
            LOG.error("Error while getting hostname:"+ex.getMessage(),ex);
        }
        return hostname;
    }

    public void setServiceDescription(String serviceDescription) {
        try {
            deregisterBroker();
        } catch (RemoteException e) {
            LOG.error("Error while changing service description: "+e.getMessage(),e);
        }
        this.serviceDescription = serviceDescription;
        try {
            registerBroker();
        } catch (RemoteException e) {
            LOG.error("Error while changing service description: "+e.getMessage(),e);
        }
    }

    public boolean isService() {
        return isService;
    }

    public void setService(boolean isService) throws RemoteException {
        if(this.isService!=isService) {
            if(this.isService){
                deregisterBroker();
            }else {
                registerBroker();
            }
            this.isService = isService;
        }
    }
}
