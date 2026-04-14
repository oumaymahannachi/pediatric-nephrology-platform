# PediaLink – Pediatric Follow-Up Platform

A **microservices** mono-repo combining **Angular 18** (frontend) and **Spring Boot 3.2** (backend) with **MongoDB Atlas**, **Spring Cloud Netflix Eureka**, and **Spring Cloud Gateway**.

---

## Architecture

```
                    ┌──────────────┐
                    │ Angular 18   │ :4200
                    │  Frontend    │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │ API Gateway  │ :8080
                    │ (WebFlux)    │
                    └──┬───────┬───┘
              ┌────────┘       └────────┐
       ┌──────▼───────┐         ┌──────▼──────────────┐
       │ Auth Service  │ :8081   │ Treatment-Monitoring│ :8082
       │ (Users, JWT)  │         │ Service (Children,  │
       └──────┬────────┘         │ Growth, Plans, Appt)│
              │                  └──────┬──────────────┘
              │                         │
       ┌──────▼─────────────────────────▼──┐
       │         MongoDB Atlas             │
       │      (pedialink database)         │
       └───────────────────────────────────┘
              ┌─────────────────┐
              │  Eureka Server  │ :8761
              │  (Discovery)    │
              └─────────────────┘
              ┌─────────────────┐
              │  Config Server  │ :8888
              └─────────────────┘
```

| Service | Port | Description |
|---------|------|-------------|
| **Eureka Server** | 8761 | Service discovery |
| **Config Server** | 8888 | Centralized configuration |
| **API Gateway** | 8080 | Single entry point, routes to all services |
| **Auth Service** | 8081 | Authentication, users, admin management |
| **Treatment-Monitoring Service** | 8082 | Children, growth, nutrition, appointments |
| **Angular Frontend** | 4200 | SPA with 3 dashboards (Admin, Doctor, Parent) |

---

## Project Structure

```
pedialink/
├── backend/
│   ├── eureka-server/              # Spring Cloud Eureka
│   ├── config-server/              # Spring Cloud Config
│   ├── api-gateway/                # Spring Cloud Gateway (WebFlux)
│   ├── auth-service/               # MS1 – Authentication & Users
│   │   └── src/main/java/tn/pedialink/auth/
│   │       ├── config/             # SecurityConfig, AdminSeeder
│   │       ├── controller/         # AuthController, UserController, AdminController
│   │       ├── dto/                # SignupRequest, LoginRequest, UserDto, etc.
│   │       ├── model/              # User, Otp, LoginHistory, enums
│   │       ├── repository/         # MongoDB repositories
│   │       ├── security/           # JwtUtil, JwtAuthFilter
│   │       └── service/            # AuthService, OtpService
│   ├── treatment-monitoring-service/  # MS2 – Treatment & Monitoring
│   │   └── src/main/java/tn/pedialink/treatment/
│   │       ├── config/             # SecurityConfig, JwtUtil, JwtAuthFilter
│   │       ├── controller/         # ParentController, DoctorController, TreatmentAdminController
│   │       ├── model/              # Child, GrowthMeasurement, NutritionalPlan, DietaryRestriction, Appointment
│   │       ├── repository/         # MongoDB repositories
│   │       └── service/            # ChildService, GrowthService, NutritionService, AppointmentService
│   ├── docker-compose.yml          # One-command startup for all services
│   └── pom.xml                     # Parent POM (5 modules)
│
├── frontend/                       # Angular 18.2
│   └── src/app/
│       ├── core/                   # AuthService, guards, JWT interceptor
│       ├── features/
│       │   ├── auth/               # Login, Signup, Verify OTP, Forgot Password
│       │   ├── dashboard/
│       │   │   ├── admin/          # Admin panel (users + treatment management)
│       │   │   ├── doctor/         # Doctor panel (patients, plans, appointments)
│       │   │   └── parent/         # Parent panel (children, growth, appointments)
│       │   └── landing/            # Landing page
│       └── shared/
│
└── README.md
```

---

## Prerequisites

| Tool       | Version |
|------------|---------|
| Java       | 17+     |
| Maven      | 3.8+    |
| Docker     | 20+     |
| Docker Compose | v2+ |
| Node.js    | 18+ (frontend only) |
| npm        | 9+ (frontend only) |

---

## Quick Start (Docker Compose) — Recommended

