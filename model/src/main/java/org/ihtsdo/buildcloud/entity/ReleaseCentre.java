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
public class ReleaseCentre implements DomainEntity{

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;

	@Column(unique = true)
	@JsonProperty("id")
	private String businessKey;

	private String name;

	private String shortName;

	@OneToMany(mappedBy = "releaseCentre")
	@JsonIgnore
	private List<Extension> extensions;

	private boolean removed;

	public ReleaseCentre() {
		extensions = new ArrayList<>();
	}

	public ReleaseCentre(String name, String shortName) {
		this();
		this.name = name;
		setShortName(shortName);
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
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
		this.businessKey = EntityHelper.formatAsBusinessKey(shortName);
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public List<Extension> getExtensions() {
		return extensions;
	}

	public boolean isRemoved() {
		return removed;
	}

	public void setRemoved(boolean removed) {
		this.removed = removed;
	}

	@Override
	public DomainEntity getParent() {
		// Release Centre is the top of the tree, so it never has a parent.
		return null;
	}

	@Override
	public String getCollectionName() {
		//You know, if it wasn't for this object having a different name from it's class, 
		//I'd have used a .getClass().getName() solution!
		return "centres";
	}
}
