package mock.delivery;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DeliveryService {
    private final NotificationGateway gateway;
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    public DeliveryService(NotificationGateway gateway) {
        this.gateway = gateway;
    }

    public DeliveryResult deliver(PaymentEvent event) {
        if (!seen.add(event.id())) {
            return DeliveryResult.DUPLICATE;
        }

        gateway.send(event);
        return DeliveryResult.DELIVERED;
    }
}
