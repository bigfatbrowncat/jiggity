package bfbc.jiggity.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bfbc.jiggity.api.exceptions.JGIClientException;
import bfbc.jiggity.api.exceptions.JGIException;
import bfbc.jiggity.api.exceptions.JGIClientException.Code;

public class JGIScript {
	public void onExecute(String target, HttpServletRequest request, HttpServletResponse response) throws JGIException {
		throw new JGIClientException(Code.BAD_REQUEST, "This file (" + target + ") can't be requested this way. Maybe it should be hidden");
	}
}
