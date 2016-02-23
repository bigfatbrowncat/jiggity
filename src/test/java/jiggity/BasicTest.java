package jiggity;

import static org.junit.Assert.*;

import java.io.BufferedReader;
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

import bfbc.jiggity.JiggityServer;

public class BasicTest {

	private static final String USER_AGENT = "Mozilla/5.0";

	private static File tmpDir;

	static {
		try {
			tmpDir = Files.createTempDirectory("test-tmp-").toFile();
			System.out.println(BasicTest.class.getName() + " - test dir " + tmpDir.getAbsolutePath());
			tmpDir.deleteOnExit();
		} catch (IOException e) {
			throw new RuntimeException("Can't create temporary directory");
		}
	}

	private String sendGet(String url) throws IOException {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine + '\n');
		}
		in.close();

		return response.toString();
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
	
	private static void addFileToGit(Git git, File gitDir, String fileName, String[] lines) throws NoFilepatternException, GitAPIException, FileNotFoundException {
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

		TestConf tstConf = createGitForServer("simpleStaticRequestTest");
		
		String[] lines = new String[] {
			"This is a text file",
			"Second line"
		};
		
		
		addFileToGit(tstConf.git, tstConf.gitDir, "test.txt", lines);
		tstConf.git.commit().setMessage("init").call();

		{
			File testConfFile = new File(tstConf.rootDir, "jiggity.conf.xml");
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(testConfFile)));
			pw.println("<jiggity>");
			pw.println("	<git path=\"../simpleStaticRequestTest-git/.git\" revision=\"master\" allow-stash=\"true\" />");
			pw.println("	<listen address=\"0.0.0.0\" port=\"8090\" />");
			pw.println("	<server>");
			pw.println("		<static>");
			pw.println("			<exclude path=\".java$\"/>");
			pw.println("		</static>");
			pw.println("	</server>");
			pw.println("</jiggity>");
			pw.close();
		}
		
		JiggityServer srv = new JiggityServer();
		srv.start(tstConf.rootDir);
		
		String readTest = sendGet("http://localhost:8090/test.txt");
		String[] readLines = readTest.split("\n");
		assertEquals(lines[0], readLines[0]);
		assertEquals(lines[1], readLines[1]);
		
		srv.stop();
	}
}
