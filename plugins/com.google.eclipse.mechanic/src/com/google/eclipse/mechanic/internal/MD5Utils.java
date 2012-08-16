package com.google.eclipse.mechanic.internal;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 utilities class, copied from apache-commons-codec
 */
public class MD5Utils {

  private static final int STREAM_BUFFER_LENGTH = 1024;

  private static final char[] DIGITS_UPPER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  public static String md5Hex(InputStream inputStream) throws IOException {
    return encodeHexString(md5(inputStream));
  }

  private static byte[] md5(InputStream data) throws IOException {
    return digest(getMD5Digest(), data);
  }

  private static MessageDigest getMD5Digest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  private static byte[] digest(MessageDigest digest, InputStream data) throws IOException {
    byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
    int read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);

    while (read > -1) {
        digest.update(buffer, 0, read);
        read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);
    }

    return digest.digest();
  }

  private static String encodeHexString(byte[] data) {
    return new String(encodeHex(data));
  }

  private static char[] encodeHex(byte[] data) {
    int l = data.length;
    char[] out = new char[l << 1];
    // two characters form the hex value.
    for (int i = 0, j = 0; i < l; i++) {
        out[j++] = DIGITS_UPPER[(0xF0 & data[i]) >>> 4];
        out[j++] = DIGITS_UPPER[0x0F & data[i]];
    }
    return out;
  }
}
