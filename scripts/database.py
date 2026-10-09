import os
import uuid
import hashlib
import secrets
from datetime import datetime
from sqlalchemy import create_engine, Column, String, Float, Boolean, DateTime, Integer, ForeignKey, Enum
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import declarative_base, relationship, sessionmaker

def hash_password(password: str) -> str:
    salt = secrets.token_hex(16)
    pwd_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt.encode('utf-8'),
        100000
    ).hex()
    return f"{salt}:{pwd_hash}"

def verify_password(password: str, stored_password: str) -> bool:
    if not stored_password:
        return False
    if ":" not in stored_password:
        # Fallback for seeded plaintext passwords if they haven't been hashed yet
        return password == stored_password
    salt, pwd_hash = stored_password.split(":", 1)
    test_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt.encode('utf-8'),
        100000
    ).hex()
    return secrets.compare_digest(pwd_hash, test_hash)

# 1. Database Configuration
# Replace with your actual PostgreSQL credentials or environment variable
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://postgres:0000@localhost/t1h214e")

engine = create_engine(DATABASE_URL, echo=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# 2. Database Models
class InstitutionalDomain(Base):
    __tablename__ = "institutional_domains"

    id = Column(Integer, primary_key=True, index=True)
    domain_name = Column(String, unique=True, nullable=False, index=True) # e.g., "nitc.ac.in"
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    users = relationship("User", back_populates="domain")


class User(Base):
    __tablename__ = "users"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    email = Column(String, unique=True, nullable=False, index=True)
    full_name = Column(String, nullable=False)
    password = Column(String, nullable=False, default="password")
    role = Column(String, default="employee") # "admin" or "employee"
    phone = Column(String, nullable=True, default="")
    domain_id = Column(Integer, ForeignKey("institutional_domains.id"), nullable=False)
    biometric_public_key = Column(String, nullable=True)
    fingerkey = Column(String, nullable=True)
    face_image = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    domain = relationship("InstitutionalDomain", back_populates="users")
    attendance_logs = relationship("AttendanceLog", back_populates="user")


class CampusBoundary(Base):
    __tablename__ = "campus_boundaries"

    id = Column(Integer, primary_key=True, index=True)
    location_name = Column(String, nullable=False) # e.g., "Main Block"
    center_latitude = Column(Float, nullable=False)  # Numeric(9,6) standard representation
    center_longitude = Column(Float, nullable=False) # Numeric(9,6) standard representation
    radius_meters = Column(Float, default=50.0)      # Fence radius allowance
    is_active = Column(Boolean, default=True)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    attendance_logs = relationship("AttendanceLog", back_populates="boundary")


class AttendanceLog(Base):
    __tablename__ = "attendance_logs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    campus_boundary_id = Column(Integer, ForeignKey("campus_boundaries.id"), nullable=False)
    device_latitude = Column(Float, nullable=False)
    device_longitude = Column(Float, nullable=False)
    calculated_distance = Column(Float, nullable=False) # Computed delta baseline
    status = Column(String, nullable=False)             # "verified" | "rejected"
    check_in_time = Column(DateTime, default=datetime.utcnow)
    check_out_time = Column(DateTime, nullable=True)
    check_out_latitude = Column(Float, nullable=True)
    check_out_longitude = Column(Float, nullable=True)
    check_out_distance = Column(Float, nullable=True)
    check_out_status = Column(String, nullable=True)

    # Relationships
    user = relationship("User", back_populates="attendance_logs")
    boundary = relationship("CampusBoundary", back_populates="attendance_logs")


class BiometricUpdateRequest(Base):
    __tablename__ = "biometric_update_requests"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    request_type = Column(String, nullable=False) # "face" | "fingerprint"
    new_public_key = Column(String, nullable=True)
    new_face_image = Column(String, nullable=True)
    status = Column(String, default="pending") # "pending" | "approved" | "rejected"
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    user = relationship("User")


class OTPCode(Base):
    __tablename__ = "otp_codes"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    email = Column(String, nullable=False, index=True)
    otp_code = Column(String, nullable=False)
    purpose = Column(String, nullable=False) # "register" | "forgot_password"
    expires_at = Column(DateTime, nullable=False)
    is_used = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)


