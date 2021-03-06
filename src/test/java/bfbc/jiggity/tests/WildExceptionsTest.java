package bfbc.jiggity.tests;

import static bfbc.jiggity.tests.tools.Tools.addFileToGitIndex;
import static bfbc.jiggity.tests.tools.Tools.createDefaultConfFile;
import static bfbc.jiggity.tests.tools.Tools.createGitForServer;
import static bfbc.jiggity.tests.tools.Tools.sendGet;
import static bfbc.jiggity.tests.tools.Tools.sendPost;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bfbc.jiggity.JiggityServer;
import bfbc.jiggity.api.exceptions.JGIClientException;
import bfbc.jiggity.api.exceptions.JGIServerException;
import bfbc.jiggity.tests.tools.Response;
import bfbc.jiggity.tests.tools.TestConf;

public class WildExceptionsTest {
	private static Logger logger = LoggerFactory.getLogger(WildExceptionsTest.class);

	private static File tmpDir;

	static {
		try {
			tmpDir = Files.createTempDirectory("test-tmp-").toFile();
			logger.info("Created test directory: " + tmpDir.getAbsolutePath());
			tmpDir.deleteOnExit();
		} catch (IOException e) {
			throw new RuntimeException("Can't create temporary directory");
		}
	}
	
	@Test
	public void processorThrowsRuntimeExceptionTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "processorThrowsRuntimeExceptionTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] code = new String[] {
					"package pkg;",
					"import java.io.IOException;",
					"import java.io.InputStream;",
	
					"import javax.servlet.http.HttpServletRequest;",
					"import javax.servlet.http.HttpServletResponse;",
	
					"import bfbc.jiggity.api.JGIProcessor;",
					"import bfbc.jiggity.api.exceptions.JGIException;",
					"import bfbc.jiggity.api.exceptions.JGIServerException;",
					"import bfbc.jiggity.api.exceptions.JGIServerException.Code;",
	
					"public class Processor extends JGIProcessor {",
						
					"	@Override",
					"	public boolean onRequest(String target, InputStream fileStream, HttpServletRequest request, HttpServletResponse response) throws JGIException {",
					"		throw new RuntimeException(\"I'm a wild exception\");",
					"	}",
					"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "Processor.java", code);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				int readTest = sendGet("http://localhost:8090/some_request").code;
				assertEquals(JGIServerException.Code.INTERNAL_ERROR.httpCode, readTest);
			}
			{
				int readTest = sendGet("http://localhost:8090/some_request").code;
				assertEquals(JGIServerException.Code.INTERNAL_ERROR.httpCode, readTest);
			}
		} finally {
			srv.stop();
		}
	}

	@Test
	public void scriptThrowsRuntimeExceptionTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "scriptThrowsRuntimeExceptionTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] code = new String[] {
				"package somepkg;",
	
				"import javax.servlet.http.*;",
	
				"import bfbc.jiggity.api.exceptions.JGIException;",
				"import bfbc.jiggity.api.exceptions.JGIServerException;",

				"import bfbc.jiggity.api.JGIScript;",
	
				"public class CallMe extends JGIScript {",
				"	@Override",
				"	public void onExecute(String target, HttpServletRequest request, HttpServletResponse response) throws JGIException {",
				"		throw new RuntimeException(\"I'm a wild exception\");",
				"	}",
				"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "CallMe.java", code);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				int respCode = sendGet("http://localhost:8090/CallMe.java").code;
				assertEquals(JGIServerException.Code.INTERNAL_ERROR.httpCode, respCode);
			}
			{
				int respCode = sendPost("http://localhost:8090/CallMe.java", "").code;
				assertEquals(JGIServerException.Code.INTERNAL_ERROR.httpCode, respCode);
			}
			
		} finally {
			srv.stop();
		}
	}
	
	@Test
	public void exceptionHandlerThrowsRuntimeExceptionTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "exceptionHandlerThrowsRuntimeExceptionTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] code = new String[] {
					"import java.io.IOException;",
					"import javax.servlet.http.HttpServletRequest;",
					"import javax.servlet.http.HttpServletResponse;",

					"import bfbc.jiggity.api.JGIExceptionHandler;",
					"import bfbc.jiggity.api.exceptions.JGIException;",
					"import bfbc.jiggity.api.exceptions.JGIClientException;",

					"public class ExHandler extends JGIExceptionHandler {",
					"	@Override",
					"	public boolean onError(String target, HttpServletRequest request, HttpServletResponse response, JGIException exception) {",
					"		throw new RuntimeException(\"I'm a wild exception\");",
					"	}",
					"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "ExHandler.java", code);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				int respCode = sendGet("http://localhost:8090/notfound.txt").code;
				assertEquals(JGIServerException.Code.INTERNAL_ERROR.httpCode, respCode);
			}
			{
				int respCode = sendPost("http://localhost:8090/notfound.txt", "").code;
				assertEquals(JGIServerException.Code.INTERNAL_ERROR.httpCode, respCode);
			}
		} finally {
			srv.stop();
		}
	}
	
	@Test
	public void exceptionHandlerCatchesWildExceptionTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "exceptionHandlerTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] code = new String[] {
					"package pkg;",
					"import java.io.IOException;",
					"import java.io.InputStream;",
	
					"import javax.servlet.http.HttpServletRequest;",
					"import javax.servlet.http.HttpServletResponse;",
	
					"import bfbc.jiggity.api.JGIProcessor;",
					"import bfbc.jiggity.api.exceptions.JGIException;",
					"import bfbc.jiggity.api.exceptions.JGIServerException;",
					"import bfbc.jiggity.api.exceptions.JGIServerException.Code;",
	
					"public class Processor extends JGIProcessor {",
						
					"	@Override",
					"	public boolean onRequest(String target, InputStream fileStream, HttpServletRequest request, HttpServletResponse response) throws JGIException {",
					"		throw new RuntimeException(\"I'm a wild exception\");",
					"	}",
					"}"
			};
			
			String[] excHandlerCode = new String[] {
					"import java.io.IOException;",
					"import javax.servlet.http.HttpServletRequest;",
					"import javax.servlet.http.HttpServletResponse;",

					"import bfbc.jiggity.api.JGIExceptionHandler;",
					"import bfbc.jiggity.api.exceptions.JGIException;",
					"import bfbc.jiggity.api.exceptions.JGIServerException;",

					"public class ExHandler extends JGIExceptionHandler {",
					"	@Override",
					"	public boolean onError(String target, HttpServletRequest request, HttpServletResponse response, JGIException exception) {",
					"		try {",
					"			response.getWriter().println(\"Error occured with code \" + ((JGIServerException)exception).getCode().httpCode);",
					"			response.getWriter().close();",
					"			return true;",
					"		} catch (IOException e) {",
					"			return false;",
					"		}",
					"	}",
					"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "Processor.java", code);
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "ExHandler.java", excHandlerCode);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				String readTest = sendGet("http://localhost:8090/somefile.txt").text;
				String[] readLines = readTest.split("\n");
				assertEquals("Error occured with code 500", readLines[0]);
			}
			{
				String readTest = sendPost("http://localhost:8090/somefile.txt", "").text;
				String[] readLines = readTest.split("\n");
				assertEquals("Error occured with code 500", readLines[0]);
			}
		} finally {
			srv.stop();
		}
	}
}
