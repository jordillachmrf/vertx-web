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
package io.vertx.ext.web.client.impl.template;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.template.Variables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VariablesImpl implements Variables {

  private static String toString(Object o) {
    if (o == null) {
      return null;
    } else {
      return o.toString();
    }
  }

  private final Map<String, Object> variables = new LinkedHashMap<>();

  public VariablesImpl() {
  }

  public VariablesImpl(JsonObject that) {
    for (Map.Entry<String, Object> entry : that) {
      String name = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof JsonObject) {
        JsonObject json = (JsonObject) value;
        LinkedHashMap<String, String> map = new LinkedHashMap<>(json.size());
        for (Map.Entry<String, Object> e : json) {
          map.put(e.getKey(), toString(e.getValue()));
        }
        set(name, map);
      } else if (value instanceof JsonArray) {
        JsonArray json = (JsonArray) value;
        List<String> list = new ArrayList<>(json.size());
        for (Object o : json) {
          list.add(toString(o));
        }
        set(name, list);
      } else {
        set(name, toString(value));
      }
    }
  }

  @Override
  public Variables set(String key, String value) {
    variables.put(key, value);
    return this;
  }

  @Override
  public Variables set(String key, List<String> values) {
    variables.put(key, values);
    return this;
  }

  @Override
  public Variables set(String key, Map<String, String> entries) {
    variables.put(key, entries);
    return this;
  }

  @Override
  public Object get(String key) {
    return variables.get(key);
  }

  @Override
  public Set<String> keys() {
    return variables.keySet();
  }

  @Override
  public String getValue(String key) {
    Object o = variables.get(key);
    return o instanceof String ? (String) o : null;
  }

  @Override
  public List<String> getValues(String key) {
    Object o = variables.get(key);
    return o instanceof List ? (List<String>) o : null;
  }

  @Override
  public Map<String, String> getEntries(String key) {
    Object o = variables.get(key);
    return o instanceof Map ? (Map<String, String>) o : null;
  }
}
