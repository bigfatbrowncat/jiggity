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

public class ExceptionsTest {
	private static Logger logger = LoggerFactory.getLogger(BasicTest.class);

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
	public void staticNotFoundRequestTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "staticNotFoundRequestTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			tstConf.git.commit().setMessage("init").call();

			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
	
			{
				Response resp = sendGet("http://localhost:8090/notfound.txt");
				assertEquals(404, resp.code);
			}
			{
				Response resp = sendPost("http://localhost:8090/notfound.txt", "");
				assertEquals(404, resp.code);
			}
		
		} finally {
			srv.stop();
		}
	}
	
	@Test
	public void processorThrowsServerExceptionTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "processorThrowsServerExceptionTest";
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
					"		throw new JGIServerException(Code.INTERNAL_ERROR);",
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
	public void processorThrowsClientExceptionTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "processorThrowsClientExceptionTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] code = new String[] {
					"package pkg;",
					"import java.io.IOException;",
					"import java.io.InputStream;",
	
					"import javax.servlet.http.HttpServletRequest;",
					"import javax.servlet.http.HttpServletResponse;",
	
					"import bfbc.jiggity.api.JGIProcessor;",
					"import bfbc.jiggity.api.exceptions.JGIException;",
					"import bfbc.jiggity.api.exceptions.JGIClientException;",
					"import bfbc.jiggity.api.exceptions.JGIClientException.Code;",
	
					"public class Processor extends JGIProcessor {",
						
					"	@Override",
					"	public boolean onRequest(String target, InputStream fileStream, HttpServletRequest request, HttpServletResponse response) throws JGIException {",
					"		throw new JGIClientException(Code.FORBIDDEN);",
					"	}",
					"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "Processor.java", code);
			tstConf.git.commit().setMessage("init").call();
			
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				int readTest = sendGet("http://localhost:8090/some_request").code;
				assertEquals(JGIClientException.Code.FORBIDDEN.httpCode, readTest);
			}
			{
				int readTest = sendGet("http://localhost:8090/some_request").code;
				assertEquals(JGIClientException.Code.FORBIDDEN.httpCode, readTest);
			}
		} finally {
			srv.stop();
		}
	}

	@Test
	public void scriptThrowsClientExceptionTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "scriptThrowsClientExceptionTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] code = new String[] {
				"package somepkg;",
	
				"import javax.servlet.http.*;",
	
				"import bfbc.jiggity.api.exceptions.JGIException;",
				"import bfbc.jiggity.api.exceptions.JGIClientException;",

				"import bfbc.jiggity.api.JGIScript;",
	
				"public class CallMe extends JGIScript {",
				"	@Override",
				"	public void onExecute(String target, HttpServletRequest request, HttpServletResponse response) throws JGIException {",
				"		throw new JGIClientException(JGIClientException.Code.BAD_REQUEST);",
				"	}",
				"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "CallMe.java", code);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				int respCode = sendGet("http://localhost:8090/CallMe.java").code;
				assertEquals(400, respCode);
			}
			{
				int respCode = sendPost("http://localhost:8090/CallMe.java", "").code;
				assertEquals(400, respCode);
			}
			
		} finally {
			srv.stop();
		}
	}
	
	@Test
	public void scriptThrowsServerExceptionTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "scriptThrowsServerExceptionTest";
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
				"		throw new JGIServerException(JGIServerException.Code.NOT_IMPLEMENTED);",
				"	}",
				"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "CallMe.java", code);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				int respCode = sendGet("http://localhost:8090/CallMe.java").code;
				assertEquals(501, respCode);
			}
			{
				int respCode = sendPost("http://localhost:8090/CallMe.java", "").code;
				assertEquals(501, respCode);
			}
			
		} finally {
			srv.stop();
		}
	}
}
