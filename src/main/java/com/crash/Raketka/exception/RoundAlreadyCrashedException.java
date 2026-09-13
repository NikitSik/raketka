package com.crash.Raketka.exception;

import com.crash.Raketka.dto.RoundStateResponse;

public class RoundAlreadyCrashedException extends RuntimeException {

    private final RoundStateResponse state;

    public RoundAlreadyCrashedException(String message, RoundStateResponse state) {
        super(message);
        this.state = state;
    }

    public RoundStateResponse getState() {
        return state;
    }
}
