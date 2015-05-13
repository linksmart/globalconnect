package eu.linksmart.gc.networkmanager.rest;

import java.util.Scanner;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import eu.linksmart.gc.api.network.ServiceAttribute;
import eu.linksmart.gc.api.network.VirtualAddress;
import eu.linksmart.gc.api.utils.Part;
import eu.linksmart.it.utils.ITConfiguration;

public class RegistrationTest {
	
	private static Logger LOG = Logger.getLogger(RegistrationTest.class.getName());
	
	private String base_url = "http://localhost:8082/NetworkManager";
	
	private static final String KEY_ENDPOINT = "Endpoint";
	private static final String KEY_BACKBONE_NAME = "BackboneName";
	private static final String KEY_ATTRIBUTES = "Attributes";
	private static final String KEY_VIRTUAL_ADDRESS = "VirtualAddress";
	
	private HttpClient httpClient = null;
	
    @Before
    public void setUp() {
    	httpClient = new HttpClient();
    }
    
    @Test
    public void testRegistration() {
    	
    	try {
    		
    		//
        	// register service
        	//
    		
    		LOG.info("testing POST method: " + base_url);
    		
    		String jsonString = createPostJSON();
    		
			PostMethod post_request = new PostMethod(base_url);
			
			StringRequestEntity requestEntity = new StringRequestEntity(jsonString, "application/json", "UTF-8");
			post_request.setRequestEntity(requestEntity);
			
			int statusCode = this.httpClient.executeMethod(post_request);
			System.out.println("post-status-code: " + statusCode);
			
    		String registrationJsonString = new String(post_request.getResponseBody());
        	System.out.println("post-response: " + registrationJsonString);
        	
        	post_request.releaseConnection();
        	assertEquals(200, statusCode);
        	
        	JSONObject jsonObject = new JSONObject(registrationJsonString.toString());
			String virtualAddress = jsonObject.getString(KEY_VIRTUAL_ADDRESS);
			
			NameValuePair[] description_qs = { new NameValuePair("description", "testserver/ServiceA") };
        	HttpMethod  description_get_request = new GetMethod(base_url);
        	description_get_request.setQueryString(description_qs);
        	
        	int statusCode2 = this.httpClient.executeMethod(description_get_request);
        	System.out.println("status-code: " + statusCode2);
        	System.out.println("get-description: " + new String(description_get_request.getResponseBody()));
        	
        	description_get_request.releaseConnection();
        	
        	
        	//
        	// sleep for some time
        	//
        	Thread.sleep(10000);
        	//
        	// removing service
        	//
    		LOG.info("testing DELETE method: " + base_url + " for VAD: " + virtualAddress);
    		
        	DeleteMethod delete_request = new DeleteMethod(base_url + "/" + virtualAddress);
        	
        	int statusCode3 = this.httpClient.executeMethod(delete_request);
    		System.out.println("delete-status-code: " + statusCode3);
    		
    		System.out.println("delete-response: " + new String(delete_request.getResponseBody()));
        	
    		delete_request.releaseConnection();
    		assertEquals(200, statusCode);
        	
        	LOG.info("testPOST successfully completed");
        	
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e.getMessage());
		}
    }
    
    private String createPostJSON() throws Exception {
    	
    	JSONObject registrationJson = new JSONObject();
		
		registrationJson.put(KEY_ENDPOINT, "http://localhost:8082/ServiceA");
		registrationJson.put(KEY_BACKBONE_NAME, "eu.linksmart.gc.network.backbone.protocol.http.HttpImpl");
		
		String cregistrationJson = readFileContents("/registration.json");
		
		Part[] attributes = { new Part(ServiceAttribute.DESCRIPTION.name(), "testserver/ServiceA"), 
				new Part("SC", cregistrationJson) };
		
		JSONObject attributesJson = new JSONObject();
		for(Part p : attributes) {
			attributesJson.put(p.getKey(), p.getValue());
		}
		registrationJson.put(KEY_ATTRIBUTES, attributesJson);
		
    	return registrationJson.toString();
    }
    
    private String readFileContents(String fileName) {
		StringBuilder builder = null;
		Scanner scanner = null;
		try {
			builder = new StringBuilder();
			scanner = new Scanner(this.getClass().getResourceAsStream(fileName));
			String lineSeparator = System.getProperty("line.separator");
	        while(scanner.hasNextLine()) {        
	            builder.append(scanner.nextLine() + lineSeparator);
	        }
	    } catch(Exception e) {
	    	e.printStackTrace();
	    } finally {
	        scanner.close();
	    }
		return builder.toString();
	}
}