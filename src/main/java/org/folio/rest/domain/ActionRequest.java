package org.folio.rest.domain;

public interface ActionRequest {
    String getComments();

    Boolean getNotifyPatron();

    String getServicePointId();

    String getUserName();
}
