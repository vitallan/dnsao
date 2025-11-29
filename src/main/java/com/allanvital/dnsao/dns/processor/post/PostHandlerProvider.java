package com.allanvital.dnsao.dns.processor.post;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.dns.processor.post.handler.*;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.infra.notification.NotificationManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class PostHandlerProvider {

    private final List<PostHandler> handlers = new LinkedList<>();

    public PostHandlerProvider(CacheManager cacheManager,
                               NotificationManager notificationManager,
                               ResolverProvider resolverProvider,
                               Set<String> urlsToNotify) {

        handlers.add(new CachePostHandler(cacheManager));
        handlers.add(new LogPostHandler());
        handlers.add(new WinnerUpstreamHandler(resolverProvider));
        handlers.add(new NotificationPostHandler(notificationManager));
        handlers.add(new HttpListenerPostHandler(urlsToNotify));
    }

    public List<PostHandler> getPostHandlers() {
        return handlers;
    }

}
