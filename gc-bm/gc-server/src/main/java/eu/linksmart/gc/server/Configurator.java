package eu.linksmart.gc.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;

import org.apache.log4j.Logger;

public class Configurator {
	
	private static final Logger LOG = Logger.getLogger(Configurator.class);

    private static String configurationPath = null;
    
    private static String configurationFileName = null;
    
    /**
     * Creates a configuration environment with the default configuration path and file.
     * 
     */
    public Configurator() {
        LOG.debug(MessageFormat.format("using default configuration path {0}. default configuration file name {1}", 
        		new Object[] { ConfigConstants.GC_CONFIGURATION_PATH, ConfigConstants.GC_CONFIGURATION_FILE_NAME })) ;
        configurationPath = ConfigConstants.GC_CONFIGURATION_PATH;
        configurationFileName = ConfigConstants.GC_CONFIGURATION_FILE_NAME;
    }
    
    /**
     * Creates a configuration environment with the specified configuration path and configuration file name.
     * 
     * @param path
     *            the GC configuration path
     *            
     * @param configFileName
     *            the GC configuration file name
     */
    public Configurator(String path, String configFileName) {
        if (path == null) {
            path = ConfigConstants.GC_CONFIGURATION_PATH;
            LOG.debug(MessageFormat.format("Configuration path not specified, using default configuration path {0}.", new Object[] { ConfigConstants.GC_CONFIGURATION_PATH }));
        } else {
            LOG.debug(MessageFormat.format("Configuration path is specified. Path: {0}.", new Object[] { path }));
        }
        configurationPath = path;
        
        if (configFileName == null) {
        	configFileName = ConfigConstants.GC_CONFIGURATION_FILE_NAME;
            LOG.debug(MessageFormat.format("Configuration file name not specified, using default configuration file name {0}.", new Object[] { ConfigConstants.GC_CONFIGURATION_FILE_NAME }));
        } else {
            LOG.debug(MessageFormat.format("Configuration file name is specified. Path: {0}.", new Object[] { configFileName }));
        }
        configurationFileName = configFileName;
    }

    public static String getConfigurationPath() {
        String defaultPath = System.getProperty(ConfigConstants.GC_CONFIGURATION_PATH_KEY);
        if (defaultPath != null) {
            LOG.info(MessageFormat.format("Found system property {0} with value {1} (overwriting directory from configuration file {2}).",
            		new Object[] { ConfigConstants.GC_CONFIGURATION_PATH_KEY, defaultPath, configurationPath }));
            return defaultPath;
        } else {
        	configurationPath = ConfigConstants.GC_CONFIGURATION_PATH;
            LOG.debug(MessageFormat.format("Configuration path not specified, using default configuration path {0}.", new Object[] { ConfigConstants.GC_CONFIGURATION_PATH }));
        }
        return configurationPath;
    }
    
    public static String getConfigurationFileName() {
        String defaultName = System.getProperty(ConfigConstants.GC_CONFIGURATION_FILE_NAME_KEY);
        if (defaultName != null) {
            LOG.info(MessageFormat.format("Found system property {0} with value {1} (overwriting file name from default configuration file name {2}).",
            		new Object[] { ConfigConstants.GC_CONFIGURATION_FILE_NAME_KEY, defaultName, configurationFileName }));
            return defaultName;
        } else {
        	configurationFileName = ConfigConstants.GC_CONFIGURATION_FILE_NAME;
            LOG.debug(MessageFormat.format("Configuration file name not specified, using default configuration file name {0}.", new Object[] { ConfigConstants.GC_CONFIGURATION_FILE_NAME }));
        }
        return configurationFileName;
    }
    
    /**
     * @param resourceName
     *            The name of the requested resource.
     * @return The requested resource as <code>InputStream</code>.
     * @throws IOException
     *             there was an error reading the resource
     */
    public static InputStream findResource(String resourceName) throws IOException {
        return findResource(getConfigurationPath(), resourceName);
    }

    /**
     * @param path
     *            The path to the requested resource. If the resource is not found at the specified location
     *            discovery will be performed.
     * @param resourceName
     *            The name of the requested resource.
     * @return The requested resource as an InputStream.
     * @throws IOException
     *             there was an error reading the resource
     */
    public static InputStream findResource(String path, String resourceName) throws IOException {
        return findResourceURL(path, resourceName).openStream();
    }

    /**
     * @param resourceName
     *            The name of the requested resource.
     * @return The URL of the requested resource.
     * @throws IOException
     *             there was an error reading the resource
     */
    public static URL findResourceURL(String resourceName) throws IOException {
        return findResourceURL(getConfigurationPath(), resourceName);
    }

    /**
     * @param path
     *            The path to the requested resource. If the resource is not found at the specified location
     *            discovery will be performed.
     * @param resourceName
     *            The name of the requested resource.
     * @return The URL of the requested resource.
     * @throws IOException
     *             there was an error reading the resource
     */
    public static URL findResourceURL(String path, String resourceName) throws IOException {
        if (path == null) {
            path = "";
        }

        if (resourceName == null) {
            throw new IOException("Could not find resource. No resource name specified (null).");
        }

        File file = null;

        // first try to find the resource in the specified directory
        // global configuration files should be kept outside the application
        // in order to easy update the application
        file = new File(path, resourceName);
        
        if (file.exists()) {
            try {
                LOG.debug(MessageFormat.format("Found resource {0} in directory {1}.", new Object[] { resourceName, path }));
                return file.toURL();
            } catch ( IOException e ) {
                LOG.error(MessageFormat.format("Could not read resource {0}.", new Object[] { resourceName }), e);
            }
        }

        // then try to load the resource by its absolute name
        file = new File(resourceName);
        if (file.exists()) {
            try {
                LOG.debug(MessageFormat.format("Found resource [{0}].", new Object[] { resourceName }));
                return file.toURL();
            } catch(IOException e) {
                LOG.error(MessageFormat.format( "Could not read resource {0}.", new Object[] { resourceName }), e);
            }
        }

        // then by its fully qualified resource name
        String qualifiedResourceName = path + System.getProperty("file.separator") + resourceName;

        // special case for empty path on Windows systems
        if(path.equals("")) {
            qualifiedResourceName = "/" + resourceName;
        }

        URL resourceUrl = Configurator.class.getResource(qualifiedResourceName);
        if(resourceUrl != null) {
            LOG.debug(MessageFormat.format("Found resource {0} by the classloader [external name: {1}]", 
            		new Object[] { qualifiedResourceName, resourceUrl.toExternalForm() }));
            return resourceUrl;
        }

        // last try to find the resource by the classloader
        // this is the default behavior
        if(resourceName.startsWith( "/" ) || resourceName.startsWith( "\\" )) {
            resourceUrl = Configurator.class.getResource(resourceName);
        } else {
            // we don't want to load the resource relative from Configuration
            resourceUrl = Configurator.class.getResource("/" + resourceName);
        }

        if(resourceUrl != null) {
            LOG.debug(MessageFormat.format("Found resource {0} by the classloader [external name: {1}]", 
            		new Object[] { resourceName, resourceUrl.toExternalForm() }));
            return resourceUrl;
        }

        LOG.error(MessageFormat.format("The resource {0} was not found.", resourceName));
        LOG.error(MessageFormat.format("Tried the following directories: [{0}] [{1}]", path, System.getProperty( "java.class.path")));
        throw new FileNotFoundException(MessageFormat.format("The resource [{0}] was not found at the system.", resourceName ));
    }
}
