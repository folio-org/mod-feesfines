package org.folio.rest.domain;

public interface ActionRequest {
    String getComments();

    String getTransactionInfo();

    Boolean getNotifyPatron();

    String getServicePointId();

    String getUserName();

    String getPaymentMethod();
}
