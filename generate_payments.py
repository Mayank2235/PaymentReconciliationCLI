"""
Program B – Real-Time Payment Generator
Generates random payment data every 30 seconds and POSTs to the backend API.
Simulates realistic scenarios: exact matches, partial payments, overpayments,
missing references, and duplicate payments.
"""

import random
import time
import datetime
import requests

API_URL_PAYMENTS = "http://localhost:8080/api/payments"
API_URL_INVOICES = "http://localhost:8080/api/invoices/recent"
INTERVAL_SECONDS = 30

BANKS = [
    "HSBC", "Barclays", "Chase", "Wells Fargo", "Citibank",
    "Deutsche Bank", "BNP Paribas", "Standard Chartered", "ICICI Bank", "Axis Bank"
]

_txn_counter = random.randint(5000, 15000)
_txn_seq = 0


def next_txn_id():
    global _txn_seq
    _txn_seq += 1
    return f"TXN-{_txn_counter + _txn_seq}"


def fetch_recent_invoice_numbers() -> list:
    """Fetch recent invoice numbers from the backend to use as references."""
    try:
        resp = requests.get(API_URL_INVOICES, timeout=5)
        if resp.status_code == 200:
            data = resp.json()
            return [item.get("invoiceNumber") for item in data if item.get("invoiceNumber")]
    except Exception:
        pass
    # Fallback: use some plausible invoice numbers
    return [f"INV-{random.randint(3000, 9999)}" for _ in range(5)]


def generate_payment(known_invoice_numbers: list) -> dict:
    scenario = random.choices(
        ["exact_match", "partial", "overpayment", "no_reference", "wrong_reference"],
        weights=[45, 25, 10, 15, 5],
        k=1
    )[0]

    txn_id = next_txn_id()
    base_amount = round(random.uniform(1_000.0, 500_000.0), 2)
    pay_date = datetime.date.today().strftime("%Y-%m-%d")

    if scenario == "exact_match" and known_invoice_numbers:
        ref = random.choice(known_invoice_numbers)
        # Simulate a realistic fetch of that invoice's amount — we use a close amount
        amount = round(base_amount, 2)
        note = f"Payment for {ref} - {random.choice(BANKS)} Wire Transfer"
    elif scenario == "partial" and known_invoice_numbers:
        ref = random.choice(known_invoice_numbers)
        amount = round(base_amount * random.uniform(0.3, 0.85), 2)
        note = f"Partial payment {ref} - Instalment {random.randint(1, 3)}"
    elif scenario == "overpayment" and known_invoice_numbers:
        ref = random.choice(known_invoice_numbers)
        amount = round(base_amount * random.uniform(1.15, 1.50), 2)
        note = f"Overpayment ref {ref}"
    elif scenario == "no_reference":
        amount = round(base_amount, 2)
        note = f"{random.choice(BANKS)} inward remittance - no ref"
        ref = ""
    else:  # wrong_reference
        amount = round(base_amount, 2)
        fake_ref = f"INV-{random.randint(1, 999)}"
        note = f"Payment for {fake_ref} - incorrect reference"

    return {
        "transactionId": txn_id,
        "amount": amount,
        "paymentDate": pay_date,
        "referenceNote": note if 'note' in dir() else ""
    }


def send_payments(payments: list):
    try:
        response = requests.post(
            API_URL_PAYMENTS,
            json=payments,
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        if response.status_code in (200, 201):
            print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] ✅ Sent {len(payments)} payment(s) → {[p['transactionId'] for p in payments]}")
        else:
            print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] ⚠️  Backend returned {response.status_code}: {response.text[:200]}")
    except requests.exceptions.ConnectionError:
        print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] ❌ Cannot reach backend at {API_URL_PAYMENTS} — is the Spring Boot server running?")
    except Exception as e:
        print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] ❌ Error: {e}")


def main():
    print("=" * 60)
    print("  💳  Payment Generator — PayRecon Pro")
    print(f"  Posting to: {API_URL_PAYMENTS}")
    print(f"  Interval:   {INTERVAL_SECONDS}s")
    print("=" * 60)
    print("Press Ctrl+C to stop.\n")

    while True:
        known_refs = fetch_recent_invoice_numbers()
        batch_size = random.randint(1, 4)
        payments = [generate_payment(known_refs) for _ in range(batch_size)]
        send_payments(payments)
        time.sleep(INTERVAL_SECONDS)


if __name__ == "__main__":
    main()
