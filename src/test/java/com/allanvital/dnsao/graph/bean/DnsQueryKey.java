package com.allanvital.dnsao.graph.bean;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.Objects;

public class DnsQueryKey {

    private final Name name;
    private final int type;
    private final int dclass;

    public DnsQueryKey(Name name, int type, int dclass) {
        this.name = name;
        this.type = type;
        this.dclass = dclass;
    }

    public static DnsQueryKey fromMessage(Message msg) {
        Record q = msg.getQuestion();
        return new DnsQueryKey(q.getName(), q.getType(), q.getDClass());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DnsQueryKey)) return false;
        DnsQueryKey that = (DnsQueryKey) o;
        return type == that.type &&
                dclass == that.dclass &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, dclass);
    }

    @Override
    public String toString() {
        return name + " " + Type.string(type) + " " + DClass.string(dclass);
    }
}