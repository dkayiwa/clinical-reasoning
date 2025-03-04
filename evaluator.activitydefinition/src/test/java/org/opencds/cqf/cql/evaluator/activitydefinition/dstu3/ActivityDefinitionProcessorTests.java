package org.opencds.cqf.cql.evaluator.activitydefinition.dstu3;

import java.util.List;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.evaluator.fhir.repository.InMemoryFhirRepository;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Repositories;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;

public class ActivityDefinitionProcessorTests {
  private Repository repository;
  private ActivityDefinitionProcessor activityDefinitionProcessor;
  private static final FhirContext fhirContext = FhirContext.forDstu3Cached();
  private static final EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();

  @BeforeClass
  public void setup() {
    var data = new InMemoryFhirRepository(fhirContext, this.getClass(), List.of("tests"), false);
    var content =
        new InMemoryFhirRepository(fhirContext, this.getClass(), List.of("resources/"), false);
    var terminology = new InMemoryFhirRepository(fhirContext, this.getClass(),
        List.of("vocabulary/CodeSystem/", "vocabulary/ValueSet/"), false);

    repository = Repositories.proxy(data, content, terminology);
    activityDefinitionProcessor =
        new ActivityDefinitionProcessor(repository, evaluationSettings);
  }

  @Test
  public void testActivityDefinitionApply() throws FHIRException {
    var libraryEngine = new LibraryEngine(repository, evaluationSettings);

    var result = this.activityDefinitionProcessor.apply(
        new IdType("ActivityDefinition", "activityDefinition-test"), null,
        null, "patient-1", null, null, null, null, null, null, null, null, null, libraryEngine);
    Assert.assertTrue(result instanceof ProcedureRequest);
    var request = (ProcedureRequest) result;
    Assert.assertTrue(request.getDoNotPerform());
  }

}
