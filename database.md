erDiagram
    INSTITUTIONAL_DOMAINS {
        int id PK
        string domain_name UK "e.g., nitc.ac.in"
        timestamp created_at
    }

    USERS {
        uuid id PK
        string email UK
        string full_name
        string phone
        string role "admin | employee"
        int domain_id FK
        timestamp created_at
    }

    CAMPUS_BOUNDARIES {
        int id PK
        string location_name "e.g., Main Block"
        float center_latitude "Numeric(9,6)"
        float center_longitude "Numeric(9,6)"
        float radius_meters "In meters (e.g., 50.0)"
        boolean is_active
        timestamp updated_at
    }

    ATTENDANCE_LOGS {
        uuid id PK
        uuid user_id FK
        int campus_boundary_id FK
        float device_latitude
        float device_longitude
        float calculated_distance "Distance from center"
        string status "verified | rejected"
        timestamp check_in_time
    }

    FEEDBACKS {
        uuid id PK
        uuid user_id FK
        string message
        timestamp created_at
    }

    INSTITUTIONAL_DOMAINS ||--o{ USERS : "restricts / filters"
    USERS ||--o{ ATTENDANCE_LOGS : "logs"
    CAMPUS_BOUNDARIES ||--o{ ATTENDANCE_LOGS : "validates against"
    USERS ||--o{ FEEDBACKS : "submits"