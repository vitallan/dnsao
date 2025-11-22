package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.HttpListenerConf;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class HttpListenerConfTest extends ConfValidation {

    @Override
    protected String getFolder() {
        return "http-listener";
    }

    private HttpListenerConf getHttpListenerConf(String file) {
        return getConf(file).getListeners();
    }

    @Test
    public void assertEmptyHttpListenerConf() {
        HttpListenerConf httpListenerConf = getHttpListenerConf("empty.yml");
        assertTrue(httpListenerConf.getHttp().isEmpty());
    }

    @Test
    public void assertWrongProtocolConf() {
        HttpListenerConf httpListenerConf = getHttpListenerConf("non-http-listener.yml");
        Set<String> urls = httpListenerConf.getHttp();
        assertFalse(urls.isEmpty());
        assertEquals(1, urls.size());
        assertTrue(urls.contains("ftp://someurl.com"));
        httpListenerConf.sanitizeHttp();
        urls = httpListenerConf.getHttp();
        assertTrue(urls.isEmpty());
    }

    @Test
    public void assertTwoHttpListener() {
        Set<String> urls = getHttpListenerConf("two-http-listener.yml").getHttp();
        assertFalse(urls.isEmpty());
        assertEquals(2, urls.size());
        assertTrue(urls.contains("http://someurl.com/endpoint"));
        assertTrue(urls.contains("https://anotherurl.com/other"));
    }

}
