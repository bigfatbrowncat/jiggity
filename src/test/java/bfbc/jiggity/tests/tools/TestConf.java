package bfbc.jiggity.tests.tools;

import java.io.File;

import org.eclipse.jgit.api.Git;

public class TestConf {
	public final File gitDir, rootDir;
	public final Git git;
	public TestConf(File gitDir, File rootDir, Git git) {
		super();
		this.gitDir = gitDir;
		this.rootDir = rootDir;
		this.git = git;
	}

	
}