#!/usr/bin/env python3
"""
QueryMind demo dataset generator — E-commerce Analytics.

Produces seed_data.sql: batched INSERT statements for a realistic,
referentially-consistent e-commerce dataset (schema.sql), sized to support
the analytics questions in the prompt (top customers, revenue trends, refund
analysis, CLV, inventory, product/supplier performance, operational KPIs).

Usage:
    pip install faker --break-system-packages
    python3 generate_seed.py > seed_data.sql
    mysql -u root -p querymind_demo < schema.sql
    mysql -u root -p querymind_demo < seed_data.sql

Row counts are tunable via the constants below; defaults land in the
50k-200k total row range requested.
"""

import random
from datetime import datetime, timedelta

from faker import Faker

fake = Faker()
Faker.seed(42)
random.seed(42)

N_SUPPLIERS = 200
N_WAREHOUSES = 6
N_CATEGORIES = 40
N_PRODUCTS = 2500
N_CUSTOMERS = 6000
N_COUPONS = 150
N_ORDERS = 18000
AVG_ITEMS_PER_ORDER = 2.6
N_REVIEWS = 9000
N_SUPPORT_TICKETS = 2200

BATCH_SIZE = 500

STATUSES_ORDER = ["PENDING", "PAID", "SHIPPED", "DELIVERED", "DELIVERED", "DELIVERED", "CANCELLED"]
PAYMENT_METHODS = ["CARD", "PAYPAL", "BANK_TRANSFER"]
SHIP_CARRIERS = ["UPS", "FedEx", "DHL", "USPS"]
CATEGORY_NAMES = [
    "Electronics", "Home & Kitchen", "Sports & Outdoors", "Books", "Toys & Games",
    "Beauty & Personal Care", "Clothing", "Footwear", "Furniture", "Office Supplies",
    "Garden & Patio", "Automotive", "Pet Supplies", "Health & Wellness", "Musical Instruments",
]


def sql_escape(value):
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("\\", "\\\\").replace("'", "\\'") + "'"


def emit_inserts(table, columns, rows):
    """Yields batched multi-row INSERT statements."""
    col_list = ", ".join(columns)
    for i in range(0, len(rows), BATCH_SIZE):
        batch = rows[i : i + BATCH_SIZE]
        values = ",\n".join(
            "(" + ", ".join(sql_escape(v) for v in row) + ")" for row in batch
        )
        print(f"INSERT INTO {table} ({col_list}) VALUES\n{values};")


