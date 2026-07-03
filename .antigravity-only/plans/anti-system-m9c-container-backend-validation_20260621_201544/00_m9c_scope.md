# Phase M9-C Scope

This document defines the scope of Phase M9-C Container Backend Validation, detailing target backends and validation constraints.

## 1. M9-C Mission and Scope
The goal of M9-C is to validate whether a real container or native backend is available on the system to execute the `/anti` runner with stronger isolation than the local ephemeral workspace.

## 2. Operating Constraints
No container backends will be installed during this validation. If Docker is unavailable, the runner will continue to use the `local_ephemeral` workspace.
