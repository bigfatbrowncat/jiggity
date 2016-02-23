package bfbc.jiggity.api;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bfbc.jiggity.api.exceptions.JGIException;

public abstract class JGIProcessor extends JGIScript {

	/**
	 * This function is called by the server to process a request form the client
	 * @param target request target &#151; the part of the request after <code>http://yoursite.com:1234/</code>
	 * @param fileStream If a real file found for the target path, it will be opened and sent here. Instead, this
	 * argument will be <code>null</code>
	 * @param response The response object to write the result into
	 * @return
	 * @throws Exception
	 */
	public abstract boolean onRequest(String target, InputStream fileStream, HttpServletRequest request, HttpServletResponse response) throws JGIException; 
}
