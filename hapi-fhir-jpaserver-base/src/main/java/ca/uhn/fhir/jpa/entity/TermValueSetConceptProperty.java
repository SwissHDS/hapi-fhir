/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.entity;

import ca.uhn.fhir.jpa.model.entity.BasePartitionable;
import ca.uhn.fhir.jpa.model.entity.IdAndPartitionId;
import ca.uhn.fhir.util.ValidateUtil;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.length;

@Entity
@Table(
		name = "TRM_VALUESET_C_PROPERTY",
		uniqueConstraints = {},
		indexes = {
			// must have same name that indexed FK or SchemaMigrationTest complains because H2 sets this index
			// automatically
			@Index(name = "FK_TRM_VALUESET_CPROP_PID", columnList = "VALUESET_CONCEPT_PID", unique = false),
			@Index(name = "FK_TRM_VSCP_VS_PID", columnList = "VALUESET_PID")
		})
@IdClass(IdAndPartitionId.class)
public class TermValueSetConceptProperty extends BasePartitionable implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id()
	@SequenceGenerator(name = "SEQ_VALUESET_C_PROP_PID", sequenceName = "SEQ_VALUESET_C_PROP_PID")
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_VALUESET_C_PROP_PID")
	@Column(name = "PID")
	private Long myId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns(
			value = {
				@JoinColumn(
						name = "VALUESET_CONCEPT_PID",
						referencedColumnName = "PID",
						insertable = true,
						updatable = false,
						nullable = false),
				@JoinColumn(
						name = "PARTITION_ID",
						referencedColumnName = "PARTITION_ID",
						insertable = true,
						updatable = false,
						nullable = false)
			},
			foreignKey = @ForeignKey(name = "FK_TRM_VALUESET_CPROP_PID"))
	private TermValueSetConcept myConcept;

	@Column(name = "VALUESET_CONCEPT_PID", insertable = false, updatable = false, nullable = false)
	private Long myConceptPid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns(
			value = {
				@JoinColumn(
						name = "VALUESET_PID",
						referencedColumnName = "PID",
						insertable = false,
						updatable = false,
						nullable = false),
				@JoinColumn(
						name = "PARTITION_ID",
						referencedColumnName = "PARTITION_ID",
						insertable = false,
						updatable = false,
						nullable = false)
			},
			foreignKey = @ForeignKey(name = "FK_TRM_VSCP_VS_PID"))
	private TermValueSet myValueSet;

	@Column(name = "VALUESET_PID", insertable = true, updatable = true, nullable = false)
	private Long myValueSetPid;

	@Column(name = "PROP_KEY", nullable = false, length = TermConceptProperty.MAX_LENGTH)
	@NotBlank
	@GenericField(searchable = Searchable.YES)
	private String myKey;

	@Column(name = "PROP_VAL", nullable = true, length = TermConceptProperty.MAX_LENGTH)
	@FullTextField(searchable = Searchable.YES, projectable = Projectable.YES, analyzer = "standardAnalyzer")
	@GenericField(name = "myValueString", searchable = Searchable.YES)
	private String myValue;

	@Column(name = "PROP_VAL_BIN", nullable = true, length = Length.LONG32)
	private byte[] myValueBin;

	@Enumerated(EnumType.ORDINAL)
	@Column(name = "PROP_TYPE", nullable = false)
	@JdbcTypeCode(SqlTypes.INTEGER)
	private TermConceptPropertyTypeEnum myType;

	/**
	 * Relevant only for properties of type {@link TermConceptPropertyTypeEnum#CODING}
	 */
	@Column(name = "PROP_CODESYSTEM", length = TermConceptProperty.MAX_LENGTH, nullable = true)
	private String myCodeSystem;

	/**
	 * Relevant only for properties of type {@link TermConceptPropertyTypeEnum#CODING}
	 */
	@Column(name = "PROP_DISPLAY", length = TermConceptProperty.MAX_LENGTH, nullable = true)
	@GenericField(name = "myDisplayString", searchable = Searchable.YES)
	private String myDisplay;

	@Transient
	private transient Integer myHashCode;

	public TermValueSetConceptProperty() {
		super();
	}

	public Long getId() {
		return myId;
	}

	public IdAndPartitionId getPartitionedId() {
		return IdAndPartitionId.forId(myId, this);
	}

	public TermValueSetConcept getConcept() {
		return myConcept;
	}

	public TermValueSetConceptProperty setConcept(TermValueSetConcept theConcept) {
		myConcept = theConcept;
		setPartitionId(theConcept.getPartitionId());
		return this;
	}

	public TermValueSet getValueSet() {
		return myValueSet;
	}

	public TermValueSetConceptProperty setValueSet(TermValueSet theValueSet) {
		myValueSet = theValueSet;
		myValueSetPid = theValueSet.getId();
		assert myValueSetPid != null;
		return this;
	}

	/**
	 * Relevant only for properties of type {@link TermConceptPropertyTypeEnum#CODING}
	 */
	public String getCodeSystem() {
		return myCodeSystem;
	}

	/**
	 * Relevant only for properties of type {@link TermConceptPropertyTypeEnum#CODING}
	 */
	public TermValueSetConceptProperty setCodeSystem(String theCodeSystem) {
		ValidateUtil.isNotTooLongOrThrowIllegalArgument(
				theCodeSystem,
				TermConceptProperty.MAX_LENGTH,
				"Property code system exceeds maximum length (" + TermConceptProperty.MAX_LENGTH + "): "
						+ length(theCodeSystem));
		myCodeSystem = theCodeSystem;
		return this;
	}

	/**
	 * Relevant only for properties of type {@link TermConceptPropertyTypeEnum#CODING}
	 */
	public String getDisplay() {
		return myDisplay;
	}

	/**
	 * Relevant only for properties of type {@link TermConceptPropertyTypeEnum#CODING}
	 */
	public TermValueSetConceptProperty setDisplay(String theDisplay) {
		myDisplay = left(theDisplay, TermConceptProperty.MAX_LENGTH);
		return this;
	}

	public String getKey() {
		return myKey;
	}

	public TermValueSetConceptProperty setKey(@Nonnull String theKey) {
		ValidateUtil.isNotBlankOrThrowIllegalArgument(theKey, "theKey must not be null or empty");
		ValidateUtil.isNotTooLongOrThrowIllegalArgument(
				theKey,
				TermConceptProperty.MAX_LENGTH,
				"Code exceeds maximum length (" + TermConceptProperty.MAX_LENGTH + "): " + length(theKey));
		myKey = theKey;
		return this;
	}

	public TermConceptPropertyTypeEnum getType() {
		return myType;
	}

	public TermValueSetConceptProperty setType(@Nonnull TermConceptPropertyTypeEnum theType) {
		Validate.notNull(theType);
		myType = theType;
		return this;
	}

	/**
	 * This will contain the value for a {@link TermConceptPropertyTypeEnum#STRING string}
	 * property, and the code for a {@link TermConceptPropertyTypeEnum#CODING coding} property.
	 */
	public String getValue() {
		if (hasValueBin()) {
			return getValueBinAsString();
		}
		return myValue;
	}

	/**
	 * This will contain the value for a {@link TermConceptPropertyTypeEnum#STRING string}
	 * property, and the code for a {@link TermConceptPropertyTypeEnum#CODING coding} property.
	 */
	public TermValueSetConceptProperty setValue(String theValue) {
		if (theValue.length() > TermConceptProperty.MAX_LENGTH) {
			setValueBin(theValue);
		} else {
			myValueBin = null;
		}
		myValue = left(theValue, TermConceptProperty.MAX_LENGTH);
		return this;
	}

	public boolean hasValueBin() {
		if (myValueBin != null && myValueBin.length > 0) {
			return true;
		}

		return false;
	}

	public TermValueSetConceptProperty setValueBin(byte[] theValueBin) {
		myValueBin = theValueBin;
		return this;
	}

	public TermValueSetConceptProperty setValueBin(String theValueBin) {
		return setValueBin(theValueBin.getBytes(StandardCharsets.UTF_8));
	}

	public String getValueBinAsString() {
		return new String(myValueBin, StandardCharsets.UTF_8);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("conceptPid", myConcept.getId())
				.append("key", myKey)
				.append("value", getValue())
				.toString();
	}

	@Override
	public boolean equals(Object theO) {
		if (this == theO) {
			return true;
		}

		if (theO == null || getClass() != theO.getClass()) {
			return false;
		}

		TermValueSetConceptProperty that = (TermValueSetConceptProperty) theO;

		return new EqualsBuilder()
				.append(myKey, that.myKey)
				.append(myValue, that.myValue)
				.append(myType, that.myType)
				.append(myCodeSystem, that.myCodeSystem)
				.append(myDisplay, that.myDisplay)
				.isEquals();
	}

	@Override
	public int hashCode() {
		if (myHashCode == null) {
			myHashCode = new HashCodeBuilder(17, 37)
					.append(myKey)
					.append(myValue)
					.append(myType)
					.append(myCodeSystem)
					.append(myDisplay)
					.toHashCode();
		}
		return myHashCode;
	}
}
