package bfbc.jiggity.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jetty.xml.XmlParser;
import org.eclipse.jgit.lib.Constants;
import org.xml.sax.SAXException;

public class Configuration {
	private String gitPath;
	private boolean gitAllowStash;
	private String gitRevStr = Constants.HEAD;
	private List<String> excludePatterns = new ArrayList<>();
	
	private String inetAddress = "0.0.0.0";
	private String port = "8080";
	
	public Configuration(File configFile) {
		try {
			XmlParser xmlParser = new XmlParser();
			XmlParser.Node root = xmlParser.parse(configFile);
			
			if (root.getTag().equals("jiggity")) {
				{
					XmlParser.Node gitConf = root.get("git");
					if (gitConf != null) {
						{
							String gitPathStr = gitConf.getAttribute("path");
							if (gitPathStr != null) {
								this.gitPath = gitPathStr;
							} else {
								throw new ConfigurationException("Missing path attribute of <git> tag");
							}
						}
						
						{
							String gitAllowStashVal = gitConf.getAttribute("allow-stash");
							if (gitAllowStashVal != null) {
								this.gitAllowStash = gitAllowStashVal.equals("true");
							} else {
								throw new ConfigurationException("Missing path attribute of <git> tag");
							}
							
						}
						
						String gitRevStr = gitConf.getAttribute("revision");
						if (gitRevStr != null) {
							this.gitRevStr = gitRevStr;
						}
					} else {
						throw new ConfigurationException("Missing <git> tag");
					}
				}
				
				{
					XmlParser.Node listenConf = root.get("listen");
					if (listenConf != null) {
						String address = listenConf.getAttribute("address");
						if (address != null) {
							this.inetAddress = address;
						}
	
						String port = listenConf.getAttribute("port");
						if (port != null) {
							this.port = port;
						}
					}
				}
				
				{
					XmlParser.Node serverConf = root.get("server");
					if (serverConf != null) {
						XmlParser.Node staticConf = serverConf.get("static");
						if (staticConf != null) {
							Iterator<XmlParser.Node> it = staticConf.iterator("exclude");
							
							while (it.hasNext()) {
								XmlParser.Node excludeConf = it.next(); 
								excludePatterns.add(excludeConf.getAttribute("path"));
							}
						}
					}
				}
				
			} else {
				throw new ConfigurationException("Root tag must be <jiggity>");
			}
		} catch (IOException | SAXException e) {
			throw new ConfigurationException(e);
		}
	}
	
	public String getGitPath() {
		return gitPath;
	}
	public String getGitRevStr() {
		return gitRevStr;
	}
	
	public String getInetAddress() {
		return inetAddress;
	}
	public String getPort() {
		return port;
	}
	public boolean isGitAllowStash() {
		return gitAllowStash;
	}
	
	public List<String> getExcludePatterns() {
		return Collections.unmodifiableList(excludePatterns);
	}
}
