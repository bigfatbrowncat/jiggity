package jiggity;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bfbc.jiggity.JiggityServer;

public class BasicTest {
	
	private static String CONF_FILE = "jiggity.conf.xml";
	private static Logger logger = LoggerFactory.getLogger(BasicTest.class);

	private static final String USER_AGENT = "Mozilla/5.0";

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

	private static class Response {
		public final int code;
		public final String text;
		public Response(int code, String text) {
			super();
			this.code = code;
			this.text = text;
		}
		
	}
	
	private Response sendGet(String url) throws IOException {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		logger.info("Sending 'GET' request to URL : " + url);
		logger.info("Response Code : " + responseCode);

		if (responseCode == 200) { 
		
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine + '\n');
			}
			in.close();
	
			logger.info("Received response: " + response);
			return new Response(200, response.toString());
		} else {
			return new Response(responseCode, null);
		}
	}
	
	private Response sendPost(String url, String urlParameters) throws Exception {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		logger.info("Sending 'POST' request to URL : " + url);
		logger.info("Post parameters : " + urlParameters);
		logger.info("Response Code : " + responseCode);

		if (responseCode == 200) { 
			
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine + '\n');
			}
			in.close();
	
			logger.info("Received response: " + response);
			return new Response(200, response.toString());
		} else {
			return new Response(responseCode, null);
		}

	}


	private static class TestConf {
		public final File gitDir, rootDir;
		public final Git git;
		public TestConf(File gitDir, File rootDir, Git git) {
			super();
			this.gitDir = gitDir;
			this.rootDir = rootDir;
			this.git = git;
		}
		
	}
	
	private static TestConf createGitForServer(String prefix) throws IllegalStateException, GitAPIException, IOException {
		File testGitDir = new File(tmpDir, prefix + "-git");
		File testRootDir = new File(tmpDir, prefix + "-root");
		testGitDir.mkdirs();
		testRootDir.mkdirs();

		Git.init().setDirectory(testGitDir).call();
		Git testGit = Git.open(testGitDir);

		return new TestConf(testGitDir, testRootDir, testGit);
	}
	
	private static void addFileToGitIndex(Git git, File gitDir, String fileName, String[] lines) throws NoFilepatternException, GitAPIException, FileNotFoundException {
		{
			File testFile = new File(gitDir, fileName);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(testFile)));
			for (String l : lines) {
				pw.println(l);
			}
			pw.close();
		}

		git.add().addFilepattern(fileName).call();
	}
	
	@Test
	public void simpleStaticRequestTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "simpleStaticRequestTest";
			TestConf tstConf = createGitForServer(testPrefix);
			
			String[] lines = new String[] {
				"This is a text file",
				"Second line"
			};
			
			
			addFileToGitIndex(tstConf.git, tstConf.gitDir, "test.txt", lines);
			tstConf.git.commit().setMessage("init").call();
	
			{
				File testConfFile = new File(tstConf.rootDir, CONF_FILE);
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(testConfFile)));
				pw.println("<jiggity>");
				pw.println("	<git path=\"../" + testPrefix + "-git/.git\" revision=\"master\" allow-stash=\"true\" />");
				pw.println("	<listen address=\"0.0.0.0\" port=\"8090\" />");
				pw.println("	<server>");
				pw.println("		<static>");
				pw.println("			<exclude path=\".java$\"/>");
				pw.println("		</static>");
				pw.println("	</server>");
				pw.println("</jiggity>");
				pw.close();
			}
			
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
	public void simpleScriptTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "simpleScriptTest";
			TestConf tstConf = createGitForServer(testPrefix);
			
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
	
			{
				File testConfFile = new File(tstConf.rootDir, CONF_FILE);
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(testConfFile)));
				pw.println("<jiggity>");
				pw.println("	<git path=\"../" + testPrefix + "-git/.git\" revision=\"master\" allow-stash=\"true\" />");
				pw.println("	<listen address=\"0.0.0.0\" port=\"8090\" />");
				pw.println("	<server>");
				pw.println("		<static>");
				pw.println("			<exclude path=\".java$\"/>");
				pw.println("		</static>");
				pw.println("	</server>");
				pw.println("</jiggity>");
				pw.close();
			}
			
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
	public void simpleProcessorWithFileTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "simpleProcessorWithFileTest";
			TestConf tstConf = createGitForServer(testPrefix);
			
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
	
			{
				File testConfFile = new File(tstConf.rootDir, CONF_FILE);
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(testConfFile)));
				pw.println("<jiggity>");
				pw.println("	<git path=\"../" + testPrefix + "-git/.git\" revision=\"master\" allow-stash=\"true\" />");
				pw.println("	<listen address=\"0.0.0.0\" port=\"8090\" />");
				pw.println("	<server>");
				pw.println("		<static>");
				pw.println("			<exclude path=\".java$\"/>");
				pw.println("		</static>");
				pw.println("	</server>");
				pw.println("</jiggity>");
				pw.close();
			}
			
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
	public void simpleExceptionHandlerTest() throws Exception {
		JiggityServer srv = new JiggityServer();
		try {
			String testPrefix = "simpleExceptionHandlerTest";
			TestConf tstConf = createGitForServer(testPrefix);
			
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
	
			{
				File testConfFile = new File(tstConf.rootDir, CONF_FILE);
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(testConfFile)));
				pw.println("<jiggity>");
				pw.println("	<git path=\"../" + testPrefix + "-git/.git\" revision=\"master\" allow-stash=\"true\" />");
				pw.println("	<listen address=\"0.0.0.0\" port=\"8090\" />");
				pw.println("	<server>");
				pw.println("		<static>");
				pw.println("			<exclude path=\".java$\"/>");
				pw.println("		</static>");
				pw.println("	</server>");
				pw.println("</jiggity>");
				pw.close();
			}
			
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

}
