package bfbc.jiggity.tests;

import static bfbc.jiggity.tests.tools.Tools.addFileToGitIndex;
import static bfbc.jiggity.tests.tools.Tools.createGitForServer;
import static bfbc.jiggity.tests.tools.Tools.sendGet;
import static bfbc.jiggity.tests.tools.Tools.sendPost;

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
import bfbc.jiggity.tests.tools.Response;
import bfbc.jiggity.tests.tools.TestConf;

public class ExceptionsTest {
	private static String CONF_FILE = "jiggity.conf.xml";
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
	
}
