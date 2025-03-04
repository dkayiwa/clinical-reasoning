package org.opencds.cqf.cql.evaluator.library;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.cql.model.ModelIdentifier;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.engine.CqlEngineOptions;
import org.opencds.cqf.cql.evaluator.engine.retrieve.RetrieveSettings;

public class EvaluationSettings {

  private Map<ModelIdentifier, Model> modelCache;
  private Map<VersionedIdentifier, Library> libraryCache;

  private CqlOptions cqlOptions;

  private RetrieveSettings retrieveSettings;

  public static EvaluationSettings getDefault() {
    EvaluationSettings settings = new EvaluationSettings();

    var options = CqlOptions.defaultOptions();
    settings.setCqlOptions(options);
    settings.setModelCache(new ConcurrentHashMap<>());
    settings.setLibraryCache(new ConcurrentHashMap<>());
    settings.setRetrieveSettings(new RetrieveSettings());

    return settings;
  }

  public Map<ModelIdentifier, Model> getModelCache() {
    return this.modelCache;
  }

  public void setModelCache(Map<ModelIdentifier, Model> modelCache) {
    this.modelCache = modelCache;
  }

  public EvaluationSettings withModelCache(Map<ModelIdentifier, Model> modelCache) {
    setModelCache(modelCache);
    return this;
  }

  public Map<VersionedIdentifier, Library> getLibraryCache() {
    return this.libraryCache;
  }

  public void setLibraryCache(Map<VersionedIdentifier, Library> libraryCache) {
    this.libraryCache = libraryCache;
  }

  public EvaluationSettings withLibraryCache(Map<VersionedIdentifier, Library> libraryCache) {
    setLibraryCache(libraryCache);
    return this;
  }

  public CqlOptions getCqlOptions() {
    return this.cqlOptions;
  }

  /**
   * @deprecated Left in for backwards compatibility. getCqlOptions().getCqlEngineOptions() should
   *             be used instead.
   */
  @Deprecated
  public CqlEngineOptions getEngineOptions() {
    return this.cqlOptions.getCqlEngineOptions();
  }

  public EvaluationSettings withCqlOptions(CqlOptions cqlOptions) {
    setCqlOptions(cqlOptions);
    return this;
  }

  public void setCqlOptions(CqlOptions cqlOptions) {
    this.cqlOptions = cqlOptions;
  }

  public RetrieveSettings getRetrieveSettings() {
    return this.retrieveSettings;
  }

  public EvaluationSettings withRetrieveSettings(RetrieveSettings retrieveSettings) {
    setRetrieveSettings(retrieveSettings);
    return this;
  }

  public void setRetrieveSettings(RetrieveSettings retrieveSettings) {
    this.retrieveSettings = retrieveSettings;
  }
}
