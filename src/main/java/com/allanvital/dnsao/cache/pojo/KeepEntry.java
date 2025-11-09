package com.allanvital.dnsao.cache.pojo;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import java.util.Objects;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class KeepEntry {

    private final Name name;
    private final int type;
    private final Record record;

    public KeepEntry(Record record) {
        this.record = record;
        this.name = record.getName();
        this.type = record.getType();
    }

    public Name getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        KeepEntry keepEntry = (KeepEntry) o;
        return type == keepEntry.type && Objects.equals(name, keepEntry.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    public Record getRecord() {
        return record;
    }

}
