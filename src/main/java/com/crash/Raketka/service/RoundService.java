package com.crash.Raketka.service;

import com.crash.Raketka.config.ConfigService;
import com.crash.Raketka.config.GameConfig;
import com.crash.Raketka.domain.Reward;
import com.crash.Raketka.domain.Round;
import com.crash.Raketka.domain.RoundStatus;
import com.crash.Raketka.domain.Theme;
import com.crash.Raketka.domain.User;
import com.crash.Raketka.dto.RoundStateResponse;
import com.crash.Raketka.dto.StartRoundRequest;
import com.crash.Raketka.dto.StartRoundResponse;
import com.crash.Raketka.exception.NotEnoughBalanceException;
import com.crash.Raketka.exception.RoundAlreadyCrashedException;
import com.crash.Raketka.repository.RoundRepository;
import com.crash.Raketka.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class RoundService {

    private static final Logger log = LoggerFactory.getLogger(RoundService.class);
    private static final Set<Integer> ALLOWED_BOOSTERS = Set.of(1, 2, 3, 4);

    private final RoundRepository roundRepository;
    private final UserRepository userRepository;
    private final ConfigService configService;
    private final CrashMathService crashMathService;
    private final JsonMapper jsonMapper;

    // Self-инъекция через @Lazy: checkOneRound/cashoutAttempt требуют транзакций,
    // которые Spring не применит при вызове через this.method().
    private final RoundService self;

    public RoundService(RoundRepository roundRepository,
                        UserRepository userRepository,
                        ConfigService configService,
                        CrashMathService crashMathService,
                        JsonMapper jsonMapper,
                        @Lazy RoundService self) {
        this.roundRepository = roundRepository;
        this.userRepository = userRepository;
        this.configService = configService;
        this.crashMathService = crashMathService;
        this.jsonMapper = jsonMapper;
        this.self = self;
    }

    @Transactional
    public StartRoundResponse startRound(Long userId, StartRoundRequest req) {
        if (req.betAmount() <= 0) {
            throw new IllegalArgumentException("betAmount must be positive");
        }
        if (req.theme() == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        if (!ALLOWED_BOOSTERS.contains(req.boosterMultiplier())) {
            throw new IllegalArgumentException("boosterMultiplier must be one of 1, 2, 3, 4");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("user not found: " + userId));
        if (req.betAmount() > user.getBalance()) {
            throw new NotEnoughBalanceException("betAmount exceeds current balance");
        }

        user.setBalance(user.getBalance() - req.betAmount());
        userRepository.save(user);

        GameConfig config = configService.get();
        GameConfig.ThemeConfig themeConfig = themeConfigFor(req.theme(), config);

        String serverSeed = generateServerSeed(config);
        String seedHash = sha256Hex(serverSeed);

        double crashMultiplier = crashMathService.computeCrashFromSeed(serverSeed, config, req.theme());
        int boosterLine = crashMathService.pickBoosterLineFromSeed(serverSeed, req.theme(), config);

        String configSnapshotJson;
        try {
            configSnapshotJson = jsonMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("unable to serialize config snapshot", e);
        }

        Round round = new Round(
                req.theme(),
                userId,
                req.betAmount(),
                req.boosterMultiplier(),
                crashMultiplier,
                boosterLine,
                false,
                RoundStatus.IN_PROGRESS,
                0,
                Instant.now(),
                serverSeed,
                seedHash,
                configSnapshotJson
        );

        round = roundRepository.save(round);

        log.info("round {} started: user={}, theme={}, bet={}, booster={}",
                round.getId(), userId, req.theme(), req.betAmount(), req.boosterMultiplier());

        return new StartRoundResponse(
                round.getId(),
                seedHash,
                round.getStartedAt(),
                req.theme(),
                themeConfig.getLevelsTotal(),
                req.betAmount(),
                req.boosterMultiplier()
        );
    }

    @Transactional(readOnly = true)
    public RoundStateResponse getState(Long userId, Long roundId) {
        Round round = roundRepository.findByIdAndUserId(roundId, userId)
                .orElseThrow(() -> new NoSuchElementException("round not found"));
        GameConfig snapshotConfig = snapshotOf(round);

        if (round.getStatus() == RoundStatus.IN_PROGRESS) {
            double rawCurrent = crashMathService.computeCurrentMultiplier(
                    snapshotConfig.getMultiplierGrowthRate(), round.getStartedAt(), Instant.now());
            double current = previewWithBooster(round, snapshotConfig, rawCurrent);

            // Окно краха: не сохраняем в БД, планировщик догонит.
            if (current >= round.getCrashMultiplier()) {
                return crashedPreviewResponse(round, snapshotConfig);
            }
            return toResponse(round, current, snapshotConfig);
        }

        return toResponse(round, currentMultiplierOrFinal(round), snapshotConfig);
    }

    public RoundStateResponse cashout(Long userId, Long roundId) {
        try {
            return self.cashoutAttempt(userId, roundId);
        } catch (OptimisticLockingFailureException e) {
            log.warn("round {}: optimistic lock conflict during cashout, retrying once", roundId);
            return self.cashoutAttempt(userId, roundId);
        }
    }

    @Transactional
    public RoundStateResponse cashoutAttempt(Long userId, Long roundId) {
        Round round = roundRepository.findByIdAndUserId(roundId, userId)
                .orElseThrow(() -> new NoSuchElementException("round not found"));

        if (round.getStatus() != RoundStatus.IN_PROGRESS) {
            log.info("round {} cashout called but round already finished with status={}",
                    round.getId(), round.getStatus());
            return toResponse(round, currentMultiplierOrFinal(round), snapshotOf(round));
        }

        GameConfig snapshotConfig = snapshotOf(round);
        double rawCurrent = crashMathService.computeCurrentMultiplier(
                snapshotConfig.getMultiplierGrowthRate(), round.getStartedAt(), Instant.now());
        double current = applyBoosterAndFlag(round, snapshotConfig, rawCurrent);

        if (current >= round.getCrashMultiplier()) {
            markCrashed(round, snapshotConfig);
            log.info("round {} crashed before cashout could be processed", round.getId());
            throw new RoundAlreadyCrashedException("round already crashed",
                    toResponse(round, current, snapshotConfig));
        }

        round.setStatus(RoundStatus.CASHED_OUT);
        round.setCashoutMultiplier(current);
        round.setFinishedAt(Instant.now());

        int levelsPassed = crashMathService.computeLevelsPassed(current, snapshotConfig, round.getTheme());
        int pointsEarned = crashMathService.computePointsEarned(
                levelsPassed, current, round.isBoosterTriggered(), snapshotConfig);
        round.setPointsEarned(pointsEarned);

        Long ownerId = round.getUserId();
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new NoSuchElementException("user not found: " + ownerId));
        int payout = (int) Math.round(round.getBetAmount() * current * round.getBoosterMultiplier());
        user.setBalance(user.getBalance() + payout);
        user.setPoints(user.getPoints() + pointsEarned);
        userRepository.save(user);

        round = roundRepository.save(round);

        log.info("round {} cashed out at {}x, payout={}, points={}",
                round.getId(), current, payout, pointsEarned);

        return toResponse(round, current, snapshotConfig);
    }

    @Scheduled(fixedDelay = 200)
    public void checkActiveRounds() {
        List<Round> active = roundRepository.findByStatus(RoundStatus.IN_PROGRESS);
        for (Round r : active) {
            try {
                self.checkOneRound(r.getId());
            } catch (OptimisticLockingFailureException e) {
                log.warn("round {}: skipped this tick due to concurrent modification", r.getId());
            } catch (RuntimeException e) {
                log.error("round {}: unexpected error during scheduled check", r.getId(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkOneRound(Long roundId) {
        Round round = roundRepository.findById(roundId).orElse(null);
        if (round == null || round.getStatus() != RoundStatus.IN_PROGRESS) {
            return;
        }

        GameConfig snapshotConfig = snapshotOf(round);
        double rawCurrent = crashMathService.computeCurrentMultiplier(
                snapshotConfig.getMultiplierGrowthRate(), round.getStartedAt(), Instant.now());
        double current = applyBoosterAndFlag(round, snapshotConfig, rawCurrent);

        if (current >= round.getCrashMultiplier()) {
            markCrashed(round, snapshotConfig);
        } else if (round.isBoosterTriggered()) {
            // сохраняем состояние, если бустер только что переключился
            roundRepository.save(round);
        }
    }

    private double applyBoosterAndFlag(Round round, GameConfig config, double rawCurrent) {
        GameConfig.ThemeConfig themeConfig = themeConfigFor(round.getTheme(), config);
        double lineMultiplier = themeConfig.getBoosterMultipliers().get(round.getBoosterLine());

        if (round.isBoosterTriggered()) {
            return rawCurrent * lineMultiplier;
        }
        if (rawCurrent >= lineMultiplier) {
            round.setBoosterTriggered(true);
            return rawCurrent * lineMultiplier;
        }
        return rawCurrent;
    }

    private double previewWithBooster(Round round, GameConfig config, double rawCurrent) {
        GameConfig.ThemeConfig themeConfig = themeConfigFor(round.getTheme(), config);
        double lineMultiplier = themeConfig.getBoosterMultipliers().get(round.getBoosterLine());

        if (round.isBoosterTriggered() || rawCurrent >= lineMultiplier) {
            return rawCurrent * lineMultiplier;
        }
        return rawCurrent;
    }

    private void markCrashed(Round round, GameConfig config) {
        round.setStatus(RoundStatus.CRASHED);
        round.setFinishedAt(Instant.now());

        int levelsPassed = crashMathService.computeLevelsPassed(
                round.getCrashMultiplier(), config, round.getTheme());
        round.setPointsEarned(crashMathService.computePointsEarned(
                levelsPassed, round.getCashoutMultiplier(), round.isBoosterTriggered(), config));

        SecureRandom rng = new SecureRandom();
        Reward reward = crashMathService.pickReward(config, rng);
        round.setReward(reward);

        User user = userRepository.findById(round.getUserId())
                .orElseThrow(() -> new NoSuchElementException("user not found: " + round.getUserId()));
        user.setPoints(user.getPoints() + round.getPointsEarned());
        userRepository.save(user);

        roundRepository.save(round);

        log.info("round {} crashed at {}x, levelsPassed={}, points={}, reward={}",
                round.getId(), round.getCrashMultiplier(), levelsPassed, round.getPointsEarned(), reward);
    }

    private double currentMultiplierOrFinal(Round round) {
        if (round.getStatus() == RoundStatus.CASHED_OUT && round.getCashoutMultiplier() != null) {
            return round.getCashoutMultiplier();
        }
        if (round.getStatus() == RoundStatus.CRASHED) {
            return round.getCrashMultiplier();
        }
        return 0.0;
    }

    private RoundStateResponse crashedPreviewResponse(Round round, GameConfig config) {
        int levelsPassed = crashMathService.computeLevelsPassed(
                round.getCrashMultiplier(), config, round.getTheme());
        String reward = round.getReward() != null ? round.getReward().toString() : null;

        return new RoundStateResponse(
                RoundStatus.CRASHED.toString(),
                round.getCrashMultiplier(),
                levelsPassed,
                round.isBoosterTriggered(),
                round.getCashoutMultiplier(),
                round.getCrashMultiplier(),
                round.getPointsEarned(),
                reward,
                null,
                round.getServerSeed()
        );
    }

    private RoundStateResponse toResponse(Round round, double current, GameConfig config) {
        int levelsPassed = crashMathService.computeLevelsPassed(
                round.getStatus() == RoundStatus.IN_PROGRESS ? current : round.getCrashMultiplier(),
                config,
                round.getTheme()
        );

        Double missedMultiplier = null;
        if (round.getStatus() == RoundStatus.CRASHED && round.getCashoutMultiplier() != null) {
            missedMultiplier = round.getCrashMultiplier();
        }

        String reward = round.getReward() != null ? round.getReward().toString() : null;
        String serverSeed = round.getStatus() != RoundStatus.IN_PROGRESS ? round.getServerSeed() : null;

        return new RoundStateResponse(
                round.getStatus().toString(),
                current,
                levelsPassed,
                round.isBoosterTriggered(),
                round.getCashoutMultiplier(),
                round.getStatus() == RoundStatus.CRASHED ? round.getCrashMultiplier() : null,
                round.getPointsEarned(),
                reward,
                missedMultiplier,
                serverSeed
        );
    }

    private GameConfig snapshotOf(Round round) {
        try {
            return jsonMapper.readValue(round.getConfigSnapshotJson(), GameConfig.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "unable to deserialize config snapshot for round " + round.getId(), e);
        }
    }

    private String generateServerSeed(GameConfig config) {
        if (config.getFixedSeed() != null) {
            return String.format("%016x", config.getFixedSeed());
        }
        byte[] seedBytes = new byte[32];
        new SecureRandom().nextBytes(seedBytes);
        return HexFormat.of().formatHex(seedBytes);
    }

    private GameConfig.ThemeConfig themeConfigFor(Theme theme, GameConfig config) {
        GameConfig.ThemeConfig themeConfig = config.getThemes().get(theme.name());
        if (themeConfig == null) {
            throw new IllegalArgumentException("No config found for theme " + theme.name());
        }
        return themeConfig;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}