package eu.linksmart.gc.sc.client;

import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import eu.linksmart.gc.api.network.ServiceAttribute;
import eu.linksmart.gc.api.network.VirtualAddress;
import eu.linksmart.gc.api.utils.Part;
import eu.linksmart.lc.sc.client.ServiceBuilder;

import eu.linksmart.lc.sc.types.Registration;

public class ClientImplTest {
	
	//@Test
	public void testClient() {
		
		//
		// create service registration
		//
		Registration cregistration = ServiceBuilder.createRegistration("testservice", "ServiceA", "http://localhost:8080");
		
		SCClientImpl client = new SCClientImpl();
		//client.activate(null);
		
		//
		// add service registration
		//
		Part[] attributes = { new Part(ServiceAttribute.DESCRIPTION.name(), cregistration.getId()), 
				new Part("SC", new Gson().toJson(cregistration)) };
		eu.linksmart.gc.api.network.Registration sregistration = new eu.linksmart.gc.api.network.Registration(new VirtualAddress(), attributes);
		
		assertTrue(client.add(sregistration));
		
		assertTrue(client.update(sregistration));
		
		assertTrue(client.delete(sregistration));
			
	}
}
