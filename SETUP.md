# Geo-Attend Setup & Installation Guide

This guide provides step-by-step instructions to set up, configure, and run all components of the **Geo-Attend** application, including the PostgreSQL database, FastAPI backend, React web dashboard, and Android mobile client.

---

## 📋 System Prerequisites

Before starting, ensure you have the following installed on your machine:

- **Git:** `v2.30+`
- **Python:** `v3.10` or higher
- **Node.js:** `v18.0` or higher (with `npm`)
- **PostgreSQL:** `v14.0` or higher
- **Android Studio:** Ladybug / Jellyfish (or latest stable release) with Android SDK 26+ (Android 8.0+)
- **Java Development Kit (JDK):** JDK 17 (bundled with Android Studio)

---

## 🛠️ Step 1: Clone the Repository

```bash
git clone https://github.com/abhiram368/Geo-Attend.git
cd Geo-Attend
```

---

## 🗄️ Step 2: Database Setup & Data Seeding

Geo-Attend uses PostgreSQL to store application data, user credentials, geofence coordinates, and attendance logs.

### 2.1 Create PostgreSQL Database
Open PostgreSQL terminal (`psql` or `pgAdmin`) and create a new database:

```sql
CREATE DATABASE geo_attend_db;
CREATE USER geo_user WITH PASSWORD 'geo_password';
GRANT ALL PRIVILEGES ON DATABASE geo_attend_db TO geo_user;
```

### 2.2 Configure Connection URL
Open [scripts/database.py](file:///c:/Projects/Geo-Attend/scripts/database.py) (or set the environment variable) and update the connection string:

```python
DATABASE_URL = "postgresql://geo_user:geo_password@localhost:5432/geo_attend_db"
```

### 2.3 Initialize Schema and Seed Data
Run the database setup scripts to create all 9 table schemas and populate them with mock users, geofences, and past attendance records:

```bash
# Recreate database tables
python scripts/recreate_db.py

# Seed initial data (Includes 50+ users, default admin, campus boundaries, and logs)
python scripts/seed_data.py
```

*Note: If you ever need to purge database records, run `python scripts/clear_data.py`.*

---

## ⚙️ Step 3: Backend API Setup (FastAPI)

The backend service handles REST API endpoints, biometric matching (YuNet/SFace), geofence validation via Haversine distance, and user authentication.

### 3.1 Navigate to Backend Directory
```bash
cd backend
```

### 3.2 Create and Activate Virtual Environment
- **Windows (PowerShell):**
  ```powershell
  python -m venv venv
  .\venv\Scripts\Activate.ps1
  ```
- **macOS / Linux:**
  ```bash
  python3 -m venv venv
  source venv/bin/activate
  ```

### 3.3 Install Dependencies
```bash
pip install fastapi uvicorn sqlalchemy psycopg2-binary pydantic opencv-python numpy
```

### 3.4 Start FastAPI Server
```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

- **API Base URL:** `http://localhost:8000`
- **Interactive Swagger Documentation:** [http://localhost:8000/docs](http://localhost:8000/docs)
- **ReDoc Documentation:** [http://localhost:8000/redoc](http://localhost:8000/redoc)

---

## 💻 Step 4: Web Dashboard Setup (React + Vite)

The web dashboard allows administrators to manage employees, geofenced boundaries, support tickets, and view real-time attendance logs.

### 4.1 Navigate to Frontend Directory
```bash
cd ../frontend
```

### 4.2 Install Node Modules
```bash
npm install
```

### 4.3 Run Development Server
```bash
npm run dev
```

- **Dashboard Application URL:** [http://localhost:5173](http://localhost:5173)

### 4.4 Build for Production (Optional)
To test the production web build:
```bash
npm run build
npm run preview
```

---

## 📱 Step 5: Mobile App Setup (Android Client)

The native Android app allows employees to clock in/out using device GPS location and biometric authentication (Fingerprint / Face Unlock).

### 5.1 Open Mobile Project in Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose the `mobile` directory (`c:\Projects\Geo-Attend\mobile`).
3. Allow Gradle to sync dependencies automatically.

### 5.2 Configure Backend Server Address
Ensure the Android app points to your running FastAPI backend server:
- Open [mobile/app/src/main/java/com/example/geoattend/Config.java](file:///c:/Projects/Geo-Attend/mobile/app/src/main/java/com/example/geoattend/Config.java).
- Set `BASE_URL`:
  - **Android Emulator:** `http://10.0.2.2:8000/`
  - **Physical Device:** `http://<YOUR_LOCAL_IP>:8000/` (e.g. `http://192.168.1.5:8000/`)

### 5.3 Build & Run
1. Connect a physical Android device (with USB Debugging enabled) or start an Android Virtual Device (AVD Emulator with API 26+).
2. Click **Run 'app'** (`Shift + F10`) in Android Studio.

---

## 🧪 Step 6: Verification & Testing

Verify that all components are communicating properly:

1. **Backend Verification:** Visit [http://localhost:8000/docs](http://localhost:8000/docs) in your browser. Ensure all REST endpoints are listed.
2. **Web Dashboard Login:** Open [http://localhost:5173](http://localhost:5173). Log in using default seeded admin credentials:
   - **Email:** `admin@nitc.ac.in`
   - **Password:** `admin123` (or as seeded in `scripts/seed_data.py`)
3. **Mobile Check-In Verification:** Open the mobile app on device/emulator, sign in as a registered user, grant location & biometric permissions, and perform a test clock-in.

---

## 🔍 Troubleshooting & FAQs

- **Database Connection Error (`psycopg2.OperationalError`):**
  - Verify PostgreSQL service is running (`pg_isready` or via Services manager on Windows).
  - Double check database name, username, and password in `scripts/database.py` and `backend/main.py`.

- **Mobile App Cannot Connect to Backend (`NetworkOnMainThreadException` or `ConnectException`):**
  - Ensure the mobile device is on the same local Wi-Fi network as the backend host machine.
  - If using Android Emulator, use `10.0.2.2` instead of `localhost`.
  - Verify backend is listening on `0.0.0.0` (`uvicorn main:app --host 0.0.0.0 --port 8000`).

- **Biometric Hardware Prompt Fails in Emulator:**
  - In Android Studio AVD Manager, edit the emulator hardware profile to enable **Fingerprint sensor**.
  - In emulator settings, register a fingerprint under **Settings -> Security -> Fingerprint**.

---

## 📚 Related Documentation

- [README.md](file:///c:/Projects/Geo-Attend/README.md) - Project Overview & Features
- [architecture.md](file:///c:/Projects/Geo-Attend/architecture.md) - System Architecture Diagram & Layer Details
- [database.md](file:///c:/Projects/Geo-Attend/database.md) - Relational Schema Specifications
