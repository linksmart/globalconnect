package eu.linksmart.gc.api.types;

import java.io.Serializable;

public class TunnelResponse extends TunnelBase implements Serializable {


    private static final long serialVersionUID = 533541821279624544L;

    private int status_code = 0;

	
	public void setStatusCode(int status_code) {
		this.status_code = status_code;
	}
	
	public int getStatusCode() {
		return this.status_code;
	}
	

}
