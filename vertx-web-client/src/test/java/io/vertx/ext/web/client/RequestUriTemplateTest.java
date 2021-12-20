package io.vertx.ext.web.client;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.template.UriTemplate;
import org.junit.Test;

public class RequestUriTemplateTest extends WebClientTestBase {

  @Test
  public void test() throws Exception {
    testRequest(client -> client.request(HttpMethod.GET, UriTemplate.of("/{action}?username={username}"))
      .setTemplateParam("action", "info")
      .setTemplateParam("username", "vietj"), req -> {
      assertEquals("/info", req.path());
      assertEquals("vietj", req.getParam("username"));
    });
  }

}
