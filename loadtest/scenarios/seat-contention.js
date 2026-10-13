import { check, sleep } from "k6";
import { Counter } from "k6/metrics";
import {
  completeFakePayment,
  getSections,
  holdSeats,
  joinQueue,
  startCheckout,
  waitForAdmission,
} from "../lib/api.js";

const EVENT_ID = __ENV.EVENT_ID;
const SECTION = __ENV.SECTION;
const SHOPPERS = Number(__ENV.SHOPPERS || 100);
const JOIN_WINDOW_SECONDS = Number(__ENV.JOIN_WINDOW || 5);
const MAX_QUANTITY = Number(__ENV.MAX_QUANTITY || 4);
const HOLD_ATTEMPTS = Number(__ENV.HOLD_ATTEMPTS || 3);
const POLL_SECONDS = Number(__ENV.POLL_SECONDS || 3);
const MAX_WAIT_SECONDS = Number(__ENV.MAX_WAIT || 180);

const shoppersNotAdmitted = new Counter("shoppers_not_admitted");
const holdsWon = new Counter("holds_won");
const holdsLost = new Counter("holds_lost");
const shoppersSoldOut = new Counter("shoppers_sold_out");
const paymentsCompleted = new Counter("payments_completed");
const seatsBought = new Counter("seats_bought");

export const options = {
  scenarios: {
    seat_contention: {
      executor: "per-vu-iterations",
      vus: SHOPPERS,
      iterations: 1,
      maxDuration: `${JOIN_WINDOW_SECONDS + MAX_WAIT_SECONDS + 60}s`,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    checks: ["rate>0.99"],
    "http_req_duration{name:hold}": ["p(95)<1000"],
    "http_req_duration{name:checkout}": ["p(95)<1000"],
    "http_req_duration{name:fake_payment}": ["p(95)<1000"],
  },
};

export function setup() {
  if (!EVENT_ID || !SECTION) {
    throw new Error("EVENT_ID and SECTION are required: k6 run -e EVENT_ID=<uuid> -e SECTION=<name> ...");
  }
}

function findSection(admissionToken) {
  const res = getSections(EVENT_ID, admissionToken);
  check(res, { "sections is 200": (r) => r.status === 200 });
  if (res.status !== 200) {
    return null;
  }
  return res.json().find((s) => s.section === SECTION) || null;
}

function holdWithRetries(admissionToken, quantity) {
  for (let attempt = 1; attempt <= HOLD_ATTEMPTS; attempt++) {
    const section = findSection(admissionToken);
    if (!section || section.available < quantity) {
      shoppersSoldOut.add(1);
      return null;
    }

    const res = holdSeats(EVENT_ID, admissionToken, SECTION, quantity);
    if (res.status === 201) {
      holdsWon.add(1);
      return res.json();
    }

    holdsLost.add(1);
    sleep(0.5 + Math.random());
  }
  return null;
}

export default function () {
  sleep(Math.random() * JOIN_WINDOW_SECONDS);

  const joined = joinQueue(EVENT_ID);
  if (!joined) {
    return;
  }

  const admission =
    joined.status.status === "ADMITTED"
      ? joined.status
      : waitForAdmission(EVENT_ID, joined.sessionId, POLL_SECONDS, MAX_WAIT_SECONDS);
  if (!admission) {
    shoppersNotAdmitted.add(1);
    return;
  }

  const token = admission.admissionToken;
  const quantity = 1 + Math.floor(Math.random() * MAX_QUANTITY);

  const hold = holdWithRetries(token, quantity);
  if (!hold) {
    return;
  }

  const seatIds = hold.seats.map((seat) => seat.id);

  const checkoutRes = startCheckout(EVENT_ID, token, hold.holdToken, seatIds);
  check(checkoutRes, { "checkout is 201": (r) => r.status === 201 });
  if (checkoutRes.status !== 201) {
    return;
  }

  const paymentRes = completeFakePayment(checkoutRes.json().orderId);
  check(paymentRes, { "payment is 204": (r) => r.status === 204 });
  if (paymentRes.status === 204) {
    paymentsCompleted.add(1);
    seatsBought.add(seatIds.length);
  }
}