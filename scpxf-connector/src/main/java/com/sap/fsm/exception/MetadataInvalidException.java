package com.sap.fsm.exception;

public class MetadataInvalidException extends RuntimeException{

	private static final long serialVersionUID = -9204263913670593402L;

	public MetadataInvalidException(String message) {
		super(message);
	}
	
	public MetadataInvalidException(String message, Throwable e) {
		super(message, e);
	}
}
