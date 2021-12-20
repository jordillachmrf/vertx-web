/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.ext.web.client.template;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.web.client.impl.template.VariablesImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

@VertxGen
public interface Variables {

  static Variables variables() {
    return new VariablesImpl();
  }

  @Fluent
  Variables set(String name, String value);

  @Fluent
  Variables set(String name, List<String> values);

  @Fluent
  Variables set(String name, Map<String, String> entries);

  Set<String> keys();

  String getValue(String key);

  List<String> getValues(String key);

  Map<String, String> getEntries(String key);

}
