package bfbc.gitready.api;

public class ServerException extends RequestException {
	public enum Code {
		INTERNAL_ERROR(500),
		NOT_IMPLEMENTED(501);
		
		public final int httpCode;
		
		Code(int httpCode) {
			this.httpCode = httpCode;
		}
	}
	
	private Code code;
	
	public ServerException(Code code) {
		super();
		this.code = code;
	}

	public ServerException(Code code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public ServerException(Code code, String message) {
		super(message);
		this.code = code;
	}

	public ServerException(Code code, Throwable cause) {
		super(cause);
		this.code = code;
	}
	
	public ServerException() {
		this(Code.INTERNAL_ERROR);
	}
	
	public Code getCode() {
		return code;
	}
	
}
