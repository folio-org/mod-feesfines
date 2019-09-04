package org.folio.rest.utils;

import static org.folio.rest.utils.FeeFineActionCommentsParser.parseFeeFineComments;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

class FeeFineActionCommentsParserTest {

  @Test
  void canParseSeveralComments() {
    Map<String, String> parsedComments =
      parseFeeFineComments("STAFF : staff comment \n PATRON : patron comment");

    assertThat(parsedComments.size(), is(2));

    assertThat(parsedComments, allOf(
      hasEntry("STAFF", "staff comment"),
      hasEntry("PATRON", "patron comment")));
  }

  @Test
  void canParseSingleComment() {
    Map<String, String> parsedComments =
      parseFeeFineComments("STAFF : staff comment");

    assertThat(parsedComments.size(), is(1));

    assertThat(parsedComments, hasEntry("STAFF", "staff comment"));
  }

  @Test
  void canParseEmptyString() {
    Map<String, String> parsedComments =
      parseFeeFineComments(StringUtils.EMPTY);

    assertThat(parsedComments.size(), is(0));
  }

  @Test
  void canHandleDuplicateKeys() {
    Map<String, String> parsedComments =
      parseFeeFineComments("STAFF : staff comment \n STAFF : second staff comment");

    assertThat(parsedComments.size(), is(1));

    assertThat(parsedComments, hasEntry("STAFF", "staff comment"));
  }

  @Test
  void canHandleInvalidFormattingFormatting() {
    Map<String, String> parsedComments =
      parseFeeFineComments("STAFF : staff comment \n PATRON:patron comment");

    assertThat(parsedComments.size(), is(1));

    assertThat(parsedComments, hasEntry("STAFF", "staff comment"));
  }
}
