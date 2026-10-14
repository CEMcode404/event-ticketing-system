# Load test results: 2026-10-14

All runs used k6 on an EC2 runner in the same VPC, hitting the API's private IP (no CloudFront). Times in screenshots are UTC.

| Component | Setup |
|---|---|
| API | EC2 `t4g.small` (2 vCPU, 2 GB), profiles `prod,loadtest` (fake payments) |
| MySQL | RDS `db.t4g.micro` |
| Redis | ElastiCache `cache.t4g.micro`, TLS |
| Seats | DynamoDB on-demand |
| Load generator | EC2 `c7g.xlarge`, k6 v2.3.0 |
| Waiting room | max 500 active shoppers, admit 50 every 5 s, admission TTL 10 min |

Each `.txt` file holds the k6 summary for one run; the first lines record the command and the change being tested. Progress output and shell noise were removed.

## Scenario 1: queue surge

10,000 users join within 60 s, poll every 3 s, give up after 180 s. Only 500 can be admitted, so ~9,500 time out waiting by design.

| | [Baseline](s1-baseline.txt) | [Fix 1: Tomcat limits](s1-fix1-tomcat.txt) | [Fix 2: event cache](s1-fix2-event-cache.txt) |
|---|---|---|---|
| Failed joins | 1,622 (16%) | 0 | 0 |
| join p95 / median | 1.39 s / 272 ms | 899 ms / 72 ms | **47 ms / 9.6 ms** |
| status p95 | 137 ms | 71 ms | **25 ms** |
| Requests | 489,517 (~1,750/s) | 587,034 (~2,440/s) | 585,250 (~2,430/s) |
| Thresholds | ✗ | ✗ (join) | ✓ |

- **Baseline:** joins failed with `dial: i/o timeout` once ~8,378 users were connected, matching Tomcat's defaults (`max-connections=8192` + `accept-count=100`). App logs showed no errors. See `images/s1-baseline-dial-timeouts.png`.
- **Fix 1:** connections reached 10,000 and failures disappeared; Java CPU sat at ~175–200% of 200% during the join surge. See `images/s1-fix1-monitor.png`.
- **Fix 2:** join no longer queries MySQL on every call (5 s in-process cache). See `images/s1-fix2-monitor.png`.

## Scenario 2: seat contention

500 shoppers want 1–4 seats each (~1,250 wanted) from one section of 1,000 seats.

| | [Cold](s2-cold.txt) | [Warm](s2-warm.txt) | [Fix 3: Apache client](s2-fix3-apache-client.txt) |
|---|---|---|---|
| hold / checkout / payment p95 | 3.84 s / 6.23 s / 2.78 s | 781 / 525 / 301 ms | 482 / 414 / 303 ms |
| Slowest request | 9.31 s | 1.25 s | 714 ms |
| Seats sold (reconciled) | 924 | 935 | 934 |
| Orders | 404 PAID, 2 REFUNDED | 406 PAID | 413 PAID |
| Double bookings | **0** | **0** | **0** |
| Reconciliation | [s2-cold-reconcile](s2-cold-reconcile.txt) | [s2-warm-reconcile](s2-warm-reconcile.txt) | [s2-fix3-reconcile](s2-fix3-reconcile.txt) |

- **Cold vs warm:** same code; the cold run was the first shop traffic after a restart. Its slow checkouts let 2 holds expire before payment, and fulfillment refunded those orders instead of double-selling. k6 counted 929 seats bought; the reconciliation found 924 sold plus 2 refunded orders.
- **Fix 3:** before, ~380 HTTPS connections to DynamoDB ended in `TIME_WAIT` per run with ~7 open (`images/s2-warm-monitor-https-churn.png`); after, 0–32 `TIME_WAIT` with ~35 open and reused (`images/s2-fix3-monitor.png`). Tail latency improved; medians did not in this single run. Java CPU still showed single 200% samples during admission bursts, so no CPU improvement is claimed.

## Scenario 3: breaking point

Arrival rate ramps 50 → 100 → 200 → 400 → 800 new users/s (2 min per step); each user joins and polls 5 times. See [s3-breaking-point.txt](s3-breaking-point.txt).

- 851,814 requests, **0 errors**, p95 844 ms (threshold 1 s): the run never aborted
- Peak ~12,000 concurrent users and ~3,700 req/s; Java CPU 120–187% (`images/s3-monitor.png`)
- `dropped_iterations: 20,059`: as requests slowed, iterations grew from 15 s to ~20 s and k6 hit its 12,000-VU cap, so the effective peak arrival rate was ~600–650/s rather than 800/s
- The exact failure point was not reached; the runner size was limited by the account's vCPU quota

## Images

| File | Shows |
|---|---|
| `s1-baseline-k6-progress.png` | k6 progress during the baseline run |
| `s1-baseline-dial-timeouts.png` | The `dial: i/o timeout` errors on join |
| `s1-baseline-cpu-avg-5min.png` | CloudWatch CPU, 5-min average (~46%): hides the surge |
| `s1-fix1-monitor.png` | Connections passing 8,192 to 10,000; CPU near 200% |
| `s1-fix1-cpu-p99.png` | CloudWatch CPU p99 for baseline (~80%) and Fix 1 (~85%) |
| `s1-fix1-cpu-avg-5min.png` | CloudWatch CPU, 5-min average, baseline and Fix 1 |
| `s1-fix2-monitor.png` | Connections and CPU during the Fix 2 run |
| `s1-fix2-cpu-avg.png` | CloudWatch CPU average including the Fix 2 run |
| `cpu-0620-0715-s1-fix1-fix2-s2-cold.png` | CPU across S1 Fix 1 (~06:30), S1 Fix 2 (~06:55), S2 cold (~07:05) |
| `cpu-p99-overview-0640-0735.png` | CPU p99: S1 Fix 2 (~73%), S2 runs (~30–38%), S2 Fix 3 warm-up + measured in one window (~55%) |
| `s2-cold-cpu-0705.png` | CloudWatch CPU around the cold Scenario 2 run |
| `s2-warm-monitor-https-churn.png` | `TIME_WAIT` climbing to ~380 with ~7 open connections |
| `s2-warm-reconcile.png` | Reconciliation output for the warm run |
| `s2-fix3-monitor.png` | `TIME_WAIT` near 0, ~35 connections held open |
| `s2-fix3-cpu-avg.png` | CloudWatch CPU average including the Fix 3 runs |
| `s3-monitor.png` | Connections climbing to 12,000 with CPU |
| `s3-cpu-p99.png` | CloudWatch CPU p99, peak ~80% during Scenario 3 |
| `s3-cpu-avg.png` | CloudWatch CPU average during Scenario 3 |

## Caveats

- Each configuration was measured once (twice for Scenario 2 warm); run-to-run variance is visible and not quantified.
- CloudWatch used basic monitoring (5-minute points). Averages hide short spikes; the per-5-second monitor output is the precise source.
- Monitor CPU readings are single `top` snapshots, out of 200% for 2 vCPUs.
