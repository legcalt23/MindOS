import os
import sys
import argparse
import imaplib

def verify_credentials(email_addr, app_password):
    print(f"Connecting to imap.gmail.com to verify credentials for {email_addr}...")
    try:
        # Connect to Gmail's IMAP server using SSL
        mail = imaplib.IMAP4_SSL("imap.gmail.com", 993)
        mail.login(email_addr, app_password)
        mail.logout()
        print("Success: Successfully authenticated with Gmail!")
        return True
    except imaplib.IMAP4.error as e:
        print(f"Authentication Failed: {e}", file=sys.stderr)
        print("Please ensure that 2-Step Verification is enabled and you are using a correct 16-character App Password (not your main Google account password).", file=sys.stderr)
        return False
    except Exception as e:
        print(f"Connection Error: {e}", file=sys.stderr)
        return False

def update_env_file(email_addr, app_password):
    env_path = os.path.join(os.getcwd(), ".env")
    lines = []
    
    # Read existing .env if present
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            lines = f.readlines()
            
    # Clean spacing and update or append keys
    email_key = "GMAIL_EMAIL"
    pass_key = "GMAIL_APP_PASSWORD"
    
    email_found = False
    pass_found = False
    
    for idx, line in enumerate(lines):
        clean_line = line.strip()
        if clean_line.startswith(f"{email_key}="):
            lines[idx] = f"{email_key}={email_addr}\n"
            email_found = True
        elif clean_line.startswith(f"{pass_key}="):
            # Strip spaces from password before saving
            clean_pwd = app_password.replace(" ", "")
            lines[idx] = f"{pass_key}={clean_pwd}\n"
            pass_found = True
            
    if not email_found:
        lines.append(f"{email_key}={email_addr}\n")
    if not pass_found:
        clean_pwd = app_password.replace(" ", "")
        lines.append(f"{pass_key}={clean_pwd}\n")
        
    # Write back to .env
    with open(env_path, "w", encoding="utf-8") as f:
        f.writelines(lines)
        
    print(f"Updated .env successfully at: {env_path}")

def main():
    parser = argparse.ArgumentParser(description="Configure and verify Gmail login credentials.")
    parser.add_argument("--email", required=True, help="Gmail email address (e.g. user@gmail.com)")
    parser.add_argument("--password", required=True, help="Gmail 16-character App Password (e.g. 'abcd efgh ijkl mnop')")
    
    args = parser.parse_args()
    
    email_addr = args.email.strip()
    app_password = args.password.strip()
    
    if verify_credentials(email_addr, app_password):
        update_env_file(email_addr, app_password)
        sys.exit(0)
    else:
        print("Configuration was NOT saved due to verification failure.", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