class Feedback(Base):
    __tablename__ = "feedbacks"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    message = Column(String, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    user = relationship("User")


class SupportRequest(Base):
    __tablename__ = "support_requests"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    message = Column(String, nullable=False)
    reply = Column(String, nullable=True)
    status = Column(String, default="pending") # "pending" | "replied"
    created_at = Column(DateTime, default=datetime.utcnow)
    replied_at = Column(DateTime, nullable=True)

    # Relationships
    user = relationship("User")


class AdminActionLog(Base):
    __tablename__ = "admin_action_logs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    admin_email = Column(String, nullable=False, index=True)
    action_type = Column(String, nullable=False) # "ADD" | "EDIT" | "DELETE"
    target_type = Column(String, nullable=False) # "USER" | "LOCATION"
    target_name = Column(String, nullable=False)
    details = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)


# 3. Initialization & Seeding Function
def init_db():
    print("[*] Creating database schema tables...")
    Base.metadata.create_all(bind=engine)
    print("[+] Tables initialized successfully.")
    
    # Run raw SQL migrations to safely update database columns on existing systems
    try:
        with engine.connect() as conn:
            with conn.begin():
                conn.exec_driver_sql("ALTER TABLE users ADD COLUMN IF NOT EXISTS biometric_public_key TEXT;")
                conn.exec_driver_sql("ALTER TABLE users ADD COLUMN IF NOT EXISTS fingerkey TEXT;")
                try:
                    conn.exec_driver_sql("UPDATE users SET fingerkey = biometric_public_key WHERE fingerkey IS NULL AND biometric_public_key IS NOT NULL;")
                except Exception:
                    pass
                conn.exec_driver_sql("ALTER TABLE attendance_logs ADD COLUMN IF NOT EXISTS check_out_time TIMESTAMP WITHOUT TIME ZONE;")
                conn.exec_driver_sql("ALTER TABLE attendance_logs ADD COLUMN IF NOT EXISTS check_out_latitude DOUBLE PRECISION;")
                conn.exec_driver_sql("ALTER TABLE attendance_logs ADD COLUMN IF NOT EXISTS check_out_longitude DOUBLE PRECISION;")
                conn.exec_driver_sql("ALTER TABLE attendance_logs ADD COLUMN IF NOT EXISTS check_out_distance DOUBLE PRECISION;")
                conn.exec_driver_sql("ALTER TABLE attendance_logs ADD COLUMN IF NOT EXISTS check_out_status VARCHAR(50);")
                conn.exec_driver_sql("ALTER TABLE users ADD COLUMN IF NOT EXISTS face_image TEXT;")
                conn.exec_driver_sql("""
                    CREATE TABLE IF NOT EXISTS biometric_update_requests (
                        id UUID PRIMARY KEY,
                        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        request_type VARCHAR(50) NOT NULL,
                        new_public_key TEXT,
                        new_face_image TEXT,
                        status VARCHAR(50) DEFAULT 'pending',
                        created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
                    );
                """)
                conn.exec_driver_sql("""
                    CREATE TABLE IF NOT EXISTS otp_codes (
                        id UUID PRIMARY KEY,
                        email VARCHAR(255) NOT NULL,
                        otp_code VARCHAR(50) NOT NULL,
                        purpose VARCHAR(50) NOT NULL,
                        expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                        is_used BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
                    );
                """)
                conn.exec_driver_sql("""
                    CREATE TABLE IF NOT EXISTS support_requests (
                        id UUID PRIMARY KEY,
                        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        message TEXT NOT NULL,
                        reply TEXT,
                        status VARCHAR(50) DEFAULT 'pending',
                        created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
                        replied_at TIMESTAMP WITHOUT TIME ZONE
                    );
                """)
                conn.exec_driver_sql("""
                    CREATE TABLE IF NOT EXISTS admin_action_logs (
                        id UUID PRIMARY KEY,
                        admin_email VARCHAR(255) NOT NULL,
                        action_type VARCHAR(50) NOT NULL,
                        target_type VARCHAR(50) NOT NULL,
                        target_name VARCHAR(255) NOT NULL,
                        details TEXT,
                        created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
                    );
                """)
            print("[+] Database columns migrated successfully.")
    except Exception as e:
        print(f"[-] Migration warning: {e}")

    # Optional: Seed initial domain constraint data if empty
    session = SessionLocal()
    if not session.query(InstitutionalDomain).first():
        print("[*] Seeding default institutional domain and a test campus boundary...")
        
        # Add primary domain filter
        test_domain = InstitutionalDomain(domain_name="nitc.ac.in")
        session.add(test_domain)
        
        # Add primary campus perimeter location 
        test_boundary = CampusBoundary(
            location_name="Main Campus Hub",
            center_latitude=11.3216,  # Sample coordinates
            center_longitude=75.9336,
            radius_meters=60.0,
            is_active=True
        )
        session.add(test_boundary)
        
        session.commit()
        print("[+] Seed data injected.")
    session.close()

if __name__ == "__main__":
    init_db()