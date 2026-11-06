package dev.rummage.net;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Wire {
    public static String pack(String... fields) { return String.join(".", Arrays.stream(fields).map(s -> Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8))).toList()); }
    public static String[] unpack(String s) { return Arrays.stream(s.split("\\.", -1)).map(f -> new String(Base64.getUrlDecoder().decode(f), StandardCharsets.UTF_8)).toArray(String[]::new); }
    public static void write(DataOutputStream out, String... fields) throws IOException {
        byte[] data = pack(fields).getBytes(StandardCharsets.UTF_8);
        if (data.length > 65536) throw new IOException("Message too large");
        synchronized (out) { out.writeInt(data.length); out.write(data); out.flush(); }
    }
    public static String[] read(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size < 0 || size > 65536) throw new IOException("Invalid message size");
        byte[] data = in.readNBytes(size);
        if (data.length != size) throw new EOFException();
        try { return unpack(new String(data, StandardCharsets.UTF_8)); } catch (IllegalArgumentException e) { throw new IOException("Invalid message", e); }
    }
    private Wire() {}
}
