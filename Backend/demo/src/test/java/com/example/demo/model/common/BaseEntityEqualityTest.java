package com.example.demo.model.common;

import com.example.demo.enums.Role;
import com.example.demo.model.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseEntityEqualityTest {

    @Test
    void distinctUnsavedRolesDoNotCollapseInASet() {
        UserRole manager = new UserRole(null, null, Role.MANAGER);
        UserRole admin = new UserRole(null, null, Role.ADMIN);

        Set<UserRole> roles = new HashSet<>();
        roles.add(manager);
        roles.add(admin);

        assertEquals(2, roles.size());
    }
}
