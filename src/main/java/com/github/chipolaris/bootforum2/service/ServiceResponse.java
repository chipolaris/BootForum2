package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dto.FileResourceDTO;

import java.util.ArrayList;
import java.util.List;

public class ServiceResponse<T> {

	public ServiceResponse() {
		ackCode = AckCodeType.SUCCESS; // default
		messages = new ArrayList<>();
	}

	private T dataObject;
	private AckCodeType ackCode;
	private List<String> messages;

	public T getDataObject() {
		return dataObject;
	}

	public ServiceResponse<T> setDataObject(T dataObject) {
		this.dataObject = dataObject;
		return this;
	}

	public AckCodeType getAckCode() {
		return ackCode;
	}

	public ServiceResponse<T> setAckCode(AckCodeType ackCode) {
		this.ackCode = ackCode;
		return this;
	}

	public List<String> getMessages() {
		return messages;
	}

	public ServiceResponse<T> setMessages(List<String> messages) {
		this.messages = messages;
		return this;
	}

	public ServiceResponse<T> addMessage(String message) {
		this.messages.add(message);
		return this;
	}

	public ServiceResponse<T> addMessages(List<String> messages) {
		this.messages.addAll(messages);
		return this;
	}

	/*
	 * Utility methods
	 */
	public boolean isSuccess() {
		return this.ackCode == AckCodeType.SUCCESS;
	}

	public boolean isWarning() {
		return this.ackCode == AckCodeType.WARNING;
	}

	public boolean isFailure() {
		return this.ackCode == AckCodeType.FAILURE;
	}

	public enum AckCodeType {
		SUCCESS,
		WARNING,
		FAILURE
	}

	/*
	 * Convenient static methods
	 */
	public static <T> ServiceResponse<T> success(String message, T dataObject) {
		ServiceResponse response = new ServiceResponse<>(); // SUCCESS
		response.addMessage(message).setDataObject(dataObject);

		return response;
	}

	public static <T> ServiceResponse<T> success(String message) {
		ServiceResponse response = new ServiceResponse<>(); // SUCCESS
		response.addMessage(message);

		return response;
	}

	public static <T> ServiceResponse<T> error(String message) {
		ServiceResponse response = new ServiceResponse<>();
		response.setAckCode(AckCodeType.FAILURE).addMessage(message);

		return response;
	}
}
