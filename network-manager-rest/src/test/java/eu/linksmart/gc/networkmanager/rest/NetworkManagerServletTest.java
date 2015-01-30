package eu.linksmart.gc.networkmanager.rest;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.Hashtable;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import eu.linksmart.gc.networkmanager.rest.NetworkManagerRestPort;
import eu.linksmart.gc.networkmanager.rest.NetworkManagerServlet;
import eu.linksmart.network.Registration;
import eu.linksmart.network.ServiceAttribute;
import eu.linksmart.network.VirtualAddress;
import eu.linksmart.network.networkmanager.NetworkManager;
import eu.linksmart.utils.Part;

public class NetworkManagerServletTest {

	private static final String vadString = "0.0.0.6986094776732394497";
	private static final String QUERY_STRING = "description=\"Calculator\"";
	private static final String CALCULATOR_DESCRIPTION = "Calculator";
	private static final String ENDPOINT = "http://localhost:9090/cxf/services/Calculator";
	private static final String BACKBONE_NAME = "eu.linksmart.network.grand.backbone.BackboneGrandImpl";
	private static final String GET_RESPONSE_JSON = "[{\"Attributes\":{\"DESCRIPTION\":\"Calculator\"},\"VirtualAddress\":\"" + vadString + "\"}]";
	private static final String POST_REQUEST_JSON = "{\"Attributes\":{\"DESCRIPTION\":\"Calculator\"}, \"Endpoint\":\"" + ENDPOINT + "\", \"BackboneName\":\"" + BACKBONE_NAME + "\"}";
	private NetworkManager nmCore;
	private HttpServletResponse response;
	private VirtualAddress calculatorVad = new VirtualAddress(vadString);
	private Part[] descriptionOnly;
	private NetworkManagerRestPort nmRestPort;
	private NetworkManagerServlet nmRestPortServlet;
	private Registration calculatorRegistration;
	private ServletOutputStream outStream;


	@Before
	public void setUp(){
		//mock objects
		this.response = mock(HttpServletResponse.class);
		this.nmCore = mock(NetworkManager.class);
		this.outStream = mock(ServletOutputStream.class);

		//init objects	
		this.nmRestPort = new NetworkManagerRestPort();
		this.nmRestPort.networkManager = this.nmCore;
		this.nmRestPortServlet = new NetworkManagerServlet(this.nmRestPort);
		this.descriptionOnly = new Part[]{new Part(ServiceAttribute.DESCRIPTION.name(), CALCULATOR_DESCRIPTION)};
		this.calculatorRegistration = new Registration(calculatorVad, descriptionOnly);

		//mock methods
		try{
			when(response.getOutputStream()).thenReturn(outStream);
			when(this.nmCore.getServiceByAttributes(any(Part[].class))).thenReturn(new Registration[]{new Registration(calculatorVad, this.descriptionOnly)});
		} catch (Exception e) {
			//NOP
		}
	}
	
	@Test
	public void doGetTest() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getQueryString()).thenReturn(QUERY_STRING);
		
		try {
			when(this.nmCore.getServiceByAttributes(descriptionOnly)).thenReturn(new Registration[]{this.calculatorRegistration});
		} catch (RemoteException e1) {
			//not relevant
		}
		
		try {
			nmRestPortServlet.doGet(request, response);
		} catch (IOException e) {
			fail(e.getMessage());
		}
		try {
			Mockito.verify(this.outStream).write(GET_RESPONSE_JSON.getBytes());
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void doPostTest() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		BufferedReader reader = new BufferedReader(new StringReader(POST_REQUEST_JSON + "\n"));
		try {
			when(request.getReader()).thenReturn(reader);
			when(request.getContentLength()).thenReturn(POST_REQUEST_JSON.length());
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
		try {
			nmRestPortServlet.doPost(request, response);
		} catch (IOException e1) {
			fail(e1.getMessage());
		}
		
		try {
			Mockito.verify(this.nmCore).registerService(descriptionOnly, ENDPOINT, BACKBONE_NAME);
		} catch (RemoteException e) {
			// not relevant as local invocation
		}
	}
	
	@Test
	public void doDeleteTest() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getPathInfo()).thenReturn("/" + this.calculatorVad.toString());
		
		try {
			nmRestPortServlet.doDelete(request, response);
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
		try {
			Mockito.verify(this.nmCore).removeService(this.calculatorVad);
		} catch (RemoteException e) {
			//not relevant as local invocation
		}
	}
}
