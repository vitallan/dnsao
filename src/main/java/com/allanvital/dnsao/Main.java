package com.allanvital.dnsao;
import com.allanvital.dnsao.infra.log.Log;


import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.ConfLoader;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.SystemGraph;


public class Main {


    public static void main( String[] args ) {
        Conf conf = ConfLoader.load();
        conf.sanitizeGroups();
        SystemGraph systemGraph = null;
        try {
            systemGraph = new SystemGraph(conf);
            systemGraph.start();
        } catch (ConfException exception) {
            Log.INFRA.error(exception.getMessage());
        } finally {
            if (systemGraph != null) {
                Runtime.getRuntime().addShutdownHook(new Thread(systemGraph::stop));
            }
        }
    }

}
