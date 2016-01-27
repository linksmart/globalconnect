package eu.linksmart.gc.api.types;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by José Ángel Carvajal on 27.01.2016 a researcher of Fraunhofer FIT.
 */
public abstract class  TunnelBase  implements Serializable {

    private static final long serialVersionUID = -4308803348123452636L;
    protected Map<String,String> http_headers = null;
    protected byte[] http_body = null;

    public Map<String, String> getHeaders() {
        return http_headers;
    }

    public void setHeaders(Map<String, String> http_headers) {
        this.http_headers = http_headers;
    }

    public byte[] getBody() {
        return http_body;
    }

    public void setBody(byte[] http_body) {
        this.http_body = http_body;
    }
}
