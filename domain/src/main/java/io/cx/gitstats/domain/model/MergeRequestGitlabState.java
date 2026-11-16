package io.cx.gitstats.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MergeRequestGitlabState {
    OPENED("opened"),
    CLOSED("closed"),
    LOCKED("locked"),
    MERGED("merged");

    private final String value;

    public static MergeRequestGitlabState fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (MergeRequestGitlabState state : values()) {
            if (state.value.equalsIgnoreCase(value) || state.name().equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown gitlab state: " + value);
    }
}
