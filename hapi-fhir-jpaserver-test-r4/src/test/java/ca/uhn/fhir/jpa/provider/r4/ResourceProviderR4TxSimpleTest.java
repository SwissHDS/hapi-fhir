package ca.uhn.fhir.jpa.provider.r4;

import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.provider.BaseResourceProviderR4Test;
import jakarta.annotation.Nonnull;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UrlType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ResourceProviderR4TxSimpleTest extends BaseResourceProviderR4Test {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ResourceProviderR4TxSimpleTest.class);
	private final Pattern myIdPattern = Pattern.compile("^[A-Za-z0-9.-]{1,64}$");
	private final Pattern myUuidPattern = Pattern.compile("^urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

	private CodeSystem loadCodeSystem() throws IOException {
		return loadResourceFromClasspath(CodeSystem.class, "/tx-ecosystem/simple/codesystem-simple.json");
	}

	private ValueSet loadValueSet(String valueSetName) throws IOException {
		return loadResourceFromClasspath(ValueSet.class, "/tx-ecosystem/simple/valueset-" + valueSetName + ".json");
	}

	private void loadAndPersistCodeSystem() throws IOException {
		CodeSystem codeSystem = loadCodeSystem();
		new TransactionTemplate(myTxManager).execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(@Nonnull TransactionStatus theStatus) {
				myCodeSystemDao.create(codeSystem, mySrd);
			}
		});
	}

	private void loadAndPersistValueSet(String valueSetName) throws IOException {
		ValueSet valueSet = loadValueSet(valueSetName);
		new TransactionTemplate(myTxManager).execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(@Nonnull TransactionStatus theStatus) {
				myValueSetDao.create(valueSet, mySrd);
			}
		});
	}

	@Test
	public void testExpandAll() throws Exception {
		loadAndPersistCodeSystem();
		loadAndPersistValueSet("all");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-all"))
			.andParameter("excludeNested", new BooleanType(true))
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandAllValueSet(expanded);
	}

	@Test
	public void testExpandAllTxResource() throws Exception {
		CodeSystem codeSystem = loadCodeSystem();
		ValueSet valueSet = loadValueSet("all");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-all"))
			.andParameter("excludeNested", new BooleanType(true))
			.andParameter("tx-resource", codeSystem)
			.andParameter("tx-resource", valueSet)
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandAllValueSet(expanded);
	}

	private void assertExpandAllValueSet(ValueSet expanded) {
		var expansion = expanded.getExpansion();
		var contains = expansion.getContains();
		logAsJson(expanded);
		assertAll(
			() -> assertThat(expanded.getUrl()).isEqualTo("http://hl7.org/fhir/test/ValueSet/simple-all"),
			() -> assertThat(expanded.getVersion()).isEqualTo("5.0.0"),
			() -> assertThat(expanded.getName()).isEqualTo("SimpleValueSetAll"),
			() -> assertThat(expanded.getTitle()).isEqualTo("Simple ValueSet All"),
			() -> assertThat(expanded.getStatus()).isEqualTo(Enumerations.PublicationStatus.ACTIVE),
			() -> assertThat(expanded.getExperimental()).isEqualTo(false),
			() -> {
				if (expanded.getIdElement().hasValue()) {
					assertThat(expanded.getIdElement().getIdPart()).matches(myIdPattern);
				}
			},
			() -> {
				if (expanded.getDateElement().hasValue()) {
					assertThat(expanded.getDateElement().asStringValue()).isEqualTo("2023-04-01");
				}
			},
			() -> {
				if (expanded.getPublisherElement().hasValue()) {
					assertThat(expanded.getPublisher()).isEqualTo("FHIR Project");
				}
			},
			() -> {
				if (!expanded.getCompose().isEmpty()) {
					var expected = new ValueSet.ValueSetComposeComponent();
					expected.addInclude().setSystem("http://hl7.org/fhir/test/CodeSystem/simple");
					assertTrue(expanded.getCompose().equalsDeep(expected));
				}
			},
			() -> {
				if (expansion.getIdElement().hasValue()) {
					assertThat(expansion.getId()).matches(myIdPattern);
				}
			},
			() -> {
				if (expansion.getOffsetElement().hasValue()) {
					assertThat(expansion.getOffset()).isEqualTo(0);
				}
			},
			() -> assertThat(expansion.getIdentifier()).matches(myUuidPattern),
			() -> assertThat(expansion.getTotal()).isEqualTo(7),
			() -> assertThat(expansion.getTimestamp()).isNotNull(),
			() -> {
				var excludeNested = expansion.getParameter("excludeNested");
				assertThat(excludeNested).isNotNull();
				assertThat(excludeNested.getValueBooleanType()).isEqualTo(new BooleanType(true));
			},
			() -> {
				var usedCodeSystem = expansion.getParameter("used-codesystem");
				assertThat(usedCodeSystem).isNotNull();
				assertThat(usedCodeSystem.getValueUriType()).isEqualTo(new UriType("http://hl7.org/fhir/test/CodeSystem/simple|0.1.0"));
			},
			() -> {
				var displayLanguage = expansion.getParameter("displayLanguage");
				if (displayLanguage != null) {
					assertThat(displayLanguage.getValueStringType()).isEqualTo(new StringType("en"));
				}
			},
			() -> contains.forEach(contain -> assertThat(contain.getSystem()).isEqualTo("http://hl7.org/fhir/test/CodeSystem/simple")),
			() -> assertThat(getContainsEntryByCode(contains, "code1").getDisplay()).isEqualTo("Display 1"),
			() -> {
				var entry = getContainsEntryByCode(contains, "code2");
				assertThat(entry.getDisplay()).isEqualTo("Display 2");
				assertThat(entry.getAbstract()).isEqualTo(true);
				assertThat(entry.getInactive()).isEqualTo(true);
			},
			() -> assertThat(getContainsEntryByCode(contains, "code2a").getDisplay()).isEqualTo("Display 2a"),
			() -> assertThat(getContainsEntryByCode(contains, "code2aI").getDisplay()).isEqualTo("Display 2aI"),
			() -> assertThat(getContainsEntryByCode(contains, "code2aII").getDisplay()).isEqualTo("Display 2aII"),
			() -> assertThat(getContainsEntryByCode(contains, "code2b").getDisplay()).isEqualTo("Display 2b"),
			() -> assertThat(getContainsEntryByCode(contains, "code3").getDisplay()).isEqualTo("Display 3")
		);
	}

	@Test
	public void testExpandActive() throws Exception {
		loadAndPersistValueSet("active");
		loadAndPersistCodeSystem();

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-active"))
			.andParameter("excludeNested", new BooleanType(true))
			.execute();

		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandActiveValueSet(expanded);
	}

	@Test
	public void testExpandActiveTxResource() throws Exception {
		CodeSystem codeSystem = loadCodeSystem();
		ValueSet valueSet = loadValueSet("active");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-active"))
			.andParameter("excludeNested", new BooleanType(true))
			.andParameter("tx-resource", codeSystem)
			.andParameter("tx-resource", valueSet)
			.execute();

		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandActiveValueSet(expanded);
	}

	private void assertExpandActiveValueSet(ValueSet expanded) {
		var expansion = expanded.getExpansion();
		var contains = expansion.getContains();
		logAsJson(expanded);

		assertAll(
			() -> assertThat(expanded.getUrl()).isEqualTo("http://hl7.org/fhir/test/ValueSet/simple-active"),
			() -> assertThat(expanded.getVersion()).isEqualTo("5.0.0"),
			() -> assertThat(expanded.getName()).isEqualTo("SimpleValueSetActive"),
			() -> assertThat(expanded.getTitle()).isEqualTo("Simple ValueSet Active"),
			() -> assertThat(expanded.getStatus()).isEqualTo(Enumerations.PublicationStatus.ACTIVE),
			() -> assertThat(expanded.getExperimental()).isFalse(),
			() -> {
				if (expanded.getIdElement().hasValue()) {
					assertThat(expanded.getIdElement().getIdPart()).matches(myIdPattern);
				}
			},
			() -> {
				if (expanded.getDateElement().hasValue()) {
					assertThat(expanded.getDateElement().asStringValue()).isEqualTo("2023-04-01");
				}
			},
			() -> {
				if (expanded.getPublisherElement().hasValue()) {
					assertThat(expanded.getPublisher()).isEqualTo("FHIR Project");
				}
			},
			() -> {
				if (!expanded.getCompose().isEmpty()) {
					assertThat(expanded.getCompose().getInactive()).isEqualTo(false);
					var expectedInc = new ValueSet.ConceptSetComponent()
						.setSystem("http://hl7.org/fhir/test/CodeSystem/simple");
					assertTrue(
						expanded.getCompose().getInclude().get(0).equalsDeep(expectedInc)
					);
				}
			},
			() -> {
				if (expansion.getIdElement().hasValue()) {
					assertThat(expansion.getId()).matches(myIdPattern);
				}
			},
			() -> assertThat(expansion.getIdentifier()).matches(myUuidPattern),
			() -> assertThat(expansion.getTotal()).isEqualTo(6),
			() -> {
				if (expansion.getOffsetElement().hasValue()) {
					assertThat(expansion.getOffset()).isEqualTo(0);
				}
			},
			() -> assertThat(expansion.getTimestamp()).isNotNull(),
			() -> {
				var excludeNested = expansion.getParameter("excludeNested");
				assertThat(excludeNested).isNotNull();
				assertThat(excludeNested.getValueBooleanType()).isEqualTo(new BooleanType(true));
			},
			() -> {
				var usedCodeSystem = expansion.getParameter("used-codesystem");
				assertThat(usedCodeSystem).isNotNull();
				assertThat(usedCodeSystem.getValueUriType()).isEqualTo(new UriType("http://hl7.org/fhir/test/CodeSystem/simple|0.1.0"));
			},
			() -> contains.forEach(c ->
				assertThat(c.getSystem()).isEqualTo("http://hl7.org/fhir/test/CodeSystem/simple")
			),
			() -> assertThat(getContainsEntryByCode(contains, "code1").getDisplay()).isEqualTo("Display 1"),
			() -> assertThat(getContainsEntryByCode(contains, "code2a").getDisplay()).isEqualTo("Display 2a"),
			() -> assertThat(getContainsEntryByCode(contains, "code2aI").getDisplay()).isEqualTo("Display 2aI"),
			() -> assertThat(getContainsEntryByCode(contains, "code2aII").getDisplay()).isEqualTo("Display 2aII"),
			() -> assertThat(getContainsEntryByCode(contains, "code2b").getDisplay()).isEqualTo("Display 2b"),
			() -> assertThat(getContainsEntryByCode(contains, "code3").getDisplay()).isEqualTo("Display 3")
		);
	}

	@Test
	public void testExpandInactive() throws Exception {
		loadAndPersistValueSet("inactive");
		loadAndPersistCodeSystem();

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-inactive"))
			.andParameter("excludeNested", new BooleanType(true))
			.execute();

		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandInactiveValueSet(expanded);
	}

	@Test
	public void testExpandInactiveTxResource() throws Exception {
		CodeSystem codeSystem = loadCodeSystem();
		ValueSet valueSet = loadValueSet("inactive");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-inactive"))
			.andParameter("excludeNested", new BooleanType(true))
			.andParameter("tx-resource", codeSystem)
			.andParameter("tx-resource", valueSet)
			.execute();

		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandInactiveValueSet(expanded);
	}

	private void assertExpandInactiveValueSet(ValueSet expanded) {
		var expansion = expanded.getExpansion();
		var contains = expansion.getContains();
		logAsJson(expanded);

		assertAll(
			() -> assertThat(expanded.getUrl()).isEqualTo("http://hl7.org/fhir/test/ValueSet/simple-inactive"),
			() -> assertThat(expanded.getVersion()).isEqualTo("5.0.0"),
			() -> assertThat(expanded.getName()).isEqualTo("SimpleValueSetInactive"),
			() -> assertThat(expanded.getTitle()).isEqualTo("Simple ValueSet Inactive"),
			() -> assertThat(expanded.getStatus()).isEqualTo(Enumerations.PublicationStatus.ACTIVE),
			() -> assertThat(expanded.getExperimental()).isEqualTo(false),
			() -> {
				if (expanded.getIdElement().hasValue()) {
					assertThat(expanded.getIdElement().getIdPart()).matches(myIdPattern);
				}
			},
			() -> {
				if (expanded.getDateElement().hasValue()) {
					assertThat(expanded.getDateElement().asStringValue()).isEqualTo("2023-04-01");
				}
			},
			() -> {
				if (expanded.getPublisherElement().hasValue()) {
					assertThat(expanded.getPublisher()).isEqualTo("FHIR Project");
				}
			},
			() -> {
				if (!expanded.getCompose().isEmpty()) {
					var expected = new ValueSet.ValueSetComposeComponent();
					expected.setInactive(true).addInclude().setSystem("http://hl7.org/fhir/test/CodeSystem/simple");
					assertTrue(expanded.getCompose().equalsDeep(expected));
				}
			},
			() -> {
				if (expansion.getIdElement().hasValue()) {
					assertThat(expansion.getId()).matches(myIdPattern);
				}
			},
			() -> {
				if (expansion.getOffsetElement().hasValue()) {
					assertThat(expansion.getOffset()).isEqualTo(0);
				}
			},
			() -> assertThat(expansion.getIdentifier()).matches(myUuidPattern),
			() -> assertThat(expansion.getTotal()).isEqualTo(7),
			() -> assertThat(expansion.getTimestamp()).isNotNull(),
			() -> {
				var excludeNested = expansion.getParameter("excludeNested");
				assertThat(excludeNested).isNotNull();
				assertThat(excludeNested.getValueBooleanType()).isEqualTo(new BooleanType(true));
			},
			() -> {
				var usedCodeSystem = expansion.getParameter("used-codesystem");
				assertThat(usedCodeSystem).isNotNull();
				assertThat(usedCodeSystem.getValueUriType()).isEqualTo(new UriType("http://hl7.org/fhir/test/CodeSystem/simple|0.1.0"));
			},
			() -> contains.forEach(contain -> assertThat(contain.getSystem()).isEqualTo("http://hl7.org/fhir/test/CodeSystem/simple")),
			() -> assertThat(getContainsEntryByCode(contains, "code1").getDisplay()).isEqualTo("Display 1"),
			() -> {
				var entry = getContainsEntryByCode(contains, "code2");
				assertThat(entry.getDisplay()).isEqualTo("Display 2");
				assertThat(entry.getAbstract()).isEqualTo(true);
				assertThat(entry.getInactive()).isEqualTo(true);
			},
			() -> assertThat(getContainsEntryByCode(contains, "code2a").getDisplay()).isEqualTo("Display 2a"),
			() -> assertThat(getContainsEntryByCode(contains, "code2aI").getDisplay()).isEqualTo("Display 2aI"),
			() -> assertThat(getContainsEntryByCode(contains, "code2aII").getDisplay()).isEqualTo("Display 2aII"),
			() -> assertThat(getContainsEntryByCode(contains, "code2b").getDisplay()).isEqualTo("Display 2b"),
			() -> assertThat(getContainsEntryByCode(contains, "code3").getDisplay()).isEqualTo("Display 3")
		);
	}

	@Test
	public void testExpandEnumerated() throws Exception {
		loadAndPersistValueSet("enumerated");
		loadAndPersistCodeSystem();

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-enumerated"))
			.andParameter("excludeNested", new BooleanType(true))
			.execute();

		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandEnumeratedValueSet(expanded);
	}

	@Test
	public void testExpandEnumeratedTxResource() throws Exception {
		CodeSystem codeSystem = loadCodeSystem();
		ValueSet valueSet = loadValueSet("enumerated");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-enumerated"))
			.andParameter("excludeNested", new BooleanType(true))
			.andParameter("tx-resource", codeSystem)
			.andParameter("tx-resource", valueSet)
			.execute();

		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandEnumeratedValueSet(expanded);
	}

	private void assertExpandEnumeratedValueSet(ValueSet expanded) {
		var expansion = expanded.getExpansion();
		var contains = expansion.getContains();
		logAsJson(expanded);

		assertAll(
			() -> assertThat(expanded.getUrl()).isEqualTo("http://hl7.org/fhir/test/ValueSet/simple-enumerated"),
			() -> assertThat(expanded.getVersion()).isEqualTo("5.0.0"),
			() -> assertThat(expanded.getName()).isEqualTo("SimpleValueSetEnumerated"),
			() -> assertThat(expanded.getTitle()).isEqualTo("Simple ValueSet Enumerated"),
			() -> assertThat(expanded.getStatus()).isEqualTo(Enumerations.PublicationStatus.ACTIVE),
			() -> assertThat(expanded.getExperimental()).isEqualTo(false),
			() -> {
				if (expanded.getIdElement().hasValue()) {
					assertThat(expanded.getIdElement().getIdPart()).matches(myIdPattern);
				}
			},
			() -> {
				if (expanded.getDateElement().hasValue()) {
					assertThat(expanded.getDateElement().asStringValue()).isEqualTo("2023-04-01");
				}
			},
			() -> {
				if (expanded.getPublisherElement().hasValue()) {
					assertThat(expanded.getPublisher()).isEqualTo("FHIR Project");
				}
			},
			() -> {
				if (!expanded.getCompose().isEmpty()) {
					var expected = new ValueSet.ValueSetComposeComponent();
					var include = expected.addInclude().setSystem("http://hl7.org/fhir/test/CodeSystem/simple");
					include.addConcept().setCode("code1");
					include.addConcept().setCode("code2");
					include.addConcept().setCode("code3");
					include.addConcept().setCode("code2a");
					include.addConcept().setCode("code2b");
					assertTrue(expanded.getCompose().equalsDeep(expected));
				}
			},
			() -> {
				if (expansion.getIdElement().hasValue()) {
					assertThat(expansion.getId()).matches(myIdPattern);
				}
			},
			() -> {
				if (expansion.getOffsetElement().hasValue()) {
					assertThat(expansion.getOffset()).isEqualTo(0);
				}
			},
			() -> assertThat(expansion.getIdentifier()).matches(myUuidPattern),
			() -> assertThat(expansion.getTotal()).isEqualTo(5),
			() -> assertThat(expansion.getTimestamp()).isNotNull(),
			() -> {
				var excludeNested = expansion.getParameter("excludeNested");
				assertThat(excludeNested).isNotNull();
				assertThat(excludeNested.getValueBooleanType()).isEqualTo(new BooleanType(true));
			},
			() -> {
				var usedCodeSystem = expansion.getParameter("used-codesystem");
				assertThat(usedCodeSystem).isNotNull();
				assertThat(usedCodeSystem.getValueUriType()).isEqualTo(new UriType("http://hl7.org/fhir/test/CodeSystem/simple|0.1.0"));
			},
			() -> contains.forEach(contain -> assertThat(contain.getSystem()).isEqualTo("http://hl7.org/fhir/test/CodeSystem/simple")),
			() -> assertThat(getContainsEntryByCode(contains, "code1").getDisplay()).isEqualTo("Display 1"),
			() -> {
				var entry = getContainsEntryByCode(contains, "code2");
				assertThat(entry.getDisplay()).isEqualTo("Display 2");
				assertThat(entry.getAbstract()).isEqualTo(true);
				assertThat(entry.getInactive()).isEqualTo(true);
			},
			() -> assertThat(getContainsEntryByCode(contains, "code2a").getDisplay()).isEqualTo("Display 2a"),
			() -> assertThat(getContainsEntryByCode(contains, "code2b").getDisplay()).isEqualTo("Display 2b"),
			() -> assertThat(getContainsEntryByCode(contains, "code3").getDisplay()).isEqualTo("Display 3")
		);
	}

	@Test
	public void testExpandEnumeratedBad() throws Exception {
		loadAndPersistValueSet("enumerated-bad");
		loadAndPersistCodeSystem();

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url",
				new UrlType("http://hl7.org/fhir/test/ValueSet/simple-enumerated-bad"))
			.andParameter("excludeNested", new BooleanType(true))
			.execute();

		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandEnumeratedBadValueSet(expanded);
	}

	@Test
	public void testExpandEnumeratedBadTxResource() throws Exception {
		CodeSystem codeSystem = loadCodeSystem();
		ValueSet valueSet = loadValueSet("enumerated-bad");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url",
				new UrlType("http://hl7.org/fhir/test/ValueSet/simple-enumerated-bad"))
			.andParameter("excludeNested", new BooleanType(true))
			.andParameter("tx-resource", codeSystem)
			.andParameter("tx-resource", valueSet)
			.execute();

		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandEnumeratedBadValueSet(expanded);
	}

	private void assertExpandEnumeratedBadValueSet(ValueSet expanded) {
		var expansion = expanded.getExpansion();
		var contains = expansion.getContains();
		logAsJson(expanded);

		assertAll(
			() -> assertThat(expanded.getUrl())
				.isEqualTo("http://hl7.org/fhir/test/ValueSet/simple-enumerated-bad"),
			() -> assertThat(expanded.getVersion()).isEqualTo("5.0.0"),
			() -> assertThat(expanded.getName()).isEqualTo("SimpleValueSetEnumeratedBad"),
			() -> assertThat(expanded.getTitle()).isEqualTo("Simple ValueSet Enumerated Bad"),
			() -> assertThat(expanded.getStatus()).isEqualTo(Enumerations.PublicationStatus.ACTIVE),
			() -> assertThat(expanded.getExperimental()).isFalse(),
			() -> {
				if (expanded.getIdElement().hasValue()) {
					assertThat(expanded.getIdElement().getIdPart()).matches(myIdPattern);
				}
			},
			() -> {
				if (expanded.getDateElement().hasValue()) {
					assertThat(expanded.getDateElement().asStringValue()).isEqualTo("2023-04-01");
				}
			},
			() -> {
				if (expanded.getPublisherElement().hasValue()) {
					assertThat(expanded.getPublisher()).isEqualTo("FHIR Project");
				}
			},
			() -> {
				if (!expanded.getCompose().isEmpty()) {
					var expected = new ValueSet.ValueSetComposeComponent();
					var include = expected.addInclude().setSystem("http://hl7.org/fhir/test/CodeSystem/simple");
					include.addConcept().setCode("code1");
					include.addConcept().setCode("code2");
					include.addConcept().setCode("codeX");
					include.addConcept().setCode("code3");
					include.addConcept().setCode("code2a");
					include.addConcept().setCode("code2b");
					assertTrue(expanded.getCompose().equalsDeep(expected));
				}
			},
			() -> {
				if (expansion.getIdElement().hasValue()) {
					assertThat(expansion.getId()).matches(myIdPattern);
				}
			},
			() -> assertThat(expansion.getIdentifier()).matches(myUuidPattern),
			() -> assertThat(expansion.getTotal()).isEqualTo(5),
			() -> {
				if (expansion.getOffsetElement().hasValue()) {
					assertThat(expansion.getOffset()).isEqualTo(0);
				}
			},
			() -> assertThat(expansion.getTimestamp()).isNotNull(),
			() -> {
				var excludeNested = expansion.getParameter("excludeNested");
				assertThat(excludeNested).isNotNull();
				assertThat(excludeNested.getValueBooleanType()).isEqualTo(new BooleanType(true));
			},
			() -> {
				var usedCodeSystem = expansion.getParameter("used-codesystem");
				assertThat(usedCodeSystem).isNotNull();
				assertThat(usedCodeSystem.getValueUriType()).isEqualTo(new UriType("http://hl7.org/fhir/test/CodeSystem/simple|0.1.0"));
			},
			() -> contains.forEach(c ->
				assertThat(c.getSystem()).isEqualTo("http://hl7.org/fhir/test/CodeSystem/simple")
			),
			() -> assertThat(getContainsEntryByCode(contains, "code1").getDisplay()).isEqualTo("Display 1"),
			() -> {
				var entry = getContainsEntryByCode(contains, "code2");
				assertThat(entry.getDisplay()).isEqualTo("Display 2");
				assertThat(entry.getAbstract()).isEqualTo(true);
				assertThat(entry.getInactive()).isEqualTo(true);
			},
			() -> assertThat(getContainsEntryByCode(contains, "code2a").getDisplay()).isEqualTo("Display 2a"),
			() -> assertThat(getContainsEntryByCode(contains, "code2b").getDisplay()).isEqualTo("Display 2b"),
			() -> assertThat(getContainsEntryByCode(contains, "code3").getDisplay()).isEqualTo("Display 3")
		);
	}


	@Test
	public void testExpandIsA() throws Exception {
		loadAndPersistValueSet("filter-isa");
		loadAndPersistCodeSystem();

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-filter-isa"))
			.andParameter("excludeNested", new BooleanType(true))
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandIsAValueSet(expanded);
	}

	@Test
	public void testExpandIsATxResource() throws Exception {
		CodeSystem codeSystem = loadCodeSystem();
		ValueSet valueSet = loadValueSet("filter-isa");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-filter-isa"))
			.andParameter("excludeNested", new BooleanType(true))
			.andParameter("tx-resource", codeSystem)
			.andParameter("tx-resource", valueSet)
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandIsAValueSet(expanded);
	}

	private void assertExpandIsAValueSet(ValueSet expanded) {
		var expansion = expanded.getExpansion();
		var contains = expansion.getContains();
		logAsJson(expanded);
		assertAll(
			() -> assertThat(expanded.getUrl()).isEqualTo("http://hl7.org/fhir/test/ValueSet/simple-filter-isa"),
			() -> assertThat(expanded.getVersion()).isEqualTo("5.0.0"),
			() -> assertThat(expanded.getName()).isEqualTo("SimpleValueSetFilterIsA"),
			() -> assertThat(expanded.getTitle()).isEqualTo("Simple ValueSet Filter by Is-A"),
			() -> assertThat(expanded.getStatus()).isEqualTo(Enumerations.PublicationStatus.ACTIVE),
			() -> assertThat(expanded.getExperimental()).isEqualTo(false),
			() -> {
				if (expanded.getIdElement().hasValue()) {
					assertThat(expanded.getIdElement().getIdPart()).matches(myIdPattern);
				}
			},
			() -> {
				if (expanded.getDateElement().hasValue()) {
					assertThat(expanded.getDateElement().asStringValue()).isEqualTo("2023-04-01");
				}
			},
			() -> {
				if (expanded.getPublisherElement().hasValue()) {
					assertThat(expanded.getPublisher()).isEqualTo("FHIR Project");
				}
			},
			() -> {
				if (!expanded.getCompose().isEmpty()) {
					var expected = new ValueSet.ValueSetComposeComponent();
					expected.addInclude()
						.setSystem("http://hl7.org/fhir/test/CodeSystem/simple")
						.addFilter()
						.setProperty("concept")
						.setOp(ValueSet.FilterOperator.ISA)
						.setValue("code2");
					assertTrue(expanded.getCompose().equalsDeep(expected));
				}
			},
			() -> {
				if (expansion.getIdElement().hasValue()) {
					assertThat(expansion.getId()).matches(myIdPattern);
				}
			},
			() -> {
				if (expansion.getOffsetElement().hasValue()) {
					assertThat(expansion.getOffset()).isEqualTo(0);
				}
			},
			() -> assertThat(expansion.getIdentifier()).matches(myUuidPattern),
			() -> assertThat(expansion.getTotal()).isEqualTo(5),
			() -> assertThat(expansion.getTimestamp()).isNotNull(),
			() -> {
				var excludeNested = expansion.getParameter("excludeNested");
				assertThat(excludeNested).isNotNull();
				assertThat(excludeNested.getValueBooleanType()).isEqualTo(new BooleanType(true));
			},
			() -> {
				var usedCodeSystem = expansion.getParameter("used-codesystem");
				assertThat(usedCodeSystem).isNotNull();
				assertThat(usedCodeSystem.getValueUriType()).isEqualTo(new UriType("http://hl7.org/fhir/test/CodeSystem/simple|0.1.0"));
			},
			() -> contains.forEach(contain -> assertThat(contain.getSystem()).isEqualTo("http://hl7.org/fhir/test/CodeSystem/simple")),
			() -> {
				var entry = getContainsEntryByCode(contains, "code2");
				assertThat(entry.getDisplay()).isEqualTo("Display 2");
				assertThat(entry.getAbstract()).isEqualTo(true);
				assertThat(entry.getInactive()).isEqualTo(true);
			},
			() -> assertThat(getContainsEntryByCode(contains, "code2a").getDisplay()).isEqualTo("Display 2a"),
			() -> assertThat(getContainsEntryByCode(contains, "code2aI").getDisplay()).isEqualTo("Display 2aI"),
			() -> assertThat(getContainsEntryByCode(contains, "code2aII").getDisplay()).isEqualTo("Display 2aII"),
			() -> assertThat(getContainsEntryByCode(contains, "code2b").getDisplay()).isEqualTo("Display 2b")
		);
	}

	@Test
	public void testExpandFilterProperty() throws Exception {
		loadAndPersistValueSet("filter-property");
		loadAndPersistCodeSystem();

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-filter-property"))
			.andParameter("excludeNested", new BooleanType(true))
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandFilterPropertyValueSet(expanded);
	}

	@Test
	public void testExpandFilterPropertyTxResource() throws Exception {
		CodeSystem codeSystem = loadCodeSystem();
		ValueSet valueSet = loadValueSet("filter-property");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-filter-property"))
			.andParameter("excludeNested", new BooleanType(true))
			.andParameter("tx-resource", codeSystem)
			.andParameter("tx-resource", valueSet)
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandFilterPropertyValueSet(expanded);
	}

	private void assertExpandFilterPropertyValueSet(ValueSet expanded) {
		var expansion = expanded.getExpansion();
		var contains = expansion.getContains();
		logAsJson(expanded);
		assertAll(
			() -> assertThat(expanded.getUrl()).isEqualTo("http://hl7.org/fhir/test/ValueSet/simple-filter-property"),
			() -> assertThat(expanded.getVersion()).isEqualTo("5.0.0"),
			() -> assertThat(expanded.getName()).isEqualTo("SimpleValueSetFilterProperty"),
			() -> assertThat(expanded.getTitle()).isEqualTo("Simple ValueSet Filter by Property"),
			() -> assertThat(expanded.getStatus()).isEqualTo(Enumerations.PublicationStatus.ACTIVE),
			() -> assertThat(expanded.getExperimental()).isEqualTo(false),
			() -> {
				if (expanded.getIdElement().hasValue()) {
					assertThat(expanded.getIdElement().getIdPart()).matches(myIdPattern);
				}
			},
			() -> {
				if (expanded.getDateElement().hasValue()) {
					assertThat(expanded.getDateElement().asStringValue()).isEqualTo("2023-04-01");
				}
			},
			() -> {
				if (expanded.getPublisherElement().hasValue()) {
					assertThat(expanded.getPublisher()).isEqualTo("FHIR Project");
				}
			},
			() -> {
				if (!expanded.getCompose().isEmpty()) {
					var expected = new ValueSet.ValueSetComposeComponent();
					expected.addInclude()
						.setSystem("http://hl7.org/fhir/test/CodeSystem/simple")
						.addFilter()
						.setProperty("prop")
						.setOp(ValueSet.FilterOperator.EQUAL)
						.setValue("new");
					assertTrue(expanded.getCompose().equalsDeep(expected));
				}
			},
			() -> {
				if (expansion.getIdElement().hasValue()) {
					assertThat(expansion.getId()).matches(myIdPattern);
				}
			},
			() -> {
				if (expansion.getOffsetElement().hasValue()) {
					assertThat(expansion.getOffset()).isEqualTo(0);
				}
			},
			() -> assertThat(expansion.getIdentifier()).matches(myUuidPattern),
			() -> assertThat(expansion.getTotal()).isEqualTo(3),
			() -> assertThat(expansion.getTimestamp()).isNotNull(),
			() -> {
				var excludeNested = expansion.getParameter("excludeNested");
				assertThat(excludeNested).isNotNull();
				assertThat(excludeNested.getValueBooleanType()).isEqualTo(new BooleanType(true));
			},
			() -> {
				var usedCodeSystem = expansion.getParameter("used-codesystem");
				assertThat(usedCodeSystem).isNotNull();
				assertThat(usedCodeSystem.getValueUriType()).isEqualTo(new UriType("http://hl7.org/fhir/test/CodeSystem/simple|0.1.0"));
			},
			() -> contains.forEach(contain -> assertThat(contain.getSystem()).isEqualTo("http://hl7.org/fhir/test/CodeSystem/simple")),
			() -> {
				var entry = getContainsEntryByCode(contains, "code2");
				assertThat(entry.getDisplay()).isEqualTo("Display 2");
				assertThat(entry.getAbstract()).isEqualTo(true);
				assertThat(entry.getInactive()).isEqualTo(true);
			},
			() -> assertThat(getContainsEntryByCode(contains, "code2a").getDisplay()).isEqualTo("Display 2a"),
			() -> assertThat(getContainsEntryByCode(contains, "code2aII").getDisplay()).isEqualTo("Display 2aII")
		);
	}


	@Test
	public void testExpandRegex() throws Exception {
		loadAndPersistValueSet("filter-regex");
		loadAndPersistCodeSystem();

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-filter-regex"))
			.andParameter("excludeNested", new BooleanType(true))
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandRegexValueSet(expanded);
	}

	@Test
	public void testExpandRegexTxResource() throws Exception {
		CodeSystem codeSystem = loadCodeSystem();
		ValueSet valueSet = loadValueSet("filter-regex");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-filter-regex"))
			.andParameter("excludeNested", new BooleanType(true))
			.andParameter("tx-resource", codeSystem)
			.andParameter("tx-resource", valueSet)
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandRegexValueSet(expanded);
	}

	private void assertExpandRegexValueSet(ValueSet expanded) {
		var expansion = expanded.getExpansion();
		var contains = expansion.getContains();
		logAsJson(expanded);

		assertAll(
			() -> assertThat(expanded.getUrl()).isEqualTo("http://hl7.org/fhir/test/ValueSet/simple-filter-regex"),
			() -> assertThat(expanded.getVersion()).isEqualTo("5.0.0"),
			() -> assertThat(expanded.getName()).isEqualTo("SimpleValueSetFilterRegex"),
			() -> assertThat(expanded.getTitle()).isEqualTo("Simple ValueSet Filter by Regex"),
			() -> assertThat(expanded.getStatus()).isEqualTo(Enumerations.PublicationStatus.ACTIVE),
			() -> assertThat(expanded.getExperimental()).isEqualTo(false),
			() -> {
				if (expanded.getIdElement().hasValue()) {
					assertThat(expanded.getIdElement().getIdPart()).matches(myIdPattern);
				}
			},
			() -> {
				if (expanded.getDateElement().hasValue()) {
					assertThat(expanded.getDateElement().asStringValue()).isEqualTo("2023-04-01");
				}
			},
			() -> {
				if (expanded.getPublisherElement().hasValue()) {
					assertThat(expanded.getPublisher()).isEqualTo("FHIR Project");
				}
			},
			() -> {
				if (!expanded.getCompose().isEmpty()) {
					var expected = new ValueSet.ValueSetComposeComponent();
					expected.addInclude().setSystem("http://hl7.org/fhir/test/CodeSystem/simple")
						.addFilter()
						.setProperty("code")
						.setOp(ValueSet.FilterOperator.REGEX)
						.setValue("[^ \\t\\r\\n\\f]{4}[0-9]");
					assertTrue(expanded.getCompose().equalsDeep(expected));
				}
			},
			() -> {
				if (expansion.getIdElement().hasValue()) {
					assertThat(expansion.getId()).matches(myIdPattern);
				}
			},
			() -> {
				if (expansion.getOffsetElement().hasValue()) {
					assertThat(expansion.getOffset()).isEqualTo(0);
				}
			},
			() -> assertThat(expansion.getIdentifier()).matches(myUuidPattern),
			() -> assertThat(expansion.getTotal()).isEqualTo(3),
			() -> assertThat(expansion.getTimestamp()).isNotNull(),
			() -> {
				var excludeNested = expansion.getParameter("excludeNested");
				assertThat(excludeNested).isNotNull();
				assertThat(excludeNested.getValueBooleanType()).isEqualTo(new BooleanType(true));
			},
			() -> {
				var usedCodeSystem = expansion.getParameter("used-codesystem");
				assertThat(usedCodeSystem).isNotNull();
				assertThat(usedCodeSystem.getValueUriType()).isEqualTo(new UriType("http://hl7.org/fhir/test/CodeSystem/simple|0.1.0"));
			},
			() -> contains.forEach(contain -> assertThat(contain.getSystem()).isEqualTo("http://hl7.org/fhir/test/CodeSystem/simple")),
			() -> assertThat(getContainsEntryByCode(contains, "code1").getDisplay()).isEqualTo("Display 1"),
			() -> {
				var entry = getContainsEntryByCode(contains, "code2");
				assertThat(entry.getDisplay()).isEqualTo("Display 2");
				assertThat(entry.getAbstract()).isEqualTo(true);
				assertThat(entry.getInactive()).isEqualTo(true);
			},
			() -> assertThat(getContainsEntryByCode(contains, "code3").getDisplay()).isEqualTo("Display 3")
		);
	}

	@Test
	public void testExpandRegex2() throws Exception {
		loadAndPersistValueSet("filter-regex2");
		loadAndPersistCodeSystem();

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-filter-regex2"))
			.andParameter("excludeNested", new BooleanType(true))
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandRegex2ValueSet(expanded);
	}

	@Test
	public void testExpandRegex2TxResource() throws Exception {
		CodeSystem codeSystem = loadCodeSystem();
		ValueSet valueSet = loadValueSet("filter-regex2");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-filter-regex2"))
			.andParameter("excludeNested", new BooleanType(true))
			.andParameter("tx-resource", codeSystem)
			.andParameter("tx-resource", valueSet)
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandRegex2ValueSet(expanded);
	}

	private void assertExpandRegex2ValueSet(ValueSet expanded) {
		var expansion = expanded.getExpansion();
		var contains = expansion.getContains();
		logAsJson(expanded);
		assertAll(
			() -> assertThat(expanded.getUrl()).isEqualTo("http://hl7.org/fhir/test/ValueSet/simple-filter-regex2"),
			() -> assertThat(expanded.getVersion()).isEqualTo("5.0.0"),
			() -> assertThat(expanded.getName()).isEqualTo("SimpleValueSetFilterRegex2"),
			() -> assertThat(expanded.getTitle()).isEqualTo("Simple ValueSet Filter by Regex2 (test start/end)"),
			() -> assertThat(expanded.getStatus()).isEqualTo(Enumerations.PublicationStatus.ACTIVE),
			() -> assertThat(expanded.getExperimental()).isEqualTo(false),
			() -> {
				if (expanded.getIdElement().hasValue()) {
					assertThat(expanded.getIdElement().getIdPart()).matches(myIdPattern);
				}
			},
			() -> {
				if (expanded.getDateElement().hasValue()) {
					assertThat(expanded.getDateElement().asStringValue()).isEqualTo("2023-04-01");
				}
			},
			() -> {
				if (expanded.getPublisherElement().hasValue()) {
					assertThat(expanded.getPublisher()).isEqualTo("FHIR Project");
				}
			},
			() -> {
				if (!expanded.getCompose().isEmpty()) {
					var expected = new ValueSet.ValueSetComposeComponent();
					expected.addInclude()
						.setSystem("http://hl7.org/fhir/test/CodeSystem/simple")
						.addFilter()
						.setProperty("code")
						.setOp(ValueSet.FilterOperator.REGEX)
						.setValue("[^ \\t\\r\\n\\f]{5}");
					assertTrue(expanded.getCompose().equalsDeep(expected));
				}
			},
			() -> {
				if (expansion.getIdElement().hasValue()) {
					assertThat(expansion.getId()).matches(myIdPattern);
				}
			},
			() -> {
				if (expansion.getOffsetElement().hasValue()) {
					assertThat(expansion.getOffset()).isEqualTo(0);
				}
			},
			() -> assertThat(expansion.getIdentifier()).matches(myUuidPattern),
			() -> assertThat(expansion.getTotal()).isEqualTo(3),
			() -> assertThat(expansion.getTimestamp()).isNotNull(),
			() -> {
				var excludeNested = expansion.getParameter("excludeNested");
				assertThat(excludeNested).isNotNull();
				assertThat(excludeNested.getValueBooleanType()).isEqualTo(new BooleanType(true));
			},
			() -> {
				var usedCodeSystem = expansion.getParameter("used-codesystem");
				assertThat(usedCodeSystem).isNotNull();
				assertThat(usedCodeSystem.getValueUriType()).isEqualTo(new UriType("http://hl7.org/fhir/test/CodeSystem/simple|0.1.0"));
			},
			() -> contains.forEach(contain -> assertThat(contain.getSystem()).isEqualTo("http://hl7.org/fhir/test/CodeSystem/simple")),
			() -> assertThat(getContainsEntryByCode(contains, "code1").getDisplay()).isEqualTo("Display 1"),
			() -> {
				var entry = getContainsEntryByCode(contains, "code2");
				assertThat(entry.getDisplay()).isEqualTo("Display 2");
				assertThat(entry.getAbstract()).isEqualTo(true);
				assertThat(entry.getInactive()).isEqualTo(true);
			},
			() -> assertThat(getContainsEntryByCode(contains, "code3").getDisplay()).isEqualTo("Display 3")
		);
	}

	@Test
	public void testExpandRegexProp() throws Exception {
		loadAndPersistValueSet("filter-regex-prop");
		loadAndPersistCodeSystem();

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-filter-regex-prop"))
			.andParameter("excludeNested", new BooleanType(true))
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandRegexPropValueSet(expanded);
	}

	@Test
	public void testExpandRegexPropTxResource() throws Exception {
		CodeSystem codeSystem = loadCodeSystem();
		ValueSet valueSet = loadValueSet("filter-regex-prop");

		var respParam = myClient
			.operation()
			.onType("ValueSet")
			.named("expand")
			.withParameter(Parameters.class, "url", new UrlType("http://hl7.org/fhir/test/ValueSet/simple-filter-regex-prop"))
			.andParameter("excludeNested", new BooleanType(true))
			.andParameter("tx-resource", codeSystem)
			.andParameter("tx-resource", valueSet)
			.execute();
		var expanded = (ValueSet) respParam.getParameter().get(0).getResource();
		assertExpandRegexPropValueSet(expanded);
	}

	private void assertExpandRegexPropValueSet(ValueSet expanded) {
		var expansion = expanded.getExpansion();
		var contains = expansion.getContains();
		logAsJson(expanded);

		assertAll(
			() -> assertThat(expanded.getUrl()).isEqualTo("http://hl7.org/fhir/test/ValueSet/simple-filter-regex-prop"),
			() -> assertThat(expanded.getVersion()).isEqualTo("5.0.0"),
			() -> assertThat(expanded.getName()).isEqualTo("SimpleValueSetFilterRegexOnProp"),
			() -> assertThat(expanded.getTitle()).isEqualTo("Simple ValueSet Filter by Regex on a Property value"),
			() -> assertThat(expanded.getStatus()).isEqualTo(Enumerations.PublicationStatus.ACTIVE),
			() -> assertThat(expanded.getExperimental()).isEqualTo(false),
			() -> {
				if (expanded.getIdElement().hasValue()) {
					assertThat(expanded.getIdElement().getIdPart()).matches(myIdPattern);
				}
			},
			() -> {
				if (expanded.getDateElement().hasValue()) {
					assertThat(expanded.getDateElement().asStringValue()).isEqualTo("2023-04-01");
				}
			},
			() -> {
				if (expanded.getPublisherElement().hasValue()) {
					assertThat(expanded.getPublisher()).isEqualTo("FHIR Project");
				}
			},
			() -> {
				if (!expanded.getCompose().isEmpty()) {
					var expected = new ValueSet.ValueSetComposeComponent().addInclude();
					expected.setSystem("http://hl7.org/fhir/test/CodeSystem/simple")
						.addFilter()
						.setProperty("prop")
						.setOp(ValueSet.FilterOperator.REGEX)
						.setValue("o[a-z]*");
					assertTrue(expanded.getCompose().equalsDeep(new ValueSet.ValueSetComposeComponent().addInclude(expected)));
				}
			},
			() -> {
				if (expansion.getIdElement().hasValue()) {
					assertThat(expansion.getId()).matches(myIdPattern);
				}
			},
			() -> {
				if (expansion.getOffsetElement().hasValue()) {
					assertThat(expansion.getOffset()).isEqualTo(0);
				}
			},
			() -> assertThat(expansion.getIdentifier()).matches(myUuidPattern),
			() -> assertThat(expansion.getTotal()).isEqualTo(4),
			() -> assertThat(expansion.getTimestamp()).isNotNull(),
			() -> {
				var excludeNested = expansion.getParameter("excludeNested");
				assertThat(excludeNested).isNotNull();
				assertThat(excludeNested.getValueBooleanType()).isEqualTo(new BooleanType(true));
			},
			() -> {
				var usedCodeSystem = expansion.getParameter("used-codesystem");
				assertThat(usedCodeSystem).isNotNull();
				assertThat(usedCodeSystem.getValueUriType()).isEqualTo(new UriType("http://hl7.org/fhir/test/CodeSystem/simple|0.1.0"));
			},
			() -> contains.forEach(contain -> assertThat(contain.getSystem()).isEqualTo("http://hl7.org/fhir/test/CodeSystem/simple")),
			() -> assertThat(getContainsEntryByCode(contains, "code1").getDisplay()).isEqualTo("Display 1"),
			() -> assertThat(getContainsEntryByCode(contains, "code2aI").getDisplay()).isEqualTo("Display 2aI"),
			() -> assertThat(getContainsEntryByCode(contains, "code2b").getDisplay()).isEqualTo("Display 2b"),
			() -> assertThat(getContainsEntryByCode(contains, "code3").getDisplay()).isEqualTo("Display 3")
		);
	}

	private ValueSet.ValueSetExpansionContainsComponent getContainsEntryByCode(List<ValueSet.ValueSetExpansionContainsComponent> theContains, String theCode) {
		var containsEntry = theContains.stream().filter(contain -> contain.getCode().equals(theCode)).toList();
		assertThat(containsEntry.size()).isEqualTo(1);
		return containsEntry.get(0);
	}

	private void logAsJson(ValueSet expanded) {
		var resp = myFhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(expanded);
		ourLog.info(resp);
	}

	@AfterEach
	public void afterResetPreExpansionDefault() {
		myStorageSettings.setPreExpandValueSets(new JpaStorageSettings().isPreExpandValueSets());
	}
}
