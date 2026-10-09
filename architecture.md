# Geo-Attend System Architecture

```mermaid
graph TD
    %% Define Styles
    classDef client fill:#d4ebf2,stroke:#333,stroke-width:2px;
    classDef gateway fill:#f9e79f,stroke:#333,stroke-width:2px;
    classDef logic fill:#d5f5e3,stroke:#333,stroke-width:2px;
    classDef storage fill:#f5cba7,stroke:#333,stroke-width:2px;

    %% Client Layer
    subgraph Client_Layer [Client Layer]
        A[Android Mobile App: Java]:::client
        B[Web Dashboard: React.js]:::client
    end

    %% API / Auth Layer
    subgraph Auth_Gateway [Backend Entry & Auth]
        C[FastAPI Backend REST API]:::gateway
        D[Google OAuth 2.0 / Password Login]:::gateway
        OTP[OTP Verification Engine]:::gateway
    end

    %% Core Services
    subgraph Core_Logic [Core Logic Layer]
        E[Geographic Boundary Validation Engine]:::logic
        F[Biometric Authentication Engine]:::logic
    end

    %% Storage Layer
    subgraph Storage_Layer [Data Storage Layer]
        G[(PostgreSQL Database)]:::storage
        
        %% Database Tables
        T1[Institutional Domains]:::storage
        T2[Users]:::storage
        T3[Campus Boundaries]:::storage
        T4[Attendance Logs]:::storage
        T5[Biometric Update Requests]:::storage
        T6[OTP Codes]:::storage
        T7[Feedbacks]:::storage
        T8[Support Requests]:::storage
        T9[Admin Action Logs]:::storage
    end

    %% Connections & Flow
    A -->|1. Submit GPS coords & biometrics| C
    B -->|Manage geofences, view logs, reply support| C
    
    C -->|Authenticate domain/login| D
    C -->|Generate/verify OTP| OTP
    
    C -->|Geofence matching| E
    C -->|Face/Finger verification| F
    
    E <-->|Query boundary coords| T3
    F <-->|Validate biometric keys| T2
    
    T1 --- G
    T2 --- G
    T3 --- G
    T4 --- G
    T5 --- G
    T6 --- G
    T7 --- G
    T8 --- G
    T9 --- G
```

---

## Architectural Breakdown

### 1. Client Layer
- **Android Mobile App (Java):** Front-facing client application used by employees to perform verification. Queries device GPS coordinates and biometric hardware APIs (Face/Fingerprint) to send verification payloads.
- **Web Dashboard (React.js):** Management console used by administrators to manage geofenced campus zones, view logs, manage user settings, resolve support requests, and inspect admin action audit logs.

### 2. Backend Entry & Auth Layer
- **FastAPI Backend REST API:** Python-based REST API serving client requests. Uses Pydantic for request/response serialization.
- **Authentication Engine:** Validates Google OAuth 2.0 logins or email/password credential pairs.
- **OTP Verification Engine:** Handles email OTP generations and verification challenges for password resets and signup registrations.

### 3. Core Logic Layer
- **Geographic Boundary Validation Engine:** Uses the Haversine formula to compute the physical distance between user GPS coordinates and active organizational geofences to verify bounds.
- **Biometric Authentication Engine:** Validates mobile biometric check-ins against registered face profiles and biometric public keys.

### 4. Data Storage Layer
A PostgreSQL database containing 9 structured tables:
- **Institutional Domains:** Domain constraints whitelist.
- **Users:** Admin and employee profiles.
- **Campus Boundaries:** Geofence locations and radii.
- **Attendance Logs:** Clock-in/out stamps and geofence distance results.
- **Biometric Update Requests:** Audited face/finger update applications.
- **OTP Codes:** Security OTP challenges database.
- **Feedbacks & Support Requests:** Channels for employee communication and administrator resolution.
- **Admin Action Logs:** Unalterable administrative audits.