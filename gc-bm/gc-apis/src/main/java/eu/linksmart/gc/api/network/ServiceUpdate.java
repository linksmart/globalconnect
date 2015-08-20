package eu.linksmart.gc.api.network;

import java.io.Serializable;

import eu.linksmart.gc.api.network.Registration;

public class ServiceUpdate implements Serializable {

	private static final long serialVersionUID = -8324166037777756972L;
	
	private Registration registration = null;
	private String operation = "N";
	
	public ServiceUpdate(Registration registration, String operation) {
		this.setRegistration(registration);
		this.setOperation(operation);
	}

	public Registration getRegistration() {
		return registration;
	}

	public void setRegistration(Registration registration) {
		this.registration = registration;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

}
