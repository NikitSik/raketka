package com.crash.Raketka.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class GameConfig {

    @JsonProperty("minCrashMultiplier")
    private double minCrashMultiplier;

    @JsonProperty("maxMultiplier")
    private double maxMultiplier;

    @JsonProperty("multiplierGrowthRate")
    private double multiplierGrowthRate;

    @JsonProperty("houseEdge")
    private double houseEdge;

    @JsonProperty("pollIntervalMs")
    private int pollIntervalMs;

    @JsonProperty("pointsPerLine")
    private int pointsPerLine;

    @JsonProperty("pointsCashoutBonus")
    private int pointsCashoutBonus;

    @JsonProperty("pointsBoosterBonus")
    private int pointsBoosterBonus;

    @JsonProperty("startingBalance")
    private int startingBalance;

    @JsonProperty("fixedSeed")
    private Long fixedSeed;

    @JsonProperty("themes")
    private Map<String, ThemeConfig> themes;

    @JsonProperty("rewardWeights")
    private Map<String, Double> rewardWeights;

    public GameConfig() {
    }

    public double getMinCrashMultiplier() {
        return minCrashMultiplier;
    }

    public void setMinCrashMultiplier(double minCrashMultiplier) {
        this.minCrashMultiplier = minCrashMultiplier;
    }

    public double getMaxMultiplier() {
        return maxMultiplier;
    }

    public void setMaxMultiplier(double maxMultiplier) {
        this.maxMultiplier = maxMultiplier;
    }

    public double getMultiplierGrowthRate() {
        return multiplierGrowthRate;
    }

    public void setMultiplierGrowthRate(double multiplierGrowthRate) {
        this.multiplierGrowthRate = multiplierGrowthRate;
    }

    public double getHouseEdge() {
        return houseEdge;
    }

    public void setHouseEdge(double houseEdge) {
        this.houseEdge = houseEdge;
    }

    public int getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(int pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getPointsPerLine() {
        return pointsPerLine;
    }

    public void setPointsPerLine(int pointsPerLine) {
        this.pointsPerLine = pointsPerLine;
    }

    public int getPointsCashoutBonus() {
        return pointsCashoutBonus;
    }

    public void setPointsCashoutBonus(int pointsCashoutBonus) {
        this.pointsCashoutBonus = pointsCashoutBonus;
    }

    public int getPointsBoosterBonus() {
        return pointsBoosterBonus;
    }

    public void setPointsBoosterBonus(int pointsBoosterBonus) {
        this.pointsBoosterBonus = pointsBoosterBonus;
    }

    public int getStartingBalance() {
        return startingBalance;
    }

    public void setStartingBalance(int startingBalance) {
        this.startingBalance = startingBalance;
    }

    public Long getFixedSeed() {
        return fixedSeed;
    }

    public void setFixedSeed(Long fixedSeed) {
        this.fixedSeed = fixedSeed;
    }

    public Map<String, ThemeConfig> getThemes() {
        return themes;
    }

    public void setThemes(Map<String, ThemeConfig> themes) {
        this.themes = themes;
    }

    public Map<String, Double> getRewardWeights() {
        return rewardWeights;
    }

    public void setRewardWeights(Map<String, Double> rewardWeights) {
        this.rewardWeights = rewardWeights;
    }

    public static class ThemeConfig {

        @JsonProperty("lineProbabilities")
        private List<Double> lineProbabilities;

        @JsonProperty("boosterMultipliers")
        private List<Double> boosterMultipliers;

        @JsonProperty("levelsTotal")
        private int levelsTotal;

        public ThemeConfig() {
        }

        public List<Double> getLineProbabilities() {
            return lineProbabilities;
        }

        public void setLineProbabilities(List<Double> lineProbabilities) {
            this.lineProbabilities = lineProbabilities;
        }

        public List<Double> getBoosterMultipliers() {
            return boosterMultipliers;
        }

        public void setBoosterMultipliers(List<Double> boosterMultipliers) {
            this.boosterMultipliers = boosterMultipliers;
        }

        public int getLevelsTotal() {
            return levelsTotal;
        }

        public void setLevelsTotal(int levelsTotal) {
            this.levelsTotal = levelsTotal;
        }
    }
}