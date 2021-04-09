package org.ihtsdo.buildcloud.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.hibernate.annotations.Type;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

@Entity
@JsonPropertyOrder({"id", "name"})
@Table(name="release_center")
public class ReleaseCenter {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonIgnore
	private Long id;

	@Column(name="business_key", unique = true)
	@JsonProperty("id")
	private String businessKey;
	
	@Column(name="name")
	private String name;
	
	@Column(name="short_name")
	private String shortName;

	@Column(name="code_system")
	private String codeSystem;

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

	public ReleaseCenter(final String name, final String shortName, final String codeSystem) {
		this();
		this.name = name;
		setShortName(shortName);
		setCodeSystem(codeSystem);
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

	public String getCodeSystem() {
		return codeSystem;
	}

	public void setCodeSystem(String codeSystem) {
		this.codeSystem = codeSystem;
	}
}
