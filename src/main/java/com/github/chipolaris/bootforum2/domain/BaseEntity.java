package com.github.chipolaris.bootforum2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

@MappedSuperclass
public abstract class BaseEntity {
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="CREATE_DATE")
	private Date createDate;
	
	@Column(name = "CREATE_BY", nullable = true, length=50)
	private String createBy;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="UPDATE_DATE")
	private Date updateDate;
	
	@Column(name = "UPDATE_BY", nullable = true, length=50)
	private String updateBy;
	
	public abstract Long getId();
	
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	
	public String getCreateBy() {
		return createBy;
	}
	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}
	
	public Date getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
	
	public String getUpdateBy() {
		return updateBy;
	}
	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? super.hashCode() : getId().hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object other) {
		
		if(this == other) {
			return true;
		}
		
		if (this.getClass() != other.getClass()) {
			return false;
		}
		BaseEntity otherObj = (BaseEntity) other;

		return this.getId().equals(otherObj.getId());
	}
}
