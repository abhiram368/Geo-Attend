import sys
import os
import math
from datetime import datetime, date, timedelta, timezone
from typing import List, Optional

# Add the parent directory to sys.path so we can import scripts.database
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, HTTPException, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy.orm import Session
from scripts.database import SessionLocal, User, CampusBoundary, AttendanceLog, InstitutionalDomain, Feedback, BiometricUpdateRequest, OTPCode, SupportRequest, AdminActionLog, hash_password, verify_password

app = FastAPI(title="Geo-Attend API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Startup event to ensure default admin user is seeded
@app.on_event("startup")
def startup_db_check():
    db = SessionLocal()
    try:
        # Ensure 'gmail.com' domain exists
        domain = db.query(InstitutionalDomain).filter_by(domain_name="gmail.com").first()
        if not domain:
            domain = InstitutionalDomain(domain_name="gmail.com")
            db.add(domain)
            db.commit()
            db.refresh(domain)
            print("[+] Added 'gmail.com' institutional domain.")
            
        # Ensure admin user exists
        admin = db.query(User).filter_by(email="kattaabhiram368@gmail.com").first()
        if not admin:
            admin = User(
                email="kattaabhiram368@gmail.com",
                full_name="Katta Abhiram",
                password=hash_password("password"),
                role="admin",
                domain_id=domain.id,
                phone="+91 98765 43210"
            )
            db.add(admin)
            db.commit()
            print("[+] Created default admin user kattaabhiram368@gmail.com")
        else:
            admin.role = "admin"
            admin.password = hash_password("password")
            db.commit()
            print("[+] Verified/updated default admin user kattaabhiram368@gmail.com")
    except Exception as e:
        print(f"[-] Startup DB check failed/skipped: {e}")
    finally:
        db.close()

# Dependency to get db session
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Pydantic schemas
class CampusBoundaryCreate(BaseModel):
    location_name: str
    center_latitude: float
    center_longitude: float
    radius_meters: float
    is_active: bool = True

class CampusBoundaryUpdate(BaseModel):
    location_name: Optional[str] = None
    center_latitude: Optional[float] = None
    center_longitude: Optional[float] = None
    radius_meters: Optional[float] = None
    is_active: Optional[bool] = None

class LoginRequest(BaseModel):
    email: str
    password: str

class RegisterRequest(BaseModel):
    email: str
    full_name: str
    password: str

class RegisterOTPRequest(BaseModel):
    email: str
    full_name: str

class RegisterVerifyRequest(BaseModel):
    email: str
    full_name: str
    password: str
    otp: str

class ForgotPasswordOTPRequest(BaseModel):
    email: str

class ForgotPasswordVerifyRequest(BaseModel):
    email: str
    otp: str
    new_password: str

class GoogleAuthRequest(BaseModel):
    id_token: str

class CheckInRequest(BaseModel):
    user_id: str
    device_latitude: float
    device_longitude: float


class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    role: Optional[str] = None


class UserCreate(BaseModel):
    email: str
    full_name: str
    password: str
    role: str # "admin" or "employee"
    phone: Optional[str] = ""


class PasswordUpdate(BaseModel):
    password: str


class FeedbackCreate(BaseModel):
    user_id: str
    message: str


class SupportRequestCreate(BaseModel):
    user_id: str
    message: str


class SupportRequestReply(BaseModel):
    reply: str

def haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    # Radius of the Earth in meters
    R = 6371000.0
    
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    
    a = math.sin(delta_phi / 2.0) ** 2 + \
        math.cos(phi1) * math.cos(phi2) * \
        math.sin(delta_lambda / 2.0) ** 2
    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))
    
    return R * c

def log_admin_action(db: Session, admin_email: str, action_type: str, target_type: str, target_name: str, details: str = None):
    log = AdminActionLog(
        admin_email=admin_email or "kattaabhiram368@gmail.com",
        action_type=action_type,
        target_type=target_type,
        target_name=target_name,
        details=details
    )
    db.add(log)
    db.commit()

# Get all users (employees and admins)
@app.get("/employees")
def get_employees(db: Session = Depends(get_db)):
    users = db.query(User).all()
    result = []
    for user in users:
        domain = db.query(InstitutionalDomain).filter(InstitutionalDomain.id == user.domain_id).first()
        result.append({
            "id": str(user.id),
            "email": user.email,
            "full_name": user.full_name,
            "role": user.role,
            "phone": user.phone or "",
            "domain_id": user.domain_id,
            "domain_name": domain.domain_name if domain else None,
            "face_registered": user.face_image is not None,
            "fingerprint_registered": user.fingerkey is not None,
            "created_at": user.created_at.isoformat() if user.created_at else None
        })
    return result

# Get all attendance logs with joined user and boundary info
@app.get("/attendance-logs")
def get_attendance_logs(db: Session = Depends(get_db)):
    logs = db.query(AttendanceLog).order_by(AttendanceLog.check_in_time.desc()).all()
    result = []
    for log in logs:
        user = db.query(User).filter(User.id == log.user_id).first()
        boundary = db.query(CampusBoundary).filter(CampusBoundary.id == log.campus_boundary_id).first()
        result.append({
            "id": str(log.id),
            "user_id": str(log.user_id),
            "user_name": user.full_name if user else "Unknown User",
            "user_email": user.email if user else "Unknown Email",
            "campus_boundary_id": log.campus_boundary_id,
            "location_name": boundary.location_name if boundary else "Unknown Boundary",
            "device_latitude": log.device_latitude,
            "device_longitude": log.device_longitude,
            "calculated_distance": log.calculated_distance,
            "status": log.status,
            "check_in_time": (log.check_in_time.isoformat() + "Z") if log.check_in_time else None,
            "check_out_time": (log.check_out_time.isoformat() + "Z") if log.check_out_time else None,
            "check_out_status": log.check_out_status,
            "check_out_distance": log.check_out_distance
        })
    return result

# Get all campus boundaries
@app.get("/campus-boundaries")
def get_campus_boundaries(db: Session = Depends(get_db)):
    boundaries = db.query(CampusBoundary).order_by(CampusBoundary.id.asc()).all()
    result = []
    for b in boundaries:
        result.append({
            "id": b.id,
            "location_name": b.location_name,
            "center_latitude": b.center_latitude,
            "center_longitude": b.center_longitude,
            "radius_meters": b.radius_meters,
            "is_active": b.is_active,
            "updated_at": b.updated_at.isoformat() if b.updated_at else None
        })
    return result

# Create new campus boundary
@app.post("/campus-boundaries")
def create_campus_boundary(boundary: CampusBoundaryCreate, request: Request, db: Session = Depends(get_db)):
    db_boundary = CampusBoundary(
        location_name=boundary.location_name,
        center_latitude=boundary.center_latitude,
        center_longitude=boundary.center_longitude,
        radius_meters=boundary.radius_meters,
        is_active=boundary.is_active,
        updated_at=datetime.utcnow()
    )
    db.add(db_boundary)
    db.commit()
    db.refresh(db_boundary)
    
    admin_email = request.headers.get("X-Admin-Email", "kattaabhiram368@gmail.com")
    log_admin_action(
        db, 
        admin_email=admin_email, 
        action_type="ADD", 
        target_type="LOCATION", 
        target_name=db_boundary.location_name,
        details=f"Radius: {db_boundary.radius_meters}m, Center: ({db_boundary.center_latitude}, {db_boundary.center_longitude})"
    )
    
    return {
        "id": db_boundary.id,
        "location_name": db_boundary.location_name,
        "center_latitude": db_boundary.center_latitude,
        "center_longitude": db_boundary.center_longitude,
        "radius_meters": db_boundary.radius_meters,
        "is_active": db_boundary.is_active,
        "updated_at": db_boundary.updated_at.isoformat() if db_boundary.updated_at else None
    }

