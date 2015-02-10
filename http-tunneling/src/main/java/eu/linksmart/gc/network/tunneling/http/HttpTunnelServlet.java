/*
 * In case of German law being applicable to this license agreement, the following warranty and liability terms shall apply:
 *
 * 1. Licensor shall be liable for any damages caused by wilful intent or malicious concealment of defects.
 * 2. Licensor's liability for gross negligence is limited to foreseeable, contractually typical damages.
 * 3. Licensor shall not be liable for damages caused by slight negligence, except in cases 
 *    of violation of essential contractual obligations (cardinal obligations). Licensee's claims for 
 *    such damages shall be statute barred within 12 months subsequent to the delivery of the software.
 * 4. As the Software is licensed on a royalty free basis, any liability of the Licensor for indirect damages 
 *    and consequential damages - except in cases of intent - is excluded.
 *
 * This limitation of liability shall also apply if this license agreement shall be subject to law 
 * stipulating liability clauses corresponding to German law.
 */
/**
 * Copyright (C) 2006-2010 [Telefonica I+D]
 *                         the HYDRA consortium, EU project IST-2005-034891
 *
 * This file is part of LinkSmart.
 *
 * LinkSmart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE
 * version 3 as published by the Free Software Foundation.
 *
 * LinkSmart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with LinkSmart.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.linksmart.gc.network.tunneling.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import eu.linksmart.gc.api.types.TunnelRequest;
import eu.linksmart.gc.api.types.TunnelResponse;
import eu.linksmart.gc.api.types.utils.SerializationUtil;
import eu.linksmart.network.NMResponse;
import eu.linksmart.network.VirtualAddress;
import eu.linksmart.network.networkmanager.core.NetworkManagerCore;

/**
 * HTTP Tunnel servlet
 */
public class HttpTunnelServlet extends HttpServlet {

	private static final long serialVersionUID = -3819571723858557513L;

	private static final Logger LOG = Logger.getLogger(HttpTunnelServlet.class.getName());
	
	private NetworkManagerCore nmCore = null;

	/**
	 * Constructor with parameters
	 * 
	 * @param nmCore
	 *            the Network Manager application
	 * 
	 */
	public HttpTunnelServlet(NetworkManagerCore nmCore) {
		this.nmCore = nmCore;
	}

