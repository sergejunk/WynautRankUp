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
package io.github.adainish.wynautrankup.tracker;

import io.github.adainish.wynautrankup.WynautRankUp;
import io.github.adainish.wynautrankup.data.Match;
import io.github.adainish.wynautrankup.util.EloCalculator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Tracks ranked battles using their unique identifiers.
 */
public class RankedBattleTracker
{
    private final HashMap<UUID, Match> rankedBattles = new HashMap<>();

    public void markAsRanked(UUID battleId, Match match) {
        rankedBattles.put(battleId, match);
    }

    public Match getRankedBattle(UUID battleId) {
        return rankedBattles.get(battleId);
    }

    public boolean isRanked(UUID battleId) {
        return rankedBattles.containsKey(battleId);
    }

    public void removeRankedBattle(UUID battleId) {
        rankedBattles.remove(battleId);
    }

    public void clearRankedBattles() {
        rankedBattles.clear();
    }

    public boolean isPlayerInRankedBattle(UUID uuid) {
        return rankedBattles.values().stream().anyMatch(match ->
                match.getPlayer1().getId().equals(uuid) || match.getPlayer2().getId().equals(uuid)
        );
    }

    public void forfeitPlayerFromBattle(UUID uuid) {
        Match match = rankedBattles.values().stream().filter(m ->
                m.getPlayer1().getId().equals(uuid) || m.getPlayer2().getId().equals(uuid)
        ).findFirst().orElse(null);
        if (match != null) {
            match.endMatch();
            rankedBattles.values().remove(match);
            // Inform players about the forfeit
            match.getPlayer1().sendMessage("You forfeited your ranked battle.");
            match.getPlayer2().sendMessage("Your ranked battle has been forfeited.");
            // mark forfeiter as loser and the other player as winner
            UUID winnerId = match.getPlayer1().getId().equals(uuid) ? match.getPlayer2().getId() : match.getPlayer1().getId();

            rankedBattleEnded(match, uuid, winnerId);
        }
    }

    public boolean isRepeatOpponent(UUID player1, UUID player2) {
        return WynautRankUp.instance.matchmakingQueue.getMatchResultRecorder().countRecentMatches(player1, player2, 1) > 0;
    }

    public void rankedBattleEnded(Match match, UUID uuid, UUID winnerId)
    {
        boolean isRepeatOpponent = isRepeatOpponent(match.getPlayer1().getId(), match.getPlayer2().getId());
        int winnerElo;
        int loserElo;
        try {
            winnerElo = WynautRankUp.instance.playerDataManager.getElo(winnerId).get();
            loserElo = WynautRankUp.instance.playerDataManager.getElo(uuid).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (winnerElo == 0 || loserElo == 0) {
            //this should never happen, but just in case
            return;
        }

        System.out.println("Winner ELO: " + winnerElo + " Loser ELO: " + loserElo);
        int eloChange = EloCalculator.calculateEloChange(winnerElo, loserElo, true);
        System.out.println("Elo change: " + eloChange);

        // Apply repeat opponent modifier if needed
        if (isRepeatOpponent) {
            double modifier = WynautRankUp.instance.generalConfig.repeatOpponentEloModifier;
            eloChange = (int) Math.round(eloChange * modifier);
            System.out.println("Elo change after modifier due to repeat opponents: " + eloChange);
        }

        int adjustedWinnerElo = winnerElo + eloChange;
        int adjustedLoserElo = loserElo - eloChange;

        // Ensure ELO does not drop below 1000
        if (adjustedWinnerElo < 1000) adjustedWinnerElo = 1000;
        if (adjustedLoserElo < 1000) adjustedLoserElo = 1000;

        System.out.println("Adjusted Winner ELO: " + adjustedWinnerElo);
        System.out.println("Adjusted Loser ELO: " + adjustedLoserElo);

        //update ELOs in database
        WynautRankUp.instance.playerDataManager.setElo(winnerId, adjustedWinnerElo);
        WynautRankUp.instance.playerDataManager.setElo(uuid, adjustedLoserElo);


        //do appropriate win/loss recording
        WynautRankUp.instance.matchmakingQueue.getMatchResultRecorder().recordMatch(winnerId, uuid, eloChange, -eloChange);
        removeRankedBattle(match.getBattleId());
        //do appropriate win/loss announcement
        match.getPlayer1().sendMessage("Match Result: " + (match.getPlayer1().getId().equals(winnerId) ? "Victory!" : "Defeat."));
        match.getPlayer2().sendMessage("Match Result: " + (match.getPlayer2().getId().equals(winnerId) ? "Victory!" : "Defeat."));

        //TODO: implement rewarding system

        //remove invulnerability and notify players of their new ELO as well as teleport them to a designated location
        if (match.getPlayer1().getOptionalServerPlayer().isPresent()) {
            ServerPlayer serverPlayer = match.getPlayer1().getOptionalServerPlayer().get();
            serverPlayer.setInvulnerable(false);
            if (match.getPlayer1().getId().equals(winnerId)) {
                serverPlayer.sendSystemMessage(Component.literal("You gained " + eloChange + " ELO! New ELO: " + adjustedWinnerElo).withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD));
            } else {
                serverPlayer.sendMessage(Component.literal("You lost " + eloChange + " ELO. New ELO: " + adjustedLoserElo).withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.BOLD));
            }

        }

        if (match.getPlayer2().getOptionalServerPlayer().isPresent()) {
            ServerPlayer serverPlayer = match.getPlayer2().getOptionalServerPlayer().get();
            serverPlayer.setInvulnerable(false);
            if (match.getPlayer2().getId().equals(winnerId)) {
                serverPlayer.sendSystemMessage(Component.literal("You gained " + eloChange + " ELO! New ELO: " + adjustedWinnerElo).withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD));
            } else {
                serverPlayer.sendMessage(Component.literal("You lost " + eloChange + " ELO. New ELO: " + adjustedLoserElo).withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.BOLD));
            }
        }

        match.endMatch();
    }
}

