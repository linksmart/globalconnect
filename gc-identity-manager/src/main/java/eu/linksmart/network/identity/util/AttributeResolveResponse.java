package eu.linksmart.network.identity.util;

import java.io.Serializable;

import eu.linksmart.network.VirtualAddress;

/**
 * Class containing virtualAddress and AttributeResponseFilter pairs.
 * @author Vinkovits
 *
 */
public class AttributeResolveResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	VirtualAddress virtualAddress;
	AttributeResolveFilter filter;

	public AttributeResolveResponse(VirtualAddress virtualAddress, AttributeResolveFilter filter) {
		this.virtualAddress = virtualAddress;
		this.filter = filter;
	}

	public VirtualAddress getService() {
		return this.virtualAddress;
	}

	public AttributeResolveFilter getFilter() {
		return this.filter;
	}
}
