package com.allanvital.dnsao.dns.processor.post.handler.json;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.utils.TimeUtils;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class HttpListenerBodyBuilder {

    public JsonObject buildHttpListenerBuild(QueryEvent queryEvent) {
        JsonObject root = Json.object();
        String formattedTime = TimeUtils.formatMillis(queryEvent.getTime(), "yyyy-MM-dd HH:mm:ss.SSS");
        root.add("requestTime", formattedTime);
        root.add("queryResolvedBy", queryEvent.getQueryResolvedBy().name());
        root.add("client", queryEvent.getClient());
        root.add("type", queryEvent.getType());
        root.add("domain", queryEvent.getDomain());
        root.add("answer", queryEvent.getAnswer());
        root.add("source", queryEvent.getSource());
        root.add("elapsedTimeInMs", queryEvent.getElapsedTime());
        return root;
    }

}
