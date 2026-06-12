package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UdpStepResolverResponseValidationTest {

    private static final String DOMAIN = "allanvital.com.";
    private static final String OTHER_DOMAIN = "otherdomain.com.";
    private static final String VALID_IP = "10.0.0.11";
    private static final String FORGED_IP = "10.0.0.12";
    private static final long TTL = 300;
    private static final int TIMEOUT_SECONDS = 1;

    private ReplyScriptUdpServer server;

    @AfterEach
    public void tearDown() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    @Test
    public void ignoresResponseFromUnexpectedSourcePort() throws Exception {
        server = new ReplyScriptUdpServer(ForgedReplyType.WRONG_SOURCE_PORT);
        server.start();

        StepResponse response = sendQuery(server.getPort());

        assertNotNull(response);
        assertEquals(VALID_IP, MessageHelper.extractIpFromResponseMessage(response.toWireMessage()));
    }

    @Test
    public void ignoresResponseWithWrongMessageId() throws Exception {
        server = new ReplyScriptUdpServer(ForgedReplyType.WRONG_MESSAGE_ID);
        server.start();

        StepResponse response = sendQuery(server.getPort());

        assertNotNull(response);
        assertEquals(VALID_IP, MessageHelper.extractIpFromResponseMessage(response.toWireMessage()));
    }

    @Test
    public void ignoresResponseWithDifferentQuestion() throws Exception {
        server = new ReplyScriptUdpServer(ForgedReplyType.WRONG_QUESTION);
        server.start();

        StepResponse response = sendQuery(server.getPort());

        assertNotNull(response);
        assertEquals(VALID_IP, MessageHelper.extractIpFromResponseMessage(response.toWireMessage()));
    }

    @Test
    public void ignoresPacketWithoutQrFlag() throws Exception {
        server = new ReplyScriptUdpServer(ForgedReplyType.MISSING_QR_FLAG);
        server.start();

        StepResponse response = sendQuery(server.getPort());

        assertNotNull(response);
        assertEquals(VALID_IP, MessageHelper.extractIpFromResponseMessage(response.toWireMessage()));
    }

    private StepResponse sendQuery(int port) throws Exception {
        UdpStepResolver resolver = new UdpStepResolver("127.0.0.1", port, TIMEOUT_SECONDS);
        StepRequest request = new StepRequest(Name.fromString(DOMAIN), Type.A, DClass.IN, DNSSecMode.OFF);
        return resolver.send(request);
    }

    private enum ForgedReplyType {
        WRONG_SOURCE_PORT,
        WRONG_MESSAGE_ID,
        WRONG_QUESTION,
        MISSING_QR_FLAG
    }

    private static final class ReplyScriptUdpServer implements AutoCloseable {

        private static final long VALID_RESPONSE_DELAY_MS = 50L;

        private final ForgedReplyType forgedReplyType;
        private final DatagramSocket serverSocket;
        private final DatagramSocket forgedSocket;
        private final Thread listenerThread;
        private volatile boolean running;

        private ReplyScriptUdpServer(ForgedReplyType forgedReplyType) throws SocketException {
            this.forgedReplyType = forgedReplyType;
            this.serverSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            this.forgedSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            this.listenerThread = new Thread(this::listen, "udp-step-validation-server");
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        public void start() {
            running = true;
            listenerThread.start();
        }

        private void listen() {
            byte[] buffer = new byte[2048];
            while (running && !serverSocket.isClosed()) {
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                try {
                    serverSocket.receive(requestPacket);
                    byte[] requestData = new byte[requestPacket.getLength()];
                    System.arraycopy(requestPacket.getData(), requestPacket.getOffset(), requestData, 0, requestPacket.getLength());
                    Message request = new Message(requestData);
                    sendForgedResponse(requestPacket, request);
                    sleep(VALID_RESPONSE_DELAY_MS);
                    send(serverSocket, requestPacket.getAddress(), requestPacket.getPort(), MessageHelper.buildAResponse(request, VALID_IP, TTL));
                } catch (SocketException e) {
                    break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void sendForgedResponse(DatagramPacket requestPacket, Message request) throws IOException {
            Message forgedResponse = buildForgedResponse(request);
            DatagramSocket socketToUse = forgedReplyType == ForgedReplyType.WRONG_SOURCE_PORT ? forgedSocket : serverSocket;
            send(socketToUse, requestPacket.getAddress(), requestPacket.getPort(), forgedResponse);
        }

        private Message buildForgedResponse(Message request) throws IOException {
            Message forgedResponse = MessageHelper.buildAResponse(request, FORGED_IP, TTL);
            if (forgedReplyType == ForgedReplyType.WRONG_MESSAGE_ID) {
                forgedResponse.getHeader().setID(request.getHeader().getID() + 1);
                return forgedResponse;
            }
            if (forgedReplyType == ForgedReplyType.WRONG_QUESTION) {
                forgedResponse.removeAllRecords(Section.QUESTION);
                forgedResponse.addRecord(Record.newRecord(Name.fromString(OTHER_DOMAIN), Type.A, DClass.IN), Section.QUESTION);
                return forgedResponse;
            }
            if (forgedReplyType == ForgedReplyType.MISSING_QR_FLAG) {
                Header header = forgedResponse.getHeader();
                header.unsetFlag(Flags.QR);
                forgedResponse.setHeader(header);
            }
            return forgedResponse;
        }

        private void send(DatagramSocket socket, InetAddress address, int port, Message response) throws IOException {
            byte[] responseWire = response.toWire();
            DatagramPacket responsePacket = new DatagramPacket(responseWire, responseWire.length, address, port);
            socket.send(responsePacket);
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void close() throws Exception {
            running = false;
            forgedSocket.close();
            serverSocket.close();
            listenerThread.join(1000);
        }
    }
}
