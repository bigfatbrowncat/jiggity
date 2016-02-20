package bfbc.gitready.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bfbc.gitready.api.ClientException.Code;

public class JGIScript {
	public void exec(String target, HttpServletRequest request, HttpServletResponse response) throws RequestException {
		throw new ClientException(Code.BAD_REQUEST, "This file (" + target + ") can't be requested this way. Maybe it should be hidden");
	}
}
