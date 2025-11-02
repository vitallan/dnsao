package com.allanvital.dnsao;


import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.ConfLoader;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.SystemGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    public static void main( String[] args ) {
        Conf conf = ConfLoader.load();
        SystemGraph systemGraph = null;
        try {
            systemGraph = new SystemGraph(conf);
            systemGraph.start();
        } catch (ConfException exception) {
            log.error(exception.getMessage());
        } finally {
            if (systemGraph != null) {
                Runtime.getRuntime().addShutdownHook(new Thread(systemGraph::stop));
            }
        }
    }

}
