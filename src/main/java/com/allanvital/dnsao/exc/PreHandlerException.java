package com.allanvital.dnsao.exc;

import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class PreHandlerException extends Exception {

    private Message preparedResponse;

    public PreHandlerException(Message preparedResponse, String message) {
        super(message);
        this.preparedResponse = preparedResponse;
    }

    public PreHandlerException(String message) {
        super(message);
    }

    public Message getPreparedResponse() {
        return preparedResponse;
    }

}
