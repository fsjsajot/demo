# DLQ Redrive Runbook for parse-result

Queue: `parse-result.queue` 
  → DLX: `parse-result.dlx` 
  → DLQ: `parse-result.dead.queue`


## 1. Check the DLQ

Management UI → **Queues** → `parse-result.dead.queue` → note the **Ready** count.

## 2. Inspect

**Get Message(s)** on the DLQ, Ack mode = `Nack message requeue true`
Description for each headers:
  - x-first-death-reason: reason for the message to get into DLQ.
  - x-retry-count how many times the consumer retried before giving up. 0 means it never reached the consumer method at all while 5 means it went through the full retry cycle before dead-lettering.

## 3. Fix the root cause

Common causes here:
- **Malformed JSON payload** fails at binding, before the consumer method runs.
  ```
  {message: test}
  ```
- **JSON payload isn't an object** consumer expects a JSON object
  ```
  "just a string"
  ```
- Unhandled exception in `onNotificationEvent` (e.g. missing required field).
  ```
  {"message": "doc-1 processed."}
  ```

Confirm the fix is deployed before continuing.

## 4. Redrive

1. **Get Message(s)** on `parse-result.dead.queue`. Select Ack mode = `Ack message requeue false` which removes it from the DLQ
2. Copy the payload.
3. **Exchanges** → `parse-result.exchange` → **Publish message**, routing key `parse-result`, paste payload, **Publish**. This resets `x-retry-count` the redriven message gets a fresh 4 attempts (1 initial attempt, 3 retries).
4. Repeat per message.

## 5. Verify

- DLQ **Ready** count back to 0.
- App logs show the message reaching `onNotificationEvent` this time.
- DLQ stays at 0 a few minutes later if it refills immediately, the root cause isn't fixed; stop and go back to step 3.