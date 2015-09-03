package eu.linksmart.gc.sc.client;

import com.google.gson.Gson;
import eu.linksmart.gc.api.engine.EngineContext;
import eu.linksmart.gc.api.network.ServiceAttribute;
import eu.linksmart.gc.api.sc.client.ServiceCatalogClient;
import eu.linksmart.gc.api.utils.Part;
import eu.linksmart.lc.sc.client.ServiceCatalog;
import eu.linksmart.lc.sc.types.Endpoint;
import eu.linksmart.lc.sc.types.Meta;
import eu.linksmart.lc.sc.types.Protocol;
import eu.linksmart.lc.sc.types.Registration;
import org.apache.log4j.Logger;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class SCClientImpl implements ServiceCatalogClient {

	private Logger LOG = Logger.getLogger(SCClientImpl.class.getName());
	
	private final String CATALOG_REGISTRATION = "SC";
	
	private final String GC_TUNNELED = "gc_tunnelled";
	
	private String CATALOG_URL = "http://gando.fit.fraunhofer.de:8090/sc";
	
	private String TUNNELING_BASE_URL = "http://localhost:8082/HttpTunneling/0/";
	
	public void activate(EngineContext ctx) {
    	LOG.info("[activating ServiceCatalogClient]");
		CATALOG_URL = ctx.get("eu.linksmart.gc.sc.client.service.catalog.url");
        LOG.info("using service catalog URL :  " + CATALOG_URL);
        TUNNELING_BASE_URL = ctx.get("eu.linksmart.gc.sc.client.tunneling.base.url");;
        LOG.info("using Tunneling Base URL :  " + TUNNELING_BASE_URL);
	}
	
	public void initialize() {	
		ServiceCatalog.setURL(CATALOG_URL);
	}

	public void deactivate() {
    	LOG.info("[de-activating ServiceCatalogClient]");
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
		return ServiceCatalog.delete(getServiceID(cregistration));
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
		return null;
	}
}
