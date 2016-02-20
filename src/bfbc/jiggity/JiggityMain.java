package bfbc.jiggity;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bfbc.jiggity.compiler.JavaCompilerTool;
import bfbc.jiggity.config.Configuration;

public class JiggityMain {
	
	private static final String CONFIG_FILE = "jiggity.conf.xml";
	private static Logger logger = LoggerFactory.getLogger(JiggityMain.class);

	public static void main(String[] args) throws Exception {

	    logger.info("Parsing configuration file: " + CONFIG_FILE);
	    Configuration conf = new Configuration(new File(CONFIG_FILE));

	    /*ResourceHandler resource_handler = new ResourceHandler();
	    resource_handler.setDirectoriesListed(true);
	    resource_handler.setWelcomeFiles(new String[]{ "index.html" });

	    resource_handler.setResourceBase(".");*/
	    
	    HandlerList handlers = new HandlerList();
	    
	    logger.info("Compiling exclude patterns");
	    List<Pattern> excludePatterns = new ArrayList<>();
	    for (String exStr : conf.getExcludePatterns()) {
	    	excludePatterns.add(Pattern.compile(exStr));
	    }
	    
	    logger.info("Loading git repo: " + conf.getGitPath() + " @ revision \"" + conf.getGitRevStr() + "\"");
	    JiggityHandler gitReadyHandler = new JiggityHandler(new File(conf.getGitPath()), conf.getGitRevStr(), excludePatterns);
	    
	    handlers.setHandlers(new Handler[] { gitReadyHandler, /*resource_handler,*/ new DefaultHandler() });

	    logger.info("Starting server");
	    InetSocketAddress addressToListen = new InetSocketAddress(InetAddress.getByName(conf.getInetAddress()), Integer.parseInt(conf.getPort()));
	    Server server = new Server(addressToListen);
	    NetworkTrafficServerConnector connector = new NetworkTrafficServerConnector(server);
	    server.addConnector(connector);

	    server.setHandler(handlers);

	    server.start();
	    server.join();
	}

}
