package io.cx.gitstats.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserGitlabState {
    ACTIVE("active"),
    BLOCKED("blocked"),
    DEACTIVATED("deactivated"),
    BANNED("banned"),
    CANNOT_BE_REASSIGNED("cannot_be_reassigned"),
    LDAP_BLOCKED("ldap_blocked"),
    BLOCKED_PENDING_APPROVAL("blocked_pending_approval"),
    UNKNOWN("unknown");

    private final String value;

    public static UserGitlabState fromValue(String value) {
        if (value == null) {
            return UserGitlabState.UNKNOWN;
        }
        for (UserGitlabState state : values()) {
            if (state.value.equalsIgnoreCase(value) || state.name().equalsIgnoreCase(value)) {
                return state;
            }
        }
        return UserGitlabState.UNKNOWN;
    }
}
