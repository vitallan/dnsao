package com.allanvital.dnsao.infra.log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SystemOutHandler extends StreamHandler {

    public SystemOutHandler(Formatter formatter) {
        setFormatter(formatter);
        setOutputStream(System.out);
    }

    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

}
