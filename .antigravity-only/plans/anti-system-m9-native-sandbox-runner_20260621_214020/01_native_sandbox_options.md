# Native Sandbox Options Evaluation

This document analyzes various isolation and containerization options for secure subagent execution.

## 1. Option Comparison Matrix
Each candidate technology is evaluated on compatibility, complexity, and isolation strength.

### Docker
* **Windows feasibility:** High (requires Docker Desktop running WSL2 backend).
* **Filesystem isolation strength:** High (completely isolated containers).
* **Network isolation strength:** High (supports `--network none` mode).
* **Gradle/Python compatibility:** High (requires proper caching configurations).
* **Transcript capture support:** High (via volume mounts or stdout capture).
* **Complexity:** Medium (requires maintaining local Dockerfiles and volume caching).
* **Residual risks:** Docker daemon escalations, WSL2 host resource exhaustion.
* **Recommendation:** Recommended (Primary candidate for cross-platform local runners).

### gVisor
* **Windows feasibility:** Low (gVisor is Linux-only, requires nesting inside WSL2 or a virtual machine).
* **Filesystem isolation strength:** Excellent (intercepts application syscalls).
* **Network isolation strength:** Excellent (custom user-space network stack).
* **Gradle/Python compatibility:** Medium (syscall translation introduces performance latency for heavy Gradle builds).
* **Transcript capture support:** High.
* **Complexity:** High (advanced kernel configurations required).
* **Residual risks:** Nested virtualization performance hits on Windows hosts.
* **Recommendation:** Not Recommended for local development (too complex for standard dev workstations).

### Firecracker
* **Windows feasibility:** Low (microVMs rely on Linux KVM; nested KVM on Windows is complex and slow).
* **Filesystem isolation strength:** Extreme (separate lightweight virtual machine).
* **Network isolation strength:** Extreme (fully isolated network interface).
* **Gradle/Python compatibility:** Medium (virtual machine boot cycles and guest file sharing introduce overhead).
* **Transcript capture support:** High (via vsock or serial ports).
* **Complexity:** Extreme (requires custom kernel compilation and guest image builds).
* **Residual risks:** Windows hypervisor layer compatibility conflicts.
* **Recommendation:** Not Recommended (Overkill for local developer review tools).

### WSL2 Isolated Workspace
* **Windows feasibility:** High (native to modern Windows environments).
* **Filesystem isolation strength:** Medium (isolated Linux root, but shared network/ports with host).
* **Network isolation strength:** Low (shares host network interfaces by default).
* **Gradle/Python compatibility:** High.
* **Transcript capture support:** High.
* **Complexity:** Low (native tooling).
* **Residual risks:** Cross-compilation differences between host Windows and target Linux.
* **Recommendation:** Recommended as a secondary deployment layer.

### Restricted Subprocess Runner
* **Windows feasibility:** High.
* **Filesystem isolation strength:** Low (relies on OS process permissions/Access Control Lists).
* **Network isolation strength:** Low.
* **Gradle/Python compatibility:** High.
* **Transcript capture support:** High.
* **Complexity:** Medium (process security policies are OS-dependent).
* **Residual risks:** Vulnerable to shell injection and system command escape.
* **Recommendation:** Not Recommended (Does not provide robust containment).

### Read-only Bind Mounts
* **Windows feasibility:** High (supported in Docker/WSL2).
* **Filesystem isolation strength:** High (prevents all modification to product source files).
* **Network isolation strength:** N/A (filesystem only).
* **Gradle/Python compatibility:** Medium (Gradle requires writing to build/ directories; requires configuring separate writeable caches).
* **Transcript capture support:** High.
* **Complexity:** Low (simple mount flag configuration).
* **Residual risks:** Read access allows exfiltration of credentials/environment files.
* **Recommendation:** Recommended (Essential component of container mounts).

### Ephemeral Copy Workspace
* **Windows feasibility:** High (supported natively).
* **Filesystem isolation strength:** High (runner operates on a duplicate temp directory; original source files are untouched).
* **Network isolation strength:** N/A (filesystem only).
* **Gradle/Python compatibility:** High.
* **Transcript capture support:** High.
* **Complexity:** Low (requires copying folder to temp path).
* **Residual risks:** Performance cost of file duplication on large workspaces.
* **Recommendation:** Recommended (Provides easy rollback properties).

### No-network Execution Mode
* **Windows feasibility:** High (supported in container engines via `--network none` or firewall configurations).
* **Filesystem isolation strength:** N/A (network only).
* **Network isolation strength:** Extreme (prevents exfiltration of credentials and telemetry).
* **Gradle/Python compatibility:** High (requires pre-resolving Gradle dependencies offline).
* **Transcript capture support:** High.
* **Complexity:** Medium.
* **Residual risks:** None.
* **Recommendation:** Recommended (Crucial security baseline for high-stakes execution).

## 2. Conclusion
A hybrid container-driven architecture using **Docker** configured with **read-only bind mounts**, an **ephemeral copy workspace** for cache directories, and **no-network execution mode** represents the optimal trade-off between Windows feasibility, security containment, and developer tool performance.
