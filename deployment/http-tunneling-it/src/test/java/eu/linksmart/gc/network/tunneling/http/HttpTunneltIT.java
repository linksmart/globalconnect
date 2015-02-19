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

@RunWith(PaxExam.class)
public class HttpTunneltIT {
	
	private static Logger LOG = Logger.getLogger(HttpTunneltIT.class.getName());
	
	private String nm_base_url = "http://localhost:8882/NetworkManager";
	
	private static final String KEY_ENDPOINT = "Endpoint";
	
	private static final String KEY_TEMPERATURE = "Temperature";
	
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
    		
    		HttpClient client = new HttpClient();
    		
    		//
    		// get service registration from network-manager using its ResT interface with queryString  ?description=name
    		//
        	NameValuePair[] description_qs = { new NameValuePair("description", "WeatherService") };
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
    		String get_service_path = "/sensor/1?loc=abc";
			LOG.info("invoking GET at endpoint: " + endPoint + get_service_path);
			HttpMethod  tunnel_get_request = new GetMethod(endPoint + get_service_path);
			int get_status_code = client.executeMethod(tunnel_get_request);
        	LOG.info("get-tunnel-response: " + new String(tunnel_get_request.getResponseBody()));
        	tunnel_get_request.releaseConnection();
        	assertEquals(200, get_status_code);
        	
        	//
        	// POST method
        	//
        	LOG.info("invoking POST at endpoint: " + endPoint);
        	PostMethod post_request = new PostMethod(endPoint);
			String reg_post_String = getPostString();
			StringRequestEntity post_requestEntity = new StringRequestEntity(reg_post_String, "application/json", "UTF-8");
			post_request.setRequestEntity(post_requestEntity);
			int post_status_code = client.executeMethod(post_request);
    		String post_JsonString = new String(post_request.getResponseBody());
        	LOG.info("post-tunnel-response: " + post_JsonString);
        	post_request.releaseConnection();
        	assertEquals(200, post_status_code);
        	
        	JSONObject jsonObject = new JSONObject(post_JsonString);
    		int temperature = jsonObject.getInt(KEY_TEMPERATURE);
    		
        	//
        	// PUT method
        	//
    		LOG.info("invoking PUT at endpoint: " + endPoint);
        	PutMethod put_request = new PutMethod(endPoint);
			String reg_put_String = getPutString();
			StringRequestEntity put_requestEntity = new StringRequestEntity(reg_put_String, "application/json", "UTF-8");
			put_request.setRequestEntity(put_requestEntity);
			int put_status_code = client.executeMethod(put_request);
    		String put_JsonString = new String(put_request.getResponseBody());
        	LOG.info("put-tunnel-response: " + put_JsonString);
        	put_request.releaseConnection();
        	assertEquals(200, put_status_code);
        	
        	JSONObject updateJsonObject = new JSONObject(put_JsonString);
        	int updated_temperature = updateJsonObject.getInt(KEY_TEMPERATURE);
			
        	//
        	// DELETE method
        	//
        	String service_path = "123";
        	endPoint = endPoint + "/" + service_path;
			LOG.info("invoking DELETE at endpoint: " + endPoint);
			DeleteMethod delete_request = new DeleteMethod(endPoint);
			int delete_status_code = client.executeMethod(delete_request);
        	LOG.info("delete-tunnel-response: " + new String(delete_request.getResponseBody()));
        	delete_request.releaseConnection();
        	assertEquals(200, delete_status_code);
        	
        	LOG.info("HttpTunnel test successfully completed");
        	
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private String getPostString() throws Exception {
    	JSONObject postJson = new JSONObject();
    	postJson.put("Temperature", 28);
		return postJson.toString();
    }
    
    private String getPutString() throws Exception {
    	
    	JSONObject updateJson = new JSONObject();
    	updateJson.put("Temperature", 20);
		return updateJson.toString();
    }
}