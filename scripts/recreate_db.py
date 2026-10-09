from database import engine, Base, init_db
import sys
import os

# Add current folder to sys.path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

print("[*] Dropping all tables...")
Base.metadata.drop_all(bind=engine)
print("[*] Recreating tables...")
init_db()
print("[+] Database recreated successfully.")
