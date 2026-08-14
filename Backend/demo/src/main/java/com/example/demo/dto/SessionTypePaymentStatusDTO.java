package com.example.demo.dto;

import com.example.demo.enums.SessionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-{@link SessionType} comparison of a client's actually-held past appointments against what
 * they've paid for - see AGENTS.md "Upgrade: payment debt tracking decisions". {@code owed} is
 * {@code max(0, held - paid)}, never negative (a client who overpaid simply has 0 owed for that
 * type, not a negative debt).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SessionTypePaymentStatusDTO {
    private SessionType type;
    private Integer held;
    private Integer paid;
    private Integer owed;
}
