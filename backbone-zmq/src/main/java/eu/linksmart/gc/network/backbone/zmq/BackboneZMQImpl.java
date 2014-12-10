package eu.linksmart.gc.network.backbone.zmq;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.scr.annotations.*;
import org.apache.log4j.Logger;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import eu.linksmart.network.NMResponse;
import eu.linksmart.network.VirtualAddress;
import eu.linksmart.network.backbone.Backbone;
import eu.linksmart.network.routing.BackboneRouter;
import eu.linksmart.security.communication.SecurityProperty;

@Component(name="BackboneZMQ", immediate=true)
@Service({Backbone.class})
public class BackboneZMQImpl implements Backbone {

	private Logger LOGGER = Logger.getLogger(BackboneZMQImpl.class.getName());
	
	private ZmqHandler zmqHandler = null;
	
	private BackboneZMQConfigurator configurator;
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
    	LOGGER.debug("Backbonezmq::binding configAdmin");
        this.mConfigAdmin = configAdmin;
    }

    protected void unbindConfigAdmin(ConfigurationAdmin configAdmin) {
    	LOGGER.debug("Backbonezmq::un-binding configAdmin");
        this.mConfigAdmin = null;
    }
    
    protected void bindBackboneRouter(BackboneRouter bbRouter) {
    	LOGGER.debug("Backbonezmq::binding backbone-router");
        this.bbRouter = bbRouter;
    }

    protected void unbindBackboneRouter(BackboneRouter bbRouter) {
    	LOGGER.debug("Backbonezmq::un-binding backbone-router");
        this.bbRouter = null;
    }
    
    @Activate
	protected void activate(ComponentContext context) {
    	LOGGER.info("[activating BackboneZMQ]");
		this.configurator = new BackboneZMQConfigurator(this, context.getBundleContext(), mConfigAdmin);
		configurator.registerConfiguration();
		zmqHandler = new ZmqHandler(this);
		zmqHandler.start();
	}

    @Deactivate
	public void deactivate(ComponentContext context) {
    	LOGGER.info("[de-activating BackboneZMQ]");
		configurator.stop();
		zmqHandler.stop();
	}
    
    @Override
	public NMResponse broadcastData(VirtualAddress senderVirtualAddress, byte[] data) {
    	return zmqHandler.broadcast(new BackboneMessage(senderVirtualAddress, null, data));
	}
	
    @Override
	public NMResponse sendDataSynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data) {
    	return zmqHandler.send(new BackboneMessage(senderVirtualAddress, receiverVirtualAddress, data), true);
	}
	
    @Override
	public NMResponse sendDataAsynch(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data) {
		return zmqHandler.send(new BackboneMessage(senderVirtualAddress, receiverVirtualAddress, data), false);
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
	public String getEndpoint(VirtualAddress virtualAddress) {
		return null;
	}

	@Override
	public boolean addEndpoint(VirtualAddress virtualAddress, String endpoint) {
		return false;
	}

	@Override
	public boolean removeEndpoint(VirtualAddress virtualAddress) {
		return false;
	}
	
	@Override
	public void addEndpointForRemoteService(VirtualAddress senderVirtualAddress, VirtualAddress remoteVirtualAddress) {
	}

	@Override
	public List<SecurityProperty> getSecurityTypesRequired() {
		return null;
	}

	@Override
	public String getName() {
		return BackboneZMQImpl.class.getName();
	}
	
	public void applyConfigurations(Hashtable updates) {
	}

	public Dictionary getConfiguration() {
		return configurator.getConfiguration();
	}
}
