package com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo;

import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveResult {

    public enum Status {
        ANSWER,
        SERVFAIL
    }

    private final Status status;
    private final Message finalMessage;
    private final String note;

    private RecursiveResult(Status status, Message finalMessage, String note) {
        this.status = status;
        this.finalMessage = finalMessage;
        this.note = note;
    }

    public static RecursiveResult answer(Message message) {
        return new RecursiveResult(Status.ANSWER, message, null);
    }

    public static RecursiveResult servfail(Message message, String note) {
        return new RecursiveResult(Status.SERVFAIL, message, note);
    }

    public Status getStatus() {
        return status;
    }

    public Message getFinalMessage() {
        return finalMessage;
    }

    public String getNote() {
        return note;
    }
}
