from sqlalchemy import text
from database import SessionLocal

def kill_data():
    confirm = input("CRITICAL: This will destroy all system records but leave schemas intact. Continue? (y/N): ")
    if confirm.lower() != 'y':
        print("[*] Operation aborted safely.")
        return

    session = SessionLocal()
    print("[*] Clearing database table records...")
    try:
        # Use TRUNCATE with CASCADE to quickly empty tables without dropping them
        # order respects independent targets if not utilizing direct cascade hooks
        session.execute(text("TRUNCATE TABLE attendance_logs RESTART IDENTITY CASCADE;"))
        session.execute(text("TRUNCATE TABLE users RESTART IDENTITY CASCADE;"))
        session.execute(text("TRUNCATE TABLE campus_boundaries RESTART IDENTITY CASCADE;"))
        session.execute(text("TRUNCATE TABLE institutional_domains RESTART IDENTITY CASCADE;"))
        
        session.commit()
        print("[+] Database wiped completely. Table schemas preserved.")
    except Exception as e:
        session.rollback()
        print(f"[-] Execution error during truncate pipeline: {e}")
    finally:
        session.close()

if __name__ == "__main__":
    kill_data()