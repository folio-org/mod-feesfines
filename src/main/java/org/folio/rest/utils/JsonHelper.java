package org.folio.rest.utils;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.StdConverter;
import io.vertx.core.json.JsonObject;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public class JsonHelper {

  private JsonHelper() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  public static void write(JsonObject target, String property, String value) {
    if (value != null) {
      target.put(property, value);
    }
  }

  public static void write(JsonObject target, String property, JsonObject value) {
    if (value != null) {
      target.put(property, value);
    }
  }

  public static void write(JsonObject target, String property, Double value) {
    if (value != null) {
      target.put(property, value);
    }
  }

  public static void write(JsonObject to, String propertyName, DateTime value) {
    if (value != null) {
      write(to, propertyName, value.toString(ISODateTimeFormat.dateTime()));
    }
  }

  public static void write(JsonObject to, String propertyName, Boolean value) {
    if (value != null) {
      to.put(propertyName, value);
    }
  }

  public static void writeIfNotBlank(JsonObject target, String key, String value) {
    if (argumentsAreValid(target, key, value)) {
      target.put(key, value);
    }
  }

  public static void writeIfDoesNotExist(JsonObject target, String key, String value) {
    if (!target.containsKey(key)) {
      writeIfNotBlank(target, key, value);
    }
  }

  private static boolean argumentsAreValid(JsonObject target, String key, String value) {
    return target != null && isNoneBlank(key, value);
  }

  public static class MonetaryValueSerializer extends StdSerializer<BigDecimal> {

    protected MonetaryValueSerializer(Class<BigDecimal> t) {
      super(t);
    }

    public MonetaryValueSerializer() {
      this(null);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeNumber(value.setScale(2, RoundingMode.HALF_UP));
    }
  }

  public static class MonetaryValueDeserializer extends StdDeserializer<BigDecimal> {

    protected MonetaryValueDeserializer(Class<?> vc) {
      super(vc);
    }

    public MonetaryValueDeserializer() {
      this(null);
    }


    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      JsonNode node = p.getCodec().readTree(p);
      return new BigDecimal(node.get("amount2").toString());
    }
  }


}