# Update campus boundary
@app.put("/campus-boundaries/{boundary_id}")
def update_campus_boundary(boundary_id: int, boundary_data: CampusBoundaryUpdate, request: Request, db: Session = Depends(get_db)):
    db_boundary = db.query(CampusBoundary).filter(CampusBoundary.id == boundary_id).first()
    if not db_boundary:
        raise HTTPException(status_code=404, detail="Boundary not found")
    
    changes = []
    if boundary_data.location_name is not None:
        changes.append(f"name: {db_boundary.location_name} -> {boundary_data.location_name}")
        db_boundary.location_name = boundary_data.location_name
    if boundary_data.center_latitude is not None:
        changes.append(f"lat: {db_boundary.center_latitude} -> {boundary_data.center_latitude}")
        db_boundary.center_latitude = boundary_data.center_latitude
    if boundary_data.center_longitude is not None:
        changes.append(f"lon: {db_boundary.center_longitude} -> {boundary_data.center_longitude}")
        db_boundary.center_longitude = boundary_data.center_longitude
    if boundary_data.radius_meters is not None:
        changes.append(f"radius: {db_boundary.radius_meters} -> {boundary_data.radius_meters}")
        db_boundary.radius_meters = boundary_data.radius_meters
    if boundary_data.is_active is not None:
        changes.append(f"active: {db_boundary.is_active} -> {boundary_data.is_active}")
        db_boundary.is_active = boundary_data.is_active
        
    db_boundary.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(db_boundary)
    
    admin_email = request.headers.get("X-Admin-Email", "kattaabhiram368@gmail.com")
    log_admin_action(
        db,
        admin_email=admin_email,
        action_type="EDIT",
        target_type="LOCATION",
        target_name=db_boundary.location_name,
        details=" | ".join(changes) if changes else "No fields changed"
    )
    
    return {
        "id": db_boundary.id,
        "location_name": db_boundary.location_name,
        "center_latitude": db_boundary.center_latitude,
        "center_longitude": db_boundary.center_longitude,
        "radius_meters": db_boundary.radius_meters,
        "is_active": db_boundary.is_active,
        "updated_at": db_boundary.updated_at.isoformat() if db_boundary.updated_at else None
    }

# Delete campus boundary
@app.delete("/campus-boundaries/{boundary_id}")
def delete_campus_boundary(boundary_id: int, request: Request, db: Session = Depends(get_db)):
    db_boundary = db.query(CampusBoundary).filter(CampusBoundary.id == boundary_id).first()
    if not db_boundary:
        raise HTTPException(status_code=404, detail="Boundary not found")
    
    name = db_boundary.location_name
    db.delete(db_boundary)
    db.commit()
    
    admin_email = request.headers.get("X-Admin-Email", "kattaabhiram368@gmail.com")
    log_admin_action(
        db,
        admin_email=admin_email,
        action_type="DELETE",
        target_type="LOCATION",
        target_name=name,
        details=f"Deleted location with ID {boundary_id}"
    )
    
    return {"detail": "Boundary deleted successfully"}

# Get dashboard stats
@app.get("/dashboard-stats")
def get_dashboard_stats(db: Session = Depends(get_db)):
    total_employees = db.query(User).filter(User.role == "employee").count()
    active_boundaries = db.query(CampusBoundary).filter(CampusBoundary.is_active == True).count()
    
    # Calculate check-ins for today (using UTC since database logs use UTC)
    today_start = datetime.utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
    
    present_today = db.query(AttendanceLog.user_id)\
        .filter(AttendanceLog.status == "verified")\
        .filter(AttendanceLog.check_in_time >= today_start)\
        .distinct().count()
        
    rejected_today = db.query(AttendanceLog)\
        .filter(AttendanceLog.status == "rejected")\
        .filter(AttendanceLog.check_in_time >= today_start).count()
        
    return {
        "total_employees": total_employees,
        "present_today": present_today,
        "rejected_today": rejected_today,
        "active_boundaries": active_boundaries
    }

# OTP generation and email verification helpers
def generate_otp() -> str:
    import secrets
    # Generates a secure random 6-digit numeric string
    return str(secrets.randbelow(900000) + 100000)

def send_otp_email(email: str, otp: str, purpose: str):
    import urllib.request
    import json
    
    BREVO_API_KEY = os.getenv("BREVO_API_KEY", "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")
    BREVO_SENDER_EMAIL = os.getenv("BREVO_SENDER_EMAIL", "kattaabhiram368@gmail.com")
    BREVO_SENDER_NAME = "Geo-Attend"

    subject = "Geo-Attend Verification Code"
    if purpose == "register":
        body = (
            f"Hello,\n\n"
            f"Thank you for choosing Geo-Attend. To complete your registration, "
            f"please enter the following One-Time Password (OTP):\n\n"
            f"Verification Code: {otp}\n\n"
            f"This code will expire in 5 minutes. Please do not share it with anyone.\n\n"
            f"Best regards,\n"
            f"Geo-Attend Team"
        )
    else:
        body = (
            f"Hello,\n\n"
            f"We received a request to reset your password for Geo-Attend. "
            f"Please enter the following One-Time Password (OTP) to proceed:\n\n"
            f"Reset Code: {otp}\n\n"
            f"This code will expire in 5 minutes. If you did not request a password reset, "
            f"please ignore this email.\n\n"
            f"Best regards,\n"
            f"Geo-Attend Team"
        )

    # Always log the OTP to standard output/console for easy development and debug fallback
    print("\n" + "=" * 60)
    print(f"[MAIL DELIVERY SIMULATOR] To: {email}")
    print(f"Subject: {subject}")
    print(f"Body: {body}")
    print("=" * 60 + "\n")

    if not BREVO_API_KEY or not BREVO_SENDER_EMAIL:
        print("[!] Brevo API credentials missing in configuration/environment variables.")
        return

    url = "https://api.brevo.com/v3/smtp/email"
    headers = {
        "accept": "application/json",
        "api-key": BREVO_API_KEY,
        "content-type": "application/json"
    }

    payload = {
        "sender": {"name": BREVO_SENDER_NAME, "email": BREVO_SENDER_EMAIL},
        "to": [{"email": email}],
        "subject": subject,
        "textContent": body
    }

    try:
        req = urllib.request.Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            headers=headers,
            method="POST"
        )
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode("utf-8")
            print(f"[+] Successfully sent OTP verification email via Brevo API to {email}. Response: {res_body}")
    except Exception as e:
        print(f"[-] Brevo API email delivery failed: {e}. (Logged to console above as debug fallback)")


