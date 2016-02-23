package bfbc.jiggity;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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

import bfbc.jiggity.config.Configuration;

public class JiggityServer {
	
	private static final String CONFIG_FILE = "jiggity.conf.xml";
	private static Logger logger = LoggerFactory.getLogger(JiggityServer.class);
	private Server server;
	
	public void start(File workingDir) throws Exception {
		// = new File("");
    	logger.info("Working directory: " + workingDir.getAbsolutePath());
    	
	    File configFile = new File(workingDir.getAbsoluteFile(), CONFIG_FILE);
	    
	    if (!configFile.exists()) {
	    	logger.error("Can't find a necessary configuration file: " + configFile.getPath());
	    	return;
	    }
	    	
	    logger.info("Parsing configuration file: " + configFile.getAbsolutePath());
	    Configuration conf = new Configuration(configFile);

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
	    
	    File gitPathFile = new File(workingDir.getAbsoluteFile(), conf.getGitPath());
	    logger.info("Loading git repo: " + gitPathFile.getAbsolutePath() + " @ revision \"" + conf.getGitRevStr() + "\"");
	    JiggityHandler gitReadyHandler = new JiggityHandler(gitPathFile, conf.getGitRevStr(), conf.isGitAllowStash(), excludePatterns);
	    
	    handlers.setHandlers(new Handler[] { gitReadyHandler, /*resource_handler,*/ new DefaultHandler() });

	    logger.info("Starting server");
	    InetSocketAddress addressToListen = new InetSocketAddress(InetAddress.getByName(conf.getInetAddress()), Integer.parseInt(conf.getPort()));
	    server = new Server(addressToListen);
	    NetworkTrafficServerConnector connector = new NetworkTrafficServerConnector(server);
	    server.addConnector(connector);

	    server.setHandler(handlers);

	    server.start();
	}
	
	public void join() throws InterruptedException {
	    server.join();
	}
	
	public void stop() throws Exception {
		server.stop();
	}
	
	public static void main(String[] args) throws Exception {
		JiggityServer srv = new JiggityServer();
		srv.start(new File(""));
		srv.join();
	}

}
