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

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.client.template.UriTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class UriTemplateImpl implements UriTemplate {

  private final List<Term> terms = new ArrayList<>();

  public abstract static class Term {

  }

  public enum Operator {

    SIMPLE_STRING_EXPANSION() {
      private final Predicate<Character> ALLOWED_CHARS = Parser::isUnreserved;
      @Override
      void expand(List<Varspec> variableList, MultiMap variables, StringBuilder sb) {
        boolean first = true;
        for (Varspec variable : variableList) {
          String value = variables.get(variable.varname);
          if (value != null) {
            if (first) {
              first = false;
            } else {
              sb.append(',');
            }
            encodeString(value, ALLOWED_CHARS, sb);
          }
        }
      }
    },

    RESERVED_EXPANSION('+'),
    LABEL_EXPANSION('.'),
    PATH_SEGMENT_EXPANSION('/'),
    PATH_STYLE_PARAMETER_EXPANSION(';'),
    FORM_STYLE_QUERY_EXPANSION('?'),
    FORM_STYLE_QUERY_CONTINUATION('&'),

    FRAGMENT_EXPANSION('#'),

    RESERVED('=', ',', '!', '@', '|')

    ;

    final int[] cps;

    Operator(int... cps) {
      this.cps = cps;
    }

    void expand(List<Varspec> variableList, MultiMap variables, StringBuilder sb) {
      throw new UnsupportedOperationException();
    }

  }

  private static final IntObjectMap<Operator> mapping;

  static {
    IntObjectHashMap<Operator> m = new IntObjectHashMap<>();
    for (Operator op : Operator.values()) {
      for (int cp : op.cps) {
        m.put(cp, op);
      }
    }
    mapping = m;
  }

  public static final class Literals extends Term {
    private final String value;
    private Literals(String value) {
      this.value = value;
    }
  }

  public static final class Expression extends Term {
    private final Operator operator;
    private final List<Varspec> value = new ArrayList<>();
    public Expression(Operator operator) {
      this.operator = operator;
    }
  }

  public static final class Varspec {
    private final String varname;
    private Varspec(String varname) {
      this.varname = varname;
    }
  }

  @Override
  public String expand(MultiMap variables) {
    StringBuilder sb = new StringBuilder();
    terms.forEach(term -> {
      if (term instanceof Literals) {
        sb.append(((Literals)term).value);
      } else {
        Expression expression = (Expression) term;
        expression.operator.expand(expression.value, variables, sb);
      }
    });
    return sb.toString();
  }

  public static class Parser {

    private UriTemplateImpl template;
    private Expression expression;

    public UriTemplateImpl parseURITemplate(String s) {
      template = new UriTemplateImpl();
      if (parseURITemplate(s, 0) != s.length()) {
        throw new IllegalArgumentException();
      }
      return template;
    }

    public int parseURITemplate(String s, int pos) {
      StringBuilder sb = new StringBuilder();
      while (true) {
        int idx = parseLiterals(s, pos, sb);
        if (idx > pos) {
          template.terms.add(new Literals(sb.toString()));
          sb.setLength(0);
          pos = idx;
        } else {
          idx = parseExpression(s, pos);
          if (idx > pos) {
            pos = idx;
          } else {
            break;
          }
        }
      }
      return pos;
    }

    public int parseExpression(String s, int pos) {
      if (pos < s.length() && s.charAt(pos) == '{') {
        int idx = pos + 1;
        Operator operator;
        if (idx < s.length() && isOperator(s.charAt(idx))) {
          operator = mapping.get(s.charAt(idx));
          idx++;
        } else {
          operator = Operator.SIMPLE_STRING_EXPANSION;
        }
        expression = new Expression(operator);
        idx = parseVariableList(s, idx);
        if (idx < s.length() && s.charAt(idx) == '}') {
          pos = idx + 1;
        }
        if (template != null) {
          template.terms.add(expression);
        }
        expression = null;
      }
      return pos;
    }

    private static boolean isALPHA(int cp) {
      return ('A' <= cp && cp <= 'Z')
        || ('a'<= cp && cp <= 'z');
    }

    public static int parseDIGIT(String s, int pos) {
      if (pos < s.length() && isDIGIT(s.charAt(pos))) {
        pos++;
      }
      return pos;
    }

    private static boolean isDIGIT(int cp) {
      return ('0' <= cp && cp <= '9');
    }

    private static boolean isHEXDIG(int cp) {
      return isDIGIT(cp) || ('A' <= cp && cp <= 'F');
    }

    private static int parsePctEncoded(String s, int pos) {
      if (pos + 2 < s.length() && s.charAt(pos) == '%' && isHEXDIG(s.charAt(pos + 1)) && isHEXDIG(s.charAt(pos + 2))) {
        return pos + 2;
      }
      return pos;
    }

    private static boolean isUnreserved(int cp) {
      return isALPHA(cp) || isDIGIT(cp) || cp == '-' || cp == '.' || cp == '_' || cp == '~';
    }

    private static boolean isReserved(int cp) {
      return isGenDelims(cp) || isSubDelims(cp);
    }

    private static boolean isGenDelims(int cp) {
      return cp == ':' || cp == '/' || cp == '?' || cp == '#' || cp == '[' || cp == ']' || cp == '@';
    }

    private static boolean isSubDelims(int cp) {
      return cp == '!' || cp == '$' || cp == '&' || cp == '\'' || cp == '(' || cp == ')' || cp == '*' || cp == '+' || cp == ',' || cp == ';' || cp == '=';
    }

    private static boolean isIprivate(int cp) {
      return (0xE000 <= cp && cp <= 0xF8FF)
        || (0xF0000 <= cp && cp <= 0xFFFFD)
        || (0x100000 <= cp && cp <= 0x10FFFD);
    }

    private static boolean isUcschar(int cp) {
      return (0xA0 <= cp && cp <= 0xD7FF)
        || (0xF900 <= cp && cp <= 0xFDCF)
        || (0xFDF0 <= cp && cp <= 0xFFEF)
        || (0x10000 <= cp && cp <= 0x1FFFD)
        || (0x20000 <= cp && cp <= 0x2FFFD)
        || (0x30000 <= cp && cp <= 0x3FFFD)
        || (0x40000 <= cp && cp <= 0x4FFFD)
        || (0x50000 <= cp && cp <= 0x5FFFD)
        || (0x60000 <= cp && cp <= 0x6FFFD)
        || (0x70000 <= cp && cp <= 0x7FFFD)
        || (0x80000 <= cp && cp <= 0x8FFFD)
        || (0x90000 <= cp && cp <= 0x9FFFD)
        || (0xA0000 <= cp && cp <= 0xAFFFD)
        || (0xB0000 <= cp && cp <= 0xBFFFD)
        || (0xC0000 <= cp && cp <= 0xCFFFD)
        || (0xD0000 <= cp && cp <= 0xDFFFD)
        || (0xE1000 <= cp && cp <= 0xEFFFD);
    }

    private static final Predicate<Character> LITERALS_ALLOWED = ch -> Parser.isUnreserved(ch) || Parser.isReserved(ch);

    private static int parseLiterals(String s, int pos, StringBuilder sb) {
      while (pos < s.length() ) {
        char ch = s.charAt(pos);
        if (ch == 0x21
          || (0x23 <= ch && ch <= 0x24)
          || ch == 0x26
          || (0x28 <= ch && ch <= 0x3B)
          || ch == 0x3D
          || (0x3F <= ch && ch <= 0x5B)
          || ch == 0x5D
          || ch == 0x5F
          || (0x61 <= ch && ch <= 0x7A)
          || ch == 0x7E
          || isUcschar(ch)
          || isIprivate(ch)) {
          pos++;
          encodeChar(ch, LITERALS_ALLOWED, sb);
        } else {
          int idx = parsePctEncoded(s, pos);
          if (idx == pos) {
            break;
          }
          // Directly insert as this is allowed
          sb.append(s, pos, idx);
          pos = idx;
        }
      }
      return pos;
    }

    private static boolean isOperator(int cp) {
      return isOpLevel2(cp) || isOpLevel2(3) || isOpReserve(cp);
    }

    private static boolean isOpLevel2(int cp) {
      return cp == '+' || cp == '#';
    }

    private static boolean isOpLevel3(int cp) {
      return cp == '.' || cp == '/' || cp == ';' || cp == '?' || cp == '&';
    }

    private static boolean isOpReserve(int cp) {
      return cp == '=' || cp == ',' || cp == '!' || cp == '@' || cp == '|';
    }

    public int parseVariableList(String s, int pos) {
      int idx = parseVarspec(s, pos);
      if (idx > pos) {
        pos = idx;
        while (pos < s.length() && s.charAt(pos) == ',' && (idx = parseVarspec(s, pos + 1)) > pos + 1) {
          pos = idx;
        }
      }
      return pos;
    }

    public int parseVarspec(String s, int pos) {
      int idx = parseVarname(s, pos);
      if (idx > pos) {
        String varname = s.substring(pos, idx);
        pos = parseModifierLevel4(s, idx);
        if (expression != null) {
          expression.value.add(new Varspec(varname));
        }
      }
      return pos;
    }

    public static int parseVarname(String s, int pos) {
      int idx = parseVarchar(s, pos);
      while (idx > pos) {
        pos = idx;
        if (pos < s.length() && s.charAt(pos) == '.') {
          int j = parseVarchar(s, pos + 1);
          if (j > pos + 1) {
            idx = j;
          }
        } else {
          idx = parseVarchar(s, pos);
        }
      }
      return idx;
    }

    private static int parseVarchar(String s, int pos) {
      if (pos < s.length()) {
        int cp = s.charAt(pos);
        if (isALPHA(cp) || isDIGIT(cp) || cp == '_')  {
          pos++;
        } else {
          pos = parsePctEncoded(s, pos);
        }
      }
      return pos;
    }

    public static int parseModifierLevel4(String s, int pos) {
      int idx = parsePrefix(s, pos);
      if (idx > pos) {
        pos = idx;
      } else if (pos < s.length() && isExplode(s.charAt(pos))) {
        pos++;
      }
      return pos;
    }

    public static int parsePrefix(String s, int pos) {
      if (pos < s.length() && s.charAt(pos) == ':') {
        int idx = parseMaxLength(s, pos + 1);
        if (idx > pos + 1) {
          pos = idx;
        }
      }
      return pos;
    }

    public static int parseMaxLength(String s, int pos) {
      if (pos < s.length()) {
        int cp = s.charAt(pos);
        if ('1' <= cp && cp <= '9') {
          pos++;
          for (int i = 0;i < 3;i++) {
            if (parseDIGIT(s, pos) > pos) {
              pos++;
            }
          }
        }
      }
      return pos;
    }

    private static boolean isExplode(int cp) {
      return cp == '*';
    }
  }

  private static final String HEX_ALPHABET = "0123456789ABCDEF";

  private static void encodeString(String s, Predicate<Character> allowedSet, StringBuilder buff) {
    for (int i = 0;i < s.length();i++) {
      char ch = s.charAt(i);
      encodeChar(ch, allowedSet, buff);
    }
  }

  private static void encodeChar(char ch, Predicate<Character> allowedSet, StringBuilder buff) {
    if (allowedSet.test(ch)) {
      buff.append(ch);
    } else {
      byte[] bytes = Character.toString(ch).getBytes(StandardCharsets.UTF_8);
      for (byte b : bytes) {
        int high = (b & 0xF0) >> 4;
        int low = b & 0x0F;
        buff.append('%');
        buff.append(HEX_ALPHABET, high, high + 1);
        buff.append(HEX_ALPHABET, low, low + 1);
      }
    }
  }
}
