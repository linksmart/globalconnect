package eu.linksmart.gc.api.types;

import java.io.Serializable;

public class TunnelRequest extends TunnelBase implements Serializable {


    private static final long serialVersionUID = 5632627658042014383L;
    private String http_method = null;
	private String http_path = null;

	
	public void setMethod(String http_method) {
		this.http_method = http_method;
	}
	
	public String getMethod() {
		return this.http_method;
	}
	
	public void setPath(String http_path) {
		this.http_path = http_path;
	}

	public String getPath() {
		return this.http_path;
	}

}
