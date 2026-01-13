package org.hl7.fhir.common.hapi.validation.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;

import java.util.List;

public class TxResourceValidationSupport implements IValidationSupport {
	private static final ThreadLocal<List<IBaseResource>> ourTxResourceThreadLocal = new ThreadLocal<>();

	private final FhirContext myCtx;

	public TxResourceValidationSupport(FhirContext theCtx) {
		Validate.notNull(theCtx, "theCtx must not be null");
		myCtx = theCtx;
	}

	@Override
	public String getName() {
		return myCtx.getVersion().getVersion() + " TxResourceValidationSupport";
	}

	@Override
	public FhirContext getFhirContext() {
		return myCtx;
	}

	@Override
	public @Nullable IBaseResource fetchValueSet(String theValueSetUrl) {
		var txResources = ourTxResourceThreadLocal.get();
		return txResources == null
				? null
				: txResources.stream()
						.filter(resource ->
								resource instanceof ValueSet vs && vs.getUrl().equals(theValueSetUrl))
						.findFirst()
						.orElse(null);
	}

	@Override
	public @Nullable IBaseResource fetchCodeSystem(String theSystem) {
		var txResources = ourTxResourceThreadLocal.get();
		return txResources == null
				? null
				: txResources.stream()
						.filter(resource ->
								resource instanceof CodeSystem cs && cs.getUrl().equals(theSystem))
						.findFirst()
						.orElse(null);
	}

	public void setTxResourceForCurrentRequest(List<IBaseResource> theTxResources) {
		ourTxResourceThreadLocal.set(theTxResources);
	}

	public void clearTxResourceForCurrentRequest() {
		ourTxResourceThreadLocal.remove();
	}
}
