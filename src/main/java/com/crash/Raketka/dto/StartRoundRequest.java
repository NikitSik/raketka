package com.crash.Raketka.dto;

import com.crash.Raketka.domain.Theme;

public record StartRoundRequest(Theme theme, int betAmount, int boosterMultiplier) {
}
