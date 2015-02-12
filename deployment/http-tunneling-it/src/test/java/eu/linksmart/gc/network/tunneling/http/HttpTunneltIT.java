package eu.linksmart.gc.network.tunneling.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import eu.linksmart.it.utils.ITConfiguration;
import eu.linksmart.network.ServiceAttribute;
import eu.linksmart.utils.Part;

@RunWith(PaxExam.class)
public class HttpTunneltIT {
	
	private static Logger LOG = Logger.getLogger(HttpTunneltIT.class.getName());
	
	private String nm_base_url = "http://localhost:8882/NetworkManager";
	
	private static final String KEY_ENDPOINT = "Endpoint";
	private static final String KEY_BACKBONE_NAME = "BackboneName";
	private static final String KEY_ATTRIBUTES = "Attributes";
	private static final String KEY_VIRTUAL_ADDRESS = "VirtualAddress";
	
	private String endPoint = null;
	
    @Configuration
    public Option[] config() {
        return new Option[] {
        		ITConfiguration.regressionDefaults(),
        		features("mvn:eu.linksmart.gc.features/linksmart-gc-features/0.0.1-SNAPSHOT/xml/features","http-tunneling-it"),  
        };
    }
    
    @Before
    public void setUp() {
    	
    	try {
    		
    		LOG.info("registreing service into NetworkManager & retrieve service Endpoint for Tunneling");
    		
    		HttpClient client = new HttpClient();
    		
    		//
        	// register service
        	//
    		JSONObject registrationJson = new JSONObject();
    		
			registrationJson.put(KEY_ENDPOINT, "http://localhost:8882/NetworkManager");
			registrationJson.put(KEY_BACKBONE_NAME, "eu.linksmart.gc.network.backbone.protocol.http.HttpImpl");
			
			Part[] attributes = { new Part(ServiceAttribute.DESCRIPTION.name(), "NetworkManagerTest"), 
					new Part(ServiceAttribute.SID.name(), "eu.linksmart.gc.testing.nm") };
			
			JSONObject attributesJson = new JSONObject();
			for(Part p : attributes) {
				attributesJson.put(p.getKey(), p.getValue());
			}
			registrationJson.put(KEY_ATTRIBUTES, attributesJson);
			
			PostMethod post_request = new PostMethod(nm_base_url);
			
			StringRequestEntity requestEntity = new StringRequestEntity(registrationJson.toString(), "application/json", "UTF-8");
			post_request.setRequestEntity(requestEntity);
			
    		assertEquals(200, client.executeMethod(post_request));
    		String registrationJsonString = new String(post_request.getResponseBody());
        	LOG.info("register-service-response: " + registrationJsonString);
        	post_request.releaseConnection();
        	
    		//
    		// get service registration from network-manager using its ResT interface with queryString  ?description=name
    		//
        	NameValuePair[] description_qs = { new NameValuePair("description", "NetworkManagerTest") };
        	HttpMethod  description_get_request = new GetMethod(nm_base_url);
        	description_get_request.setQueryString(description_qs);
        	assertEquals(200, client.executeMethod(description_get_request));
        	String serviceJsonString = new String(description_get_request.getResponseBody());
        	LOG.info("get-service-response: " + serviceJsonString);
        	description_get_request.releaseConnection();
        	
        	JSONArray registrationsJson = new JSONArray(serviceJsonString);
        	JSONObject jsonObject = registrationsJson.getJSONObject(0);
			
        	endPoint = jsonObject.getString(KEY_ENDPOINT);
        	
        	LOG.info("setup completed, service is accessible at endpoint: " + endPoint);
        	
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    @Test
    public void testHttpTunnel() {
    	
    	try {
    		
    		LOG.info("testing HttpTunnel");
    		
    		HttpClient client = new HttpClient();
    		
			//
        	// GET method
        	//
    		String get_service_path = "/test/resource_id?name=value";
			LOG.info("invoking service at endpoint: " + endPoint + get_service_path);
			HttpMethod  tunnel_get_request = new GetMethod(endPoint + get_service_path);
			assertEquals(200, client.executeMethod(tunnel_get_request));
        	LOG.info("get-tunnel-response: " + new String(tunnel_get_request.getResponseBody()));
        	tunnel_get_request.releaseConnection();
        	
        	//
        	// POST method
        	//
        	LOG.info("invoking service at endpoint: " + endPoint);
        	PostMethod post_request = new PostMethod(endPoint);
			String reg_post_String = getPostString();
			StringRequestEntity post_requestEntity = new StringRequestEntity(reg_post_String, "application/json", "UTF-8");
			post_request.setRequestEntity(post_requestEntity);
    		assertEquals(200, client.executeMethod(post_request));
    		String post_JsonString = new String(post_request.getResponseBody());
        	LOG.info("post-tunnel-response: " + post_JsonString);
        	post_request.releaseConnection();
        	
        	JSONObject jsonObject = new JSONObject(post_JsonString);
    		String virtualAddress = jsonObject.getString(KEY_VIRTUAL_ADDRESS);
    		
        	//
        	// PUT method
        	//
    		LOG.info("invoking service at endpoint: " + endPoint);
        	PutMethod put_request = new PutMethod(endPoint);
			String reg_put_String = getPutString(virtualAddress);
			StringRequestEntity put_requestEntity = new StringRequestEntity(reg_put_String, "application/json", "UTF-8");
			put_request.setRequestEntity(put_requestEntity);
    		assertEquals(200, client.executeMethod(put_request));
    		String put_JsonString = new String(put_request.getResponseBody());
        	LOG.info("put-tunnel-response: " + put_JsonString);
        	put_request.releaseConnection();
        	
        	JSONObject updateJsonObject = new JSONObject(put_JsonString);
			String updated_virtualAddress = updateJsonObject.getString(KEY_VIRTUAL_ADDRESS);
			
        	//
        	// DELETE method
        	//
        	String service_path = updated_virtualAddress;
        	endPoint = endPoint + "/" + service_path;
			LOG.info("invoking service at endpoint: " + endPoint);
			DeleteMethod delete_request = new DeleteMethod(endPoint);
    		assertEquals(200, client.executeMethod(delete_request));
        	LOG.info("delete-tunnel-response: " + new String(delete_request.getResponseBody()));
        	delete_request.releaseConnection();
			
        	LOG.info("HttpTunnel test successfully completed");
        	
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private String getPostString() throws Exception {
    	
    	JSONObject registrationJson = new JSONObject();
		
		registrationJson.put(KEY_ENDPOINT, "http://localhost:8882/NetworkManager");
		registrationJson.put(KEY_BACKBONE_NAME, "eu.linksmart.gc.network.backbone.protocol.http.HttpImpl");
		
		Part[] attributes = { new Part(ServiceAttribute.DESCRIPTION.name(), "NetworkManager_Test_Post"), 
				new Part(ServiceAttribute.SID.name(), "eu.linksmart.gc.testing.nm.post") };
		
		JSONObject attributesJson = new JSONObject();
		for(Part p : attributes) {
			attributesJson.put(p.getKey(), p.getValue());
		}
		registrationJson.put(KEY_ATTRIBUTES, attributesJson);
		return registrationJson.toString();
    }
    
    private String getPutString(String virtualAddress) throws Exception {
    	
    	JSONObject updateJson = new JSONObject();
    	
    	updateJson.put(KEY_ENDPOINT, "http://localhost:8882/NetworkManager");
    	updateJson.put(KEY_BACKBONE_NAME, "eu.linksmart.gc.network.backbone.protocol.http.HttpImpl");
    	updateJson.put(KEY_VIRTUAL_ADDRESS, virtualAddress);
    	
		Part[] update_attributes = { new Part(ServiceAttribute.DESCRIPTION.name(), "NetworkManager_Test_Put"), 
				new Part(ServiceAttribute.SID.name(), "eu.linksmart.gc.testing.nm.put") };

		JSONObject update_attributesJson = new JSONObject();
		for(Part p : update_attributes) {
			update_attributesJson.put(p.getKey(), p.getValue());
		}
		updateJson.put(KEY_ATTRIBUTES, update_attributesJson);
		
		return updateJson.toString();
    }
}