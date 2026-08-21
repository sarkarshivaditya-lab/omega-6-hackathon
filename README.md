# OMEGA 6.0

### A simple financial workflow platform for independent workers and small service businesses

> **Hackathon Project — OMEGA 6.0**

Independent workers such as **tailors, electricians, tutors, freelancers, repair technicians, consultants, beauty professionals, and other service providers** often run their businesses without dedicated accounting software.

They may be excellent at their profession but still struggle with the administrative side of the business: creating professional quotes, preparing invoices, tracking payments, following up on outstanding dues, and understanding how much money they are actually earning.

**OMEGA 6.0 is designed to turn that fragmented workflow into one simple, mobile-first system.**

---

## The Problem

For a large number of independent workers, business administration still happens through a mixture of notebooks, spreadsheets, messaging apps, calculators, and memory.

This creates practical problems:

- Quotes are created manually and inconsistently.
- Invoices may not look professional.
- Outstanding payments are easy to forget.
- Payment follow-ups are repetitive and uncomfortable.
- Income is difficult to analyse without accounting knowledge.
- Customer information is scattered across different places.
- A worker often has to spend time acting as both the service provider and the accountant.

The result is a simple but important problem:

> **The person who knows how to do the work is forced to become an accountant just to run the business.**

OMEGA 6.0 addresses that gap by making essential business-finance workflows accessible without requiring formal accounting expertise.

---

## Our Solution

OMEGA 6.0 provides a single workflow for managing the commercial side of an independent business.

Instead of switching between multiple tools, the worker can move through a natural sequence:

**Customer → Quote → Service → Invoice → Payment → Outstanding Due → Follow-up → Income Insight**

The goal is not to build another complicated accounting package.

The goal is to make everyday business administration **simple enough to use immediately** by someone whose primary skill is their profession, not bookkeeping.

---

## Core Commercial Modules

### 1. Invoicing

Create structured, professional invoices for customers and services.

The invoice workflow supports:

- Customer details
- Selected services
- Pricing
- Discounts
- Payment status
- Payment method
- Amount received
- Outstanding balance
- Receipt generation

This transforms an informal transaction into a clear business record.

### 2. Dues & Payment Follow-up

Outstanding payments are surfaced rather than forgotten.

The intended commercial workflow allows the system to identify customers with pending balances and prepare reminders that can be sent through channels such as **WhatsApp or SMS**.

The important idea is simple:

> **A worker should not have to remember who owes them money. The system should help them remember.**

### 3. UPI Reconciliation

Modern independent businesses frequently receive digital payments.

OMEGA 6.0 is designed around the concept of matching recorded transactions with actual incoming payments so that the worker can understand:

- what was billed,
- what has been received,
- what remains outstanding,
- and which transactions still require attention.

### 4. Income Analytics

The platform converts transaction records into useful business information.

Examples include:

- Today's revenue
- Total billed amount
- Paid amount
- Pending amount
- Outstanding dues
- Customer transaction history
- Daily collection summaries

The objective is to turn raw transactions into information that helps the worker understand their business.

### 5. Quote Generation

Before work begins, a customer often needs to know the expected cost.

OMEGA 6.0 is designed to support professional quote generation so that the worker can clearly communicate:

- requested services,
- quantities or scope,
- prices,
- discounts where applicable,
- and the expected total.

This provides a clean transition from **prospective work → confirmed work → invoice → payment**.

### 6. Customer Management

Customer information becomes the foundation of the workflow rather than an isolated contact list.

The system is designed to keep transaction history connected to customers so that a worker can quickly answer questions such as:

- What did this customer purchase?
- What did I charge them?
- Have they paid?
- Do they still owe money?
- When was their previous transaction?

---

## Why OMEGA 6.0?

Most accounting software is designed around accounting concepts.

OMEGA 6.0 is designed around the **worker's actual workflow**.

A tailor thinks in terms of:

> Customer → Order → Service → Price → Payment

An electrician thinks in terms of:

> Customer → Job → Materials/Services → Quote → Invoice → Payment

A tutor thinks in terms of:

> Student/Customer → Session → Fee → Payment → Outstanding

A freelancer thinks in terms of:

> Client → Project → Deliverable → Quote → Invoice → Payment

The underlying commercial workflow is remarkably similar.

OMEGA 6.0 therefore focuses on the common business layer rather than forcing each profession into a specialised accounting product.

---

## Product Philosophy

### Simple enough for first-time users

The interface should feel familiar to someone who has never used accounting software.

### Profession-agnostic

The platform should work for a tailor, electrician, tutor, freelancer, consultant, or other independent worker without requiring a different application for each profession.

### Mobile-first

Independent workers frequently operate away from a desk. The workflow is therefore designed around quick actions that can be performed from a phone.

### Transaction-first

The most important object is the transaction and its relationship with a customer, services, billing, and payment.

### Offline-friendly foundations

The current Android implementation uses local storage and local-first workflows so core records remain usable without depending on a continuous internet connection.

---

## Current Prototype

The current Android prototype demonstrates the foundational commercial workflow through:

