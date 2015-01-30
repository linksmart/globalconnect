package eu.linksmart.gc.networkmanager.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/*import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;*/

import eu.linksmart.network.Registration;
import eu.linksmart.network.VirtualAddress;
import eu.linksmart.utils.Part;

public class NetworkManagerServlet extends HttpServlet {

	private static final long serialVersionUID = 4799084413331453345L;
	
	private static final String KEY_ENDPOINT = "Endpoint";
	private static final String KEY_BACKBONE_NAME = "BackboneName";
	private static final String KEY_ATTRIBUTES = "Attributes";
	private static final String MISSING_PARAMETERS_ERROR = "Some fields necessary for registration are missing from your request!";
	private static final String KEY_VIRTUAL_ADDRESS = "VirtualAddress";
	
	private NetworkManagerRestPort networkManagerPort = null;

	public NetworkManagerServlet(NetworkManagerRestPort networkManagerPort) {
		this.networkManagerPort = networkManagerPort;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		//take request and create JSONObject
		ArrayList<Part> attributes = new ArrayList<Part>();		
		if(request.getQueryString() == null || request.getQueryString().length() == 0) {
			return;
		}
		//divide query into individual attributes
		String[] queryAttrs = request.getQueryString().split("&");

		for(String queryAttr : queryAttrs) {
			int separatorIndex = queryAttr.indexOf("=");
			String attributeName = queryAttr.substring(0, separatorIndex).toUpperCase();

			String attributeValue = queryAttr.substring(separatorIndex + 1);
			if(attributeValue.startsWith("\"") && attributeValue.endsWith("\"")) {
				//cut off quotation symbols
				attributeValue = attributeValue.substring(1, attributeValue.length() - 1);
			} else if(attributeValue.startsWith("%22") && attributeValue.endsWith("%22")) {
				//cut off quotation symbols
				attributeValue = attributeValue.substring(3, attributeValue.length() - 3);
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "False format of service query");
				return;
			}
			attributes.add(new Part(attributeName, attributeValue));
		}			
		Registration[] registrations = this.networkManagerPort.getNetworkManager().getServiceByAttributes(attributes.toArray(new Part[]{}));
		JSONArray regsJson = new JSONArray();

		try {
			for(Registration reg : registrations) {
				JSONObject regJson = new JSONObject();
				regJson.put(KEY_VIRTUAL_ADDRESS, reg.getVirtualAddressAsString());
				JSONObject attributesJson = new JSONObject();
				for(Part p : reg.getAttributes()) {
					attributesJson.put(p.getKey(), p.getValue());
				}
				regJson.put(KEY_ATTRIBUTES, attributesJson);
				regsJson.put(regJson);
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}

		String responseString = regsJson.toString();
		response.setContentLength(responseString.length());
		response.getOutputStream().write(responseString.getBytes());
		response.getOutputStream().close();
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		//take message body and create JSONObject
		StringBuilder requestBuilder = new StringBuilder();
		if (request.getContentLength() > 0) {
			try {
				BufferedReader reader = request.getReader();
				for (String line = null; (line = reader.readLine()) != null;)
					requestBuilder.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return;
		}

		ArrayList<Part> attributes = new ArrayList<Part>();
		String endpoint = null;
		String backboneName = null;
		try {
			JSONObject registrationJson = new JSONObject(requestBuilder.toString());
			endpoint = registrationJson.getString(KEY_ENDPOINT);
			backboneName = registrationJson.getString(KEY_BACKBONE_NAME);
			JSONObject attributesJson = registrationJson.getJSONObject(KEY_ATTRIBUTES);
			Iterator i = attributesJson.keys();
			while (i.hasNext()) {
				String key = (String)i.next();
				attributes.add(new Part(key.toUpperCase(), attributesJson.getString(key)));
			}
		} catch (JSONException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}
		//error handling
		if(endpoint == null || backboneName == null || attributes.size() == 0) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, MISSING_PARAMETERS_ERROR);
			return;
		}
		//register service at Network Manager
		Registration registration = null;
		try {
			registration = this.networkManagerPort.getNetworkManager().registerService(attributes.toArray(new Part[]{}), endpoint, backboneName);
		} catch (RemoteException e) {
			// local invocation - not relevant
		}

		//return new registration as json
		JSONObject regJson = new JSONObject();
		try {
			if(registration != null) {
				regJson.put(KEY_VIRTUAL_ADDRESS, registration.getVirtualAddressAsString());
				JSONObject attributesJson = new JSONObject();
				for(Part p : registration.getAttributes()) {
					attributesJson.put(p.getKey(), p.getValue());
				}
				regJson.put(KEY_ATTRIBUTES, attributesJson);
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
		
		response.setContentLength(regJson.toString().length());
		response.getOutputStream().write(regJson.toString().getBytes());
		response.getOutputStream().close();
	}
	
	public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String virtualAddress = request.getPathInfo().substring(1);
		boolean resp = this.networkManagerPort.getNetworkManager().removeService(new VirtualAddress(virtualAddress));
		if(!resp) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

}
