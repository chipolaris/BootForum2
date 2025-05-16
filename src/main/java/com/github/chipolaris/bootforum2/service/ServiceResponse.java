package com.github.chipolaris.bootforum2.service;

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

	public enum AckCodeType {
		SUCCESS,
		WARNING,
		FAILURE
	}
}
