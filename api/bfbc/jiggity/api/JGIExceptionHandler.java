package bfbc.jiggity.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bfbc.jiggity.api.exceptions.JGIException;

public abstract class JGIExceptionHandler extends JGIScript {
	public abstract boolean onError(String target, HttpServletRequest request, HttpServletResponse response, JGIException exception) throws Exception; 

}
