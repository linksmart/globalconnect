package eu.linksmart.gc.server;

public class ConfigConstants {
	
	/**
     * System property to lookup GC configuration path.
     */
    public static final String GC_CONFIGURATION_PATH_KEY = "linksmart.gc.configuration.path";

    /**
     * Default GC configuration path.
     */
    public static final String GC_CONFIGURATION_PATH = "/etc";
    
    /**
     * System property to lookup GC configuration file name.
     */
    public static final String GC_CONFIGURATION_FILE_NAME_KEY = "linksmart.gc.configuration.file.name";

    /**
     * Default GC configuration file.
     */
    public static final String GC_CONFIGURATION_FILE_NAME = "linksmart-gc.properties";
    
    /**
     * Default GC server configuration file.
     */
    public static final String GC_SERVER_CONFIGURATION_FILE_NAME = "linksmart-gc-server.properties";

}
