import os
import sys
import ssl
import imaplib
import email
import json
import re
from email.header import decode_header

def load_env():
    env = {}
    # Search for .env in current directory or parent directory
    env_path = ".env"
    if not os.path.exists(env_path):
        env_path = os.path.join("..", ".env")
        
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    parts = line.split("=", 1)
                    if len(parts) == 2:
                        env[parts[0].strip()] = parts[1].strip()
    return env

def decode_mime_header(val):
    if not val:
        return ""
    try:
        parts = decode_header(val)
        decoded = []
        for text, charset in parts:
            if isinstance(text, bytes):
                decoded.append(text.decode(charset or "utf-8", errors="ignore"))
            else:
                decoded.append(text)
        return "".join(decoded)
    except Exception:
        return str(val)

def get_email_body(msg):
    body = ""
    try:
        if msg.is_multipart():
            for part in msg.walk():
                content_type = part.get_content_type()
                content_disposition = str(part.get("Content-Disposition"))
                if content_type == "text/plain" and "attachment" not in content_disposition:
                    payload = part.get_payload(decode=True)
                    if payload:
                        body += payload.decode(part.get_content_charset() or "utf-8", errors="ignore")
        else:
            payload = msg.get_payload(decode=True)
            if payload:
                body = payload.decode(msg.get_content_charset() or "utf-8", errors="ignore")
                
        # If plain text body is empty, try html body
        if not body.strip() and msg.is_multipart():
            for part in msg.walk():
                if part.get_content_type() == "text/html":
                    payload = part.get_payload(decode=True)
                    if payload:
                        html = payload.decode(part.get_content_charset() or "utf-8", errors="ignore")
                        # Basic HTML tag stripping
                        body = re.sub(r'<[^>]+>', ' ', html)
    except Exception as e:
        body = f"[Error extracting body: {e}]"
    return body

def clean_snippet(text):
    # Clean up whitespace and line breaks
    text = re.sub(r'\s+', ' ', text).strip()
    return text[:600] + ("..." if len(text) > 600 else "")

def main():
    # Resolve directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    processed_path = os.path.join(script_dir, "processed_emails.json")
    pending_path = os.path.join(script_dir, "pending_emails.json")
    
    # Load processed list
    processed_ids = []
    if os.path.exists(processed_path):
        try:
            with open(processed_path, "r", encoding="utf-8") as f:
                processed_ids = json.load(f)
        except Exception:
            pass
            
    # Load credentials
    env = load_env()
    email_addr = env.get("GMAIL_EMAIL")
    app_password = env.get("GMAIL_APP_PASSWORD")
    
    if not email_addr or not app_password:
        print("Error: GMAIL_EMAIL and GMAIL_APP_PASSWORD must be configured in .env", file=sys.stderr)
        sys.exit(1)
        
    try:
        # Connect to Gmail
        mail = imaplib.IMAP4_SSL("imap.gmail.com", 993)
        mail.login(email_addr, app_password)
        mail.select("inbox")
        
        # Search unread messages
        status, response = mail.uid('search', None, 'UNSEEN')
        if status != 'OK' or not response[0]:
            print("No new unread messages.")
            mail.logout()
            sys.exit(0)
            
        uids = response[0].split()
        print(f"Found {len(uids)} unread messages. Checking relevance...")
        
        new_pending = []
        
        for uid in uids:
            uid_str = uid.decode("utf-8")
            
            # Fetch message with PEEK to prevent marking as read
            # Also fetch Gmail thread ID (X-GM-THRID)
            res, data = mail.uid('fetch', uid, '(BODY.PEEK[] X-GM-THRID)')
            if res != 'OK' or not data:
                continue
                
            raw_email = None
            thrid_hex = ""
            
            for part in data:
                if isinstance(part, tuple):
                    raw_email = part[1]
                    # Parse out metadata (which is part[0]) for the Gmail thread ID
                    metadata = part[0].decode("utf-8", errors="ignore")
                    thrid_match = re.search(r'X-GM-THRID\s+(\d+)', metadata)
                    if thrid_match:
                        thrid_decimal = thrid_match.group(1)
                        try:
                            thrid_hex = hex(int(thrid_decimal))[2:]
                        except Exception:
                            pass
                    break
                    
            if not raw_email:
                continue
                
            msg = email.message_from_bytes(raw_email)
            
            # Extract headers
            msg_id = msg.get("Message-ID", f"uid-{uid_str}")
            if msg_id in processed_ids:
                continue
                
            sender = decode_mime_header(msg.get("From", ""))
            subject = decode_mime_header(msg.get("Subject", ""))
            date_str = decode_mime_header(msg.get("Date", ""))
            
            # Extract sender email address using simple regex
            sender_email = ""
            email_match = re.search(r'<([^>]+)>', sender)
            if email_match:
                sender_email = email_match.group(1)
            else:
                sender_email = sender
                
            body = get_email_body(msg)
            
            # Lowercase for criteria checking
            from_lower = sender.lower()
            sub_lower = subject.lower()
            body_lower = body.lower()
            
            # Check relevance criteria
            is_openai = False
            # criteria 1: sender domain
            if any(domain in from_lower for domain in ["openai.com", "chatgpt.com"]):
                is_openai = True
            # criteria 2: subject matches
            elif any(sub in sub_lower for sub in ["openai", "chatgpt", "api", "billing", "invoice", "security", "verification", "login"]):
                is_openai = True
            # criteria 3: body content mentions
            elif any(keyword in body_lower for keyword in ["openai", "chatgpt", "openai platform", "api key", "usage", "invoice", "payment", "verification", "security alert", "account activity"]):
                is_openai = True
                
            if is_openai:
                gmail_link = f"https://mail.google.com/mail/u/0/#inbox/{thrid_hex}" if thrid_hex else ""
                
                new_pending.append({
                    "uid": uid_str,
                    "message_id": msg_id,
                    "from": sender,
                    "from_email": sender_email,
                    "subject": subject,
                    "date": date_str,
                    "body_snippet": clean_snippet(body),
                    "gmail_link": gmail_link
                })
                
        # Load any existing pending emails
        existing_pending = []
        if os.path.exists(pending_path):
            try:
                with open(pending_path, "r", encoding="utf-8") as f:
                    existing_pending = json.load(f)
            except Exception:
                pass
                
        # Append only new unique ones
        for item in new_pending:
            if not any(x["message_id"] == item["message_id"] for x in existing_pending):
                existing_pending.append(item)
                
        # Save pending emails
        with open(pending_path, "w", encoding="utf-8") as f:
            json.dump(existing_pending, f, indent=2, ensure_ascii=False)
            
        print(f"Watchdog run complete. Added {len(new_pending)} new OpenAI emails to pending queue.")
        mail.logout()
        
    except Exception as e:
        print(f"Error during watchdog run: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
