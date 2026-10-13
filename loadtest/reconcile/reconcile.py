import os
import sys
from collections import defaultdict

import boto3
import pymysql
from boto3.dynamodb.conditions import Key

EVENT_ID = os.environ["EVENT_ID"]
SEATS_TABLE = os.environ.get("SEATS_TABLE", "lineup-seats")
REGION = os.environ.get("AWS_REGION", "us-east-1")
DYNAMODB_ENDPOINT = os.environ.get("DYNAMODB_ENDPOINT")


def load_seats():
    options = {"region_name": REGION}
    if DYNAMODB_ENDPOINT:
        options.update(
            endpoint_url=DYNAMODB_ENDPOINT,
            aws_access_key_id="dummy",
            aws_secret_access_key="dummy",
        )
    table = boto3.resource("dynamodb", **options).Table(SEATS_TABLE)

    seats = []
    query = {
        "IndexName": "EventStatusIndex",
        "KeyConditionExpression": Key("eventId").eq(EVENT_ID),
    }
    while True:
        page = table.query(**query)
        seats.extend(page["Items"])
        if "LastEvaluatedKey" not in page:
            return seats
        query["ExclusiveStartKey"] = page["LastEvaluatedKey"]


def load_order_seats():
    connection = pymysql.connect(
        host=os.environ.get("DB_HOST", "127.0.0.1"),
        port=int(os.environ.get("DB_PORT", "3306")),
        user=os.environ.get("DB_USERNAME", "app_admin"),
        password=os.environ.get("DB_PASSWORD", "changeme"),
        database=os.environ.get("DB_NAME", "waitingroom"),
    )
    with connection, connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT BIN_TO_UUID(o.id), o.status, os.seat_id
            FROM orders o
            JOIN order_seats os ON os.order_id = o.id
            WHERE o.event_id = UUID_TO_BIN(%s)
            """,
            (EVENT_ID,),
        )
        return cursor.fetchall()


def main():
    seats = load_seats()
    order_rows = load_order_seats()

    sold_seats = {seat["id"]: seat for seat in seats if seat["status"] == "SOLD"}
    paid_orders_by_seat = defaultdict(set)
    order_statuses = {}
    for order_id, status, seat_id in order_rows:
        order_statuses[order_id] = status
        if status == "PAID":
            paid_orders_by_seat[seat_id].add(order_id)

    problems = []

    for seat_id, order_ids in paid_orders_by_seat.items():
        if len(order_ids) > 1:
            problems.append(f"DOUBLE BOOKING: seat {seat_id} is in paid orders {sorted(order_ids)}")

    for seat_id, seat in sold_seats.items():
        owner = seat.get("orderId")
        if owner not in paid_orders_by_seat.get(seat_id, set()):
            problems.append(f"Seat {seat_id} is SOLD to order {owner}, which has no PAID record for it")

    for seat_id in paid_orders_by_seat:
        if seat_id not in sold_seats:
            problems.append(f"Seat {seat_id} is in a PAID order but is not SOLD")

    status_counts = defaultdict(int)
    for status in order_statuses.values():
        status_counts[status] += 1

    print(f"Event {EVENT_ID}")
    print(f"  seats total:        {len(seats)}")
    print(f"  seats sold:         {len(sold_seats)}")
    print(f"  seats in paid orders: {len(paid_orders_by_seat)}")
    print(f"  orders by status:   {dict(status_counts)}")
    print()

    if problems:
        print(f"FAIL: {len(problems)} problem(s)")
        for problem in problems:
            print(f"  - {problem}")
        sys.exit(1)

    print("PASS: every sold seat belongs to exactly one paid order, 0 double bookings")


if __name__ == "__main__":
    main()