package eu.linksmart.gc.sc.client;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.scr.annotations.*;
import org.apache.log4j.Logger;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.gson.Gson;

import eu.linksmart.gc.api.network.ServiceAttribute;
import eu.linksmart.gc.api.sc.client.ServiceCatalogClient;
import eu.linksmart.gc.api.utils.Part;
import eu.linksmart.lc.sc.client.ServiceCatalog;
import eu.linksmart.lc.sc.types.Endpoint;
import eu.linksmart.lc.sc.types.Meta;
import eu.linksmart.lc.sc.types.Protocol;
import eu.linksmart.lc.sc.types.Registration;

@Component(name="ServiceCatalogClient", immediate=true)
@org.apache.felix.scr.annotations.Service({ServiceCatalogClient.class})
public class SCClientImpl implements ServiceCatalogClient {

	private Logger LOG = Logger.getLogger(SCClientImpl.class.getName());
	
	private final String CATALOG_REGISTRATION = "SC";
	
	private final String GC_TUNNELED = "gc_tunnelled";
	
	private String CATALOG_URL = "http://gando.fit.fraunhofer.de:8090/sc";
	
	private String TUNNELING_BASE_URL = "http://localhost:8082/HttpTunneling/0/";
	
	private ClientConfigurator configurator = null;
	
	@Reference(name="ConfigurationAdmin",
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            bind="bindConfigAdmin",
            unbind="unbindConfigAdmin",
            policy=ReferencePolicy.STATIC)
    private ConfigurationAdmin mConfigAdmin = null;
	
	protected void bindConfigAdmin(ConfigurationAdmin configAdmin) {
        this.mConfigAdmin = configAdmin;
    }

    protected void unbindConfigAdmin(ConfigurationAdmin configAdmin) {
        this.mConfigAdmin = null;
    }
	 
    @Activate
	protected void activate(ComponentContext context) {
    	LOG.info("[activating ServiceCatalogClient]");
    	this.configurator = new ClientConfigurator(this, context.getBundleContext(), mConfigAdmin);
		this.configurator.registerConfiguration();
		CATALOG_URL = this.configurator.get(ClientConfigurator.SERVICE_CATALOG_URL);
        LOG.info("using service catalog URL :  " + CATALOG_URL);
        TUNNELING_BASE_URL = this.configurator.get(ClientConfigurator.TUNNELING_BASE_URL);
        LOG.info("using Tunneling Base URL :  " + TUNNELING_BASE_URL);
    	ServiceCatalog.setURL(CATALOG_URL);
	}

    @Deactivate
	public void deactivate(ComponentContext context) {
    	LOG.info("[de-activating ServiceCatalogClient]");
    	this.configurator.stop();
	}

	@Override
	public boolean add(eu.linksmart.gc.api.network.Registration registration) {
		Registration cregistration = getCatalogRegistration(registration);
		if(cregistration == null)
			return false;
		updateEndPoint(cregistration, getTunnelEndPoint(registration));
		addTunneledFlag(cregistration);
		return ServiceCatalog.add(cregistration);
	}

	@Override
	public boolean update(eu.linksmart.gc.api.network.Registration registration) {
		Registration cregistration = getCatalogRegistration(registration);
		if(cregistration == null)
			return false;
		updateEndPoint(cregistration, getTunnelEndPoint(registration));
		addTunneledFlag(cregistration);
		return ServiceCatalog.update(getServiceID(cregistration), cregistration);
	}

	@Override
	public boolean delete(eu.linksmart.gc.api.network.Registration registration) {
		Registration cregistration = getCatalogRegistration(registration);
		if(cregistration == null)
			return false;
		String serviceID = getServiceID(cregistration);
		boolean status = ServiceCatalog.delete(getServiceID(cregistration));
		LOG.info("SC_Client: deleting service with ID: " + serviceID + " - status: " + status);
		return status;
	}
	
	private Registration getCatalogRegistration(eu.linksmart.gc.api.network.Registration registration) {
		Part[] attributes = registration.getAttributes();
		for(Part attribute : attributes) {
			if(attribute.getKey().equals(CATALOG_REGISTRATION)) {
				return new Gson().fromJson(attribute.getValue(), Registration.class);
			}	
		}
		return null;
	}
	
	private void updateEndPoint(Registration cregistration, String endpoint) {
		List<Protocol> protocols = cregistration.getProtocols();
		for (int i = 0; i < protocols.size(); i++) {
			Protocol protocol = protocols.get(i);
			if(protocol.getType().equals("REST")) {
				Endpoint endPoint = new Endpoint();
				endPoint.setURL(endpoint);
				protocol.setEndpoint(endPoint);
			}
		}
	}
	
	private void addTunneledFlag(Registration cregistration) {
		if(cregistration.getMeta() == null) {
			cregistration.setMeta(new Meta());
		}
		cregistration.getMeta().put(GC_TUNNELED, "true");
	}
	
	private String getServiceID(Registration registration) {
		return registration.getId();
	}
	
	private String getServiceID(eu.linksmart.gc.api.network.Registration registration) {
		Part[] attributes = registration.getAttributes();
		for(Part attribute : attributes) {
			if(attribute.getKey().equals(ServiceAttribute.DESCRIPTION.name())) {
				return attribute.getValue();
			}	
		}
		return null;
	}
	
	private String getTunnelEndPoint(eu.linksmart.gc.api.network.Registration registration) {
		String servicePath = registration.getVirtualAddressAsString();  
		return TUNNELING_BASE_URL + servicePath;
	}
	
	public void applyConfigurations(Hashtable updates) {
	}

	public Dictionary getConfiguration() {
		return this.configurator.getConfiguration();
	}

}
