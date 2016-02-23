package bfbc.jiggity.api.exceptions;

public class JGIClientException extends JGIException {
	public enum Code {
		BAD_REQUEST(400),
		UNAUTHORIZED(401),
		PAYMENT_REQUIRED(402),
		FORBIDDEN(403),
		NOT_FOUND(404),
		METHOD_NOT_ALLOWED(405);
		
		public final int httpCode;
		
		Code(int httpCode) {
			this.httpCode = httpCode;
		}
	}
	
	private Code code;
	
	public JGIClientException(Code code) {
		super();
		this.code = code;
	}

	public JGIClientException(Code code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public JGIClientException(Code code, String message) {
		super(message);
		this.code = code;
	}

	public JGIClientException(Code code, Throwable cause) {
		super(cause);
		this.code = code;
	}
	
	public JGIClientException() {
		this(Code.BAD_REQUEST);
	}
	
	public Code getCode() {
		return code;
	}
}
