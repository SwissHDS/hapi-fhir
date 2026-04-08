HAPI FHIR Fork for the Swiss Terminology Provider
=========

[![License][Badge-License]][Link-License]

This Fork of the HAPI FHIR project adds functionality required for the Swiss Terminology Provider. 
It extends operations on ValueSets, CodeSystems and Concepts maps to match the FHIR Standard.

| **Resource**                  | **Operation**          | **Example**                                                                                                              |
| ------------------------- | ------------------ | --------------------------------------------------------------------------------------------------------------------------- |
| **ValueSet**              | **$expand**        | Preview the full set of codes (with filters), check multilingual displays, paging, and active-only flags before publishing. |
| **ValueSet / CodeSystem** | **$validate-code** | Prove that a code (system+code+display) is valid and (if ValueSet) **in** the set. Usefull for CI or Publishing             |
| **CodeSystem**            | **$lookup**        | Inspect a code’s official display, properties (e.g., `inactive`) and designations in each language.                         |
| **ConceptMap**            | **$translate**     | Validate and preview mappings (source→target), including equivalence and dependency parameters.                             |

The goal of this fork is to match the majority of the requirements defined in the simple-cases tests defined in 
[FHIR Terminology Ecosystem IG](https://build.fhir.org/ig/HL7/fhir-tx-ecosystem-ig/testcases.html). 

A collection of tests is available to validate the implementation when compiled locally together with 
the [Swiss HDS Terminology Provider](https://github.com/SwissHDS/swiss-hds-terminolgy-provider) starter project. 
These tests are inspired by the NHS®. Source: https://digital.nhs.uk/services/terminology-server.

You may get the collection for Postman or Bruno here:
- [Postman Collection](https://github.com/SwissHDS/swiss-hds-terminolgy-provider/wiki/postmancollection_v0.9.json)
- [Bruno Collection](https://github.com/SwissHDS/swiss-hds-terminolgy-provider/wiki/brunocollection_v0.9.json)

## Documentation and wiki

For the documentation of this fork, please see [ Wiki](https://github.com/SwissHDS/swiss-hds-terminology-provider/wiki). 

For the documentation of the original HAPI FHIR project see http://hapifhir.io.

This project is Open Source, licensed under the Apache Software License 2.0.