- OMEGA 6.0 branded onboarding
- Customer creation
- Customer history
- Service catalogue management
- Service pricing
- Discounts
- Billing
- Payment status tracking
- Payment method tracking
- Amount received and balance due
- Receipt/PDF generation
- Daily collection reporting
- CSV export
- Local database backup and restore
- Local-only data storage

The prototype has deliberately been stripped of the original diagnostic-centre-specific terminology and medical test catalogue so the underlying workflow can be presented as a **general-purpose commercial platform**.

---

## Architecture

The Android prototype is built using a native Android architecture with:

- **Kotlin**
- **Jetpack Compose** for UI
- **Material 3** components
- **Room** for local persistence
- **Hilt** for dependency injection
- **StateFlow / Coroutines** for reactive state and asynchronous operations
- Local PDF generation for receipts/reports

The design keeps the persistence and workflow layers separate from the UI so the same core commercial model can be extended into additional channels and integrations later.

---

## High-Level Workflow

```text
                 ┌──────────────────┐
                 │     Customer     │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │      Quote       │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │    Services      │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │     Invoice      │
                 └────────┬─────────┘
                          │
                 ┌────────┴─────────┐
                 ▼                  ▼
          ┌──────────────┐   ┌──────────────┐
          │   Payment    │   │  Outstanding │
          └──────┬───────┘   └──────┬───────┘
                 │                  │
                 │                  ▼
                 │           ┌──────────────┐
                 │           │   Reminder   │
                 │           └──────┬───────┘
                 │                  │
                 └──────────┬───────┘
                            ▼
                    ┌──────────────┐
                    │    Income    │
                    │   Analytics  │
                    └──────────────┘
```

---

## Commercial Potential

The commercial opportunity is not limited to one profession.

The same core workflow can support multiple independent-worker segments:

| Segment | Example use case |
|---|---|
| Tailor | Quote garments, invoice customers, track unpaid orders |
| Electrician | Quote jobs, record service charges, track pending payments |
| Tutor | Track sessions, fees, customers and outstanding balances |
| Freelancer | Quote projects, invoice clients and track receivables |
| Repair Technician | Record jobs, service charges and payment status |
| Consultant | Create quotes, invoices and customer histories |
| Beauty Professional | Manage service bills, customer history and dues |

This creates a potential **horizontal SaaS platform** rather than a single-purpose application.

---

## Suggested Future Extensions

The prototype establishes the commercial foundation. The next layer would connect it to external services and deeper analytics.

### WhatsApp / SMS reminders

Automated or one-tap payment reminders for overdue invoices.

### UPI reconciliation

Link invoice records with incoming UPI transactions and automatically mark matching payments.

### Advanced analytics

Provide trends such as:

- revenue over time,
- repeat customers,
- average transaction value,
- outstanding receivables,
- best-performing services,
- and cash-flow patterns.

### Quote-to-invoice conversion

Allow a customer quote to become an invoice without re-entering information.

### Multi-device sync

Synchronise business records across devices while retaining an offline-capable experience.

### Profession templates

Offer optional templates for common workflows while keeping the core platform generic.

---

## Demonstration Flow for Judges

A short demonstration can show the entire concept in under a few minutes:

1. Launch **OMEGA 6.0**.
2. Enter the worker's name during onboarding.
3. Create a customer.
4. Select services and set prices.
5. Apply a discount if necessary.
6. Generate the bill.
7. Record whether the customer has paid fully, partially, or not at all.
8. Show the resulting customer history and outstanding amount.
9. Generate the receipt/report.
10. Show the dashboard summary.

This demonstrates the core value proposition without requiring the judge to understand accounting software first.

---

## Getting Started

### Requirements

- Android Studio or a compatible Android build environment
- JDK compatible with the project's Gradle configuration
- Android SDK with **API 35** available

### Build the APK

```bash
./gradlew assembleDebug
```

The generated debug APK is located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Install directly on a connected Android device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Project Structure

```text
app/
└── src/main/java/com/udc/collection/
    ├── data/
    │   ├── local/
    │   └── repository/
    ├── domain/
    │   └── model/
    ├── ui/
    │   ├── components/
    │   ├── navigation/
    │   ├── screen/
    │   │   ├── home/
    │   │   ├── onboarding/
    │   │   ├── history/
    │   │   ├── catalogue/
    │   │   ├── patient/
    │   │   └── settings/
    │   └── theme/
    └── util/
```

> **Implementation note:** Some internal Kotlin class and package names still reflect the application's original prototype terminology. These are implementation details and do not affect the general-purpose user-facing OMEGA 6.0 workflow.

---

## Hackathon Positioning

OMEGA 6.0 is not trying to replace enterprise accounting software.

It targets the underserved layer between:

**"I run a small service business"**

and

**"I need a full accounting department."**

The product's value comes from reducing the administrative burden around everyday transactions while keeping the workflow understandable to someone with no accounting background.

---

## Vision

> **Make professional business management as easy as taking an order.**

A worker should be able to open one app, record the work, send the customer a professional quote or invoice, know exactly what has been paid, know what is still owed, and understand how their business is performing.

That is the direction of **OMEGA 6.0**.

---

## Team

**OMEGA 6.0 Hackathon Project**

Built as a prototype for a college hackathon with a focus on applying a general commercial workflow to independent workers and small service businesses.
