package org.ihtsdo.buildcloud.entity;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Build {

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;

	private String name;

	private Date effectiveTime;

	private boolean firstTimeRelease;

	private boolean starred;

	@JsonIgnore
	private String businessKey;

	@ManyToOne
	@JsonIgnore
	private Product product;

	@OneToMany(mappedBy = "build")
	@JsonIgnore
	private List<Package> packages;

	public static final String SNOMED_DATE_FORMAT = "yyyyMMdd";

	public Build() {
		packages = new ArrayList<>();
		firstTimeRelease = true;
	}

	public Build(String name) {
		this();
		setName(name);
	}
	
	public Build(String name, boolean isStarred) {
		this();
		this.setName(name);
		this.starred = isStarred;
	}	

	public Build(Long id, String name) {
		this(name);
		this.id = id;
	}

	public void addPackage(Package aPackage) {
		packages.add(aPackage);
		aPackage.setBuild(this);
	}

	@JsonProperty("id")
	public String getCompositeKey() {
		return id + "_" + businessKey;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		generateBusinessKey();
	}

	@JsonProperty("effectiveTime")
	public String getEffectiveDateFormatted() {
		return effectiveTime != null ? DateFormatUtils.ISO_DATE_FORMAT.format(effectiveTime) : null;
	}

	@JsonIgnore
	public String getEffectiveTimeSnomedFormat() {
		return effectiveTime != null ? DateFormatUtils.format(effectiveTime, SNOMED_DATE_FORMAT) : null;
	}

	public Date getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(Date effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public List<Package> getPackages() {
		return packages;
	}

	private void generateBusinessKey() {
		this.businessKey = EntityHelper.formatAsBusinessKey(name);
	}

	public boolean isFirstTimeRelease() {
		return firstTimeRelease;
	}

	public void setFirstTimeRelease(boolean firstTimeRelease) {
		this.firstTimeRelease = firstTimeRelease;
	}

	public boolean isStarred() {
		return starred;
	}

	public void setStarred(boolean isStarred) {
		this.starred = isStarred;
	}
}
