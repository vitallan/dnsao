package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.List;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ValidQueryFilter implements PreHandler {

    private static final Set<Integer> ALLOWED_TYPES = Set.of(
            Type.A,
            Type.AAAA,
            Type.CNAME,
            Type.MX,
            Type.NS,
            Type.SOA,
            Type.PTR,
            Type.TXT,
            Type.SRV,
            Type.CAA,
            Type.HTTPS,
            Type.SVCB,
            Type.DNSKEY,
            Type.DS,
            Type.RRSIG,
            Type.NSEC,
            Type.NSEC3,
            Type.NSEC3PARAM,
            Type.NAPTR,
            Type.TLSA,
            Type.SSHFP
    );

    @Override
    public Message prepare(Message message) throws PreHandlerException {
        Record question = validateQuestionShape(message);
        validateQueryStructure(message, question);

        int type = question.getType();
        String typeName = Type.string(type);
        if (type == Type.ANY || !ALLOWED_TYPES.contains(type)) {
            Message refused = buildErrorResponse(question, message.getHeader().getID(), Rcode.REFUSED);
            throw new PreHandlerException(refused, "Non-allowed query type " + typeName + " to " + question.getName());
        }
        return message;
    }

    private Record validateQuestionShape(Message message) throws PreHandlerException {
        List<Record> questions = message.getSection(Section.QUESTION);
        if (questions == null || questions.size() != 1) {
            throw new PreHandlerException(
                    buildErrorResponse(message.getQuestion(), message.getHeader().getID(), Rcode.FORMERR),
                    "Malformed query: expected exactly one question"
            );
        }
        Record question = questions.get(0);
        if (question == null) {
            throw new PreHandlerException(
                    buildErrorResponse(null, message.getHeader().getID(), Rcode.FORMERR),
                    "Malformed query: missing question"
            );
        }
        return question;
    }

    private void validateQueryStructure(Message message, Record question) throws PreHandlerException {
        if (message.getHeader().getOpcode() != Opcode.QUERY) {
            throw new PreHandlerException(
                    buildErrorResponse(question, message.getHeader().getID(), Rcode.FORMERR),
                    "Malformed query: unsupported opcode " + Opcode.string(message.getHeader().getOpcode())
            );
        }
        if (question.getDClass() != DClass.IN) {
            throw new PreHandlerException(
                    buildErrorResponse(question, message.getHeader().getID(), Rcode.FORMERR),
                    "Malformed query: unsupported class " + DClass.string(question.getDClass())
            );
        }
    }

    private Message buildErrorResponse(Record question, int queryId, int rcode) {
        Message response = new Message(queryId);
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setRcode(rcode);
        if (question != null) {
            response.addRecord(question, Section.QUESTION);
        }
        return response;
    }

}
