package com.allanvital.dnsao.web;

import io.javalin.http.Context;

import java.net.Inet6Address;
import java.net.InetAddress;

public class ClientIpExtractor {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    private ClientIpExtractor() {
    }

    /**
     * we do our best, but fallback to remotAddr if no header is valid
     */
    public static String extract(Context context) {
        if (context == null) {
            return null;
        }
        return extract(
                context.header(X_FORWARDED_FOR),
                context.header(X_REAL_IP),
                context.req().getRemoteAddr()
        );
    }

    public static String extract(String xForwardedFor, String xRealIp, String remoteAddr) {
        String fromXff = firstValidIpFromXff(xForwardedFor);
        if (fromXff != null) {
            return fromXff;
        }

        String fromXReal = normalizeAndValidateIpLiteral(xRealIp);
        if (fromXReal != null) {
            return fromXReal;
        }

        return normalizeAndValidateIpLiteral(remoteAddr);
    }

    private static String firstValidIpFromXff(String xForwardedFor) {
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return null;
        }

        String[] parts = xForwardedFor.split(",");
        for (String raw : parts) {
            String ip = normalizeAndValidateIpLiteral(raw);
            if (ip != null) {
                return ip;
            }
        }
        return null;
    }

    static String normalizeAndValidateIpLiteral(String raw) {
        if (raw == null) {
            return null;
        }

        String candidate = raw.trim();
        if (candidate.isEmpty()) {
            return null;
        }

        if (candidate.startsWith("[") && candidate.contains("]")) {
            int end = candidate.indexOf(']');
            candidate = candidate.substring(1, end);
        } else {
            int colon = candidate.lastIndexOf(':');
            if (colon > 0 && candidate.indexOf(':') == colon && candidate.contains(".")) {
                candidate = candidate.substring(0, colon);
            }
        }

        if (looksLikeIpv4(candidate)) {
            return candidate;
        }

        if (looksLikeIpv6(candidate)) {
            try {
                InetAddress addr = InetAddress.getByName(candidate);
                if (addr instanceof Inet6Address) {
                    return addr.getHostAddress();
                }
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    private static boolean looksLikeIpv4(String s) {
        if (s == null) {
            return false;
        }
        String[] parts = s.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String p : parts) {
            if (p.isEmpty() || p.length() > 3) {
                return false;
            }
            int v;
            try {
                v = Integer.parseInt(p);
            } catch (NumberFormatException e) {
                return false;
            }
            if (v < 0 || v > 255) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeIpv6(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        if (!s.contains(":")) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F')
                    || c == ':';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
