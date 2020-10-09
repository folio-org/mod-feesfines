package org.folio.rest.domain;

import java.util.List;

public interface BulkActionRequest extends ActionRequestBak {
    List<String> getAccountIds();
}
