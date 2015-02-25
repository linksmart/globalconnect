package eu.linksmart.security.communication;

//do not forget to add/change the appropriate lines in /resources/BBJXTA.properties 
//when making changes to this enum
public enum SecurityProperty {
	NoEncoding,
	NoSecurity,
	Symmetric,
	Asymmetric,
	Broadcast,
	Unicast,
	Integrity,
	Confidentiality,
	NonRepudiation,
	Authenticity,
	KeyAgreement,
	SharedSecret
}
