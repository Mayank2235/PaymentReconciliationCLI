"""
Program A – Real-Time Invoice Generator
Generates random invoice data every 30 seconds and POSTs to the backend API.
"""

import random
import time
import uuid
import json
import datetime
import requests

API_URL = "http://localhost:8080/api/invoices"
INTERVAL_SECONDS = 30

COMPANIES = [
    "Acme Corp", "Global Tech Ltd", "Pinnacle Solutions", "BlueSky Enterprises",
    "Vertex Trading", "Horizon Finance", "Apex Dynamics", "Sterling Group",
    "Meridian Holdings", "Summit Capital", "Delta Logistics", "Crestwood Partners"
]

# Counter to generate sequential invoice numbers across runs
_counter_start = random.randint(3000, 9000)
_seq = 0


def next_invoice_number():
    global _seq
    _seq += 1
    return f"INV-{_counter_start + _seq}"


def random_date_within_days(days=30):
    today = datetime.date.today()
    offset = random.randint(0, days)
    d = today - datetime.timedelta(days=offset)
    return d.strftime("%Y-%m-%d")


def generate_invoice():
    amount = round(random.uniform(1_000.0, 500_000.0), 2)
    # Occasionally use rounder amounts (more realistic for enterprise invoices)
    if random.random() < 0.3:
        amount = round(random.choice([
            1000, 5000, 10000, 25000, 50000, 100000, 250000
        ]) * random.uniform(0.9, 1.1), 2)
    return {
        "invoiceNumber": next_invoice_number(),
        "amount": amount,
        "date": random_date_within_days(30),
        "vendor": random.choice(COMPANIES)
    }


def send_invoices(invoices: list):
    try:
        response = requests.post(
            API_URL,
            json=invoices,
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        if response.status_code in (200, 201):
            print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] ✅ Sent {len(invoices)} invoice(s) → {[i['invoiceNumber'] for i in invoices]}")
        else:
            print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] ⚠️  Backend returned {response.status_code}: {response.text[:200]}")
    except requests.exceptions.ConnectionError:
        print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] ❌ Cannot reach backend at {API_URL} — is the Spring Boot server running?")
    except Exception as e:
        print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] ❌ Error: {e}")


def main():
    print("=" * 60)
    print("  💼  Invoice Generator — PayRecon Pro")
    print(f"  Posting to: {API_URL}")
    print(f"  Interval:   {INTERVAL_SECONDS}s")
    print("=" * 60)
    print("Press Ctrl+C to stop.\n")

    while True:
        batch_size = random.randint(2, 5)
        invoices = [generate_invoice() for _ in range(batch_size)]
        send_invoices(invoices)
        time.sleep(INTERVAL_SECONDS)


if __name__ == "__main__":
    main()
