package org.opencds.cqf.cql.evaluator.library;

import org.cqframework.cql.elm.execution.VersionedIdentifier;

public class VersionedIdentifiers {

  private VersionedIdentifiers() {
    // empty
  }


  public static VersionedIdentifier forUrl(String url) {
    if (!url.contains("/Library/")) {
      throw new IllegalArgumentException(
          "Invalid resource type for determining library version identifier: Library");
    }
    String[] urlSplit = url.split("/Library/");
    if (urlSplit.length != 2) {
      throw new IllegalArgumentException(
          "Invalid url, Library.url SHALL be <CQL namespace url>/Library/<CQL library name>");
    }

    String cqlName = urlSplit[1];
    VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
    if (cqlName.contains("|")) {
      String[] nameVersion = cqlName.split("\\|");
      String name = nameVersion[0];
      String version = nameVersion[1];
      versionedIdentifier.setId(name);
      versionedIdentifier.setVersion(version);
    } else {
      versionedIdentifier.setId(cqlName);
    }

    return versionedIdentifier;
  }

}
