package mock.delivery;

@FunctionalInterface
public interface NotificationGateway {
    void send(PaymentEvent event);
}
