package bfbc.jiggity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bfbc.jiggity.api.JGIScript;
import bfbc.jiggity.api.exceptions.JGIClientException;
import bfbc.jiggity.api.exceptions.JGIException;
import bfbc.jiggity.api.exceptions.JGIServerException;
import bfbc.jiggity.api.exceptions.JGIServerException.Code;
import bfbc.jiggity.api.JGIExceptionHandler;
import bfbc.jiggity.api.JGIProcessor;
import bfbc.jiggity.compiler.JavaCompilerTool;
import bfbc.jiggity.compiler.JavaCompilerTool.TargetClassLoader;

public class JiggityHandler extends AbstractHandler {

	private static Logger logger = LoggerFactory.getLogger(JiggityHandler.class);
	
	private File repoFile;
	private String revStr;
	private boolean allowStash;
	private List<Pattern> excludePatterns;
	
	private class Processors {
		private Map<Class<? extends JGIScript>, JGIScript> scripts = new HashMap<>();
		
		private Map<String, Class<?>> javaClassesForFiles = new HashMap<>();
	}
	
	private Map<ObjectId, Processors> processorObjects = new HashMap<>();
	
	private boolean matchExcluded(String path) {
		for (Pattern p : excludePatterns) {
			if (p.matcher(path).find()) return true;
		}
		return false;
	}
	
