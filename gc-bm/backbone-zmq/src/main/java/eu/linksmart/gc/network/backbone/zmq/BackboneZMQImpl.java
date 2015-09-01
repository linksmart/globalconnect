package eu.linksmart.gc.network.backbone.zmq;

import eu.linksmart.gc.api.network.NMResponse;
import eu.linksmart.gc.api.network.VirtualAddress;
import eu.linksmart.gc.api.network.backbone.Backbone;
import eu.linksmart.gc.api.network.routing.BackboneRouter;
import eu.linksmart.gc.api.security.communication.SecurityProperty;
import eu.linksmart.gc.server.GcEngineSingleton;

import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class BackboneZMQImpl implements Backbone {

	private Logger LOGGER = Logger.getLogger(BackboneZMQImpl.class.getName());
	
	private Map<VirtualAddress, URL> virtualAddressUrlMap = new HashMap<VirtualAddress, URL>();
	
	private ZmqHandler zmqHandler = null;
	
	private BackboneRouter bbRouter;
	
	public static final String BACKBONE_DESCRIPTION = "backbone.description";
	public static final String BACKBONE_ZMQ_XPUB_URI = "backbone.zmq.xpub.uri";
	public static final String BACKBONE_ZMQ_XSUB_URI = "backbone.zmq.xsub.uri";
	public static final String BACKBONE_ZMQ_HEARTBEAT_INTERVAL = "backbone.zmq.heartbeat.interval";
    
	public void activate() {
    	LOGGER.info("[activating BackboneZMQ]");
    	this.bbRouter = GcEngineSingleton.getBackboneRouter();
        String xpubURI = GcEngineSingleton.get("backbone.zmq.xpub.uri");
        LOGGER.info("using xpub uri :  " + xpubURI);
        String xsubURI = GcEngineSingleton.get("backbone.zmq.xsub.uri");
        LOGGER.info("using xsub uri :  " + xsubURI);
		zmqHandler = new ZmqHandler(this, xpubURI, xsubURI);
		zmqHandler.start();
	}
	
	public void initialize() {	
		LOGGER.info("initializing ZMQ protocol");
	}

	public void deactivate() {
    	LOGGER.info("[de-activating BackboneZMQ]");
		zmqHandler.stop();
	}
    
    @Override
	public NMResponse broadcastData(VirtualAddress senderVirtualAddress, byte[] data) {
    	return zmqHandler.broadcast(new BackboneMessage(senderVirtualAddress, null, data));
	}
	
    @Override
	public NMResponse sendDataSynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data) {
    	return zmqHandler.sendData(new BackboneMessage(senderVirtualAddress, receiverVirtualAddress, data, true));
	}
	
    @Override
	public NMResponse sendDataAsynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data) {
		return zmqHandler.sendData(new BackboneMessage(senderVirtualAddress, receiverVirtualAddress, data, false));
	}
	
    @Override
	public NMResponse receiveDataSynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress,
			byte[] receivedData) {
    	if(bbRouter != null)
    		return bbRouter.receiveDataSynch(senderVirtualAddress, receiverVirtualAddress, receivedData, (Backbone) this);
    	return null;
	}
	
    @Override
	public NMResponse receiveDataAsynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress,
			byte[] receivedData) {
    	if(bbRouter != null)
    		return bbRouter.receiveDataAsynch(senderVirtualAddress, receiverVirtualAddress, receivedData, (Backbone) this);
    	return null;
	}

	@Override
	public List<SecurityProperty> getSecurityTypesRequired() {
		String configuredSecurity = GcEngineSingleton.get("backbone.zmq.SecurityParameters");
		String[] securityTypes = configuredSecurity.split("\\|");
		SecurityProperty oneProperty;
		List<SecurityProperty> answer = new ArrayList<SecurityProperty>();
		for (String s : securityTypes) {
			try {
				oneProperty = SecurityProperty.valueOf(s);
				answer.add(oneProperty);
			} catch (Exception e) {
				LOGGER.error("Security property value from configuration is not recognized: " + s + ": " + e);
			}
		}
		return answer;
	}
	
	@Override
	public String getEndpoint(VirtualAddress virtualAddress) {
        if (!virtualAddressUrlMap.containsKey(virtualAddress)) {
            return null;
        }
        return virtualAddressUrlMap.get(virtualAddress).toString();
	}

	@Override
	public boolean addEndpoint(VirtualAddress virtualAddress, String endpoint) {
        if (this.virtualAddressUrlMap.containsKey(virtualAddress)) {
            return false;
        }
        try {
            URL url = new URL(endpoint);
            this.virtualAddressUrlMap.put(virtualAddress, url);
            return true;
        } catch (MalformedURLException e) {
            LOGGER.debug("Unable to add endpoint " + endpoint + " for VirtualAddress " + virtualAddress.toString(), e);
        }
        return false;
    }

	@Override
	public boolean removeEndpoint(VirtualAddress virtualAddress) {
        return this.virtualAddressUrlMap.remove(virtualAddress) != null;
	}
	
	@Override
	public void addEndpointForRemoteService(VirtualAddress senderVirtualAddress, VirtualAddress remoteVirtualAddress) {
        URL endpoint = virtualAddressUrlMap.get(senderVirtualAddress);
        if (endpoint != null) {
            virtualAddressUrlMap.put(remoteVirtualAddress, endpoint);
        } else {
            LOGGER.warn("Network Manager endpoint of VirtualAddress " + senderVirtualAddress + " cannot be found");
        }
	}

	@Override
	public String getName() {
		return BackboneZMQImpl.class.getName();
	}
	
	public void applyConfigurations(Hashtable updates) {
	}

	public Dictionary getConfiguration() {
		return null;
	}
}
