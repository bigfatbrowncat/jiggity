package bfbc.jiggity.api.exceptions;

public class JGIServerException extends JGIException {
	public enum Code {
		INTERNAL_ERROR(500),
		NOT_IMPLEMENTED(501);
		
		public final int httpCode;
		
		Code(int httpCode) {
			this.httpCode = httpCode;
		}
	}
	
	private Code code;
	
	public JGIServerException(Code code) {
		super();
		this.code = code;
	}

	public JGIServerException(Code code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public JGIServerException(Code code, String message) {
		super(message);
		this.code = code;
	}

	public JGIServerException(Code code, Throwable cause) {
		super(cause);
		this.code = code;
	}
	
	public JGIServerException() {
		this(Code.INTERNAL_ERROR);
	}
	
	public Code getCode() {
		return code;
	}
	
}
