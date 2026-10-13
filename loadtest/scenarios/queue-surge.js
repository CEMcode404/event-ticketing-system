import { sleep } from "k6";
import { Counter, Trend } from "k6/metrics";
import { joinQueue, waitForAdmission } from "../lib/api.js";

const EVENT_ID = __ENV.EVENT_ID;
const USERS = Number(__ENV.USERS || 200);
const JOIN_WINDOW_SECONDS = Number(__ENV.JOIN_WINDOW || 30);
const POLL_SECONDS = Number(__ENV.POLL_SECONDS || 3);
const MAX_WAIT_SECONDS = Number(__ENV.MAX_WAIT || 180);

const timeToAdmission = new Trend("time_to_admission", true);
const usersAdmitted = new Counter("users_admitted");
const usersTimedOut = new Counter("users_timed_out");

export const options = {
  scenarios: {
    queue_surge: {
      executor: "per-vu-iterations",
      vus: USERS,
      iterations: 1,
      maxDuration: `${JOIN_WINDOW_SECONDS + MAX_WAIT_SECONDS + 30}s`,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{name:join}": ["p(95)<500"],
    "http_req_duration{name:status}": ["p(95)<300"],
  },
};

export function setup() {
  if (!EVENT_ID) {
    throw new Error("EVENT_ID is required: k6 run -e EVENT_ID=<uuid> ...");
  }
}

export default function () {
  sleep(Math.random() * JOIN_WINDOW_SECONDS);

  const joinedAt = Date.now();
  const joined = joinQueue(EVENT_ID);
  if (!joined) {
    return;
  }

  const admission =
    joined.status.status === "ADMITTED"
      ? joined.status
      : waitForAdmission(EVENT_ID, joined.sessionId, POLL_SECONDS, MAX_WAIT_SECONDS);

  if (admission) {
    usersAdmitted.add(1);
    timeToAdmission.add(Date.now() - joinedAt);
  } else {
    usersTimedOut.add(1);
  }
}