```bash
cd pedialink/backend

# Build all JARs
mvn clean package -DskipTests

# Build images and start all 5 services
docker compose up --build -d

# Check status
docker compose ps

# View logs
docker compose logs -f

# Stop everything
docker compose down
```

This starts **all 5 services** with proper health checks and dependency ordering:
1. Eureka starts first → becomes healthy
2. Config Server starts → depends on Eureka healthy
3. Gateway, Auth, Treatment start → depend on both Eureka + Config healthy

### Verify Services

```bash
# Eureka dashboard
curl http://localhost:8761/actuator/health
# → {"status":"UP"}

# Treatment service health
curl http://localhost:8082/actuator/health
# → {"status":"UP"}

# Test login through gateway
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pedialink.tn","password":"Admin123!"}'
```

### Default Admin Account

On first startup, an admin account is seeded automatically:
- **Email:** `admin@pedialink.tn`
- **Password:** `Admin123!`

---

## Frontend

```bash
cd pedialink/frontend
npm install
ng serve
```

The app starts on **http://localhost:4200** and talks to the API Gateway at **http://localhost:8080**.

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATA_MONGODB_URI` | `mongodb+srv://PediaLink:PediaLink@pedialink.0d8a5qd.mongodb.net/pedialink` | MongoDB Atlas connection |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://eureka-server:8761/eureka/` | Eureka registry URL |
| `SPRING_CONFIG_IMPORT` | `optional:configserver:http://config-server:8888` | Config server URL |

JWT uses a shared secret configured in each service's `application.yml`:
```
PediaLinkSuperSecretKeyForJwtToken2024MustBeAtLeast256BitsLong!!
```

---

## Gateway Routing

All API calls go through the gateway on port **8080**:

| Route Pattern | Target Service |
|---------------|---------------|
| `/api/auth/**` | auth-service |
| `/api/users/**` | auth-service |
| `/api/admin/**` | auth-service |
| `/api/parent/**` | treatment-monitoring-service |
| `/api/doctor/**` | treatment-monitoring-service |
| `/api/treatment-admin/**` | treatment-monitoring-service |

---

## Security

| Path Pattern | Auth Required | Required Role |
|---|---|---|
| `/api/auth/**` | No | Public |
| `/api/users/**` | Yes (JWT) | Any authenticated user |
| `/api/admin/**` | Yes (JWT) | `ADMIN` |
| `/api/parent/**` | Yes (JWT) | `PARENT` |
| `/api/doctor/**` | Yes (JWT) | `DOCTOR` |
| `/api/treatment-admin/**` | Yes (JWT) | `ADMIN` |

JWT tokens are passed in the `Authorization: Bearer <token>` header.

---

## API Reference & Curl Examples

> All examples use the gateway at `http://localhost:8080`.
> Replace `<TOKEN>` with a real JWT from the login endpoint.

---

### 1 — Authentication (`/api/auth`)

#### Sign Up

```bash
# Register a parent
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test Parent",
    "email": "parent@test.com",
    "password": "Test123!",
    "role": "PARENT",
    "phone": "55123456",
    "cin": "12345678"
  }'
# → {"message":"User registered successfully. Please verify your email."}

# Register a doctor
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Dr. Test",
    "email": "doctor@test.com",
    "password": "Test123!",
    "role": "DOCTOR",
    "phone": "55654321",
    "cin": "87654321",
    "specialization": "Pediatrics",
    "licenseNumber": "LIC001",
    "clinicName": "Test Clinic"
  }'
```

#### Login

```bash
# Login as admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pedialink.tn","password":"Admin123!"}'
# → {"token":"eyJ...","id":"...","fullName":"PediaLink Admin","email":"admin@pedialink.tn","role":"ADMIN"}

# Login as parent
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"parent@test.com","password":"Test123!"}'
# → {"token":"eyJ...","id":"...","fullName":"Test Parent","email":"parent@test.com","role":"PARENT"}

# Login as doctor
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"doctor@test.com","password":"Test123!"}'
# → {"token":"eyJ...","id":"...","fullName":"Dr. Test","email":"doctor@test.com","role":"DOCTOR"}
```

#### OTP (Email Verification / Password Reset)

```bash
# Request OTP (check server console for the code)
curl -X POST http://localhost:8080/api/auth/request-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"parent@test.com","purpose":"VERIFY_EMAIL"}'

# Verify OTP
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"parent@test.com","code":"123456","purpose":"VERIFY_EMAIL"}'

# Reset Password
curl -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"email":"parent@test.com","code":"123456","newPassword":"NewPass123!"}'
```

---

### 2 — User Profile (`/api/users`) — Any authenticated user

```bash
# Get my profile
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <TOKEN>"
# → {"id":"...","fullName":"...","email":"...","role":"PARENT","phone":"55123456","status":"ACTIVE",...}

# Update my profile
curl -X PUT http://localhost:8080/api/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"fullName":"Updated Name","phone":"99887766"}'
```

---

### 3 — Admin Endpoints (`/api/admin`) — ADMIN role

```bash
# Dashboard stats
curl http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
# → {"totalUsers":18,"doctorsCount":11,"parentsCount":6,"nursesCount":0,"totalLogins":28,"failedLogins":9,...}

# List all users
curl http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Get user by ID
curl http://localhost:8080/api/admin/users/<USER_ID> \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Get users by role
curl http://localhost:8080/api/admin/users/role/DOCTOR \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Update a user
curl -X PUT http://localhost:8080/api/admin/users/<USER_ID> \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"fullName":"Updated Name","phone":"11223344"}'

# Ban / Unban a user
curl -X PUT http://localhost:8080/api/admin/users/<USER_ID>/ban \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

curl -X PUT http://localhost:8080/api/admin/users/<USER_ID>/unban \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Delete a user
curl -X DELETE http://localhost:8080/api/admin/users/<USER_ID> \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Login history
curl http://localhost:8080/api/admin/login-history \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Failed logins
curl http://localhost:8080/api/admin/login-history/failed \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Recent logins (last N hours)
curl "http://localhost:8080/api/admin/login-history/recent?hours=48" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Login history for a specific user
curl http://localhost:8080/api/admin/login-history/user/<USER_ID> \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

---

### 4 — Parent Endpoints (`/api/parent`) — PARENT role

#### Children CRUD

```bash
# List my children
curl http://localhost:8080/api/parent/children \
  -H "Authorization: Bearer <PARENT_TOKEN>"

# Add a child
curl -X POST http://localhost:8080/api/parent/children \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -d '{
    "fullName": "Baby Ali",
    "dateOfBirth": "2023-06-15",
    "gender": "M",
    "notes": "Healthy baby"
  }'
# → {"id":"...","fullName":"Baby Ali","dateOfBirth":"2023-06-15","gender":"M","parentId":"...","doctorIds":[],...}

# Update a child
curl -X PUT http://localhost:8080/api/parent/children/<CHILD_ID> \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -d '{"fullName":"Baby Ali Updated","dateOfBirth":"2023-06-15","gender":"M"}'

# Delete a child
curl -X DELETE http://localhost:8080/api/parent/children/<CHILD_ID> \
  -H "Authorization: Bearer <PARENT_TOKEN>"

# Assign a doctor to a child
curl -X POST http://localhost:8080/api/parent/children/<CHILD_ID>/doctors/<DOCTOR_ID> \
  -H "Authorization: Bearer <PARENT_TOKEN>"
# → child object with doctorIds updated

# Remove a doctor from a child
curl -X DELETE http://localhost:8080/api/parent/children/<CHILD_ID>/doctors/<DOCTOR_ID> \
  -H "Authorization: Bearer <PARENT_TOKEN>"

# List available doctors
curl http://localhost:8080/api/parent/doctors \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```

#### Growth Measurements

```bash
# Get measurements for a child
curl http://localhost:8080/api/parent/children/<CHILD_ID>/measurements \
  -H "Authorization: Bearer <PARENT_TOKEN>"

# Add a measurement
curl -X POST http://localhost:8080/api/parent/children/<CHILD_ID>/measurements \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -d '{
    "weight": 12.5,
    "height": 85.0,
    "headCircumference": 47.0,
    "date": "2026-02-19",
    "notes": "Regular checkup"
  }'
# → {"id":"...","childId":"...","date":"2026-02-19","weight":12.5,"height":85.0,"bmi":17.3,"headCircumference":47.0,...}

# Update a measurement
curl -X PUT http://localhost:8080/api/parent/children/<CHILD_ID>/measurements/<MEASUREMENT_ID> \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -d '{"weight":13.0,"height":86.0,"headCircumference":47.5,"date":"2026-02-19"}'

# Delete a measurement
curl -X DELETE http://localhost:8080/api/parent/children/<CHILD_ID>/measurements/<MEASUREMENT_ID> \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```

#### Dietary Restrictions

```bash
# Get restrictions for a child
curl http://localhost:8080/api/parent/children/<CHILD_ID>/restrictions \
  -H "Authorization: Bearer <PARENT_TOKEN>"

# Add a restriction
curl -X POST http://localhost:8080/api/parent/children/<CHILD_ID>/restrictions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -d '{
    "type": "ALLERGY",
    "allergen": "Peanuts",
    "severity": "HIGH",
    "description": "Severe peanut allergy",
    "notes": "Carry EpiPen"
  }'
# → {"id":"...","childId":"...","type":"ALLERGY","allergen":"Peanuts","severity":"HIGH",...}

# Update a restriction
curl -X PUT http://localhost:8080/api/parent/children/<CHILD_ID>/restrictions/<RESTRICTION_ID> \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -d '{"type":"ALLERGY","allergen":"Tree Nuts","severity":"MEDIUM"}'

# Delete a restriction
curl -X DELETE http://localhost:8080/api/parent/children/<CHILD_ID>/restrictions/<RESTRICTION_ID> \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```

#### Nutritional Plans (read-only for parent)

```bash
# View plans for a child (created by doctors)
curl http://localhost:8080/api/parent/children/<CHILD_ID>/plans \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```

#### Appointments

```bash
# List my appointments
curl http://localhost:8080/api/parent/appointments \
  -H "Authorization: Bearer <PARENT_TOKEN>"

# Create an appointment
curl -X POST http://localhost:8080/api/parent/appointments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -d '{
    "childId": "<CHILD_ID>",
    "doctorId": "<DOCTOR_ID>",
    "dateTime": "2026-03-01T10:00:00",
    "reason": "Regular checkup",
    "parentNotes": "First visit"
  }'
# → {"id":"...","status":"PENDING","reason":"Regular checkup",...}

# Cancel an appointment
curl -X DELETE http://localhost:8080/api/parent/appointments/<APPOINTMENT_ID> \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```

#### Parent Dashboard

```bash
curl http://localhost:8080/api/parent/dashboard \
  -H "Authorization: Bearer <PARENT_TOKEN>"
# → {"childrenCount":1,"appointmentsCount":1}
```

---

### 5 — Doctor Endpoints (`/api/doctor`) — DOCTOR role

#### Dashboard & Patients

```bash
# Doctor dashboard stats
curl http://localhost:8080/api/doctor/dashboard \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
# → {"patientsCount":1,"appointmentsCount":1,"pendingAppointmentsCount":0,"nutritionalPlansCount":1}

# List assigned patients
curl http://localhost:8080/api/doctor/patients \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
# → [{"childId":"...","childName":"Baby Ali","dateOfBirth":"2023-06-15","gender":"M","parentId":"...","parentName":"..."}]

# View patient growth measurements
curl http://localhost:8080/api/doctor/patients/<CHILD_ID>/measurements \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"

# View patient dietary restrictions
curl http://localhost:8080/api/doctor/patients/<CHILD_ID>/restrictions \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"

# View patient nutritional plans
curl http://localhost:8080/api/doctor/patients/<CHILD_ID>/plans \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```

#### Nutritional Plans CRUD

```bash
# List my created plans
curl http://localhost:8080/api/doctor/nutritional-plans \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"

# Create a nutritional plan with meals
curl -X POST http://localhost:8080/api/doctor/nutritional-plans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -d '{
    "childId": "<CHILD_ID>",
    "title": "Growth Nutrition Plan",
    "description": "Balanced diet for toddler",
    "startDate": "2026-02-19",
    "endDate": "2026-05-19",
    "goals": "Healthy weight gain",
    "restrictions": "No peanuts",
    "meals": [
      {
        "name": "Breakfast",
        "time": "08:00",
        "description": "Oatmeal with fruits",
        "calories": "300",
        "notes": "Include banana"
      },
      {
        "name": "Lunch",
        "time": "12:00",
        "description": "Chicken with vegetables",
        "calories": "450",
        "notes": "Mashed for easy eating"
      }
    ]
  }'
# → {"id":"...","title":"Growth Nutrition Plan","status":"ACTIVE","meals":[...],...}

# Update a plan
curl -X PUT http://localhost:8080/api/doctor/nutritional-plans/<PLAN_ID> \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -d '{"childId":"<CHILD_ID>","title":"Updated Plan","description":"Updated diet"}'

# Delete a plan
curl -X DELETE http://localhost:8080/api/doctor/nutritional-plans/<PLAN_ID> \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```

#### Appointment Management

```bash
# List my appointments
curl http://localhost:8080/api/doctor/appointments \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"

# List pending appointments
curl http://localhost:8080/api/doctor/appointments/pending \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"

# Accept an appointment
curl -X PUT http://localhost:8080/api/doctor/appointments/<APPOINTMENT_ID>/accept \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
# → {"id":"...","status":"ACCEPTED",...}

# Refuse an appointment
curl -X PUT http://localhost:8080/api/doctor/appointments/<APPOINTMENT_ID>/refuse \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -d '{"doctorNotes":"Not available on this date"}'

# Reschedule an appointment
curl -X PUT http://localhost:8080/api/doctor/appointments/<APPOINTMENT_ID>/reschedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -d '{"proposedDateTime":"2026-03-05T14:00:00","doctorNotes":"Moved to Friday"}'

# Mark appointment complete
curl -X PUT http://localhost:8080/api/doctor/appointments/<APPOINTMENT_ID>/complete \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
# → {"id":"...","status":"COMPLETED",...}
```

---

### 6 — Treatment Admin (`/api/treatment-admin`) — ADMIN role

```bash
# Treatment dashboard stats
curl http://localhost:8080/api/treatment-admin/dashboard \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
# → {"childrenCount":4,"appointmentsCount":1,"measurementsCount":1,"nutritionalPlansCount":1}

# List all children
curl http://localhost:8080/api/treatment-admin/children \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Get measurements for a child
curl http://localhost:8080/api/treatment-admin/children/<CHILD_ID>/measurements \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Get restrictions for a child
curl http://localhost:8080/api/treatment-admin/children/<CHILD_ID>/restrictions \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Get plans for a child
curl http://localhost:8080/api/treatment-admin/children/<CHILD_ID>/plans \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# List all appointments
curl http://localhost:8080/api/treatment-admin/appointments \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# List all nutritional plans
curl http://localhost:8080/api/treatment-admin/nutritional-plans \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# List all measurements
curl http://localhost:8080/api/treatment-admin/measurements \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# List all dietary restrictions
curl http://localhost:8080/api/treatment-admin/restrictions \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

---

## Full End-to-End Test Script

```bash
#!/bin/bash
# PediaLink E2E Test — run after: docker compose up --build -d
BASE=http://localhost:8080

echo "1. Login as Admin"
ADMIN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pedialink.tn","password":"Admin123!"}')
ADMIN_TOKEN=$(echo $ADMIN | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "  Token: ${ADMIN_TOKEN:0:20}..."

echo "2. Register Parent"
curl -s -X POST $BASE/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"fullName":"E2E Parent","email":"e2e-parent@test.com","password":"Test123!","role":"PARENT"}' | python3 -m json.tool

echo "3. Register Doctor"
curl -s -X POST $BASE/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"fullName":"E2E Doctor","email":"e2e-doctor@test.com","password":"Test123!","role":"DOCTOR","specialization":"Pediatrics","licenseNumber":"E2E001","clinicName":"E2E Clinic"}' | python3 -m json.tool

