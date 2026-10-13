import http from "k6/http";
import { check, sleep } from "k6";

export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

const JSON_HEADERS = { "Content-Type": "application/json" };

export function joinQueue(eventId) {
  const res = http.post(`${BASE_URL}/api/queue/${eventId}/join`, null, {
    tags: { name: "join" },
  });
  check(res, { "join is 200": (r) => r.status === 200 });
  return res.status === 200 ? res.json() : null;
}

export function queueStatus(eventId, sessionId) {
  const res = http.get(`${BASE_URL}/api/queue/${eventId}/status`, {
    headers: { "X-Queue-Session": sessionId },
    tags: { name: "status" },
  });
  check(res, { "status is 200": (r) => r.status === 200 });
  return res.status === 200 ? res.json() : null;
}

export function waitForAdmission(eventId, sessionId, pollSeconds, timeoutSeconds) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  while (Date.now() < deadline) {
    const status = queueStatus(eventId, sessionId);
    if (status && status.status === "ADMITTED") {
      return status;
    }
    sleep(pollSeconds);
  }
  return null;
}

export function getSections(eventId, admissionToken) {
  return http.get(`${BASE_URL}/api/events/${eventId}/sections`, {
    headers: { "X-Admission-Token": admissionToken },
    tags: { name: "sections" },
  });
}

export function holdSeats(eventId, admissionToken, section, quantity) {
  return http.post(
    `${BASE_URL}/api/events/${eventId}/holds`,
    JSON.stringify({ section, quantity }),
    {
      headers: { ...JSON_HEADERS, "X-Admission-Token": admissionToken },
      tags: { name: "hold" },
    }
  );
}

export function startCheckout(eventId, admissionToken, holdToken, seatIds) {
  return http.post(
    `${BASE_URL}/api/events/${eventId}/checkout`,
    JSON.stringify({ holdToken, seatIds }),
    {
      headers: { ...JSON_HEADERS, "X-Admission-Token": admissionToken },
      tags: { name: "checkout" },
    }
  );
}

export function completeFakePayment(orderId) {
  return http.post(`${BASE_URL}/api/test/payments/orders/${orderId}/complete`, null, {
    tags: { name: "fake_payment" },
  });
}