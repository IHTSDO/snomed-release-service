package org.ihtsdo.buildcloud.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.hibernate.annotations.Type;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

@Entity
@JsonPropertyOrder({"id", "name"})
public class ReleaseCenter {

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;

	@Column(unique = true)
	@JsonProperty("id")
	private String businessKey;

	private String name;

	private String shortName;

	@OneToMany(mappedBy = "releaseCenter")
	@JsonIgnore
	private final List<Product> products;
	@Type(type="yes_no")
	private boolean removed = false;

	public ReleaseCenter() {
		products = new ArrayList<>();
	}

	public ReleaseCenter(final String name, final String shortName) {
		this();
		this.name = name;
		setShortName(shortName);
	}

	public void addProduct(final Product product) {
		products.add(product);
		product.setReleaseCenter(this);
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(final String shortName) {
		this.shortName = shortName;
		this.businessKey = EntityHelper.formatAsBusinessKey(shortName);
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public List<Product> getProducts() {
		return products;
	}

	public boolean isRemoved() {
		return removed;
	}

	public void setRemoved(final boolean removed) {
		this.removed = removed;
	}
}
