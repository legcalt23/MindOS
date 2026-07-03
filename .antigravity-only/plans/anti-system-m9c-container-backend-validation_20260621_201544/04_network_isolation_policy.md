# Network Isolation Policy

This document details the network gating and validation rules applied to container executions.

## 1. Network Constraints
Containers must run with `--network none` to prevent internet pulls, data egress, or unvetted remote calls.

## 2. Remote Dependency Blocks
All package updates and resource fetches must resolve locally from pre-cached files.
