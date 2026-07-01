package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hibernate.type.YesNoConverter;
import org.ihtsdo.buildcloud.core.entity.helper.CollectionConverter;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Entity
@Table(name="manifest_config")
public class ManifestConfig {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonIgnore
	@Column(name="id")
	private long id;
	
	@OneToOne
	@JoinColumn(name="product_id")
	@JsonIgnore
	private Product product;
	
	@Column(name="auto_generate_manifest")
	@Convert(converter = YesNoConverter.class)
	private boolean autoGenerateManifest;

	@Column(name="derivative_product")
	@Convert(converter = YesNoConverter.class)
	private boolean derivativeProduct;

	@Column(name="include_product_namespace_in_package")
	@Convert(converter = YesNoConverter.class)
	private boolean includeProductNamespaceInPackage;

	@Column(name="package_simple_refsets_individually")
	@Convert(converter = YesNoConverter.class)
	private boolean packageSimpleRefsetsIndividually;

	@Column(name="package_effective_time")
	private Date packageEffectiveTime;

	@Column(name = "excluded_refsets", columnDefinition = "TEXT DEFAULT NULL")
	private String excludedRefsets;

	@Column(name = "excluded_rf2_files", columnDefinition = "TEXT DEFAULT NULL")
	@Schema(description = "Pipe-delimited list of filename fragments used to exclude RF2 files from the generated manifest. "
			+ "Any file whose name contains one of the configured values is excluded from all release views (Snapshot, Full, and Delta). "
			+ "Example: sct2_StatedRelationship|der2_Refset_Simple. "
			+ "Matching uses substring (contains) comparison, consistent with the Remove RF2 Files build parameter. "
			+ "Use distinctive filename fragments rather than wildcards.")
	private String excludedRf2Files;

	@Column(name = "included_external_simple_refsets", columnDefinition = "TEXT DEFAULT NULL")
	@Schema(description = "Comma-separated list of Simple refset concept IDs whose manifest source should be configured as "
			+ "externally-maintained rather than terminology-server.")
	private String includedExternalSimpleRefsets;

	@Column(name="product_name")
	private String productName;

	@Column(name="product_namespace")
	private String productNamespace;

	public long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public boolean isAutoGenerateManifest() {
		return autoGenerateManifest;
	}

	public void setAutoGenerateManifest(boolean autoGenerateManifest) {
		this.autoGenerateManifest = autoGenerateManifest;
	}

	public boolean isDerivativeProduct() {
		return derivativeProduct;
	}

	public void setDerivativeProduct(boolean derivativeProduct) {
		this.derivativeProduct = derivativeProduct;
	}

	public boolean isIncludeProductNamespaceInPackage() {
		return includeProductNamespaceInPackage;
	}

	public boolean isPackageSimpleRefsetsIndividually() {
		return packageSimpleRefsetsIndividually;
	}

	public void setPackageEffectiveTime(Date packageEffectiveTime) {
		this.packageEffectiveTime = packageEffectiveTime;
	}

	@JsonIgnore
	public Date getPackageEffectiveTime() {
		return packageEffectiveTime;
	}

	@JsonProperty("packageEffectiveTime")
	public String getPackageEffectiveTimeFormatted() {
		return packageEffectiveTime != null ? DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(packageEffectiveTime) : null;
	}

	@JsonProperty("packageEffectiveTime")
	public void setPackageEffectiveTimeFormatted(final String packageEffectiveTimeFormatted) throws ParseException {
		if (packageEffectiveTimeFormatted == null || packageEffectiveTimeFormatted.isBlank()) {
			this.packageEffectiveTime = null;
		} else {
			this.packageEffectiveTime = DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(packageEffectiveTimeFormatted);
		}
	}

	@JsonIgnore
	public String getPackageEffectiveTimeSnomedFormat() {
		return packageEffectiveTime != null ? DateFormatUtils.format(packageEffectiveTime, Product.SNOMED_DATE_FORMAT) : null;
	}

	public void setIncludeProductNamespaceInPackage(boolean includeProductNamespaceInPackage) {
		this.includeProductNamespaceInPackage = includeProductNamespaceInPackage;
	}

	public void setPackageSimpleRefsetsIndividually(boolean packageSimpleRefsetsIndividually) {
		this.packageSimpleRefsetsIndividually = packageSimpleRefsetsIndividually;
	}

	public String getExcludedRefsets() {
		return excludedRefsets;
	}

	public List<String> getExcludedRefsetsAsList() {
		return excludedRefsets == null ? Collections.emptyList() : CollectionConverter.convertToEntityAttribute(excludedRefsets);
	}

	public void setExcludedRefsets(String excludedRefsets) {
		this.excludedRefsets = excludedRefsets;
	}

	public String getExcludedRf2Files() {
		return excludedRf2Files;
	}

	public List<String> getExcludedRf2FilesAsList() {
		if (excludedRf2Files == null || excludedRf2Files.isBlank()) {
			return Collections.emptyList();
		}
		return Arrays.stream(excludedRf2Files.split("\\|"))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
	}

	public void setExcludedRf2Files(String excludedRf2Files) {
		this.excludedRf2Files = excludedRf2Files;
	}

	public String getIncludedExternalSimpleRefsets() {
		return includedExternalSimpleRefsets;
	}

	public List<String> getIncludedExternalSimpleRefsetsAsList() {
		if (includedExternalSimpleRefsets == null || includedExternalSimpleRefsets.isBlank()) {
			return Collections.emptyList();
		}
		return CollectionConverter.convertToEntityAttribute(includedExternalSimpleRefsets).stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
	}

	public void setIncludedExternalSimpleRefsets(String includedExternalSimpleRefsets) {
		this.includedExternalSimpleRefsets = includedExternalSimpleRefsets;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getProductNamespace() {
		return productNamespace;
	}

	public void setProductNamespace(String productNamespace) {
		this.productNamespace = productNamespace;
	}
}
