# Rollback Plan

This document explains how to undo all subagent hardening actions and restore the original 20 simulated agents system exactly.

## Step-by-Step Restore Procedure

### 1. In-Session Subagent Deregistration
Dynamically defined subagents exist only in-session and will naturally expire when the current conversation session is closed. No explicit deregistration tool command is required.

### 2. File System Restoration (Restore files from Quarantine)
To move the 20 legacy agent markdown files back from the `quarantine` directory to the main `agents` folder, execute the following PowerShell command in the workspace root:

```powershell
Get-ChildItem -Path .antigravity-only\agents\quarantine -File | ForEach-Object { Move-Item -Path $_.FullName -Destination .antigravity-only\agents\ -Force }
Remove-Item -Path .antigravity-only\agents\quarantine -Recurse -Force
```

### 3. Cleanup Hardening Files
Delete the active state and status files that were added during the audit:

```powershell
Remove-Item -Path .antigravity-only\agents\REALITY_STATUS.md -Force
Remove-Item -Path .antigravity-only\agents\ROUTING_MAP.md -Force
Remove-Item -Path .antigravity-only\agents\PERSISTENCE_TEST.md -Force
Remove-Item -Path .antigravity-only\agents\schemas -Recurse -Force
Remove-Item -Path .antigravity-only\agents\tests -Recurse -Force
```

### 4. Revert README.md
Discard any changes to `README.md` to restore its original version:

```powershell
git checkout HEAD -- .antigravity-only/agents/README.md
```
