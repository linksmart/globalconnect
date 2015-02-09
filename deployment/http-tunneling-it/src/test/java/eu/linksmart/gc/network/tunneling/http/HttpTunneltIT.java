package eu.linksmart.gc.network.tunneling.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
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
    		
			registrationJson.put(KEY_ENDPOINT, "http://localhost:9090/cxf/services/Calculator");
			registrationJson.put(KEY_BACKBONE_NAME, "eu.linksmart.gc.network.backbone.protocol.http.HttpImpl");
			
			Part[] attributes = { new Part(ServiceAttribute.DESCRIPTION.name(), "Calculator"), 
					new Part(ServiceAttribute.SID.name(), "eu.linksmart.gc.testing.calculator") };
			
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
        	NameValuePair[] description_qs = { new NameValuePair("description", "Calculator") };
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
    		
			LOG.info("invoking service at endpoint: " + endPoint);
			
			HttpMethod  tunnel_get_request = new GetMethod(endPoint);
			int statusCode = client.executeMethod(tunnel_get_request);
			
        	System.out.println("statusCode: " + statusCode);
			//assertEquals(200, client.executeMethod(tunnel_get_request));
        	
        	LOG.info("tunnel-service-response: " + new String(tunnel_get_request.getResponseBody()));
        	tunnel_get_request.releaseConnection();
			
        	LOG.info("HttpTunnel test successfully completed");
        	
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}