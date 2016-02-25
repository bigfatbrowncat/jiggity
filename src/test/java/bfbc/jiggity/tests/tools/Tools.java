package bfbc.jiggity.tests.tools;

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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tools {
	private static Logger logger = LoggerFactory.getLogger(Tools.class);

	private static final String USER_AGENT = "Mozilla/5.0";

	private static String CONF_FILE = "jiggity.conf.xml";

	public static Response sendGet(String url) throws IOException {

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
	
	public static Response sendPost(String url, String urlParameters) throws Exception {

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

	public static TestConf createGitForServer(File tmpDir, String prefix) throws IllegalStateException, GitAPIException, IOException {
		File testGitDir = new File(tmpDir, prefix + "-git");
		File testRootDir = new File(tmpDir, prefix + "-root");
		testGitDir.mkdirs();
		testRootDir.mkdirs();

		Git.init().setDirectory(testGitDir).call();
		Git testGit = Git.open(testGitDir);

		return new TestConf(testGitDir, testRootDir, testGit);
	}
	
	public static void addFileToGitIndex(Git git, File gitDir, String fileName, String[] lines) throws NoFilepatternException, GitAPIException, FileNotFoundException {
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
	
	public static void createDefaultConfFile(String testPrefix, File rootDir) throws FileNotFoundException {
		File testConfFile = new File(rootDir, CONF_FILE);
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(testConfFile)));
		pw.println("<jiggity>");
		pw.println("	<git path=\"../" + testPrefix + "-git/.git\" revision=\"master\" />");
		pw.println("	<listen port=\"8090\" />");
		pw.println("</jiggity>");
		pw.close();
	}
}
