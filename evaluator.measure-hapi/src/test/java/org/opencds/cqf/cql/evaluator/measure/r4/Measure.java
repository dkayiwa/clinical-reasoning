package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.fhir.test.TestRepositoryFactory;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureConstants;
import org.opencds.cqf.cql.evaluator.measure.r4.Measure.SelectedGroup.SelectedReference;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.testng.TestException;

import ca.uhn.fhir.context.FhirContext;

public class Measure {

  @FunctionalInterface
  interface Validator<T> {
    void validate(T value);
  }

  @FunctionalInterface
  interface Selector<T, S> {
    T select(S from);
  }

  interface ChildOf<T> {
    T up();
  }

  interface SelectedOf<T> {
    T value();
  }

  protected static class Selected<T, P> implements SelectedOf<T>, ChildOf<P> {
    private final P parent;
    private final T value;

    public Selected(T value, P parent) {
      this.parent = parent;
      this.value = value;
    }

    @Override
    public T value() {
      return value;
    }

    @Override
    public P up() {
      return parent;
    }
  }

  public static Given given() {
    return new Given();
  }

  public static class Given {
    private Repository repository;

    public Given repository(Repository repository) {
      this.repository = repository;
      return this;
    }

    public Given repositoryFor(String repositoryPath) {
      this.repository = TestRepositoryFactory.createRepository(FhirContext.forR4Cached(),
          this.getClass(), repositoryPath);
      return this;
    }

    private static R4MeasureProcessor buildProcessor(Repository repository) {
      return new R4MeasureProcessor(repository, null, new R4RepositorySubjectProvider(repository));
    }

    public When when() {
      return new When(buildProcessor(this.repository));
    }
  }

  public static class When {
    private final R4MeasureProcessor processor;

    When(R4MeasureProcessor processor) {
      this.processor = processor;
    }

    private String measureId;

    private String periodStart;
    private String periodEnd;

    private List<String> subjectIds;
    private String reportType;

    private Bundle additionalData;

    private Supplier<MeasureReport> operation;

    public When measureId(String measureId) {
      this.measureId = measureId;
      return this;
    }

    public When periodEnd(String periodEnd) {
      this.periodEnd = periodEnd;
      return this;
    }

    public When periodStart(String periodStart) {
      this.periodStart = periodStart;
      return this;
    }

    public When subject(String subjectId) {
      this.subjectIds = Collections.singletonList(subjectId);
      return this;
    }

    public When subjects(List<String> subjectIds) {
      this.subjectIds = subjectIds;
      return this;
    }

    public When reportType(String reportType) {
      this.reportType = reportType;
      return this;
    }

    public When additionalData(Bundle additionalData) {
      this.additionalData = additionalData;
      return this;
    }

    public When evaluate() {
      this.operation =
          () -> processor.evaluateMeasure(
              Eithers.forMiddle3(new IdType("Measure", measureId)),
              periodStart, periodEnd, reportType, subjectIds, additionalData);
      return this;
    }

    public SelectedReport then() {
      if (this.operation == null) {
        throw new IllegalStateException(
            "No operation was selected as part of 'when'. Choose an operation to invoke by adding one, such as 'evaluate' to the method chain.");
      }

      MeasureReport report = null;
      try {
        report = this.operation.get();
      } catch (Exception e) {
        throw new TestException("error when running 'then' and invoking the chosen operation", e);
      }

      return new SelectedReport(report);
    }
  }
  public static class SelectedReport extends Selected<MeasureReport, Void> {
    public SelectedReport(MeasureReport report) {
      super(report, null);
    }

    public SelectedReport passes(Validator<MeasureReport> measureReportValidator) {
      measureReportValidator.validate(value());
      return this;
    }

    public MeasureReport report() {
      return this.value();
    }

    public SelectedGroup firstGroup() {
      return this.group(MeasureReport::getGroupFirstRep);
    }

    public SelectedGroup group(String id) {
      return this
          .group(x -> x.getGroup().stream().filter(g -> g.getId().equals(id)).findFirst().get());
    }

    public SelectedGroup group(Selector<MeasureReportGroupComponent, MeasureReport> groupSelector) {
      var g = groupSelector.select(value());
      return new SelectedGroup(g, this);
    }

    public SelectedReference<SelectedReport> reference(
        Selector<Reference, MeasureReport> referenceSelector) {
      var r = referenceSelector.select(value());
      return new SelectedReference<>(r, this);
    }

    public SelectedReference<SelectedReport> evaluatedResource(String name) {
      return this.reference(x -> x.getEvaluatedResource().stream()
          .filter(y -> y.getReference().equals(name)).findFirst().get());
    }

    public SelectedReport hasEvaluatedResourceCount(int count) {
      assertEquals(count, report().getEvaluatedResource().size());
      return this;
    }

    public SelectedReport hasContainedResourceCount(int count) {
      assertEquals(count, report().getContained().size());
      return this;
    }

    // TODO: SelectedContained resource class?
    public SelectedReport hasContainedResource(Predicate<Resource> criteria) {
      var contained = this.report().getContained().stream();
      assertTrue(contained.anyMatch(criteria), "Did not find a resource matching this criteria ");
      return this;
    }

    // TODO: SelectedExtension class?
    public SelectedReport hasExtension(String url, int count) {
      var ex = this.value().getExtensionsByUrl(url);
      assertEquals(ex.size(), count);

      return this;
    }
  }

