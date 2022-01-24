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

import io.netty.util.collection.CharObjectHashMap;
import io.netty.util.collection.CharObjectMap;
import io.vertx.ext.web.client.template.UriTemplate;
import io.vertx.ext.web.client.template.Variables;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vertx.ext.web.client.impl.template.UriTemplateImpl.Parser.isHEXDIG;

public class UriTemplateImpl implements UriTemplate {

  private static final String HEX_ALPHABET = "0123456789ABCDEF";
  private static final Predicate<Character> UNRESERVED_SET = Parser::isUnreserved;
  private static final Predicate<Character> LITERALS_SET = ch -> Parser.isUnreserved(ch) || Parser.isReserved(ch);

  public abstract static class Term {

  }

  public abstract static class SOperator {

    private final Predicate<Character> allowedSet;
    private final String prefix;
    private final String delimiter;
    final char[] chars;

    SOperator(Predicate<Character> allowedSet, String prefix, String delimiter, char... chars) {
      this.allowedSet = allowedSet;
      this.prefix = prefix;
      this.delimiter = delimiter;
      this.chars = chars;
    }

    String join(boolean entry, String name, String value) {
      throw new UnsupportedOperationException();
    }

    String encodeName(String s) {
      return encode(s, false);
    }

    String encodeValue(String s) {
      return encode(s, false);
    }

    String encode(String s, boolean allowPctEncoded) {
      StringBuilder sb = new StringBuilder();
      encodeString(s, allowedSet, allowPctEncoded, sb);
      return sb.toString();
    }
  }

  /**
   * Operator implementation issuing a {@code key=value} only for exploded maps, e.g
   */
  private static class Cat1 extends SOperator {

    public Cat1(Predicate<Character> allowedSet, String prefix, String delimiter, char... chars) {
      super(allowedSet, prefix, delimiter, chars);
    }

    @Override
    String join(boolean entry, String name, String value) {
      return entry ? name + "=" + value : value;
    }
  }

  /**
   * Operator implementation always issuing as {@code key=value}, e.g form style query expansion
   */
  private static class Cat2 extends SOperator {

    public Cat2(Predicate<Character> allowedSet, String prefix, String delimiter, char... chars) {
      super(allowedSet, prefix, delimiter, chars);
    }

    @Override
    String join(boolean entry, String name, String value) {
      return name + "=" + value;
    }
  }

  private static class SimpleStringExpansion extends Cat1 {
    public SimpleStringExpansion() {
      super(UNRESERVED_SET, "", ",");
    }
  }

  private static class ReservedExpansion extends Cat1 {
    public ReservedExpansion() {
      super(cp -> Parser.isReserved(cp) || Parser.isUnreserved(cp), "", ",", '+');
    }

    @Override
    String encodeValue(String s) {
      return super.encode(s, true);
    }
  }

  private static class FragmentExpansion extends Cat1 {
    public FragmentExpansion() {
      super(cp -> Parser.isReserved(cp) || Parser.isUnreserved(cp), "#", ",", '#');
    }

    String encodeValue(String s) {
      return super.encode(s, true);
    }
  }

  private static class LabelExpansionWithDotPrefix extends Cat1 {
    public LabelExpansionWithDotPrefix() {
      super(UNRESERVED_SET, ".", ".", '.');
    }
  }

  private static class PathSegmentExpansion extends Cat1 {
    public PathSegmentExpansion() {
      super(UNRESERVED_SET, "/", "/", '/');
    }
  }

  private static class PathStyleParameterExpansion extends Cat2 {
    public PathStyleParameterExpansion() {
      super(UNRESERVED_SET , ";", ";", ';');
    }

    @Override
    String join(boolean entry, String name, String value) {
      if (!entry && value.isEmpty()) {
        return name;
      }
      return super.join(entry, name, value);
    }
  }

  private static class FormStyleQueryExpansion extends Cat2 {
    public FormStyleQueryExpansion() {
      super(UNRESERVED_SET, "?", "&", '?');
    }
  }

  private static class FormStyleQueryContinuation extends Cat2 {
    public FormStyleQueryContinuation() {
      super(UNRESERVED_SET, "&", "&", '&');
    }
  }

