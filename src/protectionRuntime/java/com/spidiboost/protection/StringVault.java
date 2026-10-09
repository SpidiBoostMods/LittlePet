package com.spidiboost.protection;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class StringVault {
    private StringVault() { }

    public static CallSite open(MethodHandles.Lookup lookup, String name, MethodType type,
                                String payload, long first, long second) {
        byte[] bytes = Base64.getDecoder().decode(payload);
        long state = first ^ Long.rotateLeft(second, 23);
        for (int i = 0; i < bytes.length; i++) {
            state += 0x9E3779B97F4A7C15L;
            long mixed = state;
            mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
            mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
            bytes[i] ^= (byte) (mixed ^ (mixed >>> 31));
        }
        String value = new String(bytes, StandardCharsets.UTF_8).intern();
        return new ConstantCallSite(MethodHandles.constant(String.class, value).asType(type));
    }
}
