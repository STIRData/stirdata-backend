package com.ails.stirdatabackend.payload;

import java.util.function.Consumer;

public class ResultDTO<U> {
	U success;
	String error;
	
	
	public static <U> ResultDTO<U> ok(U val ) {
		return new ResultDTO<U>( val );
	}
	
	public static <U> ResultDTO<U> fail( String msg ) {
		return new ResultDTO<U>( null, msg );
	}
	
	
	public ResultDTO( U success ) {
		this.success = success;
	}
	
	public ResultDTO( U success, String errorMsg )  {
		error = errorMsg;
	}
	
	public void onError( Consumer<String> cons ) {
		cons.accept(error);
	}
	
	public void onSuccess( Consumer<U> cons ) {
		cons.accept( success );
	}
	
	public boolean isError( ) {
		return error != null;
	}
	
	public U getResult() {
		return success;
	}
	
	public String getError() {
		return error;
	}
}