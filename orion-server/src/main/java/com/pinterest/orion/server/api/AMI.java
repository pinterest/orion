package com.pinterest.orion.server.api;

public class AMI {
  private final String amiId;
  private final String applicationEnvironment;
  private final String creationDate;

  public AMI(String amiId, String applicationEnvironment, String creationDate) {
    this.amiId = amiId;
    this.applicationEnvironment = applicationEnvironment;
    this.creationDate = creationDate;
  }

  public String getAmiId() {
    return amiId;
  }

  public String getApplicationEnvironment() {
    return applicationEnvironment;
  }

  public String getCreationDate() {
    return creationDate;
  }
}
