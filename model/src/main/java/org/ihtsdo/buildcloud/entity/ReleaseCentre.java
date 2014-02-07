package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonPropertyOrder({"id"})
public class ReleaseCentre {

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;

	@Column(unique = true)
	@JsonProperty("id")
	private String businessKey;

	private String name;

	@OneToMany(mappedBy = "releaseCentre")
	@JsonIgnore
	private List<Extension> extensions;

	public ReleaseCentre() {
		extensions = new ArrayList<>();
	}

	public ReleaseCentre(String name) {
		this();
		setName(name);
	}

	public void addExtension(Extension extension) {
		extensions.add(extension);
		extension.setReleaseCentre(this);
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

	public List<Extension> getExtensions() {
		return extensions;
	}

}