    private Repository openRepository() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder
                //.readEnvironment() // scan environment GIT_* variables
                //.findGitDir() // scan up the file system tree
        		.setGitDir(repoFile)
        		.setMustExist(true)
                .build();
        logger.debug("Opened repo: " + repoFile);
        return repository;
    }

    private synchronized boolean checkCommitUpdated() throws IOException {
    	
	    	try (Repository repository = openRepository()) {
	
	    		// find the HEAD
	            ObjectId lastCommitId = null;
	            if (allowStash) lastCommitId = repository.resolve("refs/stash");
	            if (lastCommitId == null) lastCommitId = repository.resolve(revStr);

	            if (lastCommitId == null) {
	            	throw new IOException("Can't find a commit for \"" + revStr + "\"");
	            }
	            if (processorObjects.containsKey(lastCommitId)) {
	            	return true;
	            } else {
	            	processorObjects.put(lastCommitId, new Processors());
	
		            ArrayList<JavaCompilerTool.SourceInMemory> srcClasses = new ArrayList<>();
		            
		            // a RevWalk allows to walk over commits based on some filtering that is defined
		            try (RevWalk revWalk = new RevWalk(repository)) {
		                RevCommit commit = revWalk.parseCommit(lastCommitId);

		                // and using commit's tree find the path
		                RevTree tree = commit.getTree();
		                logger.info("Collecting java classes from commit: " + tree.getName());
		
		                try (TreeWalk treeWalk = new TreeWalk(repository)) {
		                    treeWalk.addTree(tree);
		                    treeWalk.setRecursive(true);
		                    
		                    while (treeWalk.next()) {
		                    	ObjectId objectId = treeWalk.getObjectId(0);
		                    	ObjectLoader loader = repository.open(objectId);
		                    	
		                    	String fullName = treeWalk.getPathString();
		                    	if (fullName.endsWith(".java")) {
		                    		String sourceCode = new String(loader.getBytes());
		                            srcClasses.add(new JavaCompilerTool.SourceInMemory(fullName, sourceCode));
		                        	logger.debug("Adding " + treeWalk.getPathString() + " to compilation queue");
		                    	}
		                    }
		                }
		                
		            	logger.info("Compiling the classes...");
		                TargetClassLoader compiledSourcesClassLoader = JavaCompilerTool.compile(this.getClass().getClassLoader(), srcClasses);
		                if (compiledSourcesClassLoader != null) {
		                	logger.info("All classes compiled successfully. Instantiating all JGI objects...");
		                	processorObjects.get(lastCommitId).scripts.clear();
		                	
		                	for (JavaCompilerTool.TargetClassDescriptor tcd : compiledSourcesClassLoader.nameSet()) {
	
								try {
									Class<?> clz = compiledSourcesClassLoader.loadClass(tcd.className.replace('/', '.'));
		                		
			                		boolean isJGIScript = false;
			                		if (JGIScript.class.isAssignableFrom(clz)) isJGIScript = true;
			                		
			                		if (isJGIScript) {
			                			try {
				                			logger.debug("Class " + tcd.className + " (file " + tcd.filePath + ") is a request processor. Creating an instance for the commit");
											processorObjects.get(lastCommitId).scripts.put((Class<JGIScript>)clz, (JGIScript)clz.newInstance());
										} catch (InstantiationException | IllegalAccessException e) {
											logger.error("Can't instantiate the JGI script object. There is no empty constructor or it is not accessible");
										}
			                		}
			                		
			                		processorObjects.get(lastCommitId).javaClassesForFiles.put(tcd.filePath, (Class<?>)clz);
			                		
								} catch (ClassNotFoundException e1) {
									logger.warn("Can't load the class " + tcd.className + " (file " + tcd.filePath + "). A very strange bug cause the class has just been compiled");
									e1.printStackTrace();
								}
	
		                	}
		                } else {
		                	logger.error("Compilation failed");
		                	return false;
		                }
		            }
	            }
			}
	    	return true;
    }
    
    public JiggityHandler(File repoFile, String revStr, boolean allowStash, List<Pattern> excludeMatchers) throws IOException {
    	this.repoFile = repoFile;
    	this.revStr = revStr;
    	this.allowStash = allowStash;
    	this.excludePatterns = new ArrayList<>(excludeMatchers);
    	
    	checkCommitUpdated();
    }
    
    private void handleErrorDefault(String target, HttpServletRequest request, HttpServletResponse response, JGIException exception) {
    	int code = -1;
        if (exception instanceof JGIServerException) {
        	JGIServerException serverException = (JGIServerException)exception;
        	code = serverException.getCode().httpCode;
        } else if (exception instanceof JGIClientException) {
        	JGIClientException clientException = (JGIClientException)exception;
        	code = clientException.getCode().httpCode;
        } 
        	
    	response.setStatus(code);
		try {
			response.getOutputStream().println("<html><head><title>Error " + code + "</title><style>body { margin: 25pt; min-height: 70pt; min-width: 200pt; position: relative; } .err { font-size: 150%; } .code { font-weight: normal; font-size: 120%; } .tip { position: absolute; bottom: -10pt; opacity: 0.7; }</style></head><body>");
			response.getOutputStream().print("<p class=\"err\">Error<span class=\"code\"> " + code);
			if (exception.getMessage() != null) {
				response.getOutputStream().print(": </span>" + exception.getMessage());
			}
			response.getOutputStream().println("</p>");
			response.getOutputStream().println("<p class=\"tip\"><em>To change this message design, add an error processor to your site</em></p>");
			response.getOutputStream().println("</body></html>");
		} catch (IOException e) {
	        logger.error("Facepalm. Server can't send the error response to the client (request: \"" + target + "\" from " + request.getRemoteAddr() + ", exception code " + code + ": " + exception.getMessage() + ")");
			e.printStackTrace();
		} finally {
			try {
				response.getOutputStream().close();
			} catch (IOException e) {
				logger.error("Facepalm. Can't close the output");
				e.printStackTrace();
			}
    	}


    }
    
    
    private void handleError(ObjectId lastCommitId, String target, HttpServletRequest request, HttpServletResponse response, JGIException exception) {
    	logger.info("Responding with error response to the client (request: \"" + target + "\" from " + request.getRemoteAddr() + ", exception " + exception);
    	
        // Searching for a proper exception processor
        boolean requestExceptionProcessorFound = false;
        for (Object obj : processorObjects.get(lastCommitId).scripts.values()) {
        	if (obj instanceof JGIExceptionHandler) {
	        	JGIExceptionHandler processor = (JGIExceptionHandler)obj;
	        	requestExceptionProcessorFound = processor.onError(target, request, response, exception);
	        	if (requestExceptionProcessorFound) {
	                logger.info("Error processed by " + processor.getClass().getCanonicalName());
					return;
				}
        	}
        }

        handleErrorDefault(target, request, response, exception);
    }
    
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		try {
	        // Removing first '/'
	        //if (target.startsWith("/")) target = target.substring(1);
	
			checkCommitUpdated();
	
	        logger.info("Handling request \"" + target + "\" from " + request.getRemoteAddr());
			
			try (Repository repository = openRepository()) {
	            // find the HEAD
	            ObjectId lastCommitId = null;
	            if (allowStash) lastCommitId = repository.resolve("refs/stash");
	            if (lastCommitId == null) lastCommitId = repository.resolve(revStr);
	            
	            if (lastCommitId == null) throw new IOException("Can't find a commit for \"" + revStr + "\"");
	
	            // Selecting the commit
	            try (RevWalk revWalk = new RevWalk(repository)) {
	                RevCommit commit = revWalk.parseCommit(lastCommitId);
	                	
	                // and using commit's tree find the path
	                RevTree tree = commit.getTree();
	                logger.debug("Current commit: " + tree.getName());
	                
	                // Walking the tree
	                try (TreeWalk treeWalk = new TreeWalk(repository)) {
	                    treeWalk.addTree(tree);
	                    treeWalk.setRecursive(true);
	                    
	                    String requestPath = target;
	                    
	                    // Removing first '/'
	        	        if (requestPath.startsWith("/")) requestPath = requestPath.substring(1);
	                    treeWalk.setFilter(PathFilter.create(requestPath));
	                    
	                    InputStream fileObjectInputStream = null;
	                    
	                    ObjectLoader loader = null;
	                    
	                    // 1. Searching for the file
	                    boolean fileFound = false;
	                    String foundFilePath = null;
	                    while (treeWalk.next()) {
	                        foundFilePath = treeWalk.getPathString();
	                        if (foundFilePath.equals(requestPath)) {
	                        	fileFound = true;
	                        	break;
	                        }
	                    }	                    
	                    
	                    if (fileFound) {
	                        ObjectId objectId = treeWalk.getObjectId(0);
	                        
	                        // Checking if the file is a JGI script
	                        Class<?> jgiClass = processorObjects.get(lastCommitId).javaClassesForFiles.get(target);
	                        JGIScript scriptForClass = processorObjects.get(lastCommitId).scripts.get(jgiClass);
	                        
	                        if (scriptForClass != null) {
	                        	// 1a. Executing the file if it's a script
		                        logger.info("The requested file is a JGI class. Executing it");
		                        scriptForClass.onExecute(target, request, response);
		                        return;
	                        } else {
		                        // 1b. Loading the file if it's not a script
	                        	loader = repository.open(objectId);
		                        if (!matchExcluded(foundFilePath)) {
		                        	fileObjectInputStream = loader.openStream();
		                        	logger.info("File found for request \"" + target + "\". Serving it.");
		                        }
	                        }
	                    } else {
	                        logger.info("No file for request \"" + target + "\". Trying to process without a file.");
	                    }
	
	                    // 2. Searching for a proper processor
	                    boolean requestProcessorFound = false;
	                    for (Object obj : processorObjects.get(lastCommitId).scripts.values()) {
	                    	if (obj instanceof JGIProcessor) {
		                    	JGIProcessor processor = (JGIProcessor)obj;
		                    	requestProcessorFound = processor.onRequest(target, fileObjectInputStream, request, response);
		                    	if (requestProcessorFound) {
				                    logger.info("Request \"" + target + "\" from " + request.getRemoteAddr() + " processed by " + processor.getClass().getName());
									break;
								}
	                    	}
	                    }
	                    
	                    // 3. If no processor found...
	                    if (!requestProcessorFound) {
	                    	if (loader != null && !matchExcluded(foundFilePath)) {
			                    // ...and there is a loaded object, sending it directly
			                    loader.copyTo(response.getOutputStream());
			                    response.getOutputStream().close();
			                    response.setStatus(HttpServletResponse.SC_OK);
			                    logger.info("Request \"" + target + "\" from " + request.getRemoteAddr() + " processed directly");
	                    	} else {
			                    // ...and no object is loaded object, sending 404
			                    logger.info("Request \"" + target + "\" from " + request.getRemoteAddr() + " can't be processed. No file/processor found. Responding with code 404.");
	                    		throw new JGIClientException(JGIClientException.Code.NOT_FOUND, "Request can't be processed. No file or processor found");

	                    	}
	                    }
	                }
	            } catch (JGIClientException e) {
	    			logger.error("Client exception occured with code " + e.getCode().httpCode + ": " + e.getMessage());
	    			e.printStackTrace();
	    			
	        		handleError(lastCommitId, target, request, response, e);
	    		} catch (JGIServerException e) {
	    			logger.error("Server exception occured with code " + e.getCode().httpCode + ": " + e.getMessage());
	    			e.printStackTrace();
	    			
	        		handleError(lastCommitId, target, request, response, e);
	    		} 
	        }
		} catch (Exception e) {
			logger.error("General server exception occured: " + e.getMessage());
			e.printStackTrace();
			
    		handleErrorDefault(target, request, response, new JGIServerException(Code.INTERNAL_ERROR, e));
		}
	}

}
