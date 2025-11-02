package com.allanvital.dnsao.dns.processor;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryProcessorFactory {

    private final QueryProcessorDependencies queryProcessorDependencies;

    public QueryProcessorFactory(QueryProcessorDependencies queryProcessorDependencies) {
        this.queryProcessorDependencies = queryProcessorDependencies;
    }

    public QueryProcessor buildQueryProcessor() {
        return new QueryProcessor(queryProcessorDependencies);
    }

}