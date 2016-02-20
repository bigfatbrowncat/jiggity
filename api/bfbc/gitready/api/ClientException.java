package bfbc.gitready.api;

public class ClientException extends RequestException {
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
	
	public ClientException(Code code) {
		super();
		this.code = code;
	}

	public ClientException(Code code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public ClientException(Code code, String message) {
		super(message);
		this.code = code;
	}

	public ClientException(Code code, Throwable cause) {
		super(cause);
		this.code = code;
	}
	
	public ClientException() {
		this(Code.BAD_REQUEST);
	}
	
	public Code getCode() {
		return code;
	}
}
