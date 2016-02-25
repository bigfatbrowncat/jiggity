package bfbc.jiggity.tests;

import static bfbc.jiggity.tests.tools.Tools.*;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bfbc.jiggity.JiggityServer;
import bfbc.jiggity.tests.tools.TestConf;
import bfbc.jiggity.tests.tools.Tools;

public class BasicTest {
	
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
	public void staticRequestTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "staticRequestTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] lines = new String[] {
				"This is a text file",
				"Second line"
			};
			
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "test.txt", lines);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
	
			{
				String readTest = sendGet("http://localhost:8090/test.txt").text;
				String[] readLines = readTest.split("\n");
				assertEquals(lines[0], readLines[0]);
				assertEquals(lines[1], readLines[1]);
			}
			{
				String readTest = sendPost("http://localhost:8090/test.txt", "").text;
				String[] readLines = readTest.split("\n");
				assertEquals(lines[0], readLines[0]);
				assertEquals(lines[1], readLines[1]);
			}
		
		} finally {
			srv.stop();
		}
	}
	
	@Test
	public void scriptTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "scriptTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] lines = new String[] {
				"first line",
				"second line"
			};
			
			String[] code = new String[] {
				"package somepkg;",
				"import java.io.IOException;",
	
				"import javax.servlet.http.*;",
	
				"import bfbc.jiggity.api.exceptions.JGIException;",
				"import bfbc.jiggity.api.JGIScript;",
	
				"public class CallMe extends JGIScript {",
				"	@Override",
				"	public void onExecute(String target, HttpServletRequest request, HttpServletResponse response) throws JGIException {",
				"		try {",
				"			response.getOutputStream().println(\"" + lines[0] + "\");",
				"			response.getOutputStream().println(\"" + lines[1] + "\");",
				"			response.getOutputStream().close();",
				"		} catch (IOException e) {",
				"			e.printStackTrace();",
				"		}",
				"	}",
				"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "CallMe.java", code);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				String readTest = sendGet("http://localhost:8090/CallMe.java").text;
				String[] readLines = readTest.split("\n");
				assertEquals(lines[0], readLines[0]);
				assertEquals(lines[1], readLines[1]);
			}
			{
				String readTest = sendPost("http://localhost:8090/CallMe.java", "").text;
				String[] readLines = readTest.split("\n");
				assertEquals(lines[0], readLines[0]);
				assertEquals(lines[1], readLines[1]);
			}
			
		} finally {
			srv.stop();
		}
	}

	@Test
	public void processorWithFileTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "processorWithFileTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] lines = new String[] {
				"first line",
				"second line"
			};
			
			String[] code = new String[] {
					"package pkg;",
					"import java.io.IOException;",
					"import java.io.InputStream;",
	
					"import javax.servlet.ServletOutputStream;",
					"import javax.servlet.http.HttpServletRequest;",
					"import javax.servlet.http.HttpServletResponse;",
	
					"import bfbc.jiggity.api.JGIProcessor;",
					"import bfbc.jiggity.api.exceptions.JGIException;",
					"import bfbc.jiggity.api.exceptions.JGIServerException;",
					"import bfbc.jiggity.api.exceptions.JGIServerException.Code;",
	
					"public class Processor extends JGIProcessor {",
						
					"	@Override",
					"	public boolean onRequest(String target, InputStream fileStream, HttpServletRequest request, HttpServletResponse response) throws JGIException {",
					"		try {",
					"			if (fileStream == null) return false;",
								
					"			ServletOutputStream out = response.getOutputStream();",
					"			out.write('!');",
					"			int r;",
					"			while ((r = fileStream.read()) != -1) {",
					"				out.write(r);",
					"			}",
					"			out.close();",
								
					"			return true;",
					"		} catch (IOException e) {",
					"			throw new JGIServerException(Code.INTERNAL_ERROR, e);",
					"		}",
					"	}",
					"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "file.txt", lines);
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "Processor.java", code);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				String readTest = sendGet("http://localhost:8090/file.txt").text;
				String[] readLines = readTest.split("\n");
				assertEquals('!' + lines[0], readLines[0]);
				assertEquals(lines[1], readLines[1]);
			}
			{
				String readTest = sendPost("http://localhost:8090/file.txt", "").text;
				String[] readLines = readTest.split("\n");
				assertEquals('!' + lines[0], readLines[0]);
				assertEquals(lines[1], readLines[1]);
			}
		} finally {
			srv.stop();
		}
	}

	@Test
	public void exceptionHandlerTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "exceptionHandlerTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] code = new String[] {
					"import javax.servlet.http.HttpServletRequest;",
					"import javax.servlet.http.HttpServletResponse;",

					"import bfbc.jiggity.api.JGIExceptionHandler;",
					"import bfbc.jiggity.api.exceptions.JGIException;",
					"import bfbc.jiggity.api.exceptions.JGIClientException;",

					"public class ExHandler extends JGIExceptionHandler {",
					"	@Override",
					"	public boolean onError(String target, HttpServletRequest request, HttpServletResponse response, JGIException exception) throws Exception {",
					"		response.getWriter().println(\"Error occured with code \" + ((JGIClientException)exception).getCode().httpCode);",
					"		response.getWriter().close();",
					"		return true;",
					"	}",
					"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "ExHandler.java", code);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				String readTest = sendGet("http://localhost:8090/notfound.txt").text;
				String[] readLines = readTest.split("\n");
				assertEquals("Error occured with code 404", readLines[0]);
			}
			{
				String readTest = sendPost("http://localhost:8090/notfound.txt", "").text;
				String[] readLines = readTest.split("\n");
				assertEquals("Error occured with code 404", readLines[0]);
			}
		} finally {
			srv.stop();
		}
	}

	@Test
	public void processorRequestWithoutFileTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "processorRequestWithoutFileTest";
			TestConf tstConf = createGitForServer(tmpDir, testPrefix);
			
			String[] code = new String[] {
					"package pkg;",
					"import java.io.IOException;",
					"import java.io.InputStream;",
	
					"import javax.servlet.ServletOutputStream;",
					"import javax.servlet.http.HttpServletRequest;",
					"import javax.servlet.http.HttpServletResponse;",
	
					"import bfbc.jiggity.api.JGIProcessor;",
					"import bfbc.jiggity.api.exceptions.JGIException;",
					"import bfbc.jiggity.api.exceptions.JGIServerException;",
					"import bfbc.jiggity.api.exceptions.JGIServerException.Code;",
	
					"public class Processor extends JGIProcessor {",
						
					"	@Override",
					"	public boolean onRequest(String target, InputStream fileStream, HttpServletRequest request, HttpServletResponse response) throws JGIException {",
					"		try {",
					"			ServletOutputStream out = response.getOutputStream();",
					"			out.println(\"Some text: \" + target);",
					"			out.close();",

					"			return true;",
					"		} catch (IOException e) {",
					"			throw new JGIServerException(Code.INTERNAL_ERROR, e);",
					"		}",
					"	}",
					"}"
			};
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "Processor.java", code);
			tstConf.git.commit().setMessage("init").call();
	
			createDefaultConfFile(testPrefix, tstConf.rootDir);
			
			srv.start(tstConf.rootDir);
			
			{
				String readTest = sendGet("http://localhost:8090/some_request").text;
				assertEquals("Some text: /some_request\n", readTest);
			}
			{
				String readTest = sendGet("http://localhost:8090/some_request").text;
				assertEquals("Some text: /some_request\n", readTest);
			}
		} finally {
			srv.stop();
		}
	}

}