  private static class Future extends SOperator {
    public Future() {
      super(UNRESERVED_SET, "", "", '=', ',', '!', '@', '|');
    }
  }

  public enum Operator {

    SIMPLE_STRING_EXPANSION(new SimpleStringExpansion()),
    RESERVED_EXPANSION(new ReservedExpansion()),
    LABEL_EXPANSION_WITH_DOT_PREFIX(new LabelExpansionWithDotPrefix()),
    PATH_SEGMENT_EXPANSION(new PathSegmentExpansion()),
    PATH_STYLE_PARAMETER_EXPANSION(new PathStyleParameterExpansion()),
    FORM_STYLE_QUERY_EXPANSION(new FormStyleQueryExpansion()),
    FORM_STYLE_QUERY_CONTINUATION(new FormStyleQueryContinuation()) ,
    FRAGMENT_EXPANSION(new FragmentExpansion()),
    FUTURE(new Future())

    ;

    private final SOperator so;

    Operator(SOperator s) {
      this.so = s;
    }

    void expand(List<Varspec> variableList, Variables variables, StringBuilder sb) {
      List<String> l = new ArrayList<>();
      for (Varspec variable : variableList) {
        Object o = variables.get(variable.varname);
        List<String> values;
        if (o == null) {
          continue;
        } else if (o instanceof String) {
          String s = (String) o;
          if (variable.maxLength > 0 && variable.maxLength < s.length()) {
            s = s.substring(0, variable.maxLength);
          }
          values = Collections.singletonList(format(variable, s));
        } else if (o instanceof List) {
          if (variable.maxLength > 0) {
            throw new IllegalArgumentException();
          }
          List<String> list = (List<String>) o;
          if (list.isEmpty()) {
            continue;
          }
          values = format(variable, list);
        } else if (o instanceof Map) {
          if (variable.maxLength > 0) {
            throw new IllegalArgumentException();
          }
          Map<String, String> map = (Map<String, String>) o;
          if (map.isEmpty()) {
            continue;
          }
          values = format(variable, map);
        } else {
          throw new UnsupportedOperationException();
        }
        l.addAll(values);
      }
      if (l.size() > 0) {
        sb.append(format(l));
      }
    }

    String format(Varspec variable, String value) {
      return so.join(false, so.encodeName(variable.decoded), so.encodeValue(value));
    }

    List<String> format(Varspec variable, List<String> value) {
      if (variable.exploded) {
        return value.stream().map(v -> format(variable, v)).collect(Collectors.toList());
      } else {
        return Collections.singletonList(so.join(false, so.encodeName(variable.decoded), value.stream().map(so::encodeValue).collect(Collectors.joining(","))));
      }
    }

    List<String> format(Varspec variable, Map<String, String> value) {
      if (variable.exploded) {
        return value.entrySet().stream().map(entry -> so.join(true, so.encodeName(entry.getKey()), so.encodeValue(entry.getValue()))).collect(Collectors.toList());
      } else {
        return Collections.singletonList(so.join(false, so.encodeName(variable.varname), value.entrySet().stream().flatMap(entry -> Stream.of(so.encodeValue(entry.getKey()), so.encodeValue(entry.getValue()))).collect(Collectors.joining(","))));
      }
    }

    String format(List<String> l) {
      return l.stream().collect(Collectors.joining(so.delimiter, so.prefix, ""));
    }
  }

  private static final CharObjectMap<Operator> mapping;

