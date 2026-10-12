package com.github.catvod.spider;
import java.util.Map;
public final class Proxy {
    public static Object[] proxy(Map<String, String> params) { return new Object[] {200, "text/plain", "fixture"}; }
}
