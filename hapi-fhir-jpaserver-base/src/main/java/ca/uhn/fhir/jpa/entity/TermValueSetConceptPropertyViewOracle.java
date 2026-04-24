/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2026 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Subselect;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;

@Entity
@Immutable
@Subselect(
		/*
		 * Note about the CONCAT function below- We need a primary key (an @Id) column
		 * because hibernate won't allow the view the function without it, but
		 */
		"SELECT CONCAT(vsc.PID, CONCAT(' ', vscd.PID)) AS PID, "
				+ "       vsc.PID                         AS CONCEPT_PID, "
				+ "       vsc.VALUESET_PID                AS CONCEPT_VALUESET_PID, "
				+ "       vsc.VALUESET_ORDER              AS CONCEPT_VALUESET_ORDER, "
				+ "       vsc.DISPLAY                     AS CONCEPT_DISPLAY, "
				+ "       vscp.PID                        AS PROPERTY_PID, "
				+ "       vscp.PROP_KEY                   AS PROPERTY_PROP_KEY, "
				+ "       vscp.PROP_VAL                   AS PROPERTY_PROP_VAL, "
				+ "       vscp.PROP_VAL_BIN               AS PROPERTY_PROP_VAL_BIN, "
				+ "       vscp.PROP_TYPE                  AS PROPERTY_PROP_TYPE, "
				+ "       vscp.PROP_CODESYSTEM            AS PROPERTY_PROP_CODESYSTEM, "
				+ "       vscp.PROP_DISPLAY               AS PROPERTY_PROP_DISPLAY "
				+ "FROM TRM_VALUESET_CONCEPT vsc "
				+ "INNER JOIN TRM_VALUESET_C_PROPERTY vscp ON vsc.PID = vscp.VALUESET_CONCEPT_PID")
public class TermValueSetConceptPropertyViewOracle implements Serializable, ITermValueSetConceptPropertyView {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "PID", length = 1000 /* length only needed to satisfy JpaEntityTest, it's not used*/)
	private String id; // still set automatically

	@Column(name = "CONCEPT_PID")
	private Long myConceptPid;

	@Column(name = "CONCEPT_VALUESET_PID")
	private Long myConceptValueSetPid;

	@Column(name = "CONCEPT_VALUESET_ORDER")
	private Integer myConceptOrder;

	@Column(name = "CONCEPT_DISPLAY", length = TermConcept.MAX_DESC_LENGTH)
	private String myConceptDisplay;

	@Column(name = "PROPERTY_PID")
	private Long myPropertyPid;

	@Column(name = "PROPERTY_PROP_KEY")
	private String myPropertyKey;

	@Column(name = "PROPERTY_PROP_VAL")
	private String myPropertyVal;

	@Column(name = "PROPERTY_PROP_VAL_BIN")
	private byte[] myPropertyValBin;

	@Enumerated(EnumType.ORDINAL)
	@JdbcTypeCode(SqlTypes.INTEGER)
	@Column(name = "PROPERTY_PROP_TYPE")
	private TermConceptPropertyTypeEnum myPropertyType;

	@Column(name = "PROPERTY_PROP_CODESYSTEM")
	private String myPropertyCodeSystem;

	@Column(name = "PROPERTY_PROP_DISPLAY")
	private String myPropertyDisplay;

	@Override
	public Long getConceptPid() {
		return myConceptPid;
	}

	@Override
	public Long getPropertyPid() {
		return myPropertyPid;
	}

	@Override
	public String getPropertyKey() {
		return myPropertyKey;
	}

	@Override
	public String getPropertyVal() {
		return myPropertyVal;
	}

	@Override
	public byte[] getPropertyValBin() {
		return myPropertyValBin;
	}

	@Override
	public TermConceptPropertyTypeEnum getPropertyType() {
		return myPropertyType;
	}

	@Override
	public String getPropertyCodeSystem() {
		return myPropertyCodeSystem;
	}

	@Override
	public String getPropertyDisplay() {
		return myPropertyDisplay;
	}
}
