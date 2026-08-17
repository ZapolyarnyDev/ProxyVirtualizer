package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class MinecraftWire {

  private MinecraftWire() {}

  static int readVarInt(ByteBuffer input) {
    int value = 0;
    for (int position = 0; position < 5; position++) {
      byte current = input.get();
      value |= (current & 0x7F) << (position * 7);
      if ((current & 0x80) == 0) return value;
    }
    throw new IllegalArgumentException("VarInt exceeds five bytes");
  }

  static void writeVarInt(ByteArrayOutputStream output, int value) {
    do {
      int current = value & 0x7F;
      value >>>= 7;
      output.write(current | (value == 0 ? 0 : 0x80));
    } while (value != 0);
  }

  static String readString(ByteBuffer input) {
    int length = readVarInt(input);
    if (length < 0 || length > input.remaining())
      throw new IllegalArgumentException("Invalid string length");
    byte[] bytes = new byte[length];
    input.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  static void writeString(ByteArrayOutputStream output, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    writeVarInt(output, bytes.length);
    output.writeBytes(bytes);
  }

  static List<String> readStringList(ByteBuffer input) {
    int size = readVarInt(input);
    if (size < 0) throw new IllegalArgumentException("Negative list size");
    List<String> values = new ArrayList<>(size);
    for (int index = 0; index < size; index++) values.add(readString(input));
    return List.copyOf(values);
  }

  static void writeStringList(ByteArrayOutputStream output, List<String> values) {
    writeVarInt(output, values.size());
    values.forEach(value -> writeString(output, value));
  }

  static ByteBuffer buffer(ByteArrayOutputStream output) {
    return ByteBuffer.wrap(output.toByteArray());
  }
}
