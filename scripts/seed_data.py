import random
import math
from datetime import datetime, timedelta
from sqlalchemy import text
from database import (
    SessionLocal, 
    InstitutionalDomain, 
    User, 
    CampusBoundary, 
    AttendanceLog, 
    BiometricUpdateRequest, 
    OTPCode, 
    Feedback, 
    SupportRequest, 
    AdminActionLog, 
    hash_password
)

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

def get_random_coordinate_within_radius(center_lat, center_lon, radius_meters):
    # 1 degree of latitude is approx 111,000 meters
    # 1 degree of longitude is approx 111,000 * cos(lat) meters
    r = random.uniform(0, radius_meters - 5.0) # slightly smaller than radius to guarantee it's inside
    theta = random.uniform(0, 2 * math.pi)
    
    delta_lat = (r * math.cos(theta)) / 111000.0
    delta_lon = (r * math.sin(theta)) / (111000.0 * math.cos(math.radians(center_lat)))
    
    return center_lat + delta_lat, center_lon + delta_lon

def seed_data():
    session = SessionLocal()
    print("[*] Starting database population...")

    try:
        # 1. Truncate all tables non-interactively
        print("[*] Clearing database table records...")
        tables = [
            "attendance_logs",
            "biometric_update_requests",
            "otp_codes",
            "feedbacks",
            "support_requests",
            "admin_action_logs",
            "users",
            "campus_boundaries",
            "institutional_domains"
        ]
        for table in tables:
            try:
                session.execute(text(f"TRUNCATE TABLE {table} RESTART IDENTITY CASCADE;"))
                print(f"[+] Truncated table: {table}")
            except Exception as e:
                print(f"[-] Error truncating {table}: {e}")
        session.commit()

        # 2. Seed Institutional Domains
        domain_nitc = InstitutionalDomain(domain_name="nitc.ac.in")
        domain_gmail = InstitutionalDomain(domain_name="gmail.com")
        session.add_all([domain_nitc, domain_gmail])
        session.flush() # Flush to get domain ids
        print(f"[+] Added domains: {domain_nitc.domain_name}, {domain_gmail.domain_name}")

        # 3. Seed Campus Boundaries
        b1 = CampusBoundary(
            location_name="Main Academic Block",
            center_latitude=11.321620,
            center_longitude=75.933610,
            radius_meters=70.0,
            is_active=True
        )
        b2 = CampusBoundary(
            location_name="Mega Hostel Complex",
            center_latitude=11.324150,
            center_longitude=75.935120,
            radius_meters=100.0,
            is_active=True
        )
        b3 = CampusBoundary(
            location_name="Library Complex",
            center_latitude=11.322010,
            center_longitude=75.934050,
            radius_meters=50.0,
            is_active=True
        )
        session.add_all([b1, b2, b3])
        session.flush()
        print("[+] Added campus boundaries.")

        # 4. Seed Users (1 Admin, 52 Employees)
        admin = User(
            email="kattaabhiram368@gmail.com",
            full_name="Katta Abhiram",
            role="admin",
            password=hash_password("password"),
            phone="+91 98765 43210",
            domain_id=domain_gmail.id
        )
        session.add(admin)
        
        # Name Pools for programmatically generating 52 unique users
        first_names = [
            "Arjun", "Aditi", "Amit", "Anjali", "Bhavna", "Chaitanya", "Deepak", "Divya", "Ganesh", "Gauri", 
            "Hari", "Isha", "Jaya", "Karan", "Kavita", "Madhav", "Meera", "Nikhil", "Neha", "Pranav", 
            "Pooja", "Rahul", "Riya", "Sanjay", "Sneha", "Tanvi", "Uday", "Uma", "Varun", "Vidya",
            "Vijay", "Yash", "Zara", "Rohan", "Priya", "Manish", "Kriti", "Rajesh", "Aishwarya", "Vikram",
            "Sunita", "Ramesh", "Kiran", "Suresh", "Geeta", "Anil", "Seema", "Abhishek", "Shweta", "Vivek",
            "Karthik", "Meenakshi", "Akash", "Swati"
        ]
        last_names = [
            "Sharma", "Verma", "Gupta", "Reddy", "Nair", "Pillai", "Patel", "Joshi", "Rao", "Kumar", 
            "Singh", "Mishra", "Das", "Choudhury", "Roy", "Sen", "Mehta", "Bose", "Jha", "Prasad",
            "Menon", "Shenoy", "Bhat", "Kulkarni", "Deshmukh"
        ]
        
        generated_names = []
        for f in first_names:
            for l in last_names:
                generated_names.append((f"{f} {l}", f"{f.lower()}.{l.lower()}@nitc.ac.in"))
                
        # Shuffle names and select 52
        random.seed(12345)
        random.shuffle(generated_names)
        selected_names = generated_names[:52]
        
        employees = []
        for idx, (name, email) in enumerate(selected_names):
            phone = f"+91 94460 {random.randint(10000, 99999)}"
            # 80% of employees have registered biometrics
            has_biometrics = random.random() < 0.8
            biometric_key = f"mock_key_{idx}" if has_biometrics else None
            fingerkey = f"mock_key_{idx}" if has_biometrics else None
            face_image = f"mock_face_image_{idx}" if has_biometrics else None
            
            emp = User(
                email=email,
                full_name=name,
                role="employee",
                password=hash_password("password"),
                phone=phone,
                domain_id=domain_nitc.id,
                biometric_public_key=biometric_key,
                fingerkey=fingerkey,
                face_image=face_image
            )
            session.add(emp)
            employees.append(emp)
            
        session.flush()
        print(f"[+] Added 1 Admin and {len(employees)} programmatically-generated employee users.")

        # 5. Seed Attendance Logs for Past Week (Shifts)
        # Shift simulation from 7 days ago until today (2026-07-19)
        today = datetime(2026, 7, 19)
        boundaries_list = [b1, b2, b3]
        
        print("[*] Generating high-fidelity shift logs for 52 users for the past week...")
        attendance_logs_count = 0
        for i in range(7, -1, -1):
            log_date = today - timedelta(days=i)
            is_weekend = log_date.weekday() in [5, 6] # Saturday=5, Sunday=6
            
            for emp in employees:
                # 85% attendance rate on weekdays, 15% on weekends
                attendance_chance = 0.15 if is_weekend else 0.85
                if random.random() > attendance_chance:
                    continue
                
                # Select a random boundary for the day's work
                boundary = random.choice(boundaries_list)
                
                # Check-in time: random between 08:30 and 09:30 local/naive time
                check_in_hour = random.randint(8, 9)
                check_in_minute = random.randint(0, 59) if check_in_hour == 9 else random.randint(30, 59)
                check_in = log_date.replace(hour=check_in_hour, minute=check_in_minute, second=random.randint(0, 59))
                
                # 3% probability of a completely rejected check-in (offsite)
                is_rejected = random.random() < 0.03
                
                if is_rejected:
                    # Device coordinates far outside boundary (e.g. 500-1000m away)
                    dev_lat = boundary.center_latitude + 0.007
                    dev_lon = boundary.center_longitude + 0.007
                    dist = haversine_distance(boundary.center_latitude, boundary.center_longitude, dev_lat, dev_lon)
                    status = "rejected"
                    check_out = None
                    check_out_lat = None
                    check_out_lon = None
                    check_out_dist = None
                    check_out_status = None
                else:
                    dev_lat, dev_lon = get_random_coordinate_within_radius(boundary.center_latitude, boundary.center_longitude, boundary.radius_meters)
                    dist = haversine_distance(boundary.center_latitude, boundary.center_longitude, dev_lat, dev_lon)
                    status = "verified"
                    
                    # 3% probability of missing check-out
                    is_missed_checkout = random.random() < 0.03
                    if is_missed_checkout:
                        check_out = None
                        check_out_lat = None
                        check_out_lon = None
                        check_out_dist = None
                        check_out_status = None
                    else:
                        # Check-out time: 8-9 hours after check-in
                        check_out_hour = check_in_hour + random.randint(8, 9)
                        check_out_minute = (check_in_minute + random.randint(-15, 30)) % 60
                        check_out = log_date.replace(hour=check_out_hour, minute=check_out_minute, second=random.randint(0, 59))
                        
                        # 3% probability of check-out rejection
                        is_checkout_rejected = random.random() < 0.03
                        if is_checkout_rejected:
                            check_out_lat = boundary.center_latitude + 0.007
                            check_out_lon = boundary.center_longitude + 0.007
                            check_out_dist = haversine_distance(boundary.center_latitude, boundary.center_longitude, check_out_lat, check_out_lon)
                            check_out_status = "rejected"
                        else:
                            check_out_lat, check_out_lon = get_random_coordinate_within_radius(boundary.center_latitude, boundary.center_longitude, boundary.radius_meters)
                            check_out_dist = haversine_distance(boundary.center_latitude, boundary.center_longitude, check_out_lat, check_out_lon)
                            check_out_status = "verified"
                
                log = AttendanceLog(
                    user_id=emp.id,
                    campus_boundary_id=boundary.id,
                    device_latitude=dev_lat,
                    device_longitude=dev_lon,
                    calculated_distance=dist,
                    status=status,
                    check_in_time=check_in,
                    check_out_time=check_out,
                    check_out_latitude=check_out_lat,
                    check_out_longitude=check_out_lon,
                    check_out_distance=check_out_dist,
                    check_out_status=check_out_status
                )
                session.add(log)
                attendance_logs_count += 1
                
        session.flush()
        print(f"[+] Generated {attendance_logs_count} shift attendance logs for the past week.")

        # 6. Seed Biometric Update Requests
        print("[*] Generating biometric update requests...")
        # Create about 10 biometric requests from different employees
        for i in range(10):
            emp = employees[i % len(employees)]
            req_type = "face" if i % 2 == 0 else "fingerprint"
            status = "pending" if i < 3 else ("approved" if i < 8 else "rejected")
            created_at = today - timedelta(days=random.randint(1, 5), hours=random.randint(1, 12))
            
            req = BiometricUpdateRequest(
                user_id=emp.id,
                request_type=req_type,
                new_face_image=f"mock_face_data_{emp.full_name.lower().replace(' ', '_')}_{i}" if req_type == "face" else None,
                new_public_key=f"mock_public_key_{i}" if req_type == "fingerprint" else None,
                status=status,
                created_at=created_at
            )
            session.add(req)
        print("[+] Generated 10 biometric update requests.")

        # 7. Seed OTP Codes
        print("[*] Generating OTP verification codes...")
        # Create 15 OTP records for various employee accounts
        for i in range(15):
            emp = employees[i % len(employees)]
            purpose = "forgot_password" if i % 3 == 0 else "register"
            is_used = i % 2 == 0
            created_at = today - timedelta(days=random.randint(1, 5), minutes=random.randint(10, 60))
            expires_at = created_at + timedelta(minutes=5)
            
            otp = OTPCode(
                email=emp.email,
                otp_code=str(random.randint(100000, 999999)),
                purpose=purpose,
                expires_at=expires_at,
                is_used=is_used,
                created_at=created_at
            )
            session.add(otp)
        print("[+] Generated 15 OTP verification codes.")

        # 8. Seed Feedbacks
        print("[*] Generating feedbacks...")
        # Create 12 feedback records
        feedback_messages = [
            "The check-in process is very smooth and works perfectly.",
            "Would love if we had support for darker themes on the app.",
            "Library Complex geofence seems to have a minor coordinate lag.",
            "Can we show weekly summary statistics on the main page?",
            "Good app! Makes attendance tracking extremely simple.",
            "Had minor issues logging in via Face biometric key on Thursday, but it resolved after a reboot.",
            "The distance calculations are very precise.",
            "Please expand the Mega Hostel Complex radius slightly.",
            "Very simple and intuitive user interface.",
            "Support desk responded very quickly to my ticket. Thanks!",
            "Faced coordinate mismatch issue once when mobile data was weak.",
            "Excellent geofencing app."
        ]
        for i in range(12):
            emp = employees[i % len(employees)]
            fb = Feedback(
                user_id=emp.id,
                message=feedback_messages[i],
                created_at=today - timedelta(days=random.randint(1, 6), hours=random.randint(1, 12))
            )
            session.add(fb)
        print("[+] Generated 12 feedback submissions.")

        # 9. Seed Support Requests
        print("[*] Generating support requests...")
        # Create 10 support requests (some pending, some replied)
        support_messages = [
            "I changed my phone and need to register my face biometric again. How do I proceed?",
            "Is checking in from the hostel campus allowed if we work remotely?",
            "Can we check-in multiple times a day if we leave campus for lunch?",
            "My registration OTP is not arriving on my email address.",
            "Geofence showing 'Out of boundary' even when I am standing in Main Gate area.",
            "How do I update my registered phone number?",
            "Face verification is failing in low light conditions.",
            "Is there a desktop dashboard for employees to view their records?",
            "I missed checking out yesterday. How do I correct my log?",
            "Does the application track my GPS location when I am not clocked in?"
        ]
        support_replies = [
            "Please submit a biometric update request under your Profile page, and the admin will approve it.",
            "Remote work policies must be approved by your manager first. The app validates against physical coordinates.",
            "Yes, you can check-in again. The application logs multiple sessions under daily history.",
            "Please check your spam folder or verify if the whitelisted institutional domain was used.",
            "Try toggling high-accuracy GPS on your mobile settings and retry.",
            "You can edit your phone number directly on the Profile page.",
            "Ensure proper illumination. You can also update your face reference image via a biometric update request.",
            "Currently, the employee portal is mobile-only, but admin dashboard is accessible on web.",
            "You can contact support or your admin with the exact timings to update it manually.",
            "No, the app only queries location during manual check-in and check-out actions. Your privacy is fully protected."
        ]
        for i in range(10):
            emp = employees[i % len(employees)]
            status = "pending" if i < 3 else "replied"
            created_at = today - timedelta(days=random.randint(2, 6), hours=random.randint(1, 12))
            replied_at = created_at + timedelta(hours=random.randint(1, 24)) if status == "replied" else None
            reply = support_replies[i] if status == "replied" else None
            
            sr = SupportRequest(
                user_id=emp.id,
                message=support_messages[i],
                reply=reply,
                status=status,
                created_at=created_at,
                replied_at=replied_at
            )
            session.add(sr)
        print("[+] Generated 10 support requests.")

        # 10. Seed Admin Action Logs
        print("[*] Generating admin audit logs...")
        # Create 15 admin action logs
        admin_actions = [
            ("ADD", "LOCATION", "Library Complex", "Radius: 50.0m, Center: (11.322010, 75.934050)"),
            ("ADD", "LOCATION", "Main Academic Block", "Radius: 70.0m, Center: (11.321620, 75.933610)"),
            ("ADD", "LOCATION", "Mega Hostel Complex", "Radius: 100.0m, Center: (11.324150, 75.935120)"),
            ("EDIT", "LOCATION", "Mega Hostel Complex", "Updated radius to 110m"),
            ("EDIT", "USER", "sneha.reddy@nitc.ac.in", "Approved face biometric key update request"),
            ("DELETE", "LOCATION", "Old Academic Zone", "Removed inactive campus boundary ID 4"),
            ("EDIT", "USER", "rahul.sharma@nitc.ac.in", "Updated phone number to +91 94460 55555"),
            ("EDIT", "LOCATION", "Library Complex", "Updated status to active"),
            ("EDIT", "USER", "vikram.das@nitc.ac.in", "Reset biometric enrollment"),
            ("ADD", "USER", "new.hire@nitc.ac.in", "Created user account for new employee"),
            ("EDIT", "USER", "new.hire@nitc.ac.in", "Assigned employee role"),
            ("EDIT", "LOCATION", "Main Academic Block", "Updated radius from 60m to 70m"),
            ("EDIT", "USER", "karan.rao@nitc.ac.in", "Updated employee status to active"),
            ("EDIT", "USER", "riya.sen@nitc.ac.in", "Approved face key registration"),
            ("EDIT", "USER", "yash.kumar@nitc.ac.in", "Rejected expired biometric register request")
        ]
        for idx, (action_type, target_type, target_name, details) in enumerate(admin_actions):
            log = AdminActionLog(
                admin_email="kattaabhiram368@gmail.com",
                action_type=action_type,
                target_type=target_type,
                target_name=target_name,
                details=details,
                created_at=today - timedelta(days=random.randint(1, 6), hours=random.randint(1, 12))
            )
            session.add(log)
        print("[+] Generated 15 admin audit action logs.")

        session.commit()
        print("[+] Database population complete successfully!")

    except Exception as e:
        session.rollback()
        print(f"[-] Error seeding database tables: {e}")
    finally:
        session.close()

if __name__ == "__main__":
    seed_data()