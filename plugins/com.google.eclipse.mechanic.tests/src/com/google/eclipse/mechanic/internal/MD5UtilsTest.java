package com.google.eclipse.mechanic.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import junit.framework.TestCase;

import com.google.eclipse.mechanic.tests.internal.RunAsJUnitTest;

/**
 * Tests for {@link MD5Utils}.
 */
@RunAsJUnitTest
public class MD5UtilsTest extends TestCase {

  public void testMd5Hex() throws IOException {
    assertEquals("0DC15CE25DCEA946E7DB6E7CED36B6B9",
        MD5Utils.md5Hex(new ByteArrayInputStream("a sentence to hash".getBytes())));

    assertEquals("E285DCF63312CE0CBCF5C4DF7D49D856",
        MD5Utils.md5Hex(new ByteArrayInputStream("another sentence to hash".getBytes())));
  }
}
