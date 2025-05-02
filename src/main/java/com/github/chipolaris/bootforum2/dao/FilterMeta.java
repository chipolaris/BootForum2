package com.github.chipolaris.bootforum2.dao;

public class FilterMeta {

	private Object value;
	private MatchMode matchMode;
	private Operator operator;
	
	public FilterMeta() {
		
	}
	
	public FilterMeta(Object value, String matchMode, String operator) {
		super();
		this.value = value;
		this.matchMode = toMatchMode(matchMode);
		this.operator = toOperator(operator);
	}

	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	public MatchMode getMatchMode() {
		return matchMode;
	}
	public void setMatchMode(MatchMode matchMode) {
		this.matchMode = matchMode;
	}
	public Operator getOperator() {
		return operator;
	}
	public void setOperator(Operator operator) {
		this.operator = operator;
	}
	
	private static MatchMode toMatchMode(String str) {
		switch(str) {
			case "startsWith":
				return MatchMode.STARTS_WITH;
			case "endsWith":
				return MatchMode.ENDS_WITH;
			case "equals":
				return MatchMode.EQUALS;
			case "notEquals":
				return MatchMode.NOT_EQUALS;
			case "contains":
				return MatchMode.CONTAINS;
			case "notContains":
				return MatchMode.NOT_CONTAINS;
			case "lt":
				return MatchMode.LESS_THAN;
			case "lte":
				return MatchMode.LESS_THAN_OR_EQUAL_TO;
			case "gt":
				return MatchMode.GREATER_THAN;
			case "gte":
				return MatchMode.GREATER_THAN_OR_EQUAL_TO;
			case "dateIs":
				return MatchMode.DATE_IS;
			case "dateIsNot":
				return MatchMode.DATE_IS_NOT;
			case "dateBefore":
				return MatchMode.DATE_BEFORE;
			case "dateAfter":
				return MatchMode.DATE_AFTER;
		}
		
		return null;
	}
	
	private static Operator toOperator(String str) {
		
		switch(str) {
			case "and" :
				return Operator.AND;
			case "or":
				return Operator.OR;
		}
		
		return null;
	}
	
	enum MatchMode {
		STARTS_WITH, CONTAINS, NOT_CONTAINS, ENDS_WITH, EQUALS, NOT_EQUALS, LESS_THAN, LESS_THAN_OR_EQUAL_TO,
		GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, DATE_IS, DATE_IS_NOT, DATE_BEFORE, DATE_AFTER
	}
	
	enum Operator {
		AND, OR
	}
}
