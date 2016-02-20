package bfbc.gitready.api;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class RequestExceptionProcessor extends JGIScript {
	public abstract boolean processError(String target, HttpServletRequest request, HttpServletResponse response, RequestException exception) throws Exception; 

}
