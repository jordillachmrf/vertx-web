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
package io.vertx.ext.web.client.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.HttpClientImpl;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.UriTemplate;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.predicate.PredicateInterceptor;
import io.vertx.ext.web.codec.impl.BodyCodecImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebClientBase implements WebClientInternal {

  final HttpClient client;
  final WebClientOptions options;
  final List<Handler<HttpContext<?>>> interceptors;

  public WebClientBase(HttpClient client, WebClientOptions options) {
    this.client = client;
    this.options = new WebClientOptions(options);
    this.interceptors = new CopyOnWriteArrayList<>();

    // Add base interceptor
    addInterceptor(new PredicateInterceptor());
  }

  WebClientBase(WebClientBase webClient) {
    this.client = webClient.client;
    this.options = new WebClientOptions(webClient.options);
    this.interceptors = new CopyOnWriteArrayList<>(webClient.interceptors);
  }

  @Override
  public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, String requestURI) {
    return new HttpRequestImpl<>(this, method, serverAddress, options.isSsl(), options.getDefaultPort(), options.getDefaultHost(),
      requestURI, BodyCodecImpl.BUFFER, options);
  }

  @Override
  public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, UriTemplate requestURI) {
    return new HttpRequestImpl<>(this, method, serverAddress, options.isSsl(), options.getDefaultPort(), options.getDefaultHost(),
      requestURI, BodyCodecImpl.BUFFER, options);
  }

  @Override
  public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, RequestOptions requestOptions) {
      HttpRequestImpl<Buffer> request = new HttpRequestImpl<>(this, method, serverAddress, requestOptions.isSsl(), requestOptions.getPort(),
      requestOptions.getHost(), requestOptions.getURI(), BodyCodecImpl.BUFFER, options, requestOptions.getProxyOptions());
      return requestOptions.getHeaders() == null ? request : request.putHeaders(requestOptions.getHeaders());
  }

  @Override
  public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, String host, String requestURI) {
    return new HttpRequestImpl<>(this, method, serverAddress, options.isSsl(), options.getDefaultPort(), host, requestURI, BodyCodecImpl.BUFFER, options);
  }

  @Override
  public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, int port, String host, String requestURI) {
    return new HttpRequestImpl<>(this, method, serverAddress, options.isSsl(), port, host, requestURI, BodyCodecImpl.BUFFER, options);
  }

  @Override
  public HttpRequest<Buffer> requestAbs(HttpMethod method, SocketAddress serverAddress, String surl) {
    // Note - parsing a URL this way is slower than specifying host, port and relativeURI
    URL url;
    try {
      url = new URL(surl);
    } catch (MalformedURLException e) {
      throw new VertxException("Invalid url: " + surl, e);
    }
    boolean ssl = false;
    int port = url.getPort();
    String protocol = url.getProtocol();
    if ("ftp".equals(protocol)) {
      if (port == -1) {
        port = 21;
      }
    } else {
      char chend = protocol.charAt(protocol.length() - 1);
      if (chend == 'p') {
        if (port == -1) {
          port = 80;
        }
      } else if (chend == 's'){
        ssl = true;
        if (port == -1) {
          port = 443;
        }
      }
    }
    return new HttpRequestImpl<>(this, method, serverAddress, protocol, ssl, port, url.getHost(), url.getFile(),
            BodyCodecImpl.BUFFER, options, null);
  }

  @Override
  public WebClientInternal addInterceptor(Handler<HttpContext<?>> interceptor) {
    // If a web client is constructed using another client, interceptors could get added twice.
    if (interceptors.stream().anyMatch(i -> i.getClass() == interceptor.getClass())) {
      throw new IllegalStateException(String.format("Client already contains a %s interceptor", interceptor.getClass()));
    }
    interceptors.add(interceptor);
    return this;
  }

  @Override
  public <T> HttpContext<T> createContext(Handler<AsyncResult<HttpResponse<T>>> handler) {
    HttpClientImpl client = (HttpClientImpl) this.client;
    return new HttpContext<>(client, interceptors, handler);
  }

  @Override
  public void close() {
    client.close();
  }
}
