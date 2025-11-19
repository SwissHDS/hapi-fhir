/*
 * #%L
 * HAPI FHIR - Core Library
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
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
package ca.uhn.fhir.util;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hl7.fhir.instance.model.api.IBaseDatatype;

import java.util.List;
import java.util.Objects;

public class FhirVersionIndependentConcept implements Comparable<FhirVersionIndependentConcept> {

	private final String mySystem;
	private final String mySystemVersion;
	private final String myCode;
	private final String myDisplay;
	private final List<ConceptPropertyComponent> myProperty;
	private int myHashCode;

	public FhirVersionIndependentConcept(String theSystem, String theCode) {
		this(theSystem, theCode, null, List.of());
	}

	public FhirVersionIndependentConcept(String theSystem, String theCode, List<ConceptPropertyComponent> theProperty) {
		this(theSystem, theCode, null, theProperty);
	}

	public FhirVersionIndependentConcept(String theSystem, String theCode, String theDisplay) {
		this(theSystem, theCode, theDisplay, null, List.of());
	}

	public FhirVersionIndependentConcept(
			String theSystem, String theCode, String theDisplay, List<ConceptPropertyComponent> theProperty) {
		this(theSystem, theCode, theDisplay, null, theProperty);
	}

	public FhirVersionIndependentConcept(String theSystem, String theCode, String theDisplay, String theSystemVersion) {
		this(theSystem, theCode, theDisplay, theSystemVersion, List.of());
	}

	public FhirVersionIndependentConcept(
			String theSystem,
			String theCode,
			String theDisplay,
			String theSystemVersion,
			List<ConceptPropertyComponent> theProperty) {
		mySystem = theSystem;
		mySystemVersion = theSystemVersion;
		myCode = theCode;
		myDisplay = theDisplay;
		myProperty = theProperty;
		myHashCode = new HashCodeBuilder(17, 37).append(mySystem).append(myCode).toHashCode();
	}

	public String getDisplay() {
		return myDisplay;
	}

	public String getSystem() {
		return mySystem;
	}

	public String getSystemVersion() {
		return mySystemVersion;
	}

	public String getCode() {
		return myCode;
	}

	public List<ConceptPropertyComponent> getProperty() {
		return myProperty;
	}

	public @Nullable ConceptPropertyComponent findPropertyByCode(String theCode) {
		for (ConceptPropertyComponent next : myProperty) {
			if (Objects.equals(next.getCode(), theCode)) {
				return next;
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object theO) {
		if (this == theO) {
			return true;
		}

		if (theO == null || getClass() != theO.getClass()) {
			return false;
		}

		FhirVersionIndependentConcept that = (FhirVersionIndependentConcept) theO;

		return new EqualsBuilder()
				.append(mySystem, that.mySystem)
				.append(myCode, that.myCode)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return myHashCode;
	}

	@Override
	public int compareTo(FhirVersionIndependentConcept theOther) {
		CompareToBuilder b = new CompareToBuilder();
		b.append(mySystem, theOther.getSystem());
		b.append(myCode, theOther.getCode());
		return b.toComparison();
	}

	@Override
	public String toString() {
		return "[" + mySystem + "|" + myCode + "]";
	}

	public static class ConceptPropertyComponent {
		private final String myCode;
		private final IBaseDatatype myValue;

		public ConceptPropertyComponent(String theCode, IBaseDatatype theValue) {
			myCode = theCode;
			myValue = theValue;
		}

		public String getCode() {
			return myCode;
		}

		public IBaseDatatype getValue() {
			return myValue;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ConceptPropertyComponent that = (ConceptPropertyComponent) o;
			return Objects.equals(myCode, that.myCode) && Objects.equals(myValue, that.myValue);
		}

		@Override
		public int hashCode() {
			return Objects.hash(myCode, myValue);
		}
	}
}
