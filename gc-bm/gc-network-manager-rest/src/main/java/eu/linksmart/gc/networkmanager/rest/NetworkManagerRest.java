package eu.linksmart.gc.networkmanager.rest;

import eu.linksmart.gc.api.activator.Activator;
import eu.linksmart.gc.api.engine.EngineContext;
import eu.linksmart.gc.api.network.Registration;
import eu.linksmart.gc.api.network.VirtualAddress;
import eu.linksmart.gc.api.network.networkmanager.NetworkManager;
import eu.linksmart.gc.api.utils.Part;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URLDecoder;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Network Manager REST API
 * 
 * @author hrasheed
 * 
 */
@Path("/NetworkManager")
public class NetworkManagerRest implements Activator {

	private static Logger LOG = Logger.getLogger(NetworkManagerRest.class.getName());
	
	private static final String KEY_ENDPOINT = "Endpoint";
	private static final String KEY_BACKBONE_NAME = "BackboneName";
	private static final String KEY_ATTRIBUTES = "Attributes";
	private static final String KEY_VIRTUAL_ADDRESS = "VirtualAddress";

	private final String TUNNELING_PATH = "/HttpTunneling/0/";
	private String TUNNELING_BASE_URL = "http://localhost:8080" + TUNNELING_PATH;
	
	private NetworkManager networkManager = null;
	
	public NetworkManagerRest() {
	}
	
	public void activate(EngineContext ctx) {
		LOG.info("[activating network manager rest]");
		this.networkManager = ctx.getNetworkManager();
		TUNNELING_BASE_URL = ctx.getContainerEndpoint() + TUNNELING_PATH;
	}
	
	public void initialize() {
		LOG.info("initializing network manager rest");
	}
	
