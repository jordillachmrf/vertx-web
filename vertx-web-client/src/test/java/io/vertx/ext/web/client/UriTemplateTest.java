package io.vertx.ext.web.client;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.client.impl.UriTemplateImpl;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class UriTemplateTest {

  @Test
  public void testParseURITemplate() {
    new UriTemplateImpl.Parser().parseURITemplate("http://example.org/~{username}/");
  }

  @Test
  public void testParseExpression() {
    assertEquals(0, new UriTemplateImpl.Parser().parseExpression("{", 0));
    assertEquals(2, new UriTemplateImpl.Parser().parseExpression("{}", 0));
    assertEquals(3, new UriTemplateImpl.Parser().parseExpression("{A}", 0));
    assertEquals(5, new UriTemplateImpl.Parser().parseExpression("{A,B}", 0));
    assertEquals(0, new UriTemplateImpl.Parser().parseExpression("{A,}", 0));
    assertEquals(4, new UriTemplateImpl.Parser().parseExpression("{+A}", 0));
  }

  @Test
  public void testVariableList() {
    assertEquals(1, new UriTemplateImpl.Parser().parseVariableList("A", 0));
    assertEquals(2, new UriTemplateImpl.Parser().parseVariableList("AB", 0));
    assertEquals(4, new UriTemplateImpl.Parser().parseVariableList("AB,C", 0));
    assertEquals(4, new UriTemplateImpl.Parser().parseVariableList("AB,C,", 0));
    assertEquals(6, new UriTemplateImpl.Parser().parseVariableList("AB,C,D", 0));
    assertEquals(6, new UriTemplateImpl.Parser().parseVariableList("AB,C,D}", 0));
    assertEquals(8, new UriTemplateImpl.Parser().parseVariableList("AB,C,D:1.", 0));
  }

  @Test
  public void testParseVarspec() {
    assertEquals(3, new UriTemplateImpl.Parser().parseVarspec("A:1", 0));
    assertEquals(4, new UriTemplateImpl.Parser().parseVarspec("AB:1", 0));
    assertEquals(5, new UriTemplateImpl.Parser().parseVarspec("A.B:1", 0));
    assertEquals(4, new UriTemplateImpl.Parser().parseVarspec("AB:1}", 0));
    assertEquals(3, new UriTemplateImpl.Parser().parseVarspec("A:1.", 0));
  }

  @Test
  public void testParseVarchar() {
    assertEquals(1, UriTemplateImpl.Parser.parseVarname("A", 0));
    assertEquals(2, UriTemplateImpl.Parser.parseVarname("AB", 0));
    assertEquals(3, UriTemplateImpl.Parser.parseVarname("A.B", 0));
    assertEquals(2, UriTemplateImpl.Parser.parseVarname("AB}", 0));
    assertEquals(1, UriTemplateImpl.Parser.parseVarname("A.", 0));
  }

  @Test
  public void testModifierLevel4() {
    assertEquals(0, UriTemplateImpl.Parser.parseModifierLevel4(":0", 0));
    assertEquals(2, UriTemplateImpl.Parser.parseModifierLevel4(":1", 0));
    assertEquals(3, UriTemplateImpl.Parser.parseModifierLevel4(":12", 0));
    assertEquals(4, UriTemplateImpl.Parser.parseModifierLevel4(":123", 0));
    assertEquals(5, UriTemplateImpl.Parser.parseModifierLevel4(":1234", 0));
    assertEquals(5, UriTemplateImpl.Parser.parseModifierLevel4(":12345", 0));
    assertEquals(1, UriTemplateImpl.Parser.parseModifierLevel4("*", 0));
  }

  @Test
  public void testParsePrefix() {
    assertEquals(0, UriTemplateImpl.Parser.parsePrefix(":0", 0));
    assertEquals(2, UriTemplateImpl.Parser.parsePrefix(":1", 0));
    assertEquals(3, UriTemplateImpl.Parser.parsePrefix(":12", 0));
    assertEquals(4, UriTemplateImpl.Parser.parsePrefix(":123", 0));
    assertEquals(5, UriTemplateImpl.Parser.parsePrefix(":1234", 0));
    assertEquals(5, UriTemplateImpl.Parser.parsePrefix(":12345", 0));
  }

  @Test
  public void testParseMaxLength() {
    assertEquals(0, UriTemplateImpl.Parser.parseMaxLength("0", 0));
    assertEquals(1, UriTemplateImpl.Parser.parseMaxLength("1", 0));
    assertEquals(2, UriTemplateImpl.Parser.parseMaxLength("12", 0));
    assertEquals(3, UriTemplateImpl.Parser.parseMaxLength("123", 0));
    assertEquals(4, UriTemplateImpl.Parser.parseMaxLength("1234", 0));
    assertEquals(4, UriTemplateImpl.Parser.parseMaxLength("12345", 0));
  }

  @Test
  public void testExpandLiteral() {
    assertEquals("%E2%82%AC%C3%AE%E2%82%AC", UriTemplate.of("€î%E2%82%AC").expand(MultiMap.caseInsensitiveMultiMap()));
  }

  @Test
  public void testExpandSimpleString() {
    MultiMap variables = MultiMap.caseInsensitiveMultiMap();
    variables.set("var1", "val1");
    variables.set("var2", "val2");
    variables.set("var3", "val3");
    variables.set("euro", "€");
    variables.set("slash", "/");
    variables.set("percent", "%E2%82%AC");
    assertEquals("prefixsuffix", UriTemplate.of("prefix{var}suffix").expand(variables));
    assertEquals("prefixval1suffix", UriTemplate.of("prefix{var1}suffix").expand(variables));
    assertEquals("prefixval1,val2suffix", UriTemplate.of("prefix{var1,var2}suffix").expand(variables));
    assertEquals("prefixval1suffix", UriTemplate.of("prefix{var1,var}suffix").expand(variables));
    assertEquals("prefixval2suffix", UriTemplate.of("prefix{var,var2}suffix").expand(variables));
    assertEquals("%E2%82%AC", UriTemplate.of("{euro}").expand(variables));
    assertEquals("%2F", UriTemplate.of("{slash}").expand(variables));
    assertEquals("%25E2%2582%25AC", UriTemplate.of("{percent}").expand(variables));
  }
}
