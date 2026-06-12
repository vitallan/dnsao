package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.dns.recursive.NameServerAddress;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RecursiveConcurrentRaceAllServersReceiveQueryTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String NS_HOST = "ns1.com";
    private static final String NS_IP = "127.0.0.1";
    private static final String FINAL_IP = "10.0.0.5";
    private static final String FIRST_ROOT_IP = "127.0.0.106";
    private static final String SECOND_ROOT_IP = "127.0.0.107";
    private static final long REFERRAL_TTL = 300;

    private FakeServer firstRootServer;
    private FakeServer secondRootServer;
    private CountDownLatch rootQueriesObserved;

    @Override
    protected void beforeServerStart() throws Exception {
        rootQueriesObserved = new CountDownLatch(2);

        firstRootServer = new BarrierFakeUdpServer(0, rootQueriesObserved);
        firstRootServer.start();
        trackExtraFakeServer(firstRootServer);

        secondRootServer = new BarrierFakeUdpServer(0, rootQueriesObserved);
        secondRootServer.start();
        trackExtraFakeServer(secondRootServer);
    }

    @Override
    protected List<NameServerAddress> rootHintServers() {
        return List.of(
                new NameServerAddress(FIRST_ROOT_IP, firstRootServer.getPort()),
                new NameServerAddress(SECOND_ROOT_IP, secondRootServer.getPort())
        );
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(FIRST_ROOT_IP, firstRootServer.getPort());
        stepResolverFactory.setRoute(SECOND_ROOT_IP, secondRootServer.getPort());
        stepResolverFactory.setRoute(NS_IP, fakeUpstreamServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RacingFixtureHelper helper = new RacingFixtureHelper(fakeUpstreamServer);

        Message comNsQuery = RacingFixtureHelper.buildQuery("com", Type.NS);
        Message domainNsQuery = RacingFixtureHelper.buildQuery(DOMAIN, Type.NS);
        Message domainAQuery = MessageHelper.buildARequest(DOMAIN);

        Message nsReferral = helper.buildNsReferralWithGlueResponse(comNsQuery, NS_HOST, NS_IP, REFERRAL_TTL);
        Message domainNsReferral = helper.buildNsReferralWithGlueResponse(domainNsQuery, NS_HOST, NS_IP, REFERRAL_TTL);
        Message domainAResponse = MessageHelper.buildAResponse(domainAQuery, FINAL_IP, REFERRAL_TTL);

        firstRootServer.mockResponse(comNsQuery, nsReferral);
        firstRootServer.mockResponse(domainNsQuery, domainNsReferral);
        secondRootServer.mockResponse(comNsQuery, nsReferral);
        secondRootServer.mockResponse(domainNsQuery, domainNsReferral);
        fakeUpstreamServer.mockResponse(domainAQuery, domainAResponse);
    }

    @Test
    public void allServersReceiveQuery() throws IOException {
        firstRootServer.clearReceivedQueries();
        secondRootServer.clearReceivedQueries();

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));

        assertFalse(firstRootServer.getReceivedQueries().isEmpty(),
                "First root hint server should have received at least one query");
        assertFalse(secondRootServer.getReceivedQueries().isEmpty(),
                "Second root hint server should have received at least one query");
    }

    private static final class BarrierFakeUdpServer extends FakeServer {

        private final DatagramSocket socket;
        private final Thread listenerThread;
        private final CountDownLatch receivedLatch;

        private BarrierFakeUdpServer(int port, CountDownLatch receivedLatch) throws SocketException {
            this.receivedLatch = receivedLatch;
            this.socket = new DatagramSocket(port, null);
            if (socket.getLocalPort() != this.port) {
                this.port = socket.getLocalPort();
            }
            this.listenerThread = new Thread(() -> {
                byte[] buffer = new byte[2048];
                try {
                    while (!socket.isClosed()) {
                        try {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            try {
                                socket.receive(packet);
                            } catch (SocketException e) {
                                break;
                            }

                            byte[] response = handleRequest(packet.getData(), packet.getLength());
                            if (response != null) {
                                callCounter.incrementAndGet();
                                DatagramPacket responsePacket = new DatagramPacket(
                                        response, response.length, packet.getAddress(), packet.getPort());
                                socket.send(responsePacket);
                            }

                        } catch (IOException e) {
                            if (!socket.isClosed()) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                } finally {
                    clearCallCount();
                    socket.close();
                }
            });
        }

        @Override
        public void start() {
            listenerThread.start();
        }

        @Override
        public void stop() throws Exception {
            clearCallCount();
            socket.close();
            listenerThread.join(3000);
        }

        private byte[] handleRequest(byte[] requestData, int length) {
            try {
                Message request = new Message(requestData);
                Message response = getMockedResponse(request);
                receivedLatch.countDown();
                receivedLatch.await(1, TimeUnit.SECONDS);
                if (response == null) {
                    return MessageHelper.buildServfailFrom(request).toWire();
                }
                return response.toWire();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }
}
