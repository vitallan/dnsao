package com.allanvital.dnsao.utils;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class HashUtils {

    public static long fnv1a64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= (s.charAt(i) & 0xFF);
            h *= 0x100000001b3L;
        }
        return h;
    }

}
