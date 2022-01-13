package io.vertx.ext.web.client.uritemplate;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DecodeTest {

  @Test
  public void testFoo() {
    final CharsetDecoder dec = Charset.forName("UTF-8").newDecoder();
    final CharBuffer chars = CharBuffer.allocate(3);
    final byte[] tab = new byte[]{(byte) -30, (byte) -126, (byte) -84}; //char €

    ByteBuffer buffer = ByteBuffer.allocate(10);
    buffer.put(tab[0]);
    buffer.put(tab[1]);
    buffer.put(tab[2]);
    buffer.flip();
    CoderResult result = dec.decode(buffer, chars, true);
    System.out.println(result);


    dec.flush(chars);
    chars.flip();

    System.out.println("a" + chars.toString() + "a");
  }
}
