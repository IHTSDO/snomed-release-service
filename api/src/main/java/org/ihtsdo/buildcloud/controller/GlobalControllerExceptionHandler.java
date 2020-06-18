package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.otf.rest.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalControllerExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

	@ExceptionHandler(AuthenticationException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	@ResponseBody
	public Map<String, String> handleAuthenticationError(Exception exception, HttpServletRequest request) {
		logError(request, exception);
		return getErrorPayload(exception, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(BadRequestException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public Map<String, String> handleBadRequestError(Exception exception, HttpServletRequest request) {
		logError(request, exception);
		return getErrorPayload(exception, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	@ResponseBody
	public Map<String, String> handleMethodNotSupportedError(Exception exception, HttpServletRequest request) {
		logError(request, exception);
		return getErrorPayload(exception, HttpStatus.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ResponseBody
	public Map<String, String> handleResourceNotFoundError(Exception exception, HttpServletRequest request) {
		logError(request, exception);
		return getErrorPayload(exception, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler({MissingServletRequestParameterException.class, TypeMismatchException.class})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public Map<String, String> handleMissingServletRequestParameterException(Exception exception, HttpServletRequest request) {
		logError(request, exception);
		return getErrorPayload(exception, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(BadConfigurationException.class)
	@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
	@ResponseBody
	public Map<String, String> handleBadConfigurationException(Exception exception, HttpServletRequest request) {
		logError(request, exception);
		return getErrorPayload(exception, HttpStatus.PRECONDITION_FAILED);
	}

	@ExceptionHandler(EntityAlreadyExistsException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	@ResponseBody
	public Map<String, String> handleEntityAlreadyExistsException(Exception exception, HttpServletRequest request) {
		logError(request, exception);
		return getErrorPayload(exception, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public Map<String, String> handleError(Exception exception, HttpServletRequest request) {
		logError(request, exception);
		return getErrorPayload(exception, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private Map<String, String> getErrorPayload(Exception exception, HttpStatus httpStatus) {
		Map<String, String> errorObject = new HashMap<>();
		errorObject.put(ControllerConstants.ERROR_MESSAGE, exception.getLocalizedMessage());
		errorObject.put(ControllerConstants.HTTP_STATUS, httpStatus.toString());
		return errorObject;
	}

	private void logError(HttpServletRequest request, Exception exception) {
		LOGGER.error("Request '{}' raised: " + exception.getMessage(), request.getRequestURL(), exception);
	}

}
