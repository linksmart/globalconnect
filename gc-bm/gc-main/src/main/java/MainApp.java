import eu.linksmart.gc.server.GcEngine;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.Properties;

/**
 * Created by krylovskiy on 28/09/15.
 */
public class MainApp {
    private static final Logger LOG = Logger.getLogger(MainApp.class);
    private static Properties config;

    public static void main(String[] args) throws Exception {
        Log.setLog(new StdErrLog());

        LOG.info("MainApp -> intializing...");
        // initialize the GC Engine/Server
        GcEngine gcEngine = new GcEngine();

        // load configuration

        // http bind port and address
        // FIXME: This produces and exception!?
//        int bindPort = gcEngine.getContainerPort();
        int bindPort = 8080;

        // initialize http server
        Server httpServer = new Server(bindPort);

        // configure resources / servlets
        LOG.info("MainApp -> configuring HTTP resources/servlets");

        // Configure servlets
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        httpServer.setHandler(context);
        ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter(
                "jersey.config.server.provider.classnames",
                gcEngine.getNetworkManagerRest().getClass().getCanonicalName());

        // run the http server
        LOG.info("MainApp -> starting HTTP server");

        try {
            httpServer.start();
            httpServer.join();
        } finally {
            httpServer.destroy();
            gcEngine.shutdownEngine();
        }
    }
}
