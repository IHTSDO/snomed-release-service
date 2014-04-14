package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Extension {

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;

	private String name;

	@JsonProperty("id")
	private String businessKey;

	@ManyToOne
	@JsonIgnore
	private ReleaseCenter releaseCenter;

	@OneToMany(mappedBy = "extension")
	@JsonIgnore
	private List<Product> products;

	public Extension() {
		products = new ArrayList<>();
	}

	public Extension(String name) {
		this();
		setName(name);
	}

	public void addProduct(Product product) {
		products.add(product);
		product.setExtension(this);
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
		this.businessKey = EntityHelper.formatAsBusinessKey(name);
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public ReleaseCenter getReleaseCenter() {
		return releaseCenter;
	}

	public void setReleaseCenter(ReleaseCenter releaseCenter) {
		this.releaseCenter = releaseCenter;
	}

	public List<Product> getProducts() {
		return products;
	}

}
