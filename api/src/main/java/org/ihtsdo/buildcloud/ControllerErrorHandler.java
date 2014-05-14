package org.ihtsdo.buildcloud;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class ControllerErrorHandler {

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public String handleMyException(Exception exception, HttpServletRequest request) {
		return "/error/myerror";
	}
}
