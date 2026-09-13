package com.crash.Raketka.dto;

public record RoundStateResponse(
        String status,
        double currentMultiplier,
        int levelsPassed,
        boolean boosterTriggered,
        Double cashoutMultiplier,
        Double crashMultiplier,
        Integer pointsEarned,
        String reward,
        Double missedMultiplier,
        String serverSeed
) {
}