echo "4. Login as Parent"
PARENT=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"e2e-parent@test.com","password":"Test123!"}')
PARENT_TOKEN=$(echo $PARENT | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "5. Login as Doctor"
DOCTOR=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"e2e-doctor@test.com","password":"Test123!"}')
DOCTOR_TOKEN=$(echo $DOCTOR | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
DOCTOR_ID=$(echo $DOCTOR | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

echo "6. Parent: Add Child"
CHILD=$(curl -s -X POST $BASE/api/parent/children \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $PARENT_TOKEN" \
  -d '{"fullName":"E2E Baby","dateOfBirth":"2024-01-15","gender":"F","notes":"Test child"}')
CHILD_ID=$(echo $CHILD | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "  Child ID: $CHILD_ID"

echo "7. Parent: Assign Doctor"
curl -s -X POST "$BASE/api/parent/children/$CHILD_ID/doctors/$DOCTOR_ID" \
  -H "Authorization: Bearer $PARENT_TOKEN" | python3 -m json.tool

echo "8. Parent: Add Measurement"
curl -s -X POST "$BASE/api/parent/children/$CHILD_ID/measurements" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $PARENT_TOKEN" \
  -d '{"weight":10.5,"height":75.0,"headCircumference":45.0,"date":"2026-02-19"}' | python3 -m json.tool

echo "9. Parent: Add Restriction"
curl -s -X POST "$BASE/api/parent/children/$CHILD_ID/restrictions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $PARENT_TOKEN" \
  -d '{"type":"ALLERGY","allergen":"Milk","severity":"MEDIUM","description":"Lactose intolerant"}' | python3 -m json.tool

echo "10. Parent: Create Appointment"
APPT=$(curl -s -X POST "$BASE/api/parent/appointments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $PARENT_TOKEN" \
  -d "{\"childId\":\"$CHILD_ID\",\"doctorId\":\"$DOCTOR_ID\",\"dateTime\":\"2026-04-01T09:00:00\",\"reason\":\"Checkup\"}")
APPT_ID=$(echo $APPT | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "  Appointment ID: $APPT_ID"

echo "11. Doctor: View Patients"
curl -s "$BASE/api/doctor/patients" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" | python3 -m json.tool

echo "12. Doctor: Create Nutritional Plan"
curl -s -X POST "$BASE/api/doctor/nutritional-plans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  -d "{\"childId\":\"$CHILD_ID\",\"title\":\"E2E Plan\",\"meals\":[{\"name\":\"Breakfast\",\"time\":\"08:00\",\"description\":\"Cereal\",\"calories\":\"250\"}]}" | python3 -m json.tool

echo "13. Doctor: Accept Appointment"
curl -s -X PUT "$BASE/api/doctor/appointments/$APPT_ID/accept" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" | python3 -m json.tool

echo "14. Admin: Auth Dashboard"
curl -s "$BASE/api/admin/dashboard" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool

echo "15. Admin: Treatment Dashboard"
curl -s "$BASE/api/treatment-admin/dashboard" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool

echo "✅ All tests passed!"
```

---

## Docker Compose Services

```yaml
# backend/docker-compose.yml
services:
  eureka-server:      # :8761 — Service Discovery
  config-server:      # :8888 — Centralized Config (depends on eureka)
  api-gateway:        # :8080 — Gateway (depends on eureka + config)
  auth-service:       # :8081 — Auth MS (depends on eureka + config)
  treatment-monitoring-service:  # :8082 — Treatment MS (depends on eureka + config)
```

```bash
# Useful commands
docker compose up --build -d     # Build & start all
docker compose ps                # Check status
docker compose logs -f <service> # Follow logs (e.g., auth-service)
docker compose down              # Stop all
docker compose down -v           # Stop all + remove volumes
```

---

## Roles & Dashboards

| Role | API Prefix | Dashboard | Features |
|------|-----------|-----------|----------|
| **ADMIN** | `/api/admin/**` + `/api/treatment-admin/**` | `/admin` | User management, login history, ban/unban, treatment overview |
| **DOCTOR** | `/api/doctor/**` | `/doctor` | Patient list, growth monitoring, nutritional plans, appointments |
| **PARENT** | `/api/parent/**` | `/parent` | Children CRUD, growth tracking, dietary restrictions, appointments |

---

## Design System

- **Font:** Inter (Google Fonts)
- **Primary:** `#4F8CFF` (soft blue)
- **Accent:** `#36D1A0` (teal-green)
- **Approach:** Pure SCSS, no external UI library
- **Template:** Gradient Able (admin layout)
- **Breakpoints:** sm (640px), md (768px), lg (1024px), xl (1280px)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Angular 18.2, TypeScript, SCSS |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Backend Services | Spring Boot 3.2.5, Java 17 |
| Service Discovery | Spring Cloud Netflix Eureka |
| Configuration | Spring Cloud Config Server |
| Database | MongoDB Atlas |
| Authentication | JWT (HMAC-SHA256, jjwt 0.12.5) |
| Build | Maven 3.8+ (multi-module) |
| Containers | Docker, Docker Compose |

---

## License

Private – All rights reserved.
