# Java Code Comprehension Mock

## Candidate brief

A webhook worker receives payment events from a provider that may send the same event more than once. The worker passes each event to a notification gateway. Support noticed that an event can disappear after a temporary gateway failure. A second report says an event can be reported as already delivered while the first delivery attempt is still running.

Start by reading the code and running the supplied tests. Explain the current event flow and identify the cause of each report. Then make the smallest change to the delivery service that satisfies the contract below. Keep the public method and return values usable by existing callers.

## Delivery contract

- Return `DELIVERED` when this call successfully sends the event.
- Return `DUPLICATE` when an earlier call in this service instance successfully sent the event.
- Return `IN_PROGRESS` if an attempt for the same event ID is still running. This call should return without waiting or sending a second notification.
- If the gateway throws a runtime exception, let that error reach the caller and allow a later request for the same ID to try again.
- A slow delivery for one event ID should allow another ID to make progress independently.

Assume a single service instance, valid event IDs, and the same payload for repeated copies of an event ID. For the coding task, a gateway exception means the attempt did not send a notification.

## Files to read

| File | Role |
| --- | --- |
| `DeliveryService.java` | Coordinates notification delivery and tracks event IDs. |
| `NotificationGateway.java` | Represents the notification system called by the service. |
| `PaymentEvent.java` and `DeliveryResult.java` | Define the input and the outcomes visible to the caller. |
| `DeliveryServiceTest.java` | Exercises repeat delivery, retry, and concurrent requests. |

## Run the exercise

In the repository terminal, run:

```bash
bash run-tests.sh
```

The repository includes a Java 17 Codespaces configuration. For local use, install a JDK (17 or newer) and use a Bash terminal. The tests use the Java standard library. The initial run should show failures related to the incident reports.

## During the interview

1. Explain the code path and the behavior you expect before asking AI for help. Say what you want your prompt to accomplish.
2. Review the proposed change aloud. Explain which parts you would keep and which you would change, and why.
3. Run the tests. Describe what the tests demonstrate and one scenario you would investigate before deploying this service.
