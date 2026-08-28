## Chaos Drill: Kill Mongo mid-upload, then broker

### Expected:

- Upload and parse proceed normally
- Mongo write hangs and throw MongoTimeoutException after ~15s (temporary delay).
- Error propagates via .doOnError and the client notified of failure
- Broker kill has no effect on upload path. WebSocket notification still works, since it's independent of RabbitMQ

### Observed:
- Upload and parse will proceeded as expected
- Mongo: hung for about 15s, threw MongoTimeoutException, caught by `.doOnError`, and client notified correctly
- Broker: no impact on upload path

Conclusion: Confirmed asymmetry as expected. MongoDB server availability is a hard dependency for upload completion while RabbitMQ is not, since only the standalone Day 8 consumer touches it.