# jiggity server
A tiny server with CGI-like Java interface and git as a content manager. Made of Jetty, JGIT and ECJ (Eclipse Compiler for Java)

## Features
This is a very simple way to make a small site with many static files and some scripts written in Java. Main features:
* Lightweight architecture, easy to configure (almost no configuration necessary)
* The whole site is hosted under git repository
* All the script source code is a part of site files &#151; the java files are placed in the same git
* The server compiles Java code itself! You only have to push it into git
* No need to stop/restart on redeploy. After you publish a new commit, the server cmpiles the new version of the scripts and 
starts serving fresh files to the clients immediately

## How to use

### Start
<ol>
<li><p>Create a git repo with an html file `index.html` inside. Commit the file:</p>

<pre>
> cd <your_repo_path>
> git init
> git add index.html
> git commit -m "index.html added"
</pre>

<p>This repo is your site. It will be served by Jiggity</p>
</li>

<li>Put <code>jiggity-<version>-jar-with-dependencies.jar</code> and <code>jiggity.conf.xml</code> into some path 
(other than <code>your_repo_path</code>)</li>
<li>Change the git path option in the <code>jiggity.conf.xml</code> to a proper git repo path (<code>your_repo_path/.git</code>)</li>
<li>Start the jar:
<p><pre><code>java -jar jiggity-<version>-jar-with-dependencies.jar
</code></pre></p>
</li>
<li>Open a browser and enter <code>http://localhost:8090/index.html</code> into address bar</li>
</ol>

### Deploy
Every commit/push into your repo will be handled by the server. No special operations (like stop/restart) needed

### Using Java
There are 3 types of Java handlers supported by Jiggity:
* `JGIScript` &#151; a file that can be called directly by the client. Something like `http://localhost:8090/YourScript.java`
* `JGIProcessor` &#151; a class that processes some types of request. For example it can process all the requests for `.txt` files.
* `JGIExceptionHandler` &#151; a class that processes an error case. When the server wants to returnan error page (like 404 or 500), 
this class is called to format the page in a proper style and respond to the user.

#### Implementing `JGIScript`
`JGIScript` class has only one method that should be implemented &#151; `onExecute`. This method returns nothing &#151; its duty
is filling the `response` contents.

Create a Java file `CallMe.java` inside your repo with contents:
```java
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bfbc.jiggity.api.exceptions.JGIException;
import bfbc.jiggity.api.JGIScript;

public class CallMe extends JGIScript {
	@Override
	public void onExecute(String target, 
	                      HttpServletRequest request, HttpServletResponse response) throws JGIException {
		try {
			response.getOutputStream().println("That's me!");
			response.getOutputStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
```
Commit your new class with `git add CallMe.java && git commit -m "CallMe class added"`

If the server is started already, just type `http://localhost:8090/CallMe.java` in your browser.

#### Implementing `JGIProcessor`
The method `onRequest` is similar to `JGIScript.onExecute`, but it has to return a boolean value &#151; `true` if the processor has
taken the duty and `false` if it refused it (so other processor should be called). The server will iterate thru the processors
in order to find a compatible one. In addition, `onRequest` receives `fileStream` argument &#151; an `InputStream` that reads the
contents of the requested file. If no file has been found, this argument is null.

This example processes any text (`*.txt`) file, wraps it into HTML and returns to the user:
```java
package pkg;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bfbc.jiggity.api.JGIProcessor;
import bfbc.jiggity.api.exceptions.JGIException;
import bfbc.jiggity.api.exceptions.JGIServerException;
import bfbc.jiggity.api.exceptions.JGIServerException.Code;

public class ClassTest extends JGIProcessor {
	
	@Override
	public boolean onRequest(String target, InputStream fileStream, 
	                         HttpServletRequest request, HttpServletResponse response) throws JGIException {
		try {
			// The file should exist
			if (fileStream == null) return false;
			
			// The file has to be .txt
			if (!target.endsWith(".txt")) return false;
			
			response.setContentType("text/html");
			ServletOutputStream out = response.getOutputStream();
			out.println("<http>");
			out.println("<head><title>" + target + "</title></head>");
			out.println("<body>");
			int r;
			while ((r = fileStream.read()) != -1) {
				out.write(r);
			}
			out.println("</body>");
			out.println("</html>");
			out.close();
			
			return true;
		} catch (IOException e) {
			throw new JGIServerException(Code.INTERNAL_ERROR, e);
		}
	}
}
```

#### Implementing `JGIExceptionHandler`
`JGIExceptionHandler.onError` receives an exception of class `JGIException` that could be `JGIClientException` or `JGIServerException`. 
It should work the same as `JGIProcessor`. The method should return `true` if the exception is processed.

This example just sends the stack trace to the user (it's a bad practice, though, never do that in production!)

```java
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bfbc.jiggity.api.JGIExceptionHandler;
import bfbc.jiggity.api.exceptions.JGIException;

public class ErrorTest extends JGIExceptionHandler {
	@Override
	public boolean onError(String target, 
	                       HttpServletRequest request, HttpServletResponse response, 
	                       JGIException exception) throws Exception {
		exception.printStackTrace(response.getWriter());
		response.getWriter().close();
		return true;
	}
}
```
### Using stash for preview
Each time when you change anything on the server, you need to make a commit, so the server updates the data. It's not comfortable during continuous development. If you don't want to commit every change (and make long and ugly history in your repo), you can use `git stash`.

In order to use this method, set `allow-stash="true"` option in `jiggity.conf.xml`.

Imagine that you have just made some changes in your repo and want to test them.

First of all, add all the changes to index:
```
git add <your_files>
```

Then stash the changes keeping indexed files
```
git stash save --keep-index
```

After you have done that, open your site in the browser &#151; it should update all the data.

After testing, unstash the changes with
```
git stash pop
```

And then you can commit your modifications if you want, or change something and repeat.
