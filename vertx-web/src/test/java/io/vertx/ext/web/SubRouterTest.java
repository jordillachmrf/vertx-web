/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.web;

import io.vertx.core.http.HttpMethod;
import org.junit.Test;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class SubRouterTest extends WebTestBase {

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidMountPoint1() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath*", subRouter);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidMountPoint2() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath/*", subRouter);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidMountPoint3() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("subpath", subRouter);
  }

  @Test
  public void testSimple() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath", subRouter);

    subRouter.route("/foo").handler(rc -> {
      assertEquals("/subpath", rc.mountPoint());
      rc.response().setStatusMessage(rc.request().path()).end();
    });

    testRequest(HttpMethod.GET, "/subpath/foo", 200, "/subpath/foo");

    testRequest(HttpMethod.GET, "/subpath/", 404, "Not Found");
    testRequest(HttpMethod.GET, "/subpath/bar", 404, "Not Found");
    testRequest(HttpMethod.GET, "/otherpath", 404, "Not Found");
  }

  @Test
  public void testTrailingSlash() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath/", subRouter);

    subRouter.route("/foo").handler(rc -> {
      assertEquals("/subpath/", rc.mountPoint());
      rc.response().setStatusMessage(rc.request().path()).end();
    });

    testRequest(HttpMethod.GET, "/subpath/foo", 200, "/subpath/foo");

    testRequest(HttpMethod.GET, "/subpath/", 404, "Not Found");
    testRequest(HttpMethod.GET, "/subpath/bar", 404, "Not Found");
    testRequest(HttpMethod.GET, "/otherpath", 404, "Not Found");
  }

  @Test
  public void testMultiple() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath", subRouter);

    subRouter.route("/foo").handler(rc -> {
      assertEquals("/subpath", rc.mountPoint());
      rc.response().setStatusMessage(rc.request().path()).end();
    });
    subRouter.route("/bar").handler(rc -> {
      assertEquals("/subpath", rc.mountPoint());
      rc.response().setStatusMessage(rc.request().path()).end();
    });
    subRouter.route("/wibble").handler(rc -> {
      assertEquals("/subpath", rc.mountPoint());
      rc.response().setStatusMessage(rc.request().path()).end();
    });

    testRequest(HttpMethod.GET, "/subpath/foo", 200, "/subpath/foo");
    testRequest(HttpMethod.GET, "/subpath/bar", 200, "/subpath/bar");
    testRequest(HttpMethod.GET, "/subpath/wibble", 200, "/subpath/wibble");

    testRequest(HttpMethod.GET, "/subpath/", 404, "Not Found");
    testRequest(HttpMethod.GET, "/subpath/other", 404, "Not Found");
    testRequest(HttpMethod.GET, "/otherpath", 404, "Not Found");
  }

  @Test
  public void testMultiple2() throws Exception {
    Router subRouter = Router.router(vertx);

    router.route("/otherpath1").handler(rc -> rc.response().setStatusMessage(rc.request().path()).end());

    router.mountSubRouter("/subpath", subRouter);

    subRouter.route("/foo").handler(rc -> {
      assertEquals("/subpath", rc.mountPoint());
      rc.response().setStatusMessage(rc.request().path()).end();
    });
    subRouter.route("/bar").handler(rc -> {
      assertEquals("/subpath", rc.mountPoint());
      rc.response().setStatusMessage(rc.request().path()).end();
    });
    subRouter.route("/wibble").handler(rc -> {
      assertEquals("/subpath", rc.mountPoint());
      rc.response().setStatusMessage(rc.request().path()).end();
    });

    router.route("/otherpath2").handler(rc -> {
      assertNull(rc.mountPoint());
      rc.response().setStatusMessage(rc.request().path()).end();
    });

    testRequest(HttpMethod.GET, "/subpath/foo", 200, "/subpath/foo");
    testRequest(HttpMethod.GET, "/subpath/bar", 200, "/subpath/bar");
    testRequest(HttpMethod.GET, "/subpath/wibble", 200, "/subpath/wibble");

    testRequest(HttpMethod.GET, "/otherpath1", 200, "/otherpath1");
    testRequest(HttpMethod.GET, "/otherpath2", 200, "/otherpath2");

    testRequest(HttpMethod.GET, "/subpath/", 404, "Not Found");
    testRequest(HttpMethod.GET, "/otherpath3", 404, "Not Found");
  }

  @Test
  public void testChain() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath", subRouter);

    subRouter.route("/foo").handler(rc -> {
      assertEquals("/subpath", rc.mountPoint());
      rc.response().setChunked(true);
      rc.response().write("apples");
      rc.next();
    });
    subRouter.route("/foo").handler(rc -> {
      assertEquals("/subpath", rc.mountPoint());
      rc.response().write("oranges");
      rc.next();
    });
    subRouter.route("/foo").handler(rc -> {
      assertEquals("/subpath", rc.mountPoint());
      rc.response().write("bananas");
      rc.response().end();
    });

    testRequest(HttpMethod.GET, "/subpath/foo", 200, "OK", "applesorangesbananas");

    testRequest(HttpMethod.GET, "/subpath/", 404, "Not Found");
    testRequest(HttpMethod.GET, "/subpath/other", 404, "Not Found");
    testRequest(HttpMethod.GET, "/otherpath", 404, "Not Found");
  }

  @Test
  public void testChain2() throws Exception {
    Router subRouter = Router.router(vertx);

    router.route("/foo/*").handler(rc -> {
      rc.response().setChunked(true);
      rc.response().write("red");
      rc.next();
    });

    router.mountSubRouter("/foo", subRouter);

    subRouter.route("/bar").handler(rc -> {
      assertEquals("/foo", rc.mountPoint());
      rc.response().write("apples");
      rc.next();
    });
    subRouter.route("/bar").handler(rc -> {
      assertEquals("/foo", rc.mountPoint());
      rc.response().write("oranges");
      rc.next();
    });
    subRouter.route("/bar").handler(rc -> {
      assertEquals("/foo", rc.mountPoint());
      rc.response().write("bananas");
      rc.next();
    });

    router.route("/foo/*").handler(rc -> {
      assertNull(rc.mountPoint());
      rc.response().write("pie");
      rc.response().end();
    });

    testRequest(HttpMethod.GET, "/foo/bar", 200, "OK", "redapplesorangesbananaspie");

    testRequest(HttpMethod.GET, "/otherpath", 404, "Not Found");
  }

  @Test
  public void testMultipleSubrouters() throws Exception {
    Router subRouter1 = Router.router(vertx);
    Router subRouter2 = Router.router(vertx);
    Router subRouter3 = Router.router(vertx);

    router.mountSubRouter("/foo", subRouter1);
    subRouter1.mountSubRouter("/bar", subRouter2);
    subRouter2.mountSubRouter("/wibble", subRouter3);
    subRouter3.route("/eek").handler(rc -> {
      assertEquals("/foo/bar/wibble", rc.mountPoint());
      rc.response().setStatusMessage(rc.request().path()).end();
    });

    testRequest(HttpMethod.GET, "/foo/bar/wibble/eek", 200, "/foo/bar/wibble/eek");

    testRequest(HttpMethod.GET, "/foo", 404, "Not Found");
    testRequest(HttpMethod.GET, "/foo/bar", 404, "Not Found");
    testRequest(HttpMethod.GET, "/foo/bar/wibble", 404, "Not Found");
  }

  @Test
  public void testEmptySubrouter() throws Exception {
    Router subRouter1 = Router.router(vertx);

    router.mountSubRouter("/foo", subRouter1);

    testRequest(HttpMethod.GET, "/foo", 404, "Not Found");
    testRequest(HttpMethod.GET, "/foo/bar", 404, "Not Found");
    testRequest(HttpMethod.GET, "/foo/bar/wibble", 404, "Not Found");
  }

  @Test
  public void testSubroutersState() throws Exception {
    Router subRouter1 = Router.router(vertx);
    Router subRouter2 = Router.router(vertx);
    Router subRouter3 = Router.router(vertx);

    router.route("/foo/*").handler(rc -> {
      rc.put("key0", "blah0");
      rc.next();
    });

    router.mountSubRouter("/foo", subRouter1);
    subRouter1.route("/bar/*").handler(rc -> {
      rc.put("key1", "blah1");
      rc.next();
    });
    subRouter1.mountSubRouter("/bar", subRouter2);
    subRouter2.route("/wibble/*").handler(rc -> {
      rc.put("key2", "blah2");
      rc.next();
    });
    subRouter2.mountSubRouter("/wibble", subRouter3);
    subRouter3.route("/eek").handler(rc -> {
      assertEquals("/foo/bar/wibble", rc.mountPoint());
      rc.put("key3", "blah3");
      rc.next();
    });

    router.route("/foo/*").handler(rc -> {
      assertEquals("blah0", rc.get("key0"));
      assertEquals("blah1", rc.get("key1"));
      assertEquals("blah2", rc.get("key2"));
      assertEquals("blah3", rc.get("key3"));
      rc.response().setStatusMessage(rc.currentRoute().getPath()).end();
    });

    testRequest(HttpMethod.GET, "/foo/bar/wibble/eek", 200, "/foo/");
  }

  @Test
  public void testUnhandledRuntimeException() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath", subRouter);

    subRouter.route("/foo").handler(rc -> {
      throw new RuntimeException("Balderdash!");
    });

    testRequest(HttpMethod.GET, "/subpath/foo", 500, "Internal Server Error");
  }

  @Test
  public void tesHandledRuntimeException1() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath", subRouter);

    subRouter.route("/foo/*").handler(rc -> {
      throw new RuntimeException("Balderdash!");
    });

    router.route("/subpath/*").failureHandler(rc -> {
      assertEquals(500, rc.statusCode());
      assertEquals("Balderdash!", rc.failure().getMessage());
      rc.response().setStatusCode(555).setStatusMessage("Badgers").end();
    });

    testRequest(HttpMethod.GET, "/subpath/foo/bar", 555, "Badgers");
  }

  @Test
  public void tesHandledRuntimeException2() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath", subRouter);

    subRouter.route("/foo/*").handler(rc -> {
      throw new RuntimeException("Balderdash!");
    });

    subRouter.route("/foo/*").failureHandler(rc -> {
      assertEquals(500, rc.statusCode());
      assertEquals("Balderdash!", rc.failure().getMessage());
      rc.response().setStatusCode(555).setStatusMessage("Badgers").end();
    });

    testRequest(HttpMethod.GET, "/subpath/foo/bar", 555, "Badgers");
  }

  @Test
  public void testFailCalled1() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath", subRouter);

    subRouter.route("/foo/*").handler(rc -> rc.fail(557));

    router.route("/subpath/*").failureHandler(rc -> {
      assertEquals(557, rc.statusCode());
      assertNull(rc.failure());
      rc.response().setStatusCode(rc.statusCode()).setStatusMessage("Chipmunks").end();
    });

    testRequest(HttpMethod.GET, "/subpath/foo/bar", 557, "Chipmunks");
  }

  @Test
  public void testFailCalled2() throws Exception {
    Router subRouter = Router.router(vertx);

    router.mountSubRouter("/subpath", subRouter);

    subRouter.route("/foo/*").handler(rc -> rc.fail(557));

    router.route("/subpath/*").failureHandler(rc -> {
      assertEquals(557, rc.statusCode());
      assertNull(rc.failure());
      rc.response().setStatusCode(rc.statusCode()).setStatusMessage("Chipmunks").end();
    });

    testRequest(HttpMethod.GET, "/subpath/foo/bar", 557, "Chipmunks");
  }

  @Test
  public void testSubRoutePattern() {
    Router subRouter = Router.router(vertx);
    router.mountSubRouter("/foo/:abc/bar", subRouter);
  }

  @Test
  public void testSubRouteRegex() throws Exception {
    Router subRouter = Router.router(vertx);
    router.routeWithRegex("/foo/.*").handler(subRouter::handleContext).failureHandler(subRouter::handleFailure);
    subRouter.route("/blah").handler(rc -> rc.response().setStatusMessage("sausages").end());
    testRequest(HttpMethod.GET, "/foo/blah", 500, "Internal Server Error");

  }

  @Test
  public void testRegexInSubRouter() throws Exception {
    Router subRouter = Router.router(vertx);
    router.mountSubRouter("/api", subRouter);
    subRouter.routeWithRegex("\\/test").handler(rc -> rc.response().setStatusMessage("sausages").end());
    testRequest(HttpMethod.GET, "/api/test", 200, "sausages");

  }

  @Test
  public void testNormalised1() throws Exception {
    Router subRouter = Router.router(vertx);
    router.mountSubRouter("/api", subRouter);
    subRouter.route("/foo").handler(rc -> rc.response().setStatusMessage("sausages").end());
    testRequest(HttpMethod.GET, "/api/foo", 200, "sausages");
    testRequest(HttpMethod.GET, "/api/foo/", 200, "sausages");
    testRequest(HttpMethod.GET, "/api/foo//", 200, "sausages");
    testRequest(HttpMethod.GET, "//api//foo//", 200, "sausages");
    testRequest(HttpMethod.GET, "//api//foo///", 200, "sausages");
  }

  @Test
  public void testNormalised2() throws Exception {
    Router subRouter = Router.router(vertx);
    router.mountSubRouter("/api/", subRouter);
    subRouter.route("/foo").handler(rc -> rc.response().setStatusMessage("sausages").end());
    testRequest(HttpMethod.GET, "/api/foo", 200, "sausages");
    testRequest(HttpMethod.GET, "/api/foo/", 200, "sausages");
    testRequest(HttpMethod.GET, "/api/foo//", 200, "sausages");
    testRequest(HttpMethod.GET, "//api//foo//", 200, "sausages");
    testRequest(HttpMethod.GET, "//api//foo///", 200, "sausages");
  }

  @Test
  public void testNormalised3() throws Exception {
    Router subRouter = Router.router(vertx);
    router.mountSubRouter("/api", subRouter);
    subRouter.route("/").handler(rc -> rc.response().setStatusMessage("sausages").end());
    testRequest(HttpMethod.GET, "/api/", 200, "sausages");
    testRequest(HttpMethod.GET, "/api", 200, "sausages");
    testRequest(HttpMethod.GET, "/api///", 200, "sausages");
    testRequest(HttpMethod.GET, "//api//", 200, "sausages");
  }

  @Test
  public void testNormalised4() throws Exception {
    Router subRouter = Router.router(vertx);
    router.mountSubRouter("/api/", subRouter);
    subRouter.route("/").handler(rc -> rc.response().setStatusMessage("sausages").end());
    testRequest(HttpMethod.GET, "/api/", 200, "sausages");
    testRequest(HttpMethod.GET, "/api", 404, "Not Found");
    testRequest(HttpMethod.GET, "/api///", 200, "sausages");
    testRequest(HttpMethod.GET, "//api//", 200, "sausages");
  }

  @Test
  public void testStackOverflow() throws Exception {

    router.get("/files/:id/info").handler(ctx -> ctx.response().end());

    router.mountSubRouter("/v1", router);

    testRequest(HttpMethod.GET, "/v1/files/some-file-id/info", 200, "OK");
    testRequest(HttpMethod.GET, "/v1/files//info", 404, "Not Found");
  }

  @Test
  public void testSimpleWithParams() throws Exception {
    Router subRouter = Router.router(vertx);

    subRouter.get("/files/:id/info").handler(ctx -> {
      // version is extracted from the root router
      assertEquals("1", ctx.pathParam("version"));
      // version is extracted from this router
      assertEquals("2", ctx.pathParam("id"));
      ctx.response().end();
    });

    router.mountSubRouter("/v/:version", subRouter);

    testRequest(HttpMethod.GET, "/v/1/files/2/info", 200, "OK");
  }

  @Test(expected = IllegalStateException.class)
  public void testSubRouterExclusive() throws Exception {
    Router subRouter = Router.router(vertx);

    subRouter.get("/files/:id/info").handler(ctx -> {
      // version is extracted from the root router
      assertEquals("1", ctx.pathParam("version"));
      // version is extracted from this router
      assertEquals("2", ctx.pathParam("id"));
      ctx.response().end();
    });

    router.route("/v/:version/*")
      .subRouter(subRouter)
      .handler(ctx -> {});
  }

  @Test(expected = IllegalStateException.class)
  public void testSubRouterExclusive2() throws Exception {
    Router subRouter = Router.router(vertx);

    subRouter.get("/files/:id/info").handler(ctx -> {
      // version is extracted from the root router
      assertEquals("1", ctx.pathParam("version"));
      // version is extracted from this router
      assertEquals("2", ctx.pathParam("id"));
      ctx.response().end();
    });

    router.route("/v/:version/*")
      .handler(ctx -> {})
      .subRouter(subRouter);
  }

  @Test(expected = IllegalStateException.class)
  public void testSubRouterDuplicateVariable() throws Exception {
    Router subRouter = Router.router(vertx);

    subRouter.get("/:id").handler(null);

    router.route("/v/:id*")
      .subRouter(subRouter);
  }

  @Test(expected = IllegalStateException.class)
  public void testSubRouterDuplicateVariableLaterStage() throws Exception {
    Router subRouter = Router.router(vertx);

    router.route("/v/:id*")
      .subRouter(subRouter);

    subRouter.get("/:id").handler(null);
  }

  @Test
  public void testSubRouterWithRegex() {
    Router router = Router.router(vertx);
    router.getWithRegex("some-regex").handler(null);
    router.mountSubRouter("/", router);
  }

  @Test
  public void testStrictSlashSubRouter() throws Exception {
    Router subRouter = Router.router(vertx);

    subRouter.get("/files/info").handler(ctx -> {
      ctx.response().end();
    });

    router.mountSubRouter("/v/", subRouter);

    testRequest(HttpMethod.GET, "/v/files/info", 200, "OK");
  }

  @Test
  public void testMountOnRoot() throws Exception {
    Router subRouter = Router.router(vertx);

    subRouter.get("/primary").handler(ctx -> {
      ctx.response().setStatusMessage("Hi").end();
    });

    router.mountSubRouter("/", subRouter);

    testRequest(HttpMethod.GET, "/primary", 200, "Hi");
    testRequest(HttpMethod.GET, "/primary?query=1", 200, "Hi");
    testRequest(HttpMethod.GET, "/primary/", 200, "Hi");
    testRequest(HttpMethod.GET, "/primary/random", 404, "Not Found");
  }

  @Test
  public void testWrongMethodSubRouter() throws Exception {
    Router subRouter = Router.router(vertx);

    subRouter.post("/order/deposit").handler(ctx -> {
      ctx.response().end();
    });

    router.mountSubRouter("/bank", subRouter);

    testRequest(HttpMethod.POST, "/bank/order/deposit", 200, "OK");
    testRequest(HttpMethod.GET, "/bank/order/deposit", 405, "Method Not Allowed");
  }
}
