import { sleep } from "k6";
import { joinQueue, queueStatus } from "../lib/api.js";

const EVENT_ID = __ENV.EVENT_ID;
const RATES = (__ENV.RATES || "10,25,50,100").split(",").map(Number);
const STEP_SECONDS = Number(__ENV.STEP_SECONDS || 30);
const POLLS = Number(__ENV.POLLS || 5);
const POLL_SECONDS = Number(__ENV.POLL_SECONDS || 3);
const MAX_VUS = Number(__ENV.MAX_VUS || 2000);

export const options = {
  scenarios: {
    breaking_point: {
      executor: "ramping-arrival-rate",
      startRate: 1,
      timeUnit: "1s",
      preAllocatedVUs: Math.min(200, MAX_VUS),
      maxVUs: MAX_VUS,
      stages: RATES.flatMap((rate) => [
        { target: rate, duration: `${STEP_SECONDS}s` },
        { target: rate, duration: `${STEP_SECONDS}s` },
      ]),
    },
  },
  thresholds: {
    http_req_failed: [{ threshold: "rate<0.05", abortOnFail: true, delayAbortEval: "30s" }],
    http_req_duration: [{ threshold: "p(95)<1000", abortOnFail: true, delayAbortEval: "30s" }],
  },
};

export function setup() {
  if (!EVENT_ID) {
    throw new Error("EVENT_ID is required: k6 run -e EVENT_ID=<uuid> ...");
  }
}

export default function () {
  const joined = joinQueue(EVENT_ID);
  if (!joined) {
    return;
  }

  for (let poll = 0; poll < POLLS; poll++) {
    sleep(POLL_SECONDS);
    queueStatus(EVENT_ID, joined.sessionId);
  }
}