package com.example.demo.enums;

public enum Role {
    MANAGER,
    TRAINER,
    CLIENT,
    // Super-admin, additive to MANAGER (not a replacement) - the only role allowed to grant/
    // revoke MANAGER itself. See UserServiceImpl addRole/removeRole and AGENTS.md "Upgrade:
    // manager-hierarchy decisions".
    ADMIN
}
