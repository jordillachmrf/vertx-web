package io.vertx.ext.web.client.uritemplate;

import io.vertx.ext.web.client.template.UriTemplate;
import io.vertx.ext.web.client.template.Variables;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExpansionTest {

  private Variables variables;

  @Before
  public void setUp() throws Exception {
    variables = Variables.variables();
    variables.set("var1", "val1");
    variables.set("var2", "val2");
    variables.set("var3", "val3");
    variables.set("euro", "\u20AC");
    variables.set("slash", "/");
    variables.set("comma", ",");
    variables.set("empty", "");
    variables.set("empty_list", Collections.emptyList());
    variables.set("percent", "%E2%82%AC");
    variables.set("list", Arrays.asList("one", "two", "three"));
    Map<String, String> map = new LinkedHashMap<>();
    map.put("one", "1");
    map.put("two", "2");
    map.put("three", "3");
    map.put("comma", ",");
    variables.set("map", map);
    variables.set("empty_map", Collections.emptyMap());
    variables.set("%2F", "/");
    variables.set("%2F_list", Arrays.asList("/", "/", "/"));
    variables.set("foo.bar", "foodotbar");
  }

  @Test
  public void testSimpleStringExpansion() {
    assertEquals("prefixsuffix", UriTemplate.of("prefix{undef}suffix").expand(variables));
    assertEquals("prefixsuffix", UriTemplate.of("prefix{empty}suffix").expand(variables));
    assertEquals("prefixsuffix", UriTemplate.of("prefix{empty}suffix").expand(variables));
    assertEquals("prefixval1suffix", UriTemplate.of("prefix{var1}suffix").expand(variables));
    assertEquals("prefixval1,val2suffix", UriTemplate.of("prefix{var1,var2}suffix").expand(variables));
    assertEquals("prefixval1suffix", UriTemplate.of("prefix{var1,undef}suffix").expand(variables));
    assertEquals("prefixval1,suffix", UriTemplate.of("prefix{var1,empty}suffix").expand(variables));
    assertEquals("prefixval2suffix", UriTemplate.of("prefix{undef,var2}suffix").expand(variables));
    assertEquals("prefix,val2suffix", UriTemplate.of("prefix{empty,var2}suffix").expand(variables));
    assertEquals("va", UriTemplate.of("{var1:2}").expand(variables));
    assertEquals("%E2%82%AC", UriTemplate.of("{euro}").expand(variables));
    assertEquals("%2F", UriTemplate.of("{slash}").expand(variables));
    assertEquals("%2C", UriTemplate.of("{comma}").expand(variables));
    assertEquals("%25E2%2582%25AC", UriTemplate.of("{percent}").expand(variables));
    assertEquals("one,two,three", UriTemplate.of("{list}").expand(variables));
    assertEquals("one,two,three", UriTemplate.of("{list*}").expand(variables));
    assertEquals("", UriTemplate.of("{empty_list}").expand(variables));
    assertEquals("", UriTemplate.of("{empty_list*}").expand(variables));
    assertEquals("one,1,two,2,three,3,comma,%2C", UriTemplate.of("{map}").expand(variables));
    assertEquals("one=1,two=2,three=3,comma=%2C", UriTemplate.of("{map*}").expand(variables));
    assertEquals("", UriTemplate.of("{empty_map}").expand(variables));
    assertEquals("", UriTemplate.of("{empty_map*}").expand(variables));
    assertExpansionFailure("{list:1}");
    assertExpansionFailure("{map:1}");
  }

  @Test
  public void testFormStyleQueryExpansion() {
    assertEquals("prefixsuffix", UriTemplate.of("prefix{?undef}suffix").expand(variables));
    assertEquals("prefix?empty=suffix", UriTemplate.of("prefix{?empty}suffix").expand(variables));
    assertEquals("prefix?var1=val1suffix", UriTemplate.of("prefix{?var1}suffix").expand(variables));
    assertEquals("prefix?var1=val1&var2=val2suffix", UriTemplate.of("prefix{?var1,var2}suffix").expand(variables));
    assertEquals("prefix?var1=val1suffix", UriTemplate.of("prefix{?var1,undef}suffix").expand(variables));
    assertEquals("prefix?var1=val1&empty=suffix", UriTemplate.of("prefix{?var1,empty}suffix").expand(variables));
    assertEquals("prefix?var2=val2suffix", UriTemplate.of("prefix{?undef,var2}suffix").expand(variables));
    assertEquals("prefix?empty=&var2=val2suffix", UriTemplate.of("prefix{?empty,var2}suffix").expand(variables));
    assertEquals("?foo.bar=foodotbar", UriTemplate.of("{?foo.bar}").expand(variables));
    assertEquals("?var1=va", UriTemplate.of("{?var1:2}").expand(variables));
    assertEquals("?euro=%E2%82%AC", UriTemplate.of("{?euro}").expand(variables));
    assertEquals("?slash=%2F", UriTemplate.of("{?slash}").expand(variables));
    assertEquals("?%2F=%2F", UriTemplate.of("{?%2F}").expand(variables));
    assertEquals("?comma=%2C", UriTemplate.of("{?comma}").expand(variables));
    assertEquals("?percent=%25E2%2582%25AC", UriTemplate.of("{?percent}").expand(variables));
    assertEquals("?list=one,two,three", UriTemplate.of("{?list}").expand(variables));
    assertEquals("?list=one&list=two&list=three", UriTemplate.of("{?list*}").expand(variables));
    assertEquals("", UriTemplate.of("{?empty_list}").expand(variables));
    assertEquals("", UriTemplate.of("{?empty_list*}").expand(variables));
    assertEquals("?%2F_list=%2F,%2F,%2F", UriTemplate.of("{?%2F_list}").expand(variables));
    assertEquals("?%2F_list=%2F&%2F_list=%2F&%2F_list=%2F", UriTemplate.of("{?%2F_list*}").expand(variables));
    assertEquals("?map=one,1,two,2,three,3,comma,%2C", UriTemplate.of("{?map}").expand(variables));
    assertEquals("?one=1&two=2&three=3&comma=%2C", UriTemplate.of("{?map*}").expand(variables));
    assertEquals("", UriTemplate.of("{?empty_map}").expand(variables));
    assertEquals("", UriTemplate.of("{?empty_map*}").expand(variables));
  }

  @Test
  public void testFormStyleQueryContinuation() {
    assertEquals("prefixsuffix", UriTemplate.of("prefix{&undef}suffix").expand(variables));
    assertEquals("prefix&empty=suffix", UriTemplate.of("prefix{&empty}suffix").expand(variables));
    assertEquals("prefix&var1=val1suffix", UriTemplate.of("prefix{&var1}suffix").expand(variables));
    assertEquals("prefix&var1=val1&var2=val2suffix", UriTemplate.of("prefix{&var1,var2}suffix").expand(variables));
    assertEquals("prefix&var1=val1suffix", UriTemplate.of("prefix{&var1,undef}suffix").expand(variables));
    assertEquals("prefix&var1=val1&empty=suffix", UriTemplate.of("prefix{&var1,empty}suffix").expand(variables));
    assertEquals("prefix&var2=val2suffix", UriTemplate.of("prefix{&undef,var2}suffix").expand(variables));
    assertEquals("prefix&empty=&var2=val2suffix", UriTemplate.of("prefix{&empty,var2}suffix").expand(variables));
    assertEquals("&var1=va", UriTemplate.of("{&var1:2}").expand(variables));
    assertEquals("&euro=%E2%82%AC", UriTemplate.of("{&euro}").expand(variables));
    assertEquals("&slash=%2F", UriTemplate.of("{&slash}").expand(variables));
    assertEquals("&%2F=%2F", UriTemplate.of("{&%2F}").expand(variables));
    assertEquals("&comma=%2C", UriTemplate.of("{&comma}").expand(variables));
    assertEquals("&percent=%25E2%2582%25AC", UriTemplate.of("{&percent}").expand(variables));
    assertEquals("&list=one,two,three", UriTemplate.of("{&list}").expand(variables));
    assertEquals("&list=one&list=two&list=three", UriTemplate.of("{&list*}").expand(variables));
    assertEquals("", UriTemplate.of("{&empty_list}").expand(variables));
    assertEquals("", UriTemplate.of("{&empty_list*}").expand(variables));
    assertEquals("&%2F_list=%2F,%2F,%2F", UriTemplate.of("{&%2F_list}").expand(variables));
    assertEquals("&%2F_list=%2F&%2F_list=%2F&%2F_list=%2F", UriTemplate.of("{&%2F_list*}").expand(variables));
    assertEquals("&map=one,1,two,2,three,3,comma,%2C", UriTemplate.of("{&map}").expand(variables));
    assertEquals("&one=1&two=2&three=3&comma=%2C", UriTemplate.of("{&map*}").expand(variables));
    assertEquals("", UriTemplate.of("{&empty_map}").expand(variables));
    assertEquals("", UriTemplate.of("{&empty_map*}").expand(variables));
  }

  @Test
  public void testPathSegmentExpansion() {
    assertEquals("prefixsuffix", UriTemplate.of("prefix{/undef}suffix").expand(variables));
    assertEquals("prefix/suffix", UriTemplate.of("prefix{/empty}suffix").expand(variables));
    assertEquals("prefix/val1suffix", UriTemplate.of("prefix{/var1}suffix").expand(variables));
    assertEquals("prefix/val1/val2suffix", UriTemplate.of("prefix{/var1,var2}suffix").expand(variables));
    assertEquals("prefix/val1suffix", UriTemplate.of("prefix{/var1,undef}suffix").expand(variables));
    assertEquals("prefix/val1/suffix", UriTemplate.of("prefix{/var1,empty}suffix").expand(variables));
    assertEquals("prefix/val2suffix", UriTemplate.of("prefix{/undef,var2}suffix").expand(variables));
    assertEquals("prefix//val2suffix", UriTemplate.of("prefix{/empty,var2}suffix").expand(variables));
    assertEquals("/va", UriTemplate.of("{/var1:2}").expand(variables));
    assertEquals("/%E2%82%AC", UriTemplate.of("{/euro}").expand(variables));
    assertEquals("/%2F", UriTemplate.of("{/slash}").expand(variables));
    assertEquals("/%2C", UriTemplate.of("{/comma}").expand(variables));
    assertEquals("/%25E2%2582%25AC", UriTemplate.of("{/percent}").expand(variables));
    assertEquals("/one,two,three", UriTemplate.of("{/list}").expand(variables));
    assertEquals("/one/two/three", UriTemplate.of("{/list*}").expand(variables));
    assertEquals("", UriTemplate.of("{/empty_list}").expand(variables));
    assertEquals("", UriTemplate.of("{/empty_list*}").expand(variables));
    assertEquals("/one,1,two,2,three,3,comma,%2C", UriTemplate.of("{/map}").expand(variables));
    assertEquals("/one=1/two=2/three=3/comma=%2C", UriTemplate.of("{/map*}").expand(variables));
    assertEquals("", UriTemplate.of("{/empty_map}").expand(variables));
    assertEquals("", UriTemplate.of("{/empty_map*}").expand(variables));
  }

  @Test
  public void testPathStyleParameterExpansion() {
    assertEquals("prefixsuffix", UriTemplate.of("prefix{;undef}suffix").expand(variables));
    assertEquals("prefix;emptysuffix", UriTemplate.of("prefix{;empty}suffix").expand(variables));
    assertEquals("prefix;var1=val1suffix", UriTemplate.of("prefix{;var1}suffix").expand(variables));
    assertEquals("prefix;var1=val1;var2=val2suffix", UriTemplate.of("prefix{;var1,var2}suffix").expand(variables));
    assertEquals("prefix;var1=val1suffix", UriTemplate.of("prefix{;var1,undef}suffix").expand(variables));
    assertEquals("prefix;var1=val1;emptysuffix", UriTemplate.of("prefix{;var1,empty}suffix").expand(variables));
    assertEquals("prefix;var2=val2suffix", UriTemplate.of("prefix{;undef,var2}suffix").expand(variables));
    assertEquals("prefix;empty;var2=val2suffix", UriTemplate.of("prefix{;empty,var2}suffix").expand(variables));
    assertEquals(";var1=va", UriTemplate.of("{;var1:2}").expand(variables));
    assertEquals(";euro=%E2%82%AC", UriTemplate.of("{;euro}").expand(variables));
    assertEquals(";slash=%2F", UriTemplate.of("{;slash}").expand(variables));
    assertEquals(";%2F=%2F", UriTemplate.of("{;%2F}").expand(variables));
    assertEquals(";comma=%2C", UriTemplate.of("{;comma}").expand(variables));
    assertEquals(";percent=%25E2%2582%25AC", UriTemplate.of("{;percent}").expand(variables));
    assertEquals(";list=one,two,three", UriTemplate.of("{;list}").expand(variables));
    assertEquals(";list=one;list=two;list=three", UriTemplate.of("{;list*}").expand(variables));
    assertEquals("", UriTemplate.of("{;empty_list}").expand(variables));
    assertEquals("", UriTemplate.of("{;empty_list*}").expand(variables));
    assertEquals(";%2F_list=%2F,%2F,%2F", UriTemplate.of("{;%2F_list}").expand(variables));
    assertEquals(";%2F_list=%2F;%2F_list=%2F;%2F_list=%2F", UriTemplate.of("{;%2F_list*}").expand(variables));
    assertEquals(";map=one,1,two,2,three,3,comma,%2C", UriTemplate.of("{;map}").expand(variables));
    assertEquals(";one=1;two=2;three=3;comma=%2C", UriTemplate.of("{;map*}").expand(variables));
    assertEquals("", UriTemplate.of("{;empty_map}").expand(variables));
    assertEquals("", UriTemplate.of("{;empty_map*}").expand(variables));
  }

  @Test
  public void testReservedExpansion() {
    assertEquals("prefixsuffix", UriTemplate.of("prefix{+undef}suffix").expand(variables));
    assertEquals("prefixsuffix", UriTemplate.of("prefix{+empty}suffix").expand(variables));
    assertEquals("prefixval1suffix", UriTemplate.of("prefix{+var1}suffix").expand(variables));
    assertEquals("prefixval1,val2suffix", UriTemplate.of("prefix{+var1,var2}suffix").expand(variables));
    assertEquals("prefixval1suffix", UriTemplate.of("prefix{+var1,undef}suffix").expand(variables));
    assertEquals("prefixval1,suffix", UriTemplate.of("prefix{+var1,empty}suffix").expand(variables));
    assertEquals("prefixval2suffix", UriTemplate.of("prefix{+undef,var2}suffix").expand(variables));
    assertEquals("prefix,val2suffix", UriTemplate.of("prefix{+empty,var2}suffix").expand(variables));
    assertEquals("va", UriTemplate.of("{+var1:2}").expand(variables));
    assertEquals("%E2%82%AC", UriTemplate.of("{+euro}").expand(variables));
    assertEquals("/", UriTemplate.of("{+slash}").expand(variables));
    assertEquals(",", UriTemplate.of("{+comma}").expand(variables));
    assertEquals("%E2%82%AC", UriTemplate.of("{+percent}").expand(variables));
    assertEquals("one,two,three", UriTemplate.of("{+list}").expand(variables));
    assertEquals("one,two,three", UriTemplate.of("{+list*}").expand(variables));
    assertEquals("", UriTemplate.of("{+empty_list}").expand(variables));
    assertEquals("", UriTemplate.of("{+empty_list*}").expand(variables));
    assertEquals("one,1,two,2,three,3,comma,,", UriTemplate.of("{+map}").expand(variables));
    assertEquals("one=1,two=2,three=3,comma=,", UriTemplate.of("{+map*}").expand(variables));
    assertEquals("", UriTemplate.of("{+empty_map}").expand(variables));
    assertEquals("", UriTemplate.of("{+empty_map*}").expand(variables));
  }

  @Test
  public void testFragmentExpansion() {
    assertEquals("prefixsuffix", UriTemplate.of("prefix{#undef}suffix").expand(variables));
    assertEquals("prefix#val1suffix", UriTemplate.of("prefix{#var1}suffix").expand(variables));
    assertEquals("prefix#val1,val2suffix", UriTemplate.of("prefix{#var1,var2}suffix").expand(variables));
    assertEquals("prefix#val1suffix", UriTemplate.of("prefix{#var1,undef}suffix").expand(variables));
    assertEquals("prefix#val2suffix", UriTemplate.of("prefix{#undef,var2}suffix").expand(variables));
    assertEquals("#va", UriTemplate.of("{#var1:2}").expand(variables));
    assertEquals("#%E2%82%AC", UriTemplate.of("{#euro}").expand(variables));
    assertEquals("#/", UriTemplate.of("{#slash}").expand(variables));
    assertEquals("#,", UriTemplate.of("{#comma}").expand(variables));
    assertEquals("#%E2%82%AC", UriTemplate.of("{#percent}").expand(variables));
    assertEquals("#one,two,three", UriTemplate.of("{#list}").expand(variables));
    assertEquals("#one,two,three", UriTemplate.of("{#list*}").expand(variables));
    assertEquals("", UriTemplate.of("{#empty_list}").expand(variables));
    assertEquals("", UriTemplate.of("{#empty_list*}").expand(variables));
    assertEquals("#one,1,two,2,three,3,comma,,", UriTemplate.of("{#map}").expand(variables));
    assertEquals("#one=1,two=2,three=3,comma=,", UriTemplate.of("{#map*}").expand(variables));
    assertEquals("", UriTemplate.of("{#empty_map}").expand(variables));
    assertEquals("", UriTemplate.of("{#empty_map*}").expand(variables));
  }

  @Test
  public void testLabelExpansionWithDotPrefix() {
    assertEquals("prefixsuffix", UriTemplate.of("prefix{.undef}suffix").expand(variables));
    assertEquals("prefix.val1suffix", UriTemplate.of("prefix{.var1}suffix").expand(variables));
    assertEquals("prefix.val1.val2suffix", UriTemplate.of("prefix{.var1,var2}suffix").expand(variables));
    assertEquals("prefix.val1suffix", UriTemplate.of("prefix{.var1,undef}suffix").expand(variables));
    assertEquals("prefix.val2suffix", UriTemplate.of("prefix{.undef,var2}suffix").expand(variables));
    assertEquals(".va", UriTemplate.of("{.var1:2}").expand(variables));
    assertEquals(".%E2%82%AC", UriTemplate.of("{.euro}").expand(variables));
    assertEquals(".%2F", UriTemplate.of("{.slash}").expand(variables));
    assertEquals(".%2C", UriTemplate.of("{.comma}").expand(variables));
    assertEquals(".%25E2%2582%25AC", UriTemplate.of("{.percent}").expand(variables));
    assertEquals(".one,two,three", UriTemplate.of("{.list}").expand(variables));
    assertEquals(".one.two.three", UriTemplate.of("{.list*}").expand(variables));
    assertEquals("", UriTemplate.of("{.empty_list}").expand(variables));
    assertEquals("", UriTemplate.of("{.empty_list*}").expand(variables));
    assertEquals(".one,1,two,2,three,3,comma,%2C", UriTemplate.of("{.map}").expand(variables));
    assertEquals(".one=1.two=2.three=3.comma=%2C", UriTemplate.of("{.map*}").expand(variables));
    assertEquals("", UriTemplate.of("{.empty_map}").expand(variables));
    assertEquals("", UriTemplate.of("{empty_map*}").expand(variables));
  }

  private void assertExpansionFailure(String stemplate) {
    UriTemplate template = UriTemplate.of(stemplate);
    try {
      template.expand(variables);
      fail();
    } catch (Exception ignore) {
      // Expected
    }
  }
}
