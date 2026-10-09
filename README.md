# Geo-Attend

Geo-Attend is a secure attendance tracking system that replaces traditional attendance registers with secure geofencing and biometric authentication. The application ensures that employees can only log attendance when physically present inside verified organization premises and verified using biometric details.

## Core Features

- **Geofenced Verification:** Restricts clock-in and clock-out operations to predefined campus boundaries.
- **Biometric Authentication:** Face and fingerprint authentication validation to prevent proxy attendance.
- **Support & Feedback Desk:** Integrated support request ticket channels and feedback collection.
- **Admin Dashboard:** Dynamic Web UI to manage campus geofences, register/manage employees, review support tickets, and view detailed audit action logs.
- **Security Audit Logs:** Complete admin action audit history log tracking all modifications.

---

## Tech Stack

- **Frontend:** React.js, Tailwind CSS / Vanilla CSS, Vite.js
- **Backend:** FastAPI (Python), SQLAlchemy ORM, Pydantic
- **Database:** PostgreSQL
- **Mobile Client:** Android Native App (Java)

---

## Database Models

The database contains 9 primary model tables representing organization data, users, and logs:

1. **`InstitutionalDomain`:** Whitelists allowed email domains (e.g., `nitc.ac.in`, `gmail.com`).
2. **`User`:** Stores administrator and employee credentials, phone numbers, and biometric keys.
3. **`CampusBoundary`:** Manages active geofence zones, specifying the center coordinate (latitude/longitude) and allowance radius (meters).
4. **`AttendanceLog`:** Tracks daily shift logs, storing check-in and check-out naive timestamps, calculated distances from geofence center, and statuses (`verified`/`rejected`).
5. **`BiometricUpdateRequest`:** Manages employee requests to register or reset face/fingerprint biometric profiles.
6. **`OTPCode`:** Stores generated OTP verification codes for registration and password resets.
7. **`Feedback`:** Records system feedbacks submitted by employee users.
8. **`SupportRequest`:** Manages customer support queries, ticket statuses (`pending`/`replied`), and admin replies.
9. **`AdminActionLog`:** Stores read-only audit logs of admin database actions (additions, updates, deletions).

---

## Getting Started

### 1. Database Setup

Ensure PostgreSQL is running locally.

1. **Configure Connection:** Adjust the `DATABASE_URL` environment variable or edit the default connection string in [database.py](file:///c:/Projects/Geo-Attend/scripts/database.py#L37):
   ```python
   DATABASE_URL = "postgresql://postgres:YOUR_PASSWORD@localhost/your_db_name"
   ```
2. **Initialize Schema & Seed Mock Data:** Recreate all tables and seed the database with mock records (includes 50+ users and past-week shift logs):
   ```bash
   python scripts/recreate_db.py
   python scripts/seed_data.py
   ```

### 2. Backend Setup

1. **Navigate to backend and create virtual environment:**
   ```bash
   cd backend
   python -m venv venv
   ```
2. **Activate the environment:**
   - **Windows:** `venv\Scripts\activate`
   - **macOS/Linux:** `source venv/bin/activate`
3. **Install dependencies:**
   ```bash
   pip install fastapi uvicorn sqlalchemy psycopg2-binary pydantic
   ```
4. **Run FastAPI Server:**
   ```bash
   uvicorn main:app --reload
   ```
   API docs will be available at [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs).

### 3. Frontend Setup

1. **Navigate to frontend folder:**
   ```bash
   cd ../frontend
   ```
2. **Install modules:**
   ```bash
   npm install
   ```
3. **Run Dev Web Server:**
   ```bash
   npm run dev
   ```
   The dashboard runs at [http://localhost:5173/](http://localhost:5173/).

### 4. Mobile Client

- Locate the Android studio project in the `mobile/` directory.
- Build and run on an Android Device / Emulator.
