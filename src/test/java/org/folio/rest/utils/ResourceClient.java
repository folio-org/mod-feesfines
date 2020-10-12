package org.folio.rest.utils;

import static org.folio.test.support.ApiTests.getOkapiUrl;

import org.folio.HttpStatus;

import io.restassured.response.Response;

public class ResourceClient {
  private final String baseUri;
  private final OkapiClient okapiClient;

  public ResourceClient(String baseUri) {
    this.okapiClient = new OkapiClient(getOkapiUrl());
    this.baseUri = baseUri;
  }

  public Response create(Object body) {
    return attemptCreate(body)
      .then()
      .statusCode(HttpStatus.HTTP_CREATED.toInt())
      .extract()
      .response();
  }

  public Response attemptCreate(Object body) {
    return okapiClient.post(baseUri, body);
  }

  public Response post(Object body) {
    return attemptCreate(body);
  }

  public Response update(String id, Object body) {
    return attemptUpdate(id, body)
      .then()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt())
      .extract()
      .response();
  }

  public Response attemptUpdate(String id, Object body) {
    return okapiClient.put(baseUri + "/" + id, body);
  }

  public Response getById(String id) {
    return okapiClient.get(baseUri + "/" + id)
      .then()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .extract()
      .response();
  }

  public Response getAll() {
    return okapiClient.get(baseUri + "?limit=1000")
      .then()
      .statusCode(HttpStatus.HTTP_OK.toInt())
      .extract()
      .response();
  }

  public Response delete(String accountId) {
    return okapiClient.delete(baseUri + "/" + accountId)
      .then()
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt())
      .extract()
      .response();
  }
}
