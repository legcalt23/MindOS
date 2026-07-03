# Container Mount Policy

This document details the volume mounting policy and directory restrictions defined for containerized subagent execution.

## 1. Mount Restrictions
To secure the host environment, the source repository folder must be mounted as read-only. Writable mounts are restricted to scratch folders.

## 2. Mount Exclusions
Mounting the user's home folder (`C:\Users\curro`), `.git` folder (writable), or other private system roots is strictly forbidden.
