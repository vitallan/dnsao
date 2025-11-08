package com.allanvital.dnsao.dns.processor;

import com.allanvital.dnsao.dns.processor.engine.QueryEngine;
import com.allanvital.dnsao.dns.processor.post.PostHandlerFacade;
import com.allanvital.dnsao.dns.processor.pre.PreHandlerFacade;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryProcessorDependencies {

    private final PreHandlerFacade preHandlerFacade;
    private final QueryEngine queryEngine;
    private final PostHandlerFacade postHandlerFacade;

    public QueryProcessorDependencies(PreHandlerFacade preHandlerFacade, QueryEngine queryEngine, PostHandlerFacade postHandlerFacade) {
        this.preHandlerFacade = preHandlerFacade;
        this.queryEngine = queryEngine;
        this.postHandlerFacade = postHandlerFacade;
    }

    public PreHandlerFacade getPreHandlerFacade() {
        return preHandlerFacade;
    }

    public QueryEngine getQueryEngine() {
        return queryEngine;
    }

    public PostHandlerFacade getPostHandlerFacade() {
        return postHandlerFacade;
    }

}
