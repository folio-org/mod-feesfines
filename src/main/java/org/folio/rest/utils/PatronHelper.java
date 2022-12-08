package org.folio.rest.utils;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;

public class PatronHelper {
  private static final Logger log = LogManager.getLogger(PatronHelper.class);
  private static final String FULL_USER_NAME_TEMPLATE = "%s, %s";

  private PatronHelper() {
  }

  public static String buildFormattedName(User user) {
    StringBuilder builder = new StringBuilder();
    Personal personal = user.getPersonal();

    if (personal == null) {
      log.info("Personal info not found - user {}", user.getId());
    } else {
      builder.append(personal.getLastName());

      String firstName = personal.getFirstName();
      if (firstName != null && !firstName.isBlank()) {
        builder.append(format(", %s", firstName));
      }

      String middleName = personal.getMiddleName();
      if (middleName != null && !middleName.isBlank()) {
        builder.append(format(" %s", middleName));
      }
    }

    return builder.toString();
  }

  public static String getUserName(User user) {
    Personal personal = user.getPersonal();

    return personal != null && isNoneBlank(personal.getLastName(), personal.getFirstName())
      ? String.format(FULL_USER_NAME_TEMPLATE, personal.getLastName(), personal.getFirstName())
      : user.getUsername();
  }

  public static String getEmail(User user) {
    Personal personal = user.getPersonal();
    if (personal == null) {
      log.info("Personal info not found - user {}", user.getId());
      return "";
    } else {
      return user.getPersonal().getEmail();
    }
  }
}