	public void deactivate() {
		
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response registerService(String jsonRegistrationDoc) {
		
		//
		// create JSONObject from message payload
		//	
		if(jsonRegistrationDoc == null)
			return Response.status(Response.Status.BAD_REQUEST).entity("jsonRegistrationDoc is null").build();
		
		if(jsonRegistrationDoc.length() == 0)
			return Response.status(Response.Status.NO_CONTENT).entity("jsonRegistrationDoc is empty").build();
		
		ArrayList<Part> attributes = new ArrayList<Part>();
		String endpoint = null;
		String backboneName = null;
		
		try {
			JSONObject registrationJson = new JSONObject(jsonRegistrationDoc);
			endpoint = registrationJson.getString(KEY_ENDPOINT);
			backboneName = registrationJson.getString(KEY_BACKBONE_NAME);
			JSONObject attributesJson = registrationJson.getJSONObject(KEY_ATTRIBUTES);
			Iterator iterator = attributesJson.keys();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				attributes.add(new Part(key.toUpperCase(), attributesJson.getString(key)));
			}
		} catch (JSONException e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		
		if(endpoint == null || backboneName == null || attributes.size() == 0) {
			LOG.error("Some required fields/attributes for registration are missing from request");
			return Response.status(Response.Status.BAD_REQUEST).entity("Some required fields/attributes for registration are missing from request").build();
		}
		
		Registration registration = null;
		
		try {
			registration = this.networkManager.registerService(attributes.toArray(new Part[]{}), endpoint, backboneName);
		} catch (RemoteException e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

		//
		// return new registration as json
		//
		JSONObject registrationJson = new JSONObject();
		
		try {
			if(registration != null) {
				registrationJson.put(KEY_VIRTUAL_ADDRESS, registration.getVirtualAddressAsString());
				registrationJson.put(KEY_ENDPOINT, TUNNELING_BASE_URL + registration.getVirtualAddressAsString());
				JSONObject attributesJson = new JSONObject();
				for(Part p : registration.getAttributes()) {
					attributesJson.put(p.getKey(), p.getValue());
				}
				registrationJson.put(KEY_ATTRIBUTES, attributesJson);
			} else{
				LOG.error("The registration was not successful, try again");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("The registration was not successful, try again").build();
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		
		return Response.status(Response.Status.OK).entity(registrationJson).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getService(@Context UriInfo uriInfo, @Context HttpHeaders headers) {
		
		Registration[] registrations = null;
		
		try {
			
			String queryString = uriInfo.getRequestUri().getQuery();
			
			LOG.info("received Query String in getService: " + queryString);
				
			if(queryString == null || queryString.length() == 0) {
				return Response.status(Response.Status.BAD_REQUEST).entity("service query string is empty").build();
			}
			
			String encoding = headers.getRequestHeader(HttpHeaders.CONTENT_ENCODING).get(0);
			
			if(encoding != null)
				queryString = URLDecoder.decode(queryString, encoding);
			else
				queryString = URLDecoder.decode(queryString, "UTF-8");
			
			String[] queryAttributes = queryString.split("&");

			if(queryAttributes.length == 1) {
				String queryAttribute = queryAttributes[0];
				int separatorIndex = queryAttribute.indexOf("=");
				String attributeName = queryAttribute.substring(0, separatorIndex);
				String attributeValue = queryAttribute.substring(separatorIndex + 1);
				if(attributeName.equals("description")) {
					registrations = this.networkManager.getServiceByDescription(removeCharacters(attributeValue));	
				} else if(attributeName.equals("pid")) {
					Registration registration = this.networkManager.getServiceByPID(removeCharacters(attributeValue));
					if(registration != null) {
						registrations = new Registration[] { registration };
					}
				} else if(attributeName.equals("query")) {
					registrations = this.networkManager.getServiceByQuery(removeCharacters(attributeValue));
				} else {
					Part single_part = new Part(attributeName, removeCharacters(attributeValue));
					registrations = this.networkManager.getServiceByAttributes(new Part[] { single_part });
				}
			} else {
				ArrayList<Part> nmAttributes = new ArrayList<Part>();
				long timeOut = 0;
				boolean returnFirst = true;
				boolean isStrictRequest = true;
				boolean timeOutInit = false;
				boolean returnFirstInit = false;
				boolean isStrictRequestInit = false;
				for(String queryAttribute : queryAttributes) {
					int separatorIndex = queryAttribute.indexOf("=");
					String attributeName = queryAttribute.substring(0, separatorIndex);
					String attributeValue = queryAttribute.substring(separatorIndex + 1);
					if(attributeName.equals("timeOut")) {
						timeOut = Long.parseLong(attributeValue);
						timeOutInit = true;
					}
					else if(attributeName.equals("returnFirst")) {
						returnFirst = Boolean.parseBoolean(attributeValue);
						returnFirstInit = true;
					}
					else if(attributeName.equals("isStrictRequest")) {
						isStrictRequest = Boolean.parseBoolean(attributeValue);
						isStrictRequestInit = true;
					}
					else {
						nmAttributes.add(new Part(attributeName, removeCharacters(attributeValue)));
					}
			    }
				if(timeOutInit && returnFirstInit && isStrictRequestInit)
					registrations = this.networkManager.getServiceByAttributes(nmAttributes.toArray(new Part[]{}), timeOut, returnFirst, isStrictRequest);
				else
					registrations = this.networkManager.getServiceByAttributes(nmAttributes.toArray(new Part[]{}));
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		
		if(registrations == null) {
			return Response.status(Response.Status.NOT_FOUND).entity("unable to create registration(s)").build();
		}
		
		if(registrations.length == 0) {
			return Response.status(Response.Status.NOT_FOUND).entity("no such registration(s) found").build();
		}
		
		if(registrations.length == 1) {
			if(registrations[0] == null) {
				return Response.status(Response.Status.NOT_FOUND).entity("no such registration(s) found").build();
			}
		}
		
		JSONArray registrationsJson = new JSONArray();

		try {
			for(Registration registration : registrations) {
				JSONObject registrationJson = new JSONObject();
				registrationJson.put(KEY_VIRTUAL_ADDRESS, registration.getVirtualAddressAsString());
				registrationJson.put(KEY_ENDPOINT, TUNNELING_BASE_URL + registration.getVirtualAddressAsString());
				JSONObject attributesJson = new JSONObject();
				if(registration.getAttributes() != null) {
					for(Part p : registration.getAttributes()) {
						attributesJson.put(p.getKey(), p.getValue());
					}
					registrationJson.put(KEY_ATTRIBUTES, attributesJson);
				}
				registrationsJson.put(registrationJson);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

		return Response.status(Response.Status.OK).entity(registrationsJson).build();
	}
	
	@PUT
	@Consumes(value="application/json")
	@Produces(value="application/json")
	public Response updateService(String jsonRegistrationDoc) {
		
		//
		// create JSONObject from message payload
		//
		if(jsonRegistrationDoc == null)
			return Response.status(Response.Status.BAD_REQUEST).entity("jsonRegistrationDoc is null").build();
		
		if(jsonRegistrationDoc.length() == 0)
			return Response.status(Response.Status.NO_CONTENT).entity("jsonRegistrationDoc is empty").build();

		ArrayList<Part> attributes = new ArrayList<Part>();
		String endpoint = null;
		String backboneName = null;
		String virtualAddress = null;
		
		try {
			JSONObject registrationJson = new JSONObject(jsonRegistrationDoc);
			endpoint = registrationJson.getString(KEY_ENDPOINT);
			backboneName = registrationJson.getString(KEY_BACKBONE_NAME);
			virtualAddress = registrationJson.getString(KEY_VIRTUAL_ADDRESS);
			JSONObject attributesJson = registrationJson.getJSONObject(KEY_ATTRIBUTES);
			Iterator iterator = attributesJson.keys();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				attributes.add(new Part(key.toUpperCase(), attributesJson.getString(key)));
			}
		} catch (JSONException e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		
		if(endpoint == null || backboneName == null || attributes.size() == 0) {
			LOG.error("Some required fields/attributes for registration update are missing from request");
			return Response.status(Response.Status.BAD_REQUEST).entity("Some required fields/attributes for registration are missing from request").build();
		}
		
		if(new VirtualAddress(virtualAddress).getBytes().length != VirtualAddress.VIRTUAL_ADDRESS_BYTE_LENGTH) {
			LOG.error("provided virtual-address doesn't conform to its format: " + virtualAddress);
			return Response.status(Response.Status.BAD_REQUEST).entity("provided virtual-address doesn't conform to its format: " + virtualAddress).build();
		}
		
		try {
			boolean serviceFound = this.networkManager.removeService(new VirtualAddress(virtualAddress));
			if(!serviceFound) {
				LOG.error("no such service registered with virtual address: " + virtualAddress);
				return Response.status(Response.Status.NOT_FOUND).entity("no such service registered with virtual address: " + virtualAddress).build();
			} 
		} catch (RemoteException e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		
		Registration registration = null;
		
		try {
			registration = this.networkManager.registerService(attributes.toArray(new Part[]{}), endpoint, backboneName);
		} catch (RemoteException e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

		//
		// return new registration as json
		//
		JSONObject registrationJson = new JSONObject();
		
		try {
			if(registration != null) {
				registrationJson.put(KEY_VIRTUAL_ADDRESS, registration.getVirtualAddressAsString());
				registrationJson.put(KEY_ENDPOINT, TUNNELING_BASE_URL + registration.getVirtualAddressAsString());
				JSONObject attributesJson = new JSONObject();
				for(Part p : registration.getAttributes()) {
					attributesJson.put(p.getKey(), p.getValue());
				}
				registrationJson.put(KEY_ATTRIBUTES, attributesJson);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		
		return Response.status(Response.Status.OK).entity(registrationJson).build();
	}
	
	@DELETE
	@Path("{virtualAddress}")
	public Response deleteService(@PathParam("virtualAddress") String virtualAddress) {
		
		if(virtualAddress == null || virtualAddress.length() == 0) {
			return Response.status(Response.Status.BAD_REQUEST).entity(new String("no virtual-address is given in request")).build();
		}
		
		if(new VirtualAddress(virtualAddress).getBytes().length != VirtualAddress.VIRTUAL_ADDRESS_BYTE_LENGTH) {
			return Response.status(Response.Status.BAD_REQUEST).entity("provided virtual-address doesn't conform to its format: " + virtualAddress).build();
		}
				
		try {
			boolean resp = this.networkManager.removeService(new VirtualAddress(virtualAddress));
			if(resp)
				return Response.status(Response.Status.OK).entity(Boolean.TRUE).build();
			else
				return Response.status(Response.Status.NOT_FOUND).entity("no such service registered with virtual address: " + virtualAddress).build();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}
	
	private String removeCharacters(String attributeValue) {
		//
		// remove quotation symbols
		//
		if(attributeValue.startsWith("\"") && attributeValue.endsWith("\"")) {
			attributeValue = attributeValue.substring(1, attributeValue.length() - 1);
		} else if(attributeValue.startsWith("%22") && attributeValue.endsWith("%22")) {
			attributeValue = attributeValue.substring(3, attributeValue.length() - 3);
		} 
		return attributeValue;
	}

}