def main():
    print("USE querymind_demo;")
    print("SET FOREIGN_KEY_CHECKS = 0;\n")

    # --- product_categories ---
    category_rows = []
    for i in range(1, N_CATEGORIES + 1):
        name = CATEGORY_NAMES[(i - 1) % len(CATEGORY_NAMES)]
        if i > len(CATEGORY_NAMES):
            name = f"{name} - {fake.word().capitalize()}"
        category_rows.append((i, name, None))
    emit_inserts("product_categories", ["id", "name", "parent_category_id"], category_rows)

    # --- suppliers ---
    supplier_rows = []
    for i in range(1, N_SUPPLIERS + 1):
        supplier_rows.append((
            i, fake.company(), fake.country(), fake.company_email(),
            round(random.uniform(2.0, 5.0), 2), fake.date_time_between("-5y", "-1y"),
        ))
    emit_inserts(
        "suppliers",
        ["id", "name", "country", "contact_email", "reliability_score", "created_at"],
        supplier_rows,
    )

    # --- warehouses ---
    regions = ["US-East", "US-West", "EU-Central", "APAC", "UK", "Canada"]
    warehouse_rows = [
        (i, f"{regions[i - 1]} Distribution Center", regions[i - 1], random.randint(50000, 500000))
        for i in range(1, N_WAREHOUSES + 1)
    ]
    emit_inserts("warehouses", ["id", "name", "region", "capacity_units"], warehouse_rows)

    # --- products ---
    product_rows = []
    product_prices = {}  # id -> unit_price, for order_items later
    for i in range(1, N_PRODUCTS + 1):
        cost = round(random.uniform(3, 300), 2)
        price = round(cost * random.uniform(1.3, 2.8), 2)
        product_prices[i] = price
        product_rows.append((
            i, f"SKU-{100000 + i}", fake.catch_phrase(), random.randint(1, N_CATEGORIES),
            random.randint(1, N_SUPPLIERS), cost, price,
            1 if random.random() > 0.05 else 0, fake.date_time_between("-4y", "-1M"),
        ))
    emit_inserts(
        "products",
        ["id", "sku", "name", "category_id", "supplier_id", "unit_cost", "unit_price", "is_active", "created_at"],
        product_rows,
    )

    # --- inventory (per product per warehouse) ---
    inventory_rows = []
    inv_id = 1
    for pid in range(1, N_PRODUCTS + 1):
        for wid in range(1, N_WAREHOUSES + 1):
            inventory_rows.append((
                inv_id, pid, wid, random.randint(0, 5000), random.randint(20, 200),
                fake.date_time_between("-30d", "now"),
            ))
            inv_id += 1
    emit_inserts(
        "inventory",
        ["id", "product_id", "warehouse_id", "quantity_on_hand", "reorder_threshold", "updated_at"],
        inventory_rows,
    )

    # --- customers ---
    customer_rows = []
    for i in range(1, N_CUSTOMERS + 1):
        first, last = fake.first_name(), fake.last_name()
        customer_rows.append((
            i, first, last, f"{first.lower()}.{last.lower()}{i}@{fake.free_email_domain()}",
            fake.country(), fake.date_between("-5y", "-1d"), 1 if random.random() < 0.08 else 0,
        ))
    emit_inserts(
        "customers",
        ["id", "first_name", "last_name", "email", "country", "signup_date", "is_vip"],
        customer_rows,
    )

    # --- coupons ---
    coupon_rows = []
    for i in range(1, N_COUPONS + 1):
        start = fake.date_between("-3y", "-30d")
        coupon_rows.append((
            i, f"SAVE{random.randint(10, 50)}-{fake.lexify('????').upper()}",
            random.choice([5, 10, 15, 20, 25, 30]), start, start + timedelta(days=random.randint(14, 120)),
        ))
    emit_inserts("coupons", ["id", "code", "discount_percent", "valid_from", "valid_to"], coupon_rows)

    # --- orders + order_items + payments + shipments + refunds ---
    order_rows = []
    order_item_rows = []
    payment_rows = []
    shipment_rows = []
    refund_rows = []
    order_item_id = 1
    payment_id = 1
    shipment_id = 1
    refund_id = 1

    for oid in range(1, N_ORDERS + 1):
        customer_id = random.randint(1, N_CUSTOMERS)
        warehouse_id = random.randint(1, N_WAREHOUSES)
        coupon_id = random.randint(1, N_COUPONS) if random.random() < 0.2 else None
        order_date = fake.date_time_between("-2y", "now")
        status = random.choice(STATUSES_ORDER)

        n_items = max(1, round(random.gauss(AVG_ITEMS_PER_ORDER, 1.2)))
        total = 0.0
        for _ in range(n_items):
            pid = random.randint(1, N_PRODUCTS)
            qty = random.randint(1, 4)
            price = product_prices[pid]
            total += qty * price
            order_item_rows.append((order_item_id, oid, pid, qty, price))
            order_item_id += 1

        if coupon_id:
            total *= 0.9  # approximate discount effect, fine for demo analytics
        total = round(total, 2)

        order_rows.append((oid, customer_id, warehouse_id, coupon_id, order_date, status, total))

        # payment: most orders beyond PENDING have a payment
        if status != "PENDING":
            payment_rows.append((
                payment_id, oid, total, random.choice(PAYMENT_METHODS),
                "SUCCESS" if status != "CANCELLED" else "FAILED",
                order_date + timedelta(minutes=random.randint(1, 60)) if status != "CANCELLED" else None,
            ))
            payment_id += 1

        # shipment: SHIPPED/DELIVERED orders
        if status in ("SHIPPED", "DELIVERED"):
            shipped_at = order_date + timedelta(days=random.randint(1, 3))
            delivered_at = shipped_at + timedelta(days=random.randint(1, 7)) if status == "DELIVERED" else None
            shipment_rows.append((
                shipment_id, oid, random.choice(SHIP_CARRIERS), shipped_at, delivered_at,
                "DELIVERED" if status == "DELIVERED" else "IN_TRANSIT",
            ))
            shipment_id += 1

        # refunds: ~5% of delivered orders
        if status == "DELIVERED" and random.random() < 0.05:
            refund_rows.append((
                refund_id, oid, round(total * random.uniform(0.2, 1.0), 2),
                random.choice(["Defective item", "Wrong item shipped", "No longer needed", "Late delivery", "Changed mind"]),
                order_date + timedelta(days=random.randint(8, 20)),
            ))
            refund_id += 1

    emit_inserts("orders", ["id", "customer_id", "warehouse_id", "coupon_id", "order_date", "status", "total_amount"], order_rows)
    emit_inserts("order_items", ["id", "order_id", "product_id", "quantity", "unit_price_at_purchase"], order_item_rows)
    emit_inserts("payments", ["id", "order_id", "amount", "method", "status", "paid_at"], payment_rows)
    emit_inserts("shipments", ["id", "order_id", "carrier", "shipped_at", "delivered_at", "status"], shipment_rows)
    emit_inserts("refunds", ["id", "order_id", "amount", "reason", "requested_at"], refund_rows)

    # --- reviews ---
    review_rows = []
    for i in range(1, N_REVIEWS + 1):
        rating = random.choices([1, 2, 3, 4, 5], weights=[5, 8, 15, 32, 40])[0]
        review_rows.append((
            i, random.randint(1, N_PRODUCTS), random.randint(1, N_CUSTOMERS), rating,
            fake.sentence(nb_words=12), fake.date_time_between("-2y", "now"),
        ))
    emit_inserts("reviews", ["id", "product_id", "customer_id", "rating", "comment", "created_at"], review_rows)

    # --- support_tickets ---
    ticket_rows = []
    subjects = [
        "Order not received", "Wrong item delivered", "Refund status inquiry",
        "Product defective on arrival", "Question about warranty", "Unable to apply coupon",
        "Change delivery address", "Cancel my order", "Late shipment", "Account access issue",
    ]
    for i in range(1, N_SUPPORT_TICKETS + 1):
        created = fake.date_time_between("-1y", "now")
        status = random.choice(["OPEN", "IN_PROGRESS", "RESOLVED", "RESOLVED", "CLOSED"])
        resolved = created + timedelta(days=random.randint(1, 10)) if status in ("RESOLVED", "CLOSED") else None
        ticket_rows.append((
            i, random.randint(1, N_CUSTOMERS),
            random.randint(1, N_ORDERS) if random.random() < 0.7 else None,
            random.choice(subjects), status, created, resolved,
        ))
    emit_inserts(
        "support_tickets",
        ["id", "customer_id", "order_id", "subject", "status", "created_at", "resolved_at"],
        ticket_rows,
    )

    print("\nSET FOREIGN_KEY_CHECKS = 1;")

    total_rows = (
        len(category_rows) + len(supplier_rows) + len(warehouse_rows) + len(product_rows)
        + len(inventory_rows) + len(customer_rows) + len(coupon_rows) + len(order_rows)
        + len(order_item_rows) + len(payment_rows) + len(shipment_rows) + len(refund_rows)
        + len(review_rows) + len(ticket_rows)
    )
    import sys
    print(f"-- Total rows generated: {total_rows}", file=sys.stderr)


if __name__ == "__main__":
    main()