  static class SelectedGroup
      extends Selected<MeasureReport.MeasureReportGroupComponent, SelectedReport> {

    public SelectedGroup(MeasureReportGroupComponent value, SelectedReport parent) {
      super(value, parent);
    }

    public SelectedGroup hasScore(String score) {
      MeasureValidationUtils.validateGroupScore(this.value(), score);
      return this;
    }

    public SelectedPopulation population(String name) {
      return this.population(
          g -> g.getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding()
              && x.getCode().getCoding().get(0).getCode().equals(name)).findFirst().get());
    }

    public SelectedPopulation population(
        Selector<MeasureReportGroupPopulationComponent, MeasureReportGroupComponent> populationSelector) {
      var p = populationSelector.select(value());
      return new SelectedPopulation(p, this);
    }

    public SelectedPopulation firstPopulation() {
      return this.population(MeasureReport.MeasureReportGroupComponent::getPopulationFirstRep);
    }

    public SelectedGroup hasStratifierCount(int count) {
      assertEquals(this.value().getStratifier().size(), count);
      return this;
    }

    public SelectedStratifier firstStratifier() {
      return this.stratifier(MeasureReport.MeasureReportGroupComponent::getStratifierFirstRep);
    }

    public SelectedStratifier stratifier(
        Selector<MeasureReportGroupStratifierComponent, MeasureReportGroupComponent> stratifierSelector) {
      var s = stratifierSelector.select(value());
      return new SelectedStratifier(s, this);
    }

    static class SelectedReference<P> extends Selected<Reference, P> {

      public SelectedReference(Reference value, P parent) {
        super(value, parent);
      }

      // Hmm.. may need to rethink this one a bit.
      public SelectedReference<P> hasPopulations(String... population) {
        var ex = this.value().getExtensionsByUrl(MeasureConstants.EXT_CRITERIA_REFERENCE_URL);
        if (ex.isEmpty()) {
          throw new TestException(String.format(
              "no evaluated resource extensions were found, and expected %s", population.length));
        }

        @SuppressWarnings("unchecked")
        var set = ex.stream().map(x -> ((IPrimitiveType<String>) x.getValue()).getValue())
            .collect(Collectors.toSet());

        for (var p : population) {
          assertTrue(set.contains(p),
              String.format(
                  "population: %s was not found in the evaluated resources criteria reference extension list",
                  p));
        }

        return this;
      }
    }

    static class SelectedPopulation
        extends Selected<MeasureReport.MeasureReportGroupPopulationComponent, SelectedGroup> {

      public SelectedPopulation(MeasureReportGroupPopulationComponent value, SelectedGroup parent) {
        super(value, parent);
      }

      public SelectedPopulation hasCount(int count) {
        MeasureValidationUtils.validatePopulation(value(), count);
        return this;
      }

      public SelectedPopulation passes(
          Validator<MeasureReport.MeasureReportGroupPopulationComponent> populationValidator) {
        populationValidator.validate(value());
        return this;
      }
    }
  }

  static class SelectedStratifier
      extends Selected<MeasureReport.MeasureReportGroupStratifierComponent, SelectedGroup> {

    public SelectedStratifier(MeasureReportGroupStratifierComponent value, SelectedGroup parent) {
      super(value, parent);
    }

    public SelectedStratum firstStratum() {
      return stratum(MeasureReport.MeasureReportGroupStratifierComponent::getStratumFirstRep);
    }

    public SelectedStratum stratum(CodeableConcept value) {
      return stratum(
          s -> s.getStratum().stream().filter(x -> x.hasValue() && x.getValue().equalsDeep(value))
              .findFirst().get());
    }

    public SelectedStratum stratum(String textValue) {
      return stratum(
          s -> s.getStratum().stream().filter(x -> x.hasValue() && x.getValue().hasText())
              .filter(x -> x.getValue().getText().equals(textValue))
              .findFirst().get());
    }

    public SelectedStratum stratum(
        Selector<MeasureReport.StratifierGroupComponent, MeasureReport.MeasureReportGroupStratifierComponent> stratumSelector) {
      var s = stratumSelector.select(value());
      return new SelectedStratum(s, this);
    }
  }
  static class SelectedStratum
      extends Selected<MeasureReport.StratifierGroupComponent, SelectedStratifier> {

    public SelectedStratum(MeasureReport.StratifierGroupComponent value,
        SelectedStratifier parent) {
      super(value, parent);
    }

    public SelectedStratum hasScore(String score) {
      MeasureValidationUtils.validateStratumScore(value(), score);
      return this;
    }

    public SelectedStratumPopulation firstPopulation() {
      return population(MeasureReport.StratifierGroupComponent::getPopulationFirstRep);
    }

    public SelectedStratumPopulation population(String name) {
      return population(
          s -> s.getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding()
              && x.getCode().getCoding().get(0).getCode().equals(name)).findFirst().get());
    }

    public SelectedStratumPopulation population(
        Selector<MeasureReport.StratifierGroupPopulationComponent, MeasureReport.StratifierGroupComponent> populationSelector) {
      var p = populationSelector.select(value());
      return new SelectedStratumPopulation(p, this);
    }
  }

  static class SelectedStratumPopulation
      extends Selected<MeasureReport.StratifierGroupPopulationComponent, SelectedStratum> {

    public SelectedStratumPopulation(MeasureReport.StratifierGroupPopulationComponent value,
        SelectedStratum parent) {
      super(value, parent);
    }

    public SelectedStratumPopulation hasCount(int count) {
      assertEquals(this.value().getCount(), count);
      return this;
    }
  }
}