	/**
	 * Performs the HTTP GET operation
	 * 
	 * @param request HttpServletRequest that encapsulates the request to the servlet 
	 * @param response HttpServletResponse that encapsulates the response from the servlet
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		//
		// get path info
		//
		String pathInfo = request.getPathInfo();
		
		LOG.info("doGet - pathInfo:" + pathInfo);
		
		if(pathInfo == null || pathInfo.length() == 0) {
			LOG.error("pathInfo is empty, no virtual-address is given");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "pathInfo is empty, no virtual-address is given");
			return;
		}
		
		StringTokenizer path_tokens = new StringTokenizer(pathInfo, "/");
		
		if(path_tokens.countTokens() < 2) {
			LOG.error("sender/receiver virtual-address(s) are missing");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "sender/receiver virtual-address(s) are missing");
			return;
		}
		
		VirtualAddress senderVAD = null;
		VirtualAddress receiverVAD = null;
		byte[] tunnel_request_data = null;
		
		try {
			
			//
			// extract sender virtual address from path
			//
			String sender_vad_string = path_tokens.nextToken();
			
			senderVAD = getSenderVAD(sender_vad_string);
			
			if(senderVAD == null) {
				LOG.error("unable to retrieve sender virtual-address from path token: " + sender_vad_string);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,"unable to retrieve sender virtual-address from path token: " + sender_vad_string);
				return;
			}
			if(senderVAD != null) {
				if(senderVAD.getBytes().length != VirtualAddress.VIRTUAL_ADDRESS_BYTE_LENGTH) {
					LOG.error("provided sender virtual-address doesn't conform to its format: " + senderVAD.toString());
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided sender virtual-address doesn't conform to its format: " + senderVAD.toString());
					return;
				}
				if(senderVAD.toString() == "0.0.0.0") {
					LOG.error("computed sender virtual-address doesn't conform to its format: " + senderVAD.toString());
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "computed sender virtual-address doesn't conform to its format: " + senderVAD.toString());
					return;
				}
			}
			
			//
			// extract receiver virtual address from path
			//
			String receiver_vad_string = path_tokens.nextToken();
			
			receiverVAD = getReceiverVAD(receiver_vad_string);
			
			if(receiverVAD == null) {
				LOG.error("unable to retrieve receiver virtual-address from path token: " + receiver_vad_string);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,"unable to retrieve receiver virtual-address from path token: " + receiver_vad_string);
				return;
			}
			if(receiverVAD != null) {
				if(receiverVAD.getBytes().length != VirtualAddress.VIRTUAL_ADDRESS_BYTE_LENGTH) {
					LOG.error("provided reciever virtual-address doesn't conform to its format: " + receiverVAD.toString());
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided reciever virtual-address doesn't conform to its format: " + receiverVAD.toString());
					return;
				}
				if(receiverVAD.toString() == "0.0.0.0") {
					LOG.error("computed reciever virtual-address doesn't conform to its format: " + receiverVAD.toString());
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "computed reciever virtual-address doesn't conform to its format: " + receiverVAD.toString());
					return;
				}
			}
			
			//
			// add service_path, attributes (if any) and headers
			//
			String service_path = "";
			
			while(path_tokens.hasMoreTokens()) {
				service_path = service_path + "/" + path_tokens.nextToken();
			}
			
			StringBuilder queryBuilder = new StringBuilder();
			String query_string = request.getQueryString();
			if (query_string != null && query_string.length() != 0) {
				queryBuilder.append("?");
				queryBuilder.append(query_string);
			}
			
			StringBuilder servicePathBuilder = new StringBuilder();
			servicePathBuilder.append(service_path);
			servicePathBuilder.append(queryBuilder.toString());
			
			TunnelRequest tunnel_request = new TunnelRequest();
			
			tunnel_request.setMethod(request.getMethod());
			tunnel_request.setPath(servicePathBuilder.toString());
			tunnel_request.setHeaders(buildHeaders(request));
			
			tunnel_request_data = SerializationUtil.serialize(tunnel_request);
			
			LOG.info("s-vad: " + senderVAD.toString());
			LOG.info("r-vad: " + receiverVAD.toString());		
			LOG.info("servicePath: " + servicePathBuilder.toString());
			LOG.info("headers: " + tunnel_request.getHeaders());
			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
		
		//
		// send tunnel request to NetworkManagerCore
		//
		NMResponse nm_response = null;
		try {
			nm_response = this.nmCore.sendData(senderVAD, receiverVAD, tunnel_request_data, true);
			LOG.info("received response from nm - status: " + nm_response.getStatus());
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
		
		//
		// read tunnel response
		//
		TunnelResponse tunnel_response = null;
		try {
			tunnel_response = (TunnelResponse) SerializationUtil.deserialize(nm_response.getMessageBytes());
			LOG.info("http-tunnel-status: " + tunnel_response.getStatusCode());
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
		
		byte[] servlet_response_body = null;
		
		if (tunnel_response.getStatusCode() != HttpServletResponse.SC_OK) {
			response.setStatus(tunnel_response.getStatusCode());
			//set whole response data as servlet body
			servlet_response_body = tunnel_response.getBody();
		} else {
			//take headers from data and add them to response body
			servlet_response_body = addHeadersToResponse(tunnel_response.getBody(), response);
		}
		
		//
		// write servlet_response_body data
		//
		response.setContentLength(servlet_response_body.length);
		response.getOutputStream().write(servlet_response_body);
		response.getOutputStream().close();		
	}
	
	/**
	 * Performs the HTTP POST operation
	 * 
	 * @param request
	 *            HttpServletRequest that encapsulates the request to the
	 *            servlet
	 * @param response
	 *            HttpServletResponse that encapsulates the response from the
	 *            servlet
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
//		VirtualAddress senderVirtualAddress = null;
//		VirtualAddress receiverVirtualAddress = null;
//		StringBuilder requestBuilder = null;
//
//		// split path - path contains virtual addresses and maybe "wsdl" separated by "/"
//		String path = request.getPathInfo();
//		StringTokenizer token = new StringTokenizer(path, "/");
//		if (token.countTokens() >= 2) {
//			try{
//				requestBuilder = new StringBuilder();
//				String sVirtualAddress = token.nextToken();
//				senderVirtualAddress = (sVirtualAddress.contentEquals("0")) ? nmCore.getService() : new VirtualAddress(sVirtualAddress);
//				receiverVirtualAddress = new VirtualAddress(token.nextToken());
//				// sessionID = new VirtualAddress(token.nextToken());
//
//				// append headers and blank line for end of headers
//				requestBuilder.append(buildHeaders(request));
//				requestBuilder.append("\r\n");
//
//				// append content
//				if ((request.getContentLength() > 0)) {
//					try {
//						BufferedReader reader = request.getReader();
//						for (String line = null; (line = reader.readLine()) != null;)
//							requestBuilder.append(line);
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//
//				// send request to NetworkManagerCore
//				LOG.debug("Sending soap request through tunnel");
//			}catch (Exception e) {
//				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//				return;
//			}
//			sendRequest(senderVirtualAddress, receiverVirtualAddress, requestBuilder.toString(), response);
//		} else {
//			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//		}
	}
	
	/**
	 * Performs the HTTP DELETE operation
	 * Is identical to GET
	 * @param request HttpServletRequest that encapsulates the request to the servlet 
	 * @param response HttpServletResponse that encapsulates the response from the servlet
	 */
	public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
	}

	/**
	 * Builds the string that represents the headers of a HttpServletRequest
	 * @param request the HttpServletRequest of which to extract the headers
	 * @return the String representing the headers
	 */
	private String buildHeaders(HttpServletRequest request) {
		StringBuilder builder = new StringBuilder();
		Enumeration<?> headerNames = request.getHeaderNames();

		while (headerNames.hasMoreElements()) {
			String header = (String) headerNames.nextElement();
			String value = request.getHeader(header);
			builder.append(header + ": " + value + "\r\n");
		}

		return builder.toString();
	}
	
	private byte[] addHeadersToResponse(byte[] byteData, HttpServletResponse response) {
		int bodyStartIndex = 0;
		byte[] headerEnd = new String("\r\n\r\n").getBytes();
		//find end of header
		for(;bodyStartIndex < byteData.length; bodyStartIndex++) {
			if(bodyStartIndex + headerEnd.length < byteData.length) {
				if(Arrays.equals(Arrays.copyOfRange(byteData, bodyStartIndex, bodyStartIndex + headerEnd.length), headerEnd)) {
					bodyStartIndex = bodyStartIndex + headerEnd.length;
					break;
				}
			} else {
				bodyStartIndex = byteData.length;
				break;
			}
		}
		byte[] headersBytes = Arrays.copyOf(byteData, bodyStartIndex);
		String[] headers = new String(headersBytes).split("(?<=\r\n)");
		//go through headers and put them to response until empty line is reached
		for (String header : headers) {	
			if(header.contentEquals("\r\n")) {
				break;
			}
			if(!header.toLowerCase().startsWith("http")) {
				header = header.replace("\r\n", "");
				String[] headerParts = header.split(":");
				if(headerParts.length == 2) {
					response.setHeader(headerParts[0], headerParts[1].trim());
				}
			}
		}
		return Arrays.copyOfRange(byteData, bodyStartIndex, byteData.length);
	}
	
	private VirtualAddress getSenderVAD(String token) throws Exception {
		VirtualAddress senderVAD = null;
		if(token.contentEquals("0")) {
			senderVAD = this.nmCore.getVirtualAddress();
		} else {
			Pattern s_pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
			Matcher s_matcher = s_pattern.matcher(token);
			if(s_matcher.find()) {
				senderVAD = new VirtualAddress(s_matcher.group());
			}
		}
		return senderVAD;
	}
	
	private VirtualAddress getReceiverVAD(String token) throws Exception {
		VirtualAddress receiverVAD = null;
		Pattern r_pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
		Matcher r_matcher = r_pattern.matcher(token);
		if(r_matcher.find()) {
			receiverVAD = new VirtualAddress(r_matcher.group());
		} 
		return receiverVAD;
	}
}
