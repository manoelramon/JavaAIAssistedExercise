package mock.delivery;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class DeliveryServiceTest {
    private static final int TIMEOUT_SECONDS = 5;
    private static final PaymentEvent PAYMENT = new PaymentEvent("evt-001", "Payment received");
    private static final PaymentEvent OTHER_PAYMENT = new PaymentEvent("evt-002", "Another payment");

    public static void main(String[] args) {
        int failures = 0;
        failures += run("delivered event is suppressed", DeliveryServiceTest::suppressesDeliveredEvent);
        failures += run("failed delivery can be retried", DeliveryServiceTest::retriesFailedDelivery);
        failures += run("overlapping duplicate reports in progress", DeliveryServiceTest::reportsInProgress);
        failures += run("different event ID can make progress", DeliveryServiceTest::allowsIndependentDelivery);
        System.out.printf("%n%d passed, %d failed%n", 4 - failures, failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void suppressesDeliveredEvent() {
        AtomicInteger calls = new AtomicInteger();
        DeliveryService service = new DeliveryService(event -> calls.incrementAndGet());

        expect(DeliveryResult.DELIVERED, service.deliver(PAYMENT));
        expect(DeliveryResult.DUPLICATE, service.deliver(PAYMENT));
        expect(1, calls.get());
    }

    private static void retriesFailedDelivery() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeException outage = new IllegalStateException("gateway unavailable");
        DeliveryService service = new DeliveryService(event -> {
            if (calls.incrementAndGet() == 1) {
                throw outage;
            }
        });

        try {
            service.deliver(PAYMENT);
            throw new AssertionError("Expected the gateway error to reach the caller");
        } catch (IllegalStateException exception) {
            expect(outage, exception);
        }

        expect(DeliveryResult.DELIVERED, service.deliver(PAYMENT));
        expect(DeliveryResult.DUPLICATE, service.deliver(PAYMENT));
        expect(2, calls.get());
    }

    private static void reportsInProgress() throws Exception {
        try (BlockedDelivery scenario = new BlockedDelivery()) {
            Future<DeliveryResult> first = scenario.start(PAYMENT);
            await(scenario.gatewayEntered, "first attempt did not reach the gateway");

            Future<DeliveryResult> duplicate = scenario.start(PAYMENT);
            expect(DeliveryResult.IN_PROGRESS, duplicate.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            expect(1, scenario.firstEventCalls.get());

            scenario.releaseGateway.countDown();
            expect(DeliveryResult.DELIVERED, first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            expect(DeliveryResult.DUPLICATE, scenario.service.deliver(PAYMENT));
        }
    }

    private static void allowsIndependentDelivery() throws Exception {
        try (BlockedDelivery scenario = new BlockedDelivery()) {
            Future<DeliveryResult> first = scenario.start(PAYMENT);
            await(scenario.gatewayEntered, "first attempt did not reach the gateway");

            Future<DeliveryResult> independent = scenario.start(OTHER_PAYMENT);
            expect(DeliveryResult.DELIVERED, independent.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            scenario.releaseGateway.countDown();
            expect(DeliveryResult.DELIVERED, first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    private static final class BlockedDelivery implements AutoCloseable {
        private final CountDownLatch gatewayEntered = new CountDownLatch(1);
        private final CountDownLatch releaseGateway = new CountDownLatch(1);
        private final AtomicInteger firstEventCalls = new AtomicInteger();
        private final ExecutorService threads = Executors.newFixedThreadPool(2);
        private final DeliveryService service = new DeliveryService(event -> {
            if (PAYMENT.id().equals(event.id()) && firstEventCalls.incrementAndGet() == 1) {
                gatewayEntered.countDown();
                await(releaseGateway, "test did not release the gateway");
            }
        });

        private Future<DeliveryResult> start(PaymentEvent event) {
            return threads.submit(() -> service.deliver(event));
        }

        @Override
        public void close() {
            releaseGateway.countDown();
            threads.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch, String failure) {
        try {
            if (!latch.await(TIMEOUT_SECONDS * 2, TimeUnit.SECONDS)) {
                throw new AssertionError(failure);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Test was interrupted", exception);
        }
    }

    private static void expect(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static int run(String name, TestCase test) {
        try {
            test.run();
            System.out.println("PASS " + name);
            return 0;
        } catch (Exception | AssertionError failure) {
            System.out.println("FAIL " + name + ": " + failure);
            return 1;
        }
    }

    @FunctionalInterface
    private interface TestCase {
        void run() throws Exception;
    }
}