# Login endpoint
@app.post("/login")
def login(request: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == request.email).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if not verify_password(request.password, user.password):
        raise HTTPException(status_code=401, detail="Invalid password credentials")
    domain = db.query(InstitutionalDomain).filter(InstitutionalDomain.id == user.domain_id).first()
    return {
        "id": str(user.id),
        "email": user.email,
        "full_name": user.full_name,
        "phone": user.phone or "",
        "role": user.role,
        "domain_name": domain.domain_name if domain else None,
        "face_registered": user.face_image is not None,
        "fingerprint_registered": user.fingerkey is not None
    }

# Register endpoint
@app.post("/register")
def register(request: RegisterRequest, db: Session = Depends(get_db)):
    # Check if user already exists
    existing_user = db.query(User).filter(User.email == request.email).first()
    if existing_user:
        raise HTTPException(status_code=400, detail="User already registered")
        
    # Extract domain from email
    parts = request.email.split("@")
    if len(parts) != 2:
        raise HTTPException(status_code=400, detail="Invalid email address format")
    email_domain = parts[1]
    
    # Check institutional domain
    domain = db.query(InstitutionalDomain).filter(InstitutionalDomain.domain_name == email_domain).first()
    if not domain:
        raise HTTPException(status_code=400, detail=f"Domain '{email_domain}' is not in the institutional whitelist")
        
    # Create new user with hashed password
    db_user = User(
        email=request.email,
        full_name=request.full_name,
        password=hash_password(request.password),
        role="employee",
        domain_id=domain.id
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    
    return {
        "id": str(db_user.id),
        "email": db_user.email,
        "full_name": db_user.full_name,
        "phone": db_user.phone or "",
        "role": db_user.role,
        "domain_name": domain.domain_name
    }


# Register OTP endpoints
@app.post("/register/send-otp")
def register_send_otp(request: RegisterOTPRequest, db: Session = Depends(get_db)):
    existing_user = db.query(User).filter(User.email == request.email).first()
    if existing_user:
        raise HTTPException(status_code=400, detail="User already registered")
        
    parts = request.email.split("@")
    if len(parts) != 2:
        raise HTTPException(status_code=400, detail="Invalid email address format")
    email_domain = parts[1]
    
    domain = db.query(InstitutionalDomain).filter(InstitutionalDomain.domain_name == email_domain).first()
    if not domain:
        raise HTTPException(status_code=400, detail=f"Domain '{email_domain}' is not in the institutional whitelist")
        
    otp = generate_otp()
    expires_at = datetime.utcnow() + timedelta(minutes=5)
    
    # Invalidate previous registration OTPs for this email
    db.query(OTPCode).filter(
        OTPCode.email == request.email, 
        OTPCode.purpose == "register",
        OTPCode.is_used == False
    ).update({OTPCode.is_used: True})
    
    db_otp = OTPCode(
        email=request.email,
        otp_code=otp,
        purpose="register",
        expires_at=expires_at
    )
    db.add(db_otp)
    db.commit()
    
    send_otp_email(request.email, otp, "register")
    return {"detail": "OTP sent to email successfully"}


@app.post("/register/verify")
def register_verify(request: RegisterVerifyRequest, db: Session = Depends(get_db)):
    otp_record = db.query(OTPCode).filter(
        OTPCode.email == request.email,
        OTPCode.otp_code == request.otp,
        OTPCode.purpose == "register",
        OTPCode.is_used == False
    ).order_by(OTPCode.created_at.desc()).first()
    
    if not otp_record:
        raise HTTPException(status_code=400, detail="Invalid verification code")
        
    if datetime.utcnow() > otp_record.expires_at:
        raise HTTPException(status_code=400, detail="Verification code has expired")
        
    otp_record.is_used = True
    
    existing_user = db.query(User).filter(User.email == request.email).first()
    if existing_user:
        db.commit()
        raise HTTPException(status_code=400, detail="User already registered")
        
    parts = request.email.split("@")
    email_domain = parts[1]
    domain = db.query(InstitutionalDomain).filter(InstitutionalDomain.domain_name == email_domain).first()
    if not domain:
        db.commit()
        raise HTTPException(status_code=400, detail=f"Domain '{email_domain}' is not in whitelist")
        
    db_user = User(
        email=request.email,
        full_name=request.full_name,
        password=hash_password(request.password),
        role="employee",
        domain_id=domain.id
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    
    return {
        "id": str(db_user.id),
        "email": db_user.email,
        "full_name": db_user.full_name,
        "phone": db_user.phone or "",
        "role": db_user.role,
        "domain_name": domain.domain_name,
        "face_registered": False,
        "fingerprint_registered": False
    }


# Forgot Password endpoints
@app.post("/forgot-password/send-otp")
def forgot_password_send_otp(request: ForgotPasswordOTPRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == request.email).first()
    if not user:
        raise HTTPException(status_code=404, detail="Email is not registered in the system")
        
    otp = generate_otp()
    expires_at = datetime.utcnow() + timedelta(minutes=5)
    
    # Invalidate previous password reset OTPs
    db.query(OTPCode).filter(
        OTPCode.email == request.email, 
        OTPCode.purpose == "forgot_password",
        OTPCode.is_used == False
    ).update({OTPCode.is_used: True})
    
    db_otp = OTPCode(
        email=request.email,
        otp_code=otp,
        purpose="forgot_password",
        expires_at=expires_at
    )
    db.add(db_otp)
    db.commit()
    
    send_otp_email(request.email, otp, "forgot_password")
    return {"detail": "OTP sent to email successfully"}


@app.post("/forgot-password/verify")
def forgot_password_verify(request: ForgotPasswordVerifyRequest, db: Session = Depends(get_db)):
    otp_record = db.query(OTPCode).filter(
        OTPCode.email == request.email,
        OTPCode.otp_code == request.otp,
        OTPCode.purpose == "forgot_password",
        OTPCode.is_used == False
    ).order_by(OTPCode.created_at.desc()).first()
    
    if not otp_record:
        raise HTTPException(status_code=400, detail="Invalid verification code")
        
    if datetime.utcnow() > otp_record.expires_at:
        raise HTTPException(status_code=400, detail="Verification code has expired")
        
    otp_record.is_used = True
    
    user = db.query(User).filter(User.email == request.email).first()
    if not user:
        db.commit()
        raise HTTPException(status_code=404, detail="User not found")
        
    user.password = hash_password(request.new_password)
    db.commit()
    return {"detail": "Password reset completed successfully"}


# Google Authentication Endpoint
@app.post("/google-auth")
def google_auth(request: GoogleAuthRequest, db: Session = Depends(get_db)):
    import urllib.request
    import json
    
    # 1. Fetch token validation details from Google Tokeninfo API
    tokeninfo_url = f"https://oauth2.googleapis.com/tokeninfo?id_token={request.id_token}"
    try:
        req = urllib.request.Request(tokeninfo_url)
        with urllib.request.urlopen(req, timeout=5) as response:
            payload = json.loads(response.read().decode('utf-8'))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to authenticate with Google: {e}")
        
    # 2. Verify response payload
    email = payload.get("email")
    email_verified = payload.get("email_verified")
    full_name = payload.get("name", "Google User")
    
    if not email or (email_verified != "true" and email_verified != True):
        raise HTTPException(status_code=400, detail="Verified Google email is required")
        
    # 3. Whitelist check
    parts = email.split("@")
    if len(parts) != 2:
        raise HTTPException(status_code=400, detail="Invalid email format returned by Google")
    email_domain = parts[1]
    
    domain = db.query(InstitutionalDomain).filter(InstitutionalDomain.domain_name == email_domain).first()
    if not domain:
        raise HTTPException(status_code=400, detail=f"Domain '{email_domain}' is not in whitelist")
        
    # 4. Check user record
    user = db.query(User).filter(User.email == email).first()
    if not user:
        # Create Google account with random robust password
        import secrets
        random_pwd = secrets.token_urlsafe(24)
        user = User(
            email=email,
            full_name=full_name,
            password=hash_password(random_pwd),
            role="employee",
            domain_id=domain.id
        )
        db.add(user)
        db.commit()
        db.refresh(user)
        
    domain_obj = db.query(InstitutionalDomain).filter(InstitutionalDomain.id == user.domain_id).first()
    return {
        "id": str(user.id),
        "email": user.email,
        "full_name": user.full_name,
        "phone": user.phone or "",
        "role": user.role,
        "domain_name": domain_obj.domain_name if domain_obj else None,
        "face_registered": user.face_image is not None,
        "fingerprint_registered": user.fingerkey is not None
    }

# Check-in endpoint
@app.post("/check-in")
def check_in(request: CheckInRequest, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(request.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
        
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    # Fetch all active campus boundaries
    boundaries = db.query(CampusBoundary).filter(CampusBoundary.is_active == True).all()
    if not boundaries:
        raise HTTPException(status_code=400, detail="No active campus boundaries found")
        
    closest_boundary = None
    min_distance = float('inf')
    is_verified = False
    target_boundary = None
    
    for b in boundaries:
        distance = haversine_distance(
            request.device_latitude, request.device_longitude,
            b.center_latitude, b.center_longitude
        )
        if distance <= b.radius_meters:
            is_verified = True
            target_boundary = b
            min_distance = distance
            break
        else:
            if distance < min_distance:
                min_distance = distance
                closest_boundary = b
                
    if is_verified:
        status = "verified"
        selected_boundary = target_boundary
    else:
        status = "rejected"
        selected_boundary = closest_boundary if closest_boundary else boundaries[0]
        
    db_log = AttendanceLog(
        user_id=user.id,
        campus_boundary_id=selected_boundary.id,
        device_latitude=request.device_latitude,
        device_longitude=request.device_longitude,
        calculated_distance=min_distance,
        status=status,
        check_in_time=datetime.utcnow()
    )
    db.add(db_log)
    db.commit()
    db.refresh(db_log)
    
    return {
        "id": str(db_log.id),
        "status": db_log.status,
        "location_name": selected_boundary.location_name,
        "calculated_distance": db_log.calculated_distance,
        "check_in_time": db_log.check_in_time.isoformat() + "Z"
    }

# Get logs for specific user
@app.get("/attendance-logs/user/{user_id}")
def get_user_attendance_logs(user_id: str, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
        
    logs = db.query(AttendanceLog).filter(AttendanceLog.user_id == user_uuid).order_by(AttendanceLog.check_in_time.desc()).all()
    result = []
    for log in logs:
        boundary = db.query(CampusBoundary).filter(CampusBoundary.id == log.campus_boundary_id).first()
        result.append({
            "id": str(log.id),
            "campus_boundary_id": log.campus_boundary_id,
            "location_name": boundary.location_name if boundary else "Unknown Boundary",
            "device_latitude": log.device_latitude,
            "device_longitude": log.device_longitude,
            "calculated_distance": log.calculated_distance,
            "status": log.status,
            "check_in_time": (log.check_in_time.isoformat() + "Z") if log.check_in_time else None,
            "check_out_time": (log.check_out_time.isoformat() + "Z") if log.check_out_time else None,
            "check_out_status": log.check_out_status
        })
    return result


@app.get("/users/{user_id}")
def get_user_profile(user_id: str, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    domain = db.query(InstitutionalDomain).filter(InstitutionalDomain.id == user.domain_id).first()
    return {
        "id": str(user.id),
        "email": user.email,
        "full_name": user.full_name,
        "phone": user.phone or "",
        "role": user.role,
        "domain_name": domain.domain_name if domain else None,
        "face_registered": user.face_image is not None,
        "fingerprint_registered": user.fingerkey is not None,
        "created_at": user.created_at.isoformat() if user.created_at else None
    }


@app.put("/users/{user_id}")
def update_user_profile(user_id: str, profile_data: UserUpdate, request: Request, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    changes = []
    if profile_data.full_name is not None:
        changes.append(f"name: {user.full_name} -> {profile_data.full_name}")
        user.full_name = profile_data.full_name
    if profile_data.phone is not None:
        changes.append(f"phone: {user.phone} -> {profile_data.phone}")
        user.phone = profile_data.phone
    if profile_data.email is not None:
        changes.append(f"email: {user.email} -> {profile_data.email}")
        user.email = profile_data.email
    if profile_data.role is not None:
        changes.append(f"role: {user.role} -> {profile_data.role}")
        user.role = profile_data.role
        
    db.commit()
    db.refresh(user)
    
    admin_email = request.headers.get("X-Admin-Email", "kattaabhiram368@gmail.com")
    log_admin_action(
        db,
        admin_email=admin_email,
        action_type="EDIT",
        target_type="USER",
        target_name=user.full_name,
        details=" | ".join(changes) if changes else "No fields changed"
    )
    
    domain = db.query(InstitutionalDomain).filter(InstitutionalDomain.id == user.domain_id).first()
    return {
        "id": str(user.id),
        "email": user.email,
        "full_name": user.full_name,
        "phone": user.phone or "",
        "role": user.role,
        "domain_name": domain.domain_name if domain else None
    }


# Add user endpoint
@app.post("/users")
def create_user(user_data: UserCreate, request: Request, db: Session = Depends(get_db)):
    existing_user = db.query(User).filter(User.email == user_data.email).first()
    if existing_user:
        raise HTTPException(status_code=400, detail="User already registered with this email")
        
    parts = user_data.email.split("@")
    if len(parts) != 2:
        raise HTTPException(status_code=400, detail="Invalid email address format")
    email_domain = parts[1]
    
    domain = db.query(InstitutionalDomain).filter(InstitutionalDomain.domain_name == email_domain).first()
    if not domain:
        domain = InstitutionalDomain(domain_name=email_domain)
        db.add(domain)
        db.commit()
        db.refresh(domain)
        
    db_user = User(
        email=user_data.email,
        full_name=user_data.full_name,
        password=hash_password(user_data.password),
        role=user_data.role,
        domain_id=domain.id,
        phone=user_data.phone
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    
    admin_email = request.headers.get("X-Admin-Email", "kattaabhiram368@gmail.com")
    log_admin_action(
        db,
        admin_email=admin_email,
        action_type="ADD",
        target_type="USER",
        target_name=db_user.full_name,
        details=f"Email: {db_user.email}, Role: {db_user.role}"
    )
    
    return {
        "id": str(db_user.id),
        "email": db_user.email,
        "full_name": db_user.full_name,
        "phone": db_user.phone or "",
        "role": db_user.role,
        "domain_name": domain.domain_name
    }


# Delete user endpoint
@app.delete("/users/{user_id}")
def delete_user(user_id: str, request: Request, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
        
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    db.query(AttendanceLog).filter(AttendanceLog.user_id == user_uuid).delete()
    db.query(BiometricUpdateRequest).filter(BiometricUpdateRequest.user_id == user_uuid).delete()
    db.query(Feedback).filter(Feedback.user_id == user_uuid).delete()
    db.query(SupportRequest).filter(SupportRequest.user_id == user_uuid).delete()
    
    name = user.full_name
    email = user.email
    db.delete(user)
    db.commit()
    
    admin_email = request.headers.get("X-Admin-Email", "kattaabhiram368@gmail.com")
    log_admin_action(
        db,
        admin_email=admin_email,
        action_type="DELETE",
        target_type="USER",
        target_name=name,
        details=f"Deleted user with Email: {email}"
    )
    
    return {"detail": "User deleted successfully"}


# Get feedbacks endpoint for admin
@app.get("/feedbacks")
def get_feedbacks(db: Session = Depends(get_db)):
    feedbacks = db.query(Feedback).order_by(Feedback.created_at.desc()).all()
    result = []
    for fb in feedbacks:
        user = db.query(User).filter(User.id == fb.user_id).first()
        result.append({
            "id": str(fb.id),
            "user_id": str(fb.user_id),
            "user_name": user.full_name if user else "Unknown User",
            "user_email": user.email if user else "Unknown Email",
            "message": fb.message,
            "created_at": fb.created_at.isoformat() if fb.created_at else None
        })
    return result


# Get support requests endpoint for admin
@app.get("/support-requests")
def get_support_requests(db: Session = Depends(get_db)):
    requests = db.query(SupportRequest).order_by(SupportRequest.created_at.desc()).all()
    result = []
    for req in requests:
        user = db.query(User).filter(User.id == req.user_id).first()
        result.append({
            "id": str(req.id),
            "user_id": str(req.user_id),
            "user_name": user.full_name if user else "Unknown User",
            "user_email": user.email if user else "Unknown Email",
            "message": req.message,
            "reply": req.reply,
            "status": req.status,
            "created_at": req.created_at.isoformat() if req.created_at else None,
            "replied_at": req.replied_at.isoformat() if req.replied_at else None
        })
    return result


# Create support request endpoint (used by mobile app)
@app.post("/support-requests")
def create_support_request(req_data: SupportRequestCreate, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(req_data.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
        
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    db_req = SupportRequest(
        user_id=user.id,
        message=req_data.message,
        status="pending"
    )
    db.add(db_req)
    db.commit()
    db.refresh(db_req)
    
    return {
        "id": str(db_req.id),
        "user_id": str(db_req.user_id),
        "message": db_req.message,
        "status": db_req.status,
        "created_at": db_req.created_at.isoformat()
    }


# Reply to support request endpoint
@app.post("/support-requests/{request_id}/reply")
def reply_to_support_request(request_id: str, reply_data: SupportRequestReply, db: Session = Depends(get_db)):
    import uuid
    try:
        req_uuid = uuid.UUID(request_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid support request ID format")
        
    req = db.query(SupportRequest).filter(SupportRequest.id == req_uuid).first()
    if not req:
        raise HTTPException(status_code=404, detail="Support request not found")
        
    req.reply = reply_data.reply
    req.status = "replied"
    req.replied_at = datetime.utcnow()
    db.commit()
    
    return {"detail": "Reply saved successfully"}


# Get admin action logs endpoint
@app.get("/admin/action-logs")
def get_admin_action_logs(db: Session = Depends(get_db)):
    logs = db.query(AdminActionLog).order_by(AdminActionLog.created_at.desc()).all()
    return [
        {
            "id": str(log.id),
            "admin_email": log.admin_email,
            "action_type": log.action_type,
            "target_type": log.target_type,
            "target_name": log.target_name,
            "details": log.details,
            "created_at": log.created_at.isoformat() if log.created_at else None
        } for log in logs
    ]


@app.put("/users/{user_id}/password")
def change_password(user_id: str, pw_data: PasswordUpdate, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    user.password = hash_password(pw_data.password)
    db.commit()
    return {"detail": "Password updated successfully"}


@app.post("/feedbacks")
def create_feedback(fb_data: FeedbackCreate, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(fb_data.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    db_feedback = Feedback(
        user_id=user.id,
        message=fb_data.message
    )
    db.add(db_feedback)
    db.commit()
    db.refresh(db_feedback)
    return {
        "id": str(db_feedback.id),
        "user_id": str(db_feedback.user_id),
        "message": db_feedback.message,
        "created_at": db_feedback.created_at.isoformat()
    }


# Biometric schemas
class FaceRegisterRequest(BaseModel):
    user_id: str
    face_image: str

class FaceCheckInRequest(BaseModel):
    user_id: str
    device_latitude: float
    device_longitude: float
    face_image: str

class BiometricRegisterRequest(BaseModel):
    user_id: str
    public_key: str

class BiometricCheckInRequest(BaseModel):
    user_id: str
    device_latitude: float
    device_longitude: float
    challenge: str
    timestamp: str
    signature: str

biometric_challenges = {}

@app.get("/get-biometric-challenge/{user_id}")
def get_biometric_challenge(user_id: str):
    import secrets
    import uuid
    try:
        uuid.UUID(user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
        
    challenge = secrets.token_hex(32)
    expires_at = datetime.utcnow() + timedelta(minutes=5)
    
    biometric_challenges[user_id] = {
        "challenge": challenge,
        "expires_at": expires_at
    }
    
    return {
        "challenge": challenge,
        "expires_at": expires_at.isoformat()
    }

@app.post("/register-biometric")
def register_biometric(request: BiometricRegisterRequest, db: Session = Depends(get_db)):
    import uuid
    import base64
    from cryptography.hazmat.primitives.serialization import load_der_public_key
    
    try:
        user_uuid = uuid.UUID(request.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
        
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    try:
        public_key_bytes = base64.b64decode(request.public_key)
        load_der_public_key(public_key_bytes)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid public key format: {e}")
        
    user.fingerkey = request.public_key
    db.commit()
    return {"detail": "Biometric public key registered successfully"}

@app.post("/check-in-biometric")
def check_in_biometric(request: BiometricCheckInRequest, db: Session = Depends(get_db)):
    import uuid
    import base64
    from cryptography.hazmat.primitives.serialization import load_der_public_key
    from cryptography.hazmat.primitives.asymmetric import ec
    from cryptography.hazmat.primitives import hashes
    
    try:
        user_uuid = uuid.UUID(request.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
        
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    if not user.fingerkey:
        raise HTTPException(status_code=400, detail="Biometric credentials not registered for this user")
        
    cached = biometric_challenges.get(request.user_id)
    if not cached:
        raise HTTPException(status_code=400, detail="Challenge session not found. Please request a new challenge.")
        
    if datetime.utcnow() > cached["expires_at"]:
        if request.user_id in biometric_challenges:
            del biometric_challenges[request.user_id]
        raise HTTPException(status_code=400, detail="Challenge session expired. Please request a new challenge.")
        
    if cached["challenge"] != request.challenge:
        raise HTTPException(status_code=400, detail="Challenge verification failed. Nonce mismatch.")
        
    try:
        timestamp_dt = datetime.fromisoformat(request.timestamp.replace("Z", "+00:00"))
        timestamp_dt = timestamp_dt.astimezone(timezone.utc).replace(tzinfo=None)
        age = (datetime.utcnow() - timestamp_dt).total_seconds()
        if abs(age) > 300:
            raise HTTPException(status_code=400, detail="Verification timestamp is too old or out of sync.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid timestamp format: {e}")

    payload = f"{request.challenge}:{request.timestamp}:{request.device_latitude:.6f}:{request.device_longitude:.6f}"
    payload_bytes = payload.encode("utf-8")
    
    try:
        public_key_bytes = base64.b64decode(user.fingerkey)
        pub_key = load_der_public_key(public_key_bytes)
        signature_bytes = base64.b64decode(request.signature)
        pub_key.verify(
            signature_bytes,
            payload_bytes,
            ec.ECDSA(hashes.SHA256())
        )
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Cryptographic biometric signature verification failed: {e}")
        
    if request.user_id in biometric_challenges:
        del biometric_challenges[request.user_id]

    boundaries = db.query(CampusBoundary).filter(CampusBoundary.is_active == True).all()
    if not boundaries:
        raise HTTPException(status_code=400, detail="No active campus boundaries found")
        
    closest_boundary = None
    min_distance = float('inf')
    is_verified = False
    target_boundary = None
    
    for b in boundaries:
        distance = haversine_distance(
            request.device_latitude, request.device_longitude,
            b.center_latitude, b.center_longitude
        )
        if distance <= b.radius_meters:
            is_verified = True
            target_boundary = b
            min_distance = distance
            break
        else:
            if distance < min_distance:
                min_distance = distance
                closest_boundary = b
                
    if is_verified:
        status = "verified"
        selected_boundary = target_boundary
    else:
        status = "rejected"
        selected_boundary = closest_boundary if closest_boundary else boundaries[0]
        
    db_log = AttendanceLog(
        user_id=user.id,
        campus_boundary_id=selected_boundary.id,
        device_latitude=request.device_latitude,
        device_longitude=request.device_longitude,
        calculated_distance=min_distance,
        status=status,
        check_in_time=datetime.utcnow()
    )
    db.add(db_log)
    db.commit()
    db.refresh(db_log)
    
    return {
        "id": str(db_log.id),
        "status": db_log.status,
        "location_name": selected_boundary.location_name,
        "calculated_distance": db_log.calculated_distance,
        "check_in_time": db_log.check_in_time.isoformat() + "Z"
    }

@app.get("/users/{user_id}/attendance-status")
def get_user_attendance_status(user_id: str, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    today_start = datetime.utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
    latest_log = db.query(AttendanceLog)\
        .filter(AttendanceLog.user_id == user_uuid)\
        .filter(AttendanceLog.check_in_time >= today_start)\
        .order_by(AttendanceLog.check_in_time.desc())\
        .first()
    face_reg = user.face_image is not None
    finger_reg = user.fingerkey is not None

    if not latest_log:
        return {
            "can_clock_in": True,
            "can_clock_out": False,
            "last_action": None,
            "check_in_time": None,
            "check_out_time": None,
            "face_registered": face_reg,
            "fingerprint_registered": finger_reg
        }
    if latest_log.check_out_time is None:
        return {
            "can_clock_in": False,
            "can_clock_out": True,
            "last_action": "check-in",
            "check_in_time": latest_log.check_in_time.isoformat() + "Z",
            "check_out_time": None,
            "face_registered": face_reg,
            "fingerprint_registered": finger_reg
        }
    else:
        return {
            "can_clock_in": True,
            "can_clock_out": False,
            "last_action": "check-out",
            "check_in_time": latest_log.check_in_time.isoformat() + "Z",
            "check_out_time": latest_log.check_out_time.isoformat() + "Z",
            "face_registered": face_reg,
            "fingerprint_registered": finger_reg
        }

@app.post("/check-out-face")
def check_out_face(request: FaceCheckInRequest, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(request.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if not user.face_image:
        raise HTTPException(status_code=400, detail="Face biometric not registered. Please register first.")
    try:
        verify_face_in_image(request.face_image)
        is_match, similarity, features = verify_face_match(user.face_image, request.face_image)
        if not is_match:
            raise ValueError(f"Face does not match registered profile (Similarity: {similarity:.2f}, Features: {features}).")
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=400, detail=f"Face verification failed: {e}")
    log = db.query(AttendanceLog)\
        .filter(AttendanceLog.user_id == user.id)\
        .filter(AttendanceLog.check_out_time == None)\
        .order_by(AttendanceLog.check_in_time.desc())\
        .first()
    if not log:
        raise HTTPException(status_code=400, detail="No active check-in session found. Please check in first.")
    boundaries = db.query(CampusBoundary).filter(CampusBoundary.is_active == True).all()
    if not boundaries:
        raise HTTPException(status_code=400, detail="No active campus boundaries found")
    closest_boundary = None
    min_distance = float('inf')
    is_verified = False
    target_boundary = None
    for b in boundaries:
        distance = haversine_distance(
            request.device_latitude, request.device_longitude,
            b.center_latitude, b.center_longitude
        )
        if distance <= b.radius_meters:
            is_verified = True
            target_boundary = b
            min_distance = distance
            break
        else:
            if distance < min_distance:
                min_distance = distance
                closest_boundary = b
    if is_verified:
        status = "verified"
        selected_boundary = target_boundary
    else:
        closest_name = closest_boundary.location_name if closest_boundary else "Unknown"
        raise HTTPException(
            status_code=400,
            detail=f"Out of bounds. Clock-out is only permitted within designated campus boundaries (Closest: {closest_name}, Distance: {min_distance:.1f}m)."
        )
    log.check_out_time = datetime.utcnow()
    log.check_out_latitude = request.device_latitude
    log.check_out_longitude = request.device_longitude
    log.check_out_distance = min_distance
    log.check_out_status = status
    db.commit()
    db.refresh(log)
    return {
        "id": str(log.id),
        "status": log.check_out_status,
        "location_name": selected_boundary.location_name,
        "calculated_distance": log.check_out_distance,
        "check_out_time": log.check_out_time.isoformat() + "Z"
    }

@app.post("/check-out-biometric")
def check_out_biometric(request: BiometricCheckInRequest, db: Session = Depends(get_db)):
    import uuid
    import base64
    from cryptography.hazmat.primitives.serialization import load_der_public_key
    from cryptography.hazmat.primitives.asymmetric import ec
    from cryptography.hazmat.primitives import hashes
    try:
        user_uuid = uuid.UUID(request.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if not user.fingerkey:
        raise HTTPException(status_code=400, detail="Biometric credentials not registered for this user")
    cached = biometric_challenges.get(request.user_id)
    if not cached:
        raise HTTPException(status_code=400, detail="Challenge session not found. Please request a new challenge.")
    if datetime.utcnow() > cached["expires_at"]:
        if request.user_id in biometric_challenges:
            del biometric_challenges[request.user_id]
        raise HTTPException(status_code=400, detail="Challenge session expired. Please request a new challenge.")
    if cached["challenge"] != request.challenge:
        raise HTTPException(status_code=400, detail="Challenge verification failed. Nonce mismatch.")
    try:
        timestamp_dt = datetime.fromisoformat(request.timestamp.replace("Z", "+00:00"))
        timestamp_dt = timestamp_dt.astimezone(timezone.utc).replace(tzinfo=None)
        age = (datetime.utcnow() - timestamp_dt).total_seconds()
        if abs(age) > 300:
            raise HTTPException(status_code=400, detail="Verification timestamp is too old or out of sync.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid timestamp format: {e}")
    payload = f"{request.challenge}:{request.timestamp}:{request.device_latitude:.6f}:{request.device_longitude:.6f}"
    payload_bytes = payload.encode("utf-8")
    try:
        public_key_bytes = base64.b64decode(user.fingerkey)
        pub_key = load_der_public_key(public_key_bytes)
        signature_bytes = base64.b64decode(request.signature)
        pub_key.verify(
            signature_bytes,
            payload_bytes,
            ec.ECDSA(hashes.SHA256())
        )
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Cryptographic biometric signature verification failed: {e}")
    if request.user_id in biometric_challenges:
        del biometric_challenges[request.user_id]
    log = db.query(AttendanceLog)\
        .filter(AttendanceLog.user_id == user.id)\
        .filter(AttendanceLog.check_out_time == None)\
        .order_by(AttendanceLog.check_in_time.desc())\
        .first()
    if not log:
        raise HTTPException(status_code=400, detail="No active check-in session found. Please check in first.")
    boundaries = db.query(CampusBoundary).filter(CampusBoundary.is_active == True).all()
    if not boundaries:
        raise HTTPException(status_code=400, detail="No active campus boundaries found")
    closest_boundary = None
    min_distance = float('inf')
    is_verified = False
    target_boundary = None
    for b in boundaries:
        distance = haversine_distance(
            request.device_latitude, request.device_longitude,
            b.center_latitude, b.center_longitude
        )
        if distance <= b.radius_meters:
            is_verified = True
            target_boundary = b
            min_distance = distance
            break
        else:
            if distance < min_distance:
                min_distance = distance
                closest_boundary = b
    if is_verified:
        status = "verified"
        selected_boundary = target_boundary
    else:
        closest_name = closest_boundary.location_name if closest_boundary else "Unknown"
        raise HTTPException(
            status_code=400,
            detail=f"Out of bounds. Clock-out is only permitted within designated campus boundaries (Closest: {closest_name}, Distance: {min_distance:.1f}m)."
        )
    log.check_out_time = datetime.utcnow()
    log.check_out_latitude = request.device_latitude
    log.check_out_longitude = request.device_longitude
    log.check_out_distance = min_distance
    log.check_out_status = status
    db.commit()
    db.refresh(log)
    return {
        "id": str(log.id),
        "status": log.check_out_status,
        "location_name": selected_boundary.location_name,
        "calculated_distance": log.check_out_distance,
        "check_out_time": log.check_out_time.isoformat() + "Z"
    }





class BiometricUpdateRequestSchema(BaseModel):
    user_id: str
    request_type: str
    new_public_key: Optional[str] = None
    new_face_image: Optional[str] = None


def get_or_download_models():
    import os
    import urllib.request
    
    backend_dir = os.path.dirname(os.path.abspath(__file__))
    models_dir = os.path.join(backend_dir, "models")
    if not os.path.exists(models_dir):
        os.makedirs(models_dir, exist_ok=True)
        
    yunet_path = os.path.join(models_dir, "face_detection_yunet_2023mar.onnx")
    sface_path = os.path.join(models_dir, "face_recognition_sface_2021dec.onnx")
    
    yunet_url = "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx"
    sface_url = "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx"
    
    if not os.path.exists(yunet_path):
        print(f"Downloading YuNet face detector model to {yunet_path}...")
        urllib.request.urlretrieve(yunet_url, yunet_path)
        
    if not os.path.exists(sface_path):
        print(f"Downloading SFace face recognizer model to {sface_path}...")
        urllib.request.urlretrieve(sface_url, sface_path)
        
    return yunet_path, sface_path


def verify_face_in_image(base64_image_str: str) -> bytes:
    import base64
    import cv2
    import numpy as np
    
    if base64_image_str.startswith("data:image"):
        comma_idx = base64_image_str.find(",")
        if comma_idx != -1:
            base64_image_str = base64_image_str[comma_idx+1:]
            
    try:
        image_bytes = base64.b64decode(base64_image_str)
    except Exception as e:
        raise ValueError(f"Failed to decode base64 image: {e}")
        
    is_jpeg = image_bytes.startswith(b'\xff\xd8')
    is_png = image_bytes.startswith(b'\x89PNG\r\n\x1a\n')
    if not (is_jpeg or is_png) or len(image_bytes) < 100:
        raise ValueError("Invalid image file format header.")
        
    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img is None:
        raise ValueError("Unable to parse frame as image.")
        
    h, w = img.shape[:2]
    
    # Get model paths
    yunet_path, _ = get_or_download_models()
    
    # Initialize YuNet face detector
    detector = cv2.FaceDetectorYN.create(yunet_path, "", (w, h))
    
    # Try detection
    _, faces = detector.detect(img)
    
    # Try rotation if no face found directly
    if faces is None or len(faces) == 0:
        for angle in [cv2.ROTATE_90_CLOCKWISE, cv2.ROTATE_180, cv2.ROTATE_90_COUNTERCLOCKWISE]:
            rotated_img = cv2.rotate(img, angle)
            rh, rw = rotated_img.shape[:2]
            detector.setInputSize((rw, rh))
            _, rotated_faces = detector.detect(rotated_img)
            if rotated_faces is not None and len(rotated_faces) > 0:
                faces = rotated_faces
                break
                
    if faces is None or len(faces) == 0:
        raise ValueError(f"No face detected in the frame (Image: {w}x{h}). Please align your face inside the scanner frame.")
        
    return image_bytes


@app.post("/register-face")
def register_face(request: FaceRegisterRequest, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(request.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    try:
        verify_face_in_image(request.face_image)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Face registration failed: {e}")
        
    user.face_image = request.face_image
    db.commit()
    return {"detail": "Face profile registered successfully"}


def verify_face_match(registered_base64: str, captured_base64: str):
    import base64
    import cv2
    import numpy as np
    
    try:
        reg_bytes = base64.b64decode(registered_base64)
        cap_bytes = base64.b64decode(captured_base64)
    except Exception:
        return False, 0.0, 0
        
    nparr1 = np.frombuffer(reg_bytes, np.uint8)
    nparr2 = np.frombuffer(cap_bytes, np.uint8)
    img1 = cv2.imdecode(nparr1, cv2.IMREAD_COLOR)
    img2 = cv2.imdecode(nparr2, cv2.IMREAD_COLOR)
    if img1 is None or img2 is None:
        return False, 0.0, 0
        
    yunet_path, sface_path = get_or_download_models()
    
    detector = cv2.FaceDetectorYN.create(yunet_path, "", (0, 0))
    recognizer = cv2.FaceRecognizerSF.create(sface_path, "")
    
    def detect_and_align(img):
        h, w = img.shape[:2]
        detector.setInputSize((w, h))
        _, faces = detector.detect(img)
        
        # Try rotations if no face is detected
        rotated_img = img
        if faces is None or len(faces) == 0:
            for angle in [cv2.ROTATE_90_CLOCKWISE, cv2.ROTATE_180, cv2.ROTATE_90_COUNTERCLOCKWISE]:
                temp_img = cv2.rotate(img, angle)
                th, tw = temp_img.shape[:2]
                detector.setInputSize((tw, th))
                _, temp_faces = detector.detect(temp_img)
                if temp_faces is not None and len(temp_faces) > 0:
                    faces = temp_faces
                    rotated_img = temp_img
                    break
                    
        if faces is None or len(faces) == 0:
            return None
            
        # Get the face with largest area
        faces = sorted(faces, key=lambda f: f[2]*f[3], reverse=True)
        aligned_face = recognizer.alignCrop(rotated_img, faces[0])
        return aligned_face

    aligned1 = detect_and_align(img1)
    aligned2 = detect_and_align(img2)
    
    if aligned1 is None or aligned2 is None:
        return False, 0.0, 0
        
    feat1 = recognizer.feature(aligned1)
    feat2 = recognizer.feature(aligned2)
    
    # Match using Cosine similarity and L2 Norm distance
    cosine_similarity = recognizer.match(feat1, feat2, cv2.FaceRecognizerSF_FR_COSINE)
    l2_dist = recognizer.match(feat1, feat2, cv2.FaceRecognizerSF_FR_NORM_L2)
    
    # Official EER thresholds for SFace:
    # Cosine Similarity >= 0.363
    # L2 distance <= 1.128
    # For a high-security attendance system, we require:
    # Cosine Similarity >= 0.50 and L2 distance <= 1.0 (balancing FAR and FRR safely)
    is_match = cosine_similarity >= 0.50 and l2_dist <= 1.0
    
    return bool(is_match), float(cosine_similarity), float(l2_dist)


@app.post("/check-in-face")
def check_in_face(request: FaceCheckInRequest, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(request.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if not user.face_image:
        raise HTTPException(status_code=400, detail="Face biometric not registered. Please register first.")
        
    try:
        verify_face_in_image(request.face_image)
        is_match, similarity, features = verify_face_match(user.face_image, request.face_image)
        if not is_match:
            raise ValueError(f"Face does not match registered profile (Similarity: {similarity:.2f}, Features: {features}).")
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=400, detail=f"Face verification failed: {e}")

    boundaries = db.query(CampusBoundary).filter(CampusBoundary.is_active == True).all()
    if not boundaries:
        raise HTTPException(status_code=400, detail="No active campus boundaries found")
        
    closest_boundary = None
    min_distance = float('inf')
    is_verified = False
    target_boundary = None
    
    for b in boundaries:
        distance = haversine_distance(
            request.device_latitude, request.device_longitude,
            b.center_latitude, b.center_longitude
        )
        if distance <= b.radius_meters:
            is_verified = True
            target_boundary = b
            min_distance = distance
            break
        else:
            if distance < min_distance:
                min_distance = distance
                closest_boundary = b
                
    if is_verified:
        status = "verified"
        selected_boundary = target_boundary
    else:
        status = "rejected"
        selected_boundary = closest_boundary if closest_boundary else boundaries[0]
        
    db_log = AttendanceLog(
        user_id=user.id,
        campus_boundary_id=selected_boundary.id,
        device_latitude=request.device_latitude,
        device_longitude=request.device_longitude,
        calculated_distance=min_distance,
        status=status,
        check_in_time=datetime.utcnow()
    )
    db.add(db_log)
    db.commit()
    db.refresh(db_log)
    
    return {
        "id": str(db_log.id),
        "status": db_log.status,
        "location_name": selected_boundary.location_name,
        "calculated_distance": db_log.calculated_distance,
        "check_in_time": db_log.check_in_time.isoformat() + "Z"
    }


@app.post("/request-biometric-update")
def request_biometric_update(request: BiometricUpdateRequestSchema, db: Session = Depends(get_db)):
    import uuid
    try:
        user_uuid = uuid.UUID(request.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user ID format")
    user = db.query(User).filter(User.id == user_uuid).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    if request.request_type == "face":
        if not request.new_face_image:
            raise HTTPException(status_code=400, detail="Missing new face image payload.")
        try:
            verify_face_in_image(request.new_face_image)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Face verification failed: {e}")

    db_req = BiometricUpdateRequest(
        user_id=user.id,
        request_type=request.request_type,
        new_public_key=request.new_public_key,
        new_face_image=request.new_face_image,
        status="pending"
    )
    db.add(db_req)
    db.commit()
    return {"detail": "Biometric update request submitted for admin approval"}


@app.get("/biometric-requests")
def list_biometric_requests(status: Optional[str] = None, db: Session = Depends(get_db)):
    query = db.query(BiometricUpdateRequest)
    if status and status != "all":
        query = query.filter(BiometricUpdateRequest.status == status)
    requests = query.order_by(BiometricUpdateRequest.created_at.desc()).all()
    result = []
    for req in requests:
        user = db.query(User).filter(User.id == req.user_id).first()
        result.append({
            "id": str(req.id),
            "user_id": str(req.user_id),
            "user_name": user.full_name if user else "Unknown",
            "user_email": user.email if user else "Unknown",
            "request_type": req.request_type,
            "new_public_key": req.new_public_key,
            "new_face_image": req.new_face_image,
            "status": req.status,
            "created_at": req.created_at.isoformat() if req.created_at else None
        })
    return result


@app.post("/biometric-requests/{request_id}/approve")
def approve_biometric_request(request_id: str, db: Session = Depends(get_db)):
    import uuid
    try:
        req_uuid = uuid.UUID(request_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid request ID format")
        
    req = db.query(BiometricUpdateRequest).filter(BiometricUpdateRequest.id == req_uuid).first()
    if not req:
        raise HTTPException(status_code=404, detail="Request not found")
        
    user = db.query(User).filter(User.id == req.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    if req.request_type == "face":
        user.face_image = req.new_face_image
    elif req.request_type == "fingerprint":
        user.fingerkey = req.new_public_key
        
    req.status = "approved"
    db.commit()
    return {"detail": "Biometric update request approved successfully"}


@app.post("/biometric-requests/{request_id}/reject")
def reject_biometric_request(request_id: str, db: Session = Depends(get_db)):
    import uuid
    try:
        req_uuid = uuid.UUID(request_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid request ID format")
        
    req = db.query(BiometricUpdateRequest).filter(BiometricUpdateRequest.id == req_uuid).first()
    if not req:
        raise HTTPException(status_code=404, detail="Request not found")
        
    req.status = "rejected"
    db.commit()
    return {"detail": "Biometric update request rejected"}

