package com.allanvital.dnsao.dns.processor.post;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.dns.processor.post.handler.CachePostHandler;
import com.allanvital.dnsao.dns.processor.post.handler.LogPostHandler;
import com.allanvital.dnsao.dns.processor.post.handler.NotificationPostHandler;
import com.allanvital.dnsao.dns.processor.post.handler.PostHandler;
import com.allanvital.dnsao.infra.notification.NotificationManager;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class PostHandlerProvider {

    private final List<PostHandler> handlers = new LinkedList<>();

    public PostHandlerProvider(CacheManager cacheManager, NotificationManager notificationManager) {
        handlers.add(new CachePostHandler(cacheManager));
        handlers.add(new LogPostHandler());
        handlers.add(new NotificationPostHandler(notificationManager));
    }

    public List<PostHandler> getPostHandlers() {
        return handlers;
    }

}
