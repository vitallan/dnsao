package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.exc.PreHandlerException;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import static org.junit.jupiter.api.Assertions.*;

class ValidQueryFilterTest {

    private final ValidQueryFilter filter = new ValidQueryFilter();

    @Test
    void shouldAcceptAllowedTypes() throws Exception {
        assertDoesNotThrow(() -> filter.prepare(buildQuery(Type.A, DClass.IN)));
        assertDoesNotThrow(() -> filter.prepare(buildQuery(Type.HTTPS, DClass.IN)));
        assertDoesNotThrow(() -> filter.prepare(buildQuery(Type.SVCB, DClass.IN)));
        assertDoesNotThrow(() -> filter.prepare(buildQuery(Type.DNSKEY, DClass.IN)));
        assertDoesNotThrow(() -> filter.prepare(buildQuery(Type.DS, DClass.IN)));
        assertDoesNotThrow(() -> filter.prepare(buildQuery(Type.TLSA, DClass.IN)));
    }

    @Test
    void shouldRefuseAnyQueries() {
        PreHandlerException exception = assertThrows(PreHandlerException.class, () -> filter.prepare(buildQuery(Type.ANY, DClass.IN)));

        assertEquals(Rcode.REFUSED, exception.getPreparedResponse().getRcode());
        assertEquals(Type.ANY, exception.getPreparedResponse().getQuestion().getType());
    }

    @Test
    void shouldRefuseUnsupportedTypes() {
        PreHandlerException exception = assertThrows(PreHandlerException.class, () -> filter.prepare(buildQuery(Type.AXFR, DClass.IN)));

        assertEquals(Rcode.REFUSED, exception.getPreparedResponse().getRcode());
        assertEquals(Type.AXFR, exception.getPreparedResponse().getQuestion().getType());
    }

    @Test
    void shouldReturnFormerrForNonInClass() {
        PreHandlerException exception = assertThrows(PreHandlerException.class, () -> filter.prepare(buildQuery(Type.A, DClass.CH)));

        assertEquals(Rcode.FORMERR, exception.getPreparedResponse().getRcode());
    }

    @Test
    void shouldReturnFormerrForNonQueryOpcode() throws Exception {
        Message message = buildQuery(Type.A, DClass.IN);
        message.getHeader().setOpcode(Opcode.STATUS);

        PreHandlerException exception = assertThrows(PreHandlerException.class, () -> filter.prepare(message));

        assertEquals(Rcode.FORMERR, exception.getPreparedResponse().getRcode());
    }

    @Test
    void shouldReturnFormerrWhenQuestionIsMissing() {
        Message message = new Message(200);

        PreHandlerException exception = assertThrows(PreHandlerException.class, () -> filter.prepare(message));

        assertEquals(Rcode.FORMERR, exception.getPreparedResponse().getRcode());
        assertNull(exception.getPreparedResponse().getQuestion());
    }

    @Test
    void shouldReturnFormerrWhenThereAreMultipleQuestions() throws Exception {
        Message message = buildQuery(Type.A, DClass.IN);
        message.addRecord(Record.newRecord(Name.fromString("example.net."), Type.AAAA, DClass.IN), Section.QUESTION);

        PreHandlerException exception = assertThrows(PreHandlerException.class, () -> filter.prepare(message));

        assertEquals(Rcode.FORMERR, exception.getPreparedResponse().getRcode());
    }

    private Message buildQuery(int type, int dclass) throws Exception {
        return Message.newQuery(Record.newRecord(Name.fromString("example.com."), type, dclass));
    }
}
