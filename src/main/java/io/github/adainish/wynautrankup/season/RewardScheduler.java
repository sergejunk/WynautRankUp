/*
 * Program: WynautRankup - Add a competitive ranked system to Cobblemon
 * Copyright (C) <2025> <Nicole "Adenydd" Catherine Stuut>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * See the `LICENSE` file in the project root or <https://www.gnu.org/licenses/>.
 */
package io.github.adainish.wynautrankup.season;

import io.github.adainish.wynautrankup.WynautRankUp;
import io.github.adainish.wynautrankup.database.PlayerDataManager;
import io.github.adainish.wynautrankup.util.AsyncExecutor;

import java.time.LocalDate;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

public class RewardScheduler
{
    private final ScheduledExecutorService scheduler;
    private final AsyncExecutor asyncExecutor;
    private final boolean manageExecutorLifecycle;

    public RewardScheduler() {
        this(new AsyncExecutor(2), true);
    }

    public RewardScheduler(AsyncExecutor asyncExecutor) {
        this(asyncExecutor, false);
    }

    public RewardScheduler(AsyncExecutor asyncExecutor, boolean manageExecutorLifecycle) {
        this.asyncExecutor = asyncExecutor;
        this.manageExecutorLifecycle = manageExecutorLifecycle;
        // single daemon thread for scheduling
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "wynautrankup-reward-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        SeasonManager seasonManager = WynautRankUp.instance.seasonManager;
        PlayerDataManager playerManager = WynautRankUp.instance.playerDataManager;
        Executor executor = asyncExecutor.getExecutorService();

        // Daily evaluation: enqueue all rewards (offline/online unified)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDate today = LocalDate.now();
                seasonManager.getSeasons().forEach(season -> {
                    try {
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                // Ensure the internal future is awaited on the provided executor so default pools are not used.
                                playerManager.evaluateAndDistributeRewards(season)
                                        .exceptionally(ex -> { ex.printStackTrace(); return null; })
                                        .join();
                            } catch (RejectedExecutionException rex) {
                                // executor inside PlayerDataManager is shutdown; log and skip this run
                                rex.printStackTrace();
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                            return null;
                        }, executor).exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
                    } catch (RejectedExecutionException rex) {
                        // executor inside PlayerDataManager is shutdown; log and skip this run
                        rex.printStackTrace();
                    }
                });
            } catch (RejectedExecutionException rex) {
                rex.printStackTrace();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 0, 24L * 60L * 60L * 1000L, TimeUnit.MILLISECONDS);

        // Frequent dispatcher: deliver pending rewards to online players
        scheduler.scheduleAtFixedRate(() -> {
            try {
                CompletableFuture.supplyAsync(() -> {
                    try {
                        playerManager.dispatchPendingRewardsToOnlinePlayers()
                                .exceptionally(ex -> { ex.printStackTrace(); return 0; })
                                .join();
                    } catch (RejectedExecutionException rex) {
                        // executor inside PlayerDataManager is shutdown; log and skip this run
                        rex.printStackTrace();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    return null;
                }, executor).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 10_000L, 30_000L, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
        if (manageExecutorLifecycle) {
            asyncExecutor.shutdownExecutor();
        }
    }
}
