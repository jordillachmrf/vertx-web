package io.vertx.ext.web.client.uritemplate;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.template.UriTemplate;
import io.vertx.ext.web.client.template.Variables;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

public class TckTest {

  private static JsonObject load(String name) throws IOException {
    InputStream is = TckTest.class.getClassLoader().getResourceAsStream(name);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[256];
    int l;
    while ((l = is.read(buffer, 0, 256)) != -1) {
      baos.write(buffer, 0, l);
    }
    return new JsonObject(Buffer.buffer(baos.toByteArray()));
  }

  private static Variables loadVariables(JsonObject json) {
    Variables variables = Variables.variables();
    json.forEach(entry -> {
      Object value = entry.getValue();
      String name = entry.getKey();
      if (value == null) {
        variables.set(name, (String)null);
      } else if (value instanceof String) {
        variables.set(name, (String) value);
      } else if (value instanceof JsonArray) {
        variables.set(name, ((JsonArray) value).stream().map(o -> (String)o).collect(Collectors.toList()));
      } else if (value instanceof JsonObject) {
        variables.set(name, ((JsonObject) value).stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (String)e.getValue())));
      } else if (value instanceof Number) {
        variables.set(name, value.toString());
      } else {
        throw new AssertionError("Unsupported variable " + value.getClass());
      }
    });
    return variables;
  }

  @Test
  public void testSpecExamples() throws IOException {
    runTCK("uritemplate-test/spec-examples.json");
  }

  @Test
  public void testSpecExamplesBySection() throws IOException {
    runTCK("uritemplate-test/spec-examples-by-section.json");
  }

  @Test
  public void testExtendedTest() throws IOException {
    runTCK("uritemplate-test/extended-tests.json");
  }

  @Test
  public void testNegativeTest() throws IOException {
    runTCK("uritemplate-test/negative-tests.json");
  }

  private void runTCK(String name) throws IOException {
    JsonObject groups = load(name);
    for (String desc : groups.fieldNames()) {
      JsonObject group = groups.getJsonObject(desc);
      String level = group.getString("level");
      Variables variables = loadVariables(group.getJsonObject("variables"));
      JsonArray testcases = group.getJsonArray("testcases");
      testcases.forEach(testcase -> {
        JsonArray array = (JsonArray) testcase;
        String template = array.getString(0);
        Object expected = array.getValue(1);
        List<String> expectations;
        if (expected instanceof String) {
          expectations = Collections.singletonList((String) expected);
        } else if (expected instanceof JsonArray) {
          expectations = ((JsonArray) expected).stream().map(o -> (String) o).collect(Collectors.toList());
        } else if (expected == Boolean.FALSE) {
          // Failure
          try {
            UriTemplate.of(template).expand(variables);
            fail("Was expecting " + template + " compilation or evaluation to fail");
          } catch (Exception ignore) {
          }
          return;
        } else {
          throw new UnsupportedOperationException("Not supported: " + expected);
        }
        String result;
        try {
          result = UriTemplate.of(template).expand(variables);
        } catch (Exception e) {
          throw new AssertionError("Failed to evaluate " + template + " with variables " + group.getJsonObject("variables") + " to evaluate to " + expected, e);
        }
        if (!expectations.contains(result)) {
          fail("Expected " + template + " evaluated with variables " + group.getJsonObject("variables") + " to <" + result + "> to match one of " + expectations);
        }
      });
    }
  }
}