  static {
    CharObjectMap<Operator> m = new CharObjectHashMap<>();
    for (Operator op : Operator.values()) {
      for (char ch : op.so.chars) {
        m.put(ch, op);
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
    public final String varname;
    public final String decoded;
    public final int maxLength;
    public final boolean exploded;
    private Varspec(String varname, String decoded, int maxLength, boolean exploded) {
      this.varname = varname;
      this.decoded = decoded;
      this.maxLength = maxLength;
      this.exploded = exploded;
    }
  }

  public static class Parser {

    private UriTemplateImpl template;
    private Expression expression;

    public UriTemplateImpl parseURITemplate(String s) {
      template = new UriTemplateImpl();
      if (parseURITemplate(s, 0) != s.length()) {
        throw new IllegalArgumentException();
      }
      for (Term term : template.terms) {
        if (term instanceof Expression && ((Expression) term).operator == Operator.FUTURE) {
          throw new IllegalArgumentException("Invalid reserved operator");
        }
      }
      return template;
    }

    public int parseURITemplate(String s, int pos) {
      while (true) {
        int idx = parseLiterals(s, pos);
        if (idx > pos) {
          template.terms.add(new Literals(literals.toString()));
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

    private static boolean isALPHA(char ch) {
      return ('A' <= ch && ch <= 'Z')
        || ('a'<= ch && ch <= 'z');
    }

    private int digit;

    public int parseDIGIT(String s, int pos) {
      char c;
      if (pos < s.length() && isDIGIT(c = s.charAt(pos))) {
        digit = c - '0';
        pos++;
      }
      return pos;
    }

    private static boolean isDIGIT(char ch) {
      return ('0' <= ch && ch <= '9');
    }

    static boolean isHEXDIG(char ch) {
      return isDIGIT(ch) || ('A' <= ch && ch <= 'F') || ('a' <= ch && ch <= 'f');
    }

    private char pctEncoded;

    private int parsePctEncoded(String s, int pos) {
      byte[] buffer = new byte[0]; //
      while (pos + 2 < s.length() && s.charAt(pos) == '%' && isHEXDIG(s.charAt(pos + 1)) && isHEXDIG(s.charAt(pos + 2))) {
        buffer = Arrays.copyOf(buffer, buffer.length + 1);
        buffer[buffer.length - 1] = (byte)Integer.parseInt(s.substring(pos + 1, pos + 3), 16);
        pos += 3;
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        CharsetDecoder dec = StandardCharsets.UTF_8.newDecoder();
        CharBuffer chars = CharBuffer.allocate(1);
        CoderResult result = dec.decode(bb, chars, true);
        if (result.isUnderflow()) {
          dec.flush(chars);
          chars.flip();
          pctEncoded = chars.charAt(0);
          break;
        }
      }
      return pos;
    }

    private static boolean isUnreserved(char ch) {
      return isALPHA(ch) || isDIGIT(ch) || ch == '-' || ch == '.' || ch == '_' || ch == '~';
    }

    private static boolean isReserved(char ch) {
      return isGenDelims(ch) || isSubDelims(ch);
    }

    private static boolean isGenDelims(char ch) {
      return ch == ':' || ch == '/' || ch == '?' || ch == '#' || ch == '[' || ch == ']' || ch == '@';
    }

    private static boolean isSubDelims(char ch) {
      return ch == '!' || ch == '$' || ch == '&' || ch == '\'' || ch == '(' || ch == ')' || ch == '*' || ch == '+' || ch == ',' || ch == ';' || ch == '=';
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

    public StringBuilder literals;

    public int parseLiterals(String s, int pos) {
      literals = new StringBuilder();
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
          || ch == 0x7E) {
          pos++;
          encodeChar(ch, LITERALS_SET, literals);
        } else {
          if (Character.isSurrogate(ch)) {
            if (pos + 1 >= s.length()) {
              throw new IllegalArgumentException();
            }
            int cp = s.codePointAt(pos);
            if (isUcschar(cp) || isIprivate(cp)) {
              pctEncode(s.substring(pos, pos + 2), literals);
              pos += 2;
            } else {
              break;
            }
          } else {
            int idx = parsePctEncoded(s, pos);
            if (idx == pos) {
              break;
            }
            // Directly insert as this is allowed
            literals.append(s, pos, idx);
            pos = idx;
          }
        }
      }
      return pos;
    }

    private static boolean isOperator(char ch) {
      return isOpLevel2(ch) || isOpLevel3(ch) || isOpReserve(ch);
    }

    private static boolean isOpLevel2(char ch) {
      return ch == '+' || ch == '#';
    }

    private static boolean isOpLevel3(char ch) {
      return ch == '.' || ch == '/' || ch == ';' || ch == '?' || ch == '&';
    }

    private static boolean isOpReserve(char ch) {
      return ch == '=' || ch == ',' || ch == '!' || ch == '@' || ch == '|';
    }

    public int parseVariableList(String s, int pos) {
      int idx = parseVarspec(s, pos);
      if (expression != null) {
        expression.value.add(varspec);
      }
      if (idx > pos) {
        pos = idx;
        while (pos < s.length() && s.charAt(pos) == ',' && (idx = parseVarspec(s, pos + 1)) > pos + 1) {
          if (expression != null) {
            expression.value.add(varspec);
          }
          pos = idx;
        }
      }
      return pos;
    }

    public Varspec varspec;

    public int parseVarspec(String s, int pos) {
      varspec = null;
      int idx = parseVarname(s, pos);
      if (idx > pos) {
        String varname = s.substring(pos, idx);
        pos = parseModifierLevel4(s, idx);
        varspec = new Varspec(varname, sb.toString(), maxLength, exploded);
      }
      return pos;
    }

    private StringBuilder sb;

    public int parseVarname(String s, int pos) {
      sb = new StringBuilder();
      int idx = parseVarchar(s, pos);
      while (idx > pos) {
        pos = idx;
        if (pos < s.length() && s.charAt(pos) == '.') {
          sb.append('.');
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

    private int parseVarchar(String s, int pos) {
      if (pos < s.length()) {
        char ch = s.charAt(pos);
        if (isALPHA(ch) || isDIGIT(ch) || ch == '_')  {
          sb.append(ch);
          pos++;
        } else {
          int idx = parsePctEncoded(s, pos);
          if (idx > pos) {
            sb.append(pctEncoded);
            pos = idx;
          }
        }
      }
      return pos;
    }

    private boolean exploded;

    public int parseModifierLevel4(String s, int pos) {
      exploded = false;
      maxLength = -1;
      int idx = parsePrefixModifier(s, pos);
      if (idx > pos) {
        pos = idx;
      } else if (pos < s.length() && isExplode(s.charAt(pos))) {
        exploded = true;
        pos++;
      }
      return pos;
    }

    public int parsePrefixModifier(String s, int pos) {
      if (pos < s.length() && s.charAt(pos) == ':') {
        int idx = parseMaxLength(s, pos + 1);
        if (idx > pos + 1) {
          pos = idx;
        }
      }
      return pos;
    }

    private int maxLength;

    public int parseMaxLength(String s, int pos) {
      if (pos < s.length()) {
        char ch = s.charAt(pos);
        if ('1' <= ch && ch <= '9') {
          pos++;
          maxLength = ch - '0';
          for (int i = 0;i < 3;i++) {
            if (parseDIGIT(s, pos) > pos) {
              maxLength = maxLength * 10 + digit;
              pos++;
            }
          }
        }
      }
      return pos;
    }

    private static boolean isExplode(char ch) {
      return ch == '*';
    }
  }

  private final List<Term> terms = new ArrayList<>();

  @Override
  public String expand(Variables variables) {
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

  private static void encodeString(String s, Predicate<Character> allowedSet, boolean allowPctEncoded, StringBuilder buff) {
    int i = 0;
    while (i < s.length()) {
      char ch = s.charAt(i++);
      if (Character.isSurrogate(ch)) {
        pctEncode(s.substring(i - 1, i + 1), buff);
        i++;
      } else if (allowPctEncoded && ch == '%' && i + 1 < s.length() && isHEXDIG(s.charAt(i)) && isHEXDIG(s.charAt(i + 1))) {
        buff.append(s, i - 1, i + 2);
        i+= 2;
      } else {
        encodeChar(ch, allowedSet, buff);
      }
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

  private static void pctEncode(String s, StringBuilder buff) {
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    for (byte b : bytes) {
      int high = (b & 0xF0) >> 4;
      int low = b & 0x0F;
      buff.append('%');
      buff.append(HEX_ALPHABET, high, high + 1);
      buff.append(HEX_ALPHABET, low, low + 1);
    }
  }
}
