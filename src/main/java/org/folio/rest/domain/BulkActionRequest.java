package org.folio.rest.domain;

import java.util.List;

public interface BulkActionRequest extends ActionRequest {
    List<String> getAccountIds();
}
