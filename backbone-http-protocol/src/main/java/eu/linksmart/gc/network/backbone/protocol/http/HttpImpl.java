package eu.linksmart.gc.network.backbone.protocol.http;

import eu.linksmart.gc.api.types.TunnelRequest;
import eu.linksmart.gc.api.types.TunnelResponse;
import eu.linksmart.gc.api.types.utils.SerializationUtil;
import eu.linksmart.network.NMResponse;
import eu.linksmart.network.VirtualAddress;
import eu.linksmart.network.backbone.Backbone;
import eu.linksmart.network.routing.BackboneRouter;
import eu.linksmart.security.communication.SecurityProperty;

import org.apache.felix.scr.annotations.*;
import org.apache.log4j.Logger;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Component(name="BackboneHTTP", immediate=true)
@Service({Backbone.class})
public class HttpImpl implements Backbone {

	private Logger LOGGER = Logger.getLogger(HttpImpl.class.getName());
	
	private Map<VirtualAddress, URL> virtualAddressUrlMap = new HashMap<VirtualAddress, URL>();
	
	private HttpConfigurator configurator = null;
	
	@Reference(name="ConfigurationAdmin",
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            bind="bindConfigAdmin",
            unbind="unbindConfigAdmin",
            policy=ReferencePolicy.STATIC)
    private ConfigurationAdmin mConfigAdmin = null;
	
	@Reference(name="BackboneRouter",
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            bind="bindBackboneRouter",
            unbind="unbindBackboneRouter",
            policy= ReferencePolicy.STATIC)
	private BackboneRouter bbRouter;
	
    protected void bindConfigAdmin(ConfigurationAdmin configAdmin) {
    	LOGGER.debug("HttpImpl::binding configAdmin");
        this.mConfigAdmin = configAdmin;
    }

    protected void unbindConfigAdmin(ConfigurationAdmin configAdmin) {
    	LOGGER.debug("HttpImpl::un-binding configAdmin");
        this.mConfigAdmin = null;
    }
    
    protected void bindBackboneRouter(BackboneRouter bbRouter) {
    	LOGGER.debug("HttpImpl::binding backbone-router");
        this.bbRouter = bbRouter;
    }

    protected void unbindBackboneRouter(BackboneRouter bbRouter) {
    	LOGGER.debug("HttpImpl::un-binding backbone-router");
        this.bbRouter = null;
    }
    
    @Activate
	protected void activate(ComponentContext context) {
    	LOGGER.info("[activating Backbone HttpProtocol]");
    	this.configurator = new HttpConfigurator(this, context.getBundleContext(), mConfigAdmin);
		this.configurator.registerConfiguration();
	}

    @Deactivate
	public void deactivate(ComponentContext context) {
    	LOGGER.info("[de-activating Backbone HttpProtocol]");
	}
    
    @Override
	public NMResponse sendDataSynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data) {
    	
		NMResponse resp = null;
		
		try {
			
			//
			// check if service endpoint is available
			//
			//TODO create proper NMResponse with exception message
			URL urlEndpoint = virtualAddressUrlMap.get(receiverVirtualAddress);
			if (urlEndpoint == null) {
				throw new IllegalArgumentException("Cannot send data to VirtualAddress " + receiverVirtualAddress.toString() + ", unknown endpoint");
			}
			
			//
			// reading tunnel request
			//
			TunnelRequest tunnel_request = (TunnelRequest) SerializationUtil.deserialize(data);
			
			LOGGER.info("method: " + tunnel_request.getMethod());
			LOGGER.info("path: " + tunnel_request.getPath());
			LOGGER.info("headers: " + tunnel_request.getHeaders());
			LOGGER.info("body: " + tunnel_request.getBody());
			
			//
			// invoke service endpoint
			//
			
			//
			// create tunnel response
			//
			TunnelResponse tunnel_response = new TunnelResponse();
			tunnel_response.setStatusCode(200);
			tunnel_response.setHeaders("");
			tunnel_response.setBody("HttpImpl-service-response".getBytes());
			
			//
			// wrap tunnel response inside network-manager response object
			//
			resp = new NMResponse();
			resp.setStatus(NMResponse.STATUS_SUCCESS);
			resp.setBytesPrimary(true);
			resp.setMessageBytes(SerializationUtil.serialize(tunnel_response));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resp;
	}
	
    @Override
	public NMResponse sendDataAsynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data) {
    	throw new RuntimeException("Asynchronous sending not supported by HTTP!");
	}
	
    @Override
	public NMResponse receiveDataSynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress,
			byte[] receivedData) {
    	return null;
	}
	
    @Override
	public NMResponse receiveDataAsynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress,
			byte[] receivedData) {
    	return null;
	}
    
    @Override
	public NMResponse broadcastData(VirtualAddress senderVirtualAddress, byte[] data) {
    	return null;
	}

	@Override
	public List<SecurityProperty> getSecurityTypesRequired() {
		String configuredSecurity = this.configurator.get(HttpConfigurator.SECURITY_PARAMETERS);
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
        	LOGGER.info("virtual-addess is already store for endpoint: " + endpoint);
            return false;
        }
        try {
            URL url = new URL(endpoint);
            this.virtualAddressUrlMap.put(virtualAddress, url);
            LOGGER.info("virtual-addess is added for endpoint: " + endpoint);
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
            LOGGER.warn("endpoint of VirtualAddress " + senderVirtualAddress + " cannot be found");
        }
	}

	@Override
	public String getName() {
		return HttpImpl.class.getName();
	}
	
	public void applyConfigurations(Hashtable updates) {
	}

	public Dictionary getConfiguration() {
		return this.configurator.getConfiguration();
	}
}
