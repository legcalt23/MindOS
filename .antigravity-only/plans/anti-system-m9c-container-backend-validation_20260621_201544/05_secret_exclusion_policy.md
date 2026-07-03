# Secret Exclusion Policy

This document records the file-level filtering policy applied to mounted directories.

## 1. Excluded Files
Private environment files (`.env`), Git credentials, SSH private keys, and Android keystore/signing keys must never be mounted into the container.

## 2. Gated Paths
The sandbox policy filters out all matches to private patterns, forcing in-container executions to operate without credential access.
