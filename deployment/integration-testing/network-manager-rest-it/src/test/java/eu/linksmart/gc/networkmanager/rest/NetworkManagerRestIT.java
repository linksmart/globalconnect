package eu.linksmart.gc.networkmanager.rest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import eu.linksmart.it.utils.ITConfiguration;
import eu.linksmart.network.ServiceAttribute;
import eu.linksmart.utils.Part;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NetworkManagerRestIT {
	
	private static Logger LOG = Logger.getLogger(NetworkManagerRestIT.class.getName());
	
	private String base_url = "http://localhost:8882/NetworkManager";
	
	private static final String KEY_ENDPOINT = "Endpoint";
	private static final String KEY_BACKBONE_NAME = "BackboneName";
	private static final String KEY_ATTRIBUTES = "Attributes";
	private static final String KEY_VIRTUAL_ADDRESS = "VirtualAddress";
	
    @Configuration
    public Option[] config() {
        return new Option[] {
        		ITConfiguration.regressionDefaults(),
        		features(ITConfiguration.getTestingFeaturesRepoURL(),"gc-network-manager-rest-it"),  
        };
    }
    
    @Test
    public void testGetMethod() {
    	
    	try {

    		HttpClient client = new HttpClient();
    		
    		//
    		// test service discovery/search by paramerization
    		//
    		
    		LOG.info("testing Get Method: " + base_url);
    			  	
        	//
        	// with queryString  ?description=NetworkManager:LinkSmartUser
        	//
        	NameValuePair[] description_qs = { new NameValuePair("description", "NetworkManager:LinkSmartUser") };
        	HttpMethod  description_get_request = new GetMethod(base_url);
        	description_get_request.setQueryString(description_qs);
        	assertEquals(200, client.executeMethod(description_get_request));
        	System.out.println("description-response-string" + new String(description_get_request.getResponseBody()));
        	description_get_request.releaseConnection();
        	
        	//
        	// with queryString  ?pid=eu.linksmart.network
        	//
        	NameValuePair[] pid_qs = { new NameValuePair("pid", "eu.linksmart.network") };
        	HttpMethod  pid_qs_get_request = new GetMethod(base_url);
        	pid_qs_get_request.setQueryString(pid_qs);
        	assertEquals(404, client.executeMethod(pid_qs_get_request));
        	System.out.println("pid-response-string" + new String(pid_qs_get_request.getResponseBody()));
        	pid_qs_get_request.releaseConnection();
        	
        	//
        	// with queryString  ?query=querytoexecute
        	//
        	NameValuePair[] query_qs = { new NameValuePair("query", "NetworkManager:LinkSmartUser") };
        	HttpMethod  query_qs_get_request = new GetMethod(base_url);
        	query_qs_get_request.setQueryString(query_qs);
        	assertEquals(404, client.executeMethod(query_qs_get_request));
        	System.out.println("query-response-string" + new String(query_qs_get_request.getResponseBody()));
        	query_qs_get_request.releaseConnection();
        	
        	//
        	// with queryString  ?att-name=att-value
        	//
        	NameValuePair[] single_att_qs = { new NameValuePair("att-name", "att-value") };
        	HttpMethod  single_att_qs_get_request = new GetMethod(base_url);
        	single_att_qs_get_request.setQueryString(single_att_qs);
        	assertEquals(404, client.executeMethod(single_att_qs_get_request));
        	System.out.println("single_att-response-string" + new String(single_att_qs_get_request.getResponseBody()));
        	single_att_qs_get_request.releaseConnection();
        	
        	//
        	// with queryString  ?description=NetworkManager:LinkSmartUser&pid=eu.linksmart.network
        	//
        	NameValuePair[] multi_att_qs = { new NameValuePair("description", "NetworkManager:LinkSmartUser"), new NameValuePair("pid", "eu.linksmart.network") };
        	HttpMethod  multi_att_qs_get_request = new GetMethod(base_url);
        	multi_att_qs_get_request.setQueryString(multi_att_qs);
        	assertEquals(404, client.executeMethod(multi_att_qs_get_request));
        	System.out.println("multi_att-response-string" + new String(multi_att_qs_get_request.getResponseBody()));
        	multi_att_qs_get_request.releaseConnection();
        	
        	//
        	// with queryString  ?description=NetworkManager:LinkSmartUser&timeOut=12345678&returnFirst=false&isStrictRequest=false
        	//
        	NameValuePair[] multi_att_params_qs = { new NameValuePair("description", "NetworkManager:LinkSmartUser"),
        			new NameValuePair("timeOut", "30000"),
        			new NameValuePair("returnFirst", "true"),
        			new NameValuePair("isStrictRequest", "false") };
        	HttpMethod  multi_att_params_qs_get_request = new GetMethod(base_url);
        	multi_att_params_qs_get_request.setQueryString(multi_att_params_qs);
        	assertEquals(404, client.executeMethod(multi_att_params_qs_get_request));
        	System.out.println("multi_att_params-response-string" + new String(multi_att_params_qs_get_request.getResponseBody()));
        	multi_att_params_qs_get_request.releaseConnection();
        	
        	LOG.info("testGetMethod successfully completed");
        	
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    @Test
    public void testOtherMethods() {
    	
    	try {
    		
    		HttpClient client = new HttpClient();
    		
    		//
        	// register service
        	//
    		
    		LOG.info("testing POST method: " + base_url);
    		
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
			
			PostMethod post_request = new PostMethod(base_url);
			
			StringRequestEntity requestEntity = new StringRequestEntity(registrationJson.toString(), "application/json", "UTF-8");
			post_request.setRequestEntity(requestEntity);
			
    		assertEquals(200, client.executeMethod(post_request));
    		String registrationJsonString = new String(post_request.getResponseBody());
        	System.out.println("post-response-string" + registrationJsonString);
        	post_request.releaseConnection();
        	
        	JSONObject jsonObject = new JSONObject(registrationJsonString.toString());
			String virtualAddress = jsonObject.getString(KEY_VIRTUAL_ADDRESS);
	
        	//
        	// updating service
        	//
			
			LOG.info("testing PUT method: " + base_url);
        	
        	JSONObject updateJson = new JSONObject();
        	
        	updateJson.put(KEY_ENDPOINT, "http://localhost:9090/cxf/services/Calculator2");
        	updateJson.put(KEY_BACKBONE_NAME, "eu.linksmart.gc.network.backbone.protocol.http.HttpImpl2");
        	updateJson.put(KEY_VIRTUAL_ADDRESS, virtualAddress);
        	
			Part[] update_attributes = { new Part(ServiceAttribute.DESCRIPTION.name(), "Calculator2"), 
					new Part(ServiceAttribute.SID.name(), "eu.linksmart.gc.testing.calculator2") };

			JSONObject update_attributesJson = new JSONObject();
			for(Part p : update_attributes) {
				update_attributesJson.put(p.getKey(), p.getValue());
			}
			updateJson.put(KEY_ATTRIBUTES, update_attributesJson);
			
			PutMethod put_request = new PutMethod(base_url);
			
			StringRequestEntity put_requestEntity = new StringRequestEntity(updateJson.toString(), "application/json", "UTF-8");
			put_request.setRequestEntity(put_requestEntity);
			
    		assertEquals(200, client.executeMethod(put_request));
    		String updateJsonString = new String(put_request.getResponseBody());
        	System.out.println("put-response-string" + updateJsonString);
        	put_request.releaseConnection();
        	
        	JSONObject updateJsonObject = new JSONObject(updateJsonString.toString());
			String updated_virtualAddress = updateJsonObject.getString(KEY_VIRTUAL_ADDRESS);
			
			//
        	// removing service
        	//
			
			LOG.info("testing DELETE method: " + base_url);
			
        	DeleteMethod delete_request = new DeleteMethod(base_url + "/" + updated_virtualAddress);
			assertEquals(200, client.executeMethod(delete_request));
			System.out.println("delete-response-string" + new String(delete_request.getResponseBody()));
        	delete_request.releaseConnection();
        	
        	LOG.info("testOtherMethods successfully completed");
        	
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}