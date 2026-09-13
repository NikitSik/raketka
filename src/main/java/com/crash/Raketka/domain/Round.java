package com.crash.Raketka.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "rounds")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Theme theme;

    private int betAmount;

    private int boosterMultiplier;

    private double crashMultiplier;

    private int boosterLine;

    private boolean boosterTriggered;

    private Double cashoutMultiplier;

    @Enumerated(EnumType.STRING)
    private RoundStatus status;

    private int pointsEarned;

    @Enumerated(EnumType.STRING)
    private Reward reward;

    private Instant startedAt;

    private Instant finishedAt;

    private String serverSeed;

    private String seedHash;

    @Column(columnDefinition = "text")
    private String configSnapshotJson;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long userId;

    protected Round() {
    }

    public Round(Theme theme,
                 Long userId,
                 int betAmount,
                 int boosterMultiplier,
                 double crashMultiplier,
                 int boosterLine,
                 boolean boosterTriggered,
                 RoundStatus status,
                 int pointsEarned,
                 Instant startedAt,
                 String serverSeed,
                 String seedHash,
                 String configSnapshotJson) {
        this.theme = theme;
        this.userId = userId;
        this.betAmount = betAmount;
        this.boosterMultiplier = boosterMultiplier;
        this.crashMultiplier = crashMultiplier;
        this.boosterLine = boosterLine;
        this.boosterTriggered = boosterTriggered;
        this.status = status;
        this.pointsEarned = pointsEarned;
        this.startedAt = startedAt;
        this.serverSeed = serverSeed;
        this.seedHash = seedHash;
        this.configSnapshotJson = configSnapshotJson;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public int getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(int betAmount) {
        this.betAmount = betAmount;
    }

    public int getBoosterMultiplier() {
        return boosterMultiplier;
    }

    public void setBoosterMultiplier(int boosterMultiplier) {
        this.boosterMultiplier = boosterMultiplier;
    }

    public double getCrashMultiplier() {
        return crashMultiplier;
    }

    public void setCrashMultiplier(double crashMultiplier) {
        this.crashMultiplier = crashMultiplier;
    }

    public int getBoosterLine() {
        return boosterLine;
    }

    public void setBoosterLine(int boosterLine) {
        this.boosterLine = boosterLine;
    }

    public boolean isBoosterTriggered() {
        return boosterTriggered;
    }

    public void setBoosterTriggered(boolean boosterTriggered) {
        this.boosterTriggered = boosterTriggered;
    }

    public Double getCashoutMultiplier() {
        return cashoutMultiplier;
    }

    public void setCashoutMultiplier(Double cashoutMultiplier) {
        this.cashoutMultiplier = cashoutMultiplier;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public void setStatus(RoundStatus status) {
        this.status = status;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public Reward getReward() {
        return reward;
    }

    public void setReward(Reward reward) {
        this.reward = reward;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getServerSeed() {
        return serverSeed;
    }

    public void setServerSeed(String serverSeed) {
        this.serverSeed = serverSeed;
    }

    public String getSeedHash() {
        return seedHash;
    }

    public void setSeedHash(String seedHash) {
        this.seedHash = seedHash;
    }

    public String getConfigSnapshotJson() {
        return configSnapshotJson;
    }

    public void setConfigSnapshotJson(String configSnapshotJson) {
        this.configSnapshotJson = configSnapshotJson;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}