package com.allanvital.dnsao.dns.processor.engine.unit.upstream;

import com.allanvital.dnsao.dns.processor.engine.pojo.DnsQueryResult;
import org.xbill.DNS.Message;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryExecutor {

    private final TaskProvider taskProvider;
    private final TaskExecutorAndDecider executorAndDecider;
    private final int maxRetries;

    public QueryExecutor(TaskProvider taskProvider, TaskExecutorAndDecider executorAndDecider, int maxRetries) {
        this.taskProvider = taskProvider;
        this.executorAndDecider = executorAndDecider;
        this.maxRetries = maxRetries;
    }

    public DnsQueryResult executeWithRetry(Message query) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            try {
                List<Callable<DnsQueryResult>> queryTasks = taskProvider.buildTasksToExecute(query);
                return executorAndDecider.executeAndPickResult(queryTasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                //ignore to retry
            }
        }
        return null;
    }

}
