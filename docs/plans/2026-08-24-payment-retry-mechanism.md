# Payment Retry Mechanism Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Retry transient payment callback failures with bounded exponential backoff and request deadlines.

**Architecture:** Add a reusable coroutine retry utility that retries only an explicit `RetryableException`. Update `PaymentPushService` to classify HTTP 5xx and I/O failures as retryable, use a derived five-second OkHttp call timeout, and impose a 30-second operation timeout. Inject its dispatcher so local tests avoid real backoff waits.

**Tech Stack:** Kotlin, kotlinx.coroutines, OkHttp, MockWebServer, JUnit 4.

---

## File structure

- Create: `app/src/main/java/vmq/util/Retry.kt` — retry configuration, marker exception, and exponential-backoff function.
- Modify: `app/src/main/java/vmq/network/PaymentPushService.kt` — bounded retry, timeout, failure classification, and dispatcher injection.
- Create: `app/src/test/java/vmq/util/RetryTest.kt` — unit coverage for retry outcomes and delay calculation.
- Modify: `app/src/test/java/vmq/network/PaymentPushServiceTest.kt` — HTTP retry/non-retry integration coverage through MockWebServer.

### Task 1: Add retry utility tests (RED)

**Files:**
- Create: `app/src/test/java/vmq/util/RetryTest.kt`

- [ ] Write tests verifying first-attempt success, recovery after `RetryableException`, final retryable failure, immediate propagation of another exception, and capped exponential delays.
- [ ] Run: `./gradlew testDebugUnitTest --tests vmq.util.RetryTest`
- [ ] Expected: compilation failure because retry utility symbols do not exist.
- [ ] Commit: `test: add retry utility reproducer`

### Task 2: Implement retry utility (GREEN)

**Files:**
- Create: `app/src/main/java/vmq/util/Retry.kt`

- [ ] Add `RetryConfig(maxAttempts, initialDelayMs, maxDelayMs)`, `delayFor(attempt)`, `RetryableException`, and `retry(config, block)`.
- [ ] Ensure only `RetryableException` is retried and the last one is thrown after all attempts.
- [ ] Run: `./gradlew testDebugUnitTest --tests vmq.util.RetryTest`
- [ ] Expected: PASS.
- [ ] Commit: `feat: add retry utility`

### Task 3: Add payment retry tests (RED)

**Files:**
- Modify: `app/src/test/java/vmq/network/PaymentPushServiceTest.kt`

- [ ] Inject `Dispatchers.Unconfined` into each service under test.
- [ ] Add MockWebServer tests: 500 then successful response sends two requests and succeeds; 400 sends one request and fails; four 500 responses send four requests and fail.
- [ ] Run: `./gradlew testDebugUnitTest --tests vmq.network.PaymentPushServiceTest`
- [ ] Expected: existing 500 expectation or new assertions fail because the service performs no application-level retry.
- [ ] Commit: `test: add payment callback retry reproducer`

### Task 4: Implement bounded payment retry (GREEN)

**Files:**
- Modify: `app/src/main/java/vmq/network/PaymentPushService.kt`

- [ ] Add injectable `CoroutineDispatcher`, defaulting to `Dispatchers.IO`.
- [ ] Derive a private OkHttp client with `callTimeout(5, TimeUnit.SECONDS)`.
- [ ] Wrap retry execution in `withTimeout(30_000L)`.
- [ ] Build the request inside each attempt, retry HTTP 5xx and `IOException`, and leave HTTP 4xx/non-retryable failures as immediate failures.
- [ ] Run: `./gradlew testDebugUnitTest --tests vmq.network.PaymentPushServiceTest`
- [ ] Expected: PASS.
- [ ] Commit: `feat: retry transient payment callbacks`

### Task 5: Verify regression suite

**Files:**
- Verify only

- [ ] Run: `./gradlew testDebugUnitTest`
- [ ] Expected: all local unit tests pass.
- [ ] Inspect: `git diff --check` and `git status --short`.

## Self-review

- Spec coverage: Tasks 1–2 implement reusable retry and deterministic delay tests. Tasks 3–4 cover 5xx, 4xx, I/O classification through the service design, dispatcher injection, five-second call timeout, and 30-second overall deadline. Task 5 verifies no regression.
- Placeholder scan: no placeholders or undefined implementation symbols remain.
- Type consistency: `RetryConfig`, `RetryableException`, and `retry` are defined in Task 2 and consumed by Task 4.
