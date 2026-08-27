package org.folio.test.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import org.folio.rest.tools.utils.NetworkUtils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public final class OkapiDeployment extends WireMockServer {
  private static final int OKAPI_PORT = NetworkUtils.nextFreePort();

  public OkapiDeployment() {
    super(new WireMockConfiguration().dynamicPort());
  }

  public String getOkapiUrl() {
    return baseUrl();
  }

  String getVerticleUrl() {
    return "http://localhost:" + getVerticlePort();
  }

  int getVerticlePort() {
    return OKAPI_PORT;
  }

  void setUpMapping() {
    resetAll();

    // forward everything to okapi
    stubFor(any(anyUrl())
      .atPriority(Integer.MAX_VALUE)
      .willReturn(aResponse().proxiedFrom(getVerticleUrl())));
  }
}
