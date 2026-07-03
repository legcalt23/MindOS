import os
import re
import json
import shutil
from pathlib import Path

class AntiSandboxViolation(PermissionError):
    """Exception raised when a subagent attempts to access a path denied by the sandbox policy."""
    pass

def load_policy(policy_path=None):
    """Loads the sandbox policy JSON from the specified path or default location."""
    if policy_path is None:
        policy_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'sandbox_policy.json')
    
    # Ensure policy file exists
    if not os.path.exists(policy_path):
        # Return a fallback default policy if missing
        return {
            "sandbox_policy": {
                "enforcement_mode": "DENY_BY_DEFAULT",
                "denied_path_patterns": [
                    "\\.env$", "\\.env\\..*", ".*secret.*", ".*credential.*",
                    "\\.pem$", "\\.key$", "\\.p12$", "\\.keystore$",
                    "google-services\\.json$", "firebase\\.json$",
                    "firestore\\.rules$", "storage\\.rules$"
                ]
            }
        }
        
    with open(policy_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def normalize_path(path):
    """Expands environment variables and normalizes path separators/case for Windows comparison."""
    if not path:
        return ""
    # Convert path to string if it is a Path object
    path_str = str(path)
    # Expand environment variables first
    expanded = os.path.expandvars(path_str)
    # Convert to absolute path
    absolute = os.path.abspath(expanded)
    # Normcase handles Windows case-insensitivity and standardizes slashes
    return os.path.normcase(absolute)

def resolve_real_path(path):
    """Resolves symlinks, junctions, and relative segments to get the canonical physical path."""
    if not path:
        return ""
    # Expand environment variables and get absolute path
    abs_path = os.path.abspath(os.path.expandvars(str(path)))
    # Use realpath to resolve junctions/symlinks and get filesystem casing
    real_path = os.path.realpath(abs_path)
    # Apply normcase as final step to ensure consistent case on Windows
    return os.path.normcase(real_path)


def is_denied(path, policy):
    """Checks if the normalized/resolved path matches any denied pattern in the policy."""
    resolved_path = resolve_real_path(path)
    
    # Replaces path separators for uniform regex matching on both Windows and Posix
    portable_path = resolved_path.replace('\\', '/')
    
    # Whitelist markdown and json plans under .antigravity-only/plans/ to avoid false positives on doc names like secret_exclusion_policy
    if "/.antigravity-only/plans/" in portable_path and (portable_path.endswith(".md") or portable_path.endswith(".json")):
        return False
        
    # 1. Check regex denied patterns
    denied_patterns = policy.get("sandbox_policy", {}).get("denied_path_patterns", [])
    
    for pattern in denied_patterns:
        # Perform case-insensitive check
        if re.search(pattern, portable_path, re.IGNORECASE):
            return True
            
    return False

def assert_allowed(path, operation='read', policy=None):
    """Asserts that a path access is allowed, raising AntiSandboxViolation if denied."""
    if policy is None:
        policy = load_policy()
        
    resolved_path = resolve_real_path(path)
    
    # Rule: If path is denied by regex patterns, block it
    if is_denied(resolved_path, policy):
        raise AntiSandboxViolation("SECURITY_BLOCKED: denied path access")
        
    # Rule: If operation is write/delete, enforce approved write roots
    if operation in ('write', 'delete'):
        # Get repository root
        repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        allowed_write_dir = os.path.normcase(os.path.join(repo_root, '.antigravity-only'))
        allowed_report_file = os.path.normcase(os.path.join(repo_root, 'anti_runtime_hardening_report.md'))
        
        # Verify path is under .antigravity-only/ or matches the report file
        is_under_anti_dir = resolved_path.startswith(allowed_write_dir + os.sep) or resolved_path == allowed_write_dir
        is_report_file = resolved_path == allowed_report_file
        
        # Also allow writes to scratch/tmp dirs inside the workspace for tests/operations
        scratch_dir = os.path.normcase(os.path.join(repo_root, 'scratch'))
        is_under_scratch = resolved_path.startswith(scratch_dir + os.sep) or resolved_path == scratch_dir
        
        if not (is_under_anti_dir or is_report_file or is_under_scratch):
            raise AntiSandboxViolation("SECURITY_BLOCKED: denied path access")

def safe_open(path, mode='r', *args, **kwargs):
    """Safely opens a file after validating sandbox policy constraints."""
    operation = 'write' if any(m in mode for m in ('w', 'a', 'x', '+')) else 'read'
    assert_allowed(path, operation)
    return open(path, mode, *args, **kwargs)

def safe_read_text(path, encoding='utf-8', *args, **kwargs):
    """Safely reads file contents after validating sandbox policy constraints."""
    assert_allowed(path, 'read')
    with open(path, 'r', encoding=encoding, *args, **kwargs) as f:
        return f.read()

def safe_write_text(path, data, encoding='utf-8', *args, **kwargs):
    """Safely writes content to a file after validating sandbox policy constraints."""
    assert_allowed(path, 'write')
    # Ensure parent directories are created
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, 'w', encoding=encoding, *args, **kwargs) as f:
        f.write(data)

def safe_listdir(path):
    """Safely lists a directory after validating sandbox policy constraints."""
    assert_allowed(path, 'read')
    return os.listdir(path)

def safe_copy(src, dst):
    """Safely copies a file after validating sandbox policy constraints on both src and dst."""
    assert_allowed(src, 'read')
    assert_allowed(dst, 'write')
    # Ensure parent destination directory exists
    os.makedirs(os.path.dirname(os.path.abspath(dst)), exist_ok=True)
    shutil.copyfile(src, dst)
