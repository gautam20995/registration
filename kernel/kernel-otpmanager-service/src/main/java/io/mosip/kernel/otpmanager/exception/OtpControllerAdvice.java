package io.mosip.kernel.otpmanager.exception;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.mosip.kernel.core.exception.ErrorResponse;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.otpmanager.constant.OtpErrorConstants;

/**
 * Central class for handling exceptions.
 * 
 * @author Ritesh Sinha
 * @author Sagar Mahapatra
 * @since 1.0.0
 */
@RestControllerAdvice
public class OtpControllerAdvice {

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * This method handles MethodArgumentNotValidException.
	 * 
	 * @param e
	 *            The exception
	 * @return The response entity.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse<ServiceError>> otpGeneratorValidity(final MethodArgumentNotValidException e) {
		ErrorResponse<ServiceError> errorResponse = new ErrorResponse<>();
		final List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
		fieldErrors.forEach(x -> {
			ServiceError error = new ServiceError(OtpErrorConstants.OTP_GEN_ILLEGAL_KEY_INPUT.getErrorCode(),
					x.getField() + ": " + x.getDefaultMessage());
			errorResponse.getErrors().add(error);
		});
		errorResponse.setStatus(HttpStatus.OK.value());
		return new ResponseEntity<>(errorResponse, HttpStatus.OK);
	}

	/**
	 * This method handles OtpInvalidArgumentException.
	 * 
	 * @param exception
	 *            The exception.
	 * @return The response entity.
	 */
	@ExceptionHandler(OtpInvalidArgumentException.class)
	public ResponseEntity<ErrorResponse<ServiceError>> otpValidationArgumentValidity(
			final OtpInvalidArgumentException exception) {
		ErrorResponse<ServiceError> errorResponse = new ErrorResponse<>();
		errorResponse.getErrors().addAll(exception.getList());
		errorResponse.setStatus(HttpStatus.OK.value());
		return new ResponseEntity<>(errorResponse, HttpStatus.OK);
	}

	/**
	 * This method handles RequiredKeyNotFoundException.
	 * 
	 * @param exception
	 *            The exception.
	 * @return The response entity.
	 */
	@ExceptionHandler(RequiredKeyNotFoundException.class)
	public ResponseEntity<ErrorResponse<ServiceError>> otpValidationKeyNullValidity(
			final RequiredKeyNotFoundException exception) {
		ErrorResponse<ServiceError> errorResponse = new ErrorResponse<>();
		errorResponse.getErrors().addAll(exception.getList());
		errorResponse.setStatus(HttpStatus.OK.value());
		return new ResponseEntity<>(errorResponse, HttpStatus.OK);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse<ServiceError>> onHttpMessageNotReadable(
			final HttpMessageNotReadableException e) {
		ErrorResponse<ServiceError> errorResponse = new ErrorResponse<>();
		ServiceError error = new ServiceError(OtpErrorConstants.OTP_VAL_INVALID_OTP_INPUT.getErrorCode(),
				e.getMessage());
		errorResponse.getErrors().add(error);
		errorResponse.setStatus(HttpStatus.OK.value());
		return new ResponseEntity<>(errorResponse, HttpStatus.OK);
	}

	@ExceptionHandler(value = { Exception.class, RuntimeException.class })
	public ResponseEntity<ResponseWrapper<ServiceError>> defaultErrorHandler(HttpServletRequest httpServletRequest,
			Exception e) throws IOException {
		ResponseWrapper<ServiceError> responseWrapper = setErrors(httpServletRequest);
		ServiceError error = new ServiceError(OtpErrorConstants.INTERNAL_SERVER_ERROR.getErrorCode(), e.getMessage());
		responseWrapper.getErrors().add(error);
		return new ResponseEntity<>(responseWrapper, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private ResponseWrapper<ServiceError> setErrors(HttpServletRequest httpServletRequest) throws IOException {
		RequestWrapper<?> requestWrapper = null;
		ResponseWrapper<ServiceError> responseWrapper = new ResponseWrapper<>();
		String requestBody = null;
		if (httpServletRequest instanceof ContentCachingRequestWrapper) {
			requestBody = new String(((ContentCachingRequestWrapper) httpServletRequest).getContentAsByteArray());
		}
		objectMapper.registerModule(new JavaTimeModule());
		requestWrapper = objectMapper.readValue(requestBody, RequestWrapper.class);
		responseWrapper.setId(requestWrapper.getId());
		responseWrapper.setVersion(requestWrapper.getVersion());
		responseWrapper.setResponsetime(LocalDateTime.now(ZoneId.of("UTC")));
		return responseWrapper;
	}

}
