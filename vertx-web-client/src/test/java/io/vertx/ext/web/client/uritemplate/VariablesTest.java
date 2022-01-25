package io.vertx.ext.web.client.uritemplate;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.template.Variables;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class VariablesTest {

  @Test
  public void testFromJson() {
    JsonObject json = new JsonObject();
    json.put("string", "the_string");
    json.put("int", 4);
    json.put("boolean", false);
    json.put("list", new JsonArray().add("foo").add(1).add(true));
    json.put("map", new JsonObject().put("map_string", "bar"));
    Variables var = Variables.variables(json);
    assertEquals(new HashSet<>(Arrays.asList("string", "int", "boolean", "list", "map")), var.names());
    assertEquals("the_string", var.get("string"));
    assertEquals("4", var.get("int"));
    assertEquals("false", var.get("boolean"));
    assertEquals(Arrays.asList("foo", "1", "true"), var.get("list"));
    assertEquals(Collections.singletonMap("map_string", "bar"), var.get("map"));
  }
}
