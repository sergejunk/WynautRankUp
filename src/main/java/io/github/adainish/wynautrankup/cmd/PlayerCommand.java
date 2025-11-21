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
package io.github.adainish.wynautrankup.cmd;

import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.adainish.wynautrankup.WynautRankUp;
import io.github.adainish.wynautrankup.gui.BannedPokemonGUI;
import io.github.adainish.wynautrankup.gui.LeaderboardGUI;
import io.github.adainish.wynautrankup.gui.SeasonsGUI;
import io.github.adainish.wynautrankup.util.BattleUtil;
import io.github.adainish.wynautrankup.util.PermissionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class PlayerCommand {
    public static final String PERMISSION_NODE = "wynautrankup.base";
    public static int help(CommandSourceStack source) {
        List<Component> messages = new ArrayList<>();
        messages.add(Component.literal("Wynaut Rankup Commands:").withStyle(s -> s.withBold(true)).withStyle(ChatFormatting.AQUA));
        messages.add(Component.literal("/ranked help - Displays this message.").withStyle(ChatFormatting.GOLD));
        messages.add(Component.literal("/ranked banned - Displays a GUI with info about banned Pokemon.").withStyle(ChatFormatting.GOLD));
        messages.add(Component.literal("/ranked stats - Displays your current ELO and rank as well as open the extended info menu.").withStyle(ChatFormatting.GOLD));
        messages.add(Component.literal("/ranked leaderboard - Displays the top 10 players by ELO.").withStyle(ChatFormatting.GOLD));
        messages.add(Component.literal("/ranked queue - Shows your current queue status and players in it within your elo range.").withStyle(ChatFormatting.GOLD));
        messages.add(Component.literal("/ranked queue leave - Opts you out of the ranked queue.").withStyle(ChatFormatting.GOLD));
        messages.add(Component.literal("/ranked queue join - Opts you back into the ranked queue.").withStyle(ChatFormatting.GOLD));
        messages.forEach(source::sendSystemMessage);
        return 1;
    }

    public static int stats(CommandSourceStack source) {
        if (source.isPlayer()) {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                source.sendSystemMessage(Component.literal("An error occurred while fetching your player data. Please try again later.").withStyle(ChatFormatting.RED));
                return 1;
            }
            source.sendSystemMessage(Component.literal("Fetching your ranked stats...").withStyle(ChatFormatting.YELLOW));
            //player elo and rank
            int currentElo = 1000; //get from db
            currentElo = WynautRankUp.instance.playerDataManager.getElo(player.getUUID()).join();
            String rank = WynautRankUp.instance.rankManager.getRankStringForElo(currentElo);

            //send message
            source.sendSystemMessage(Component.literal("Your current ELO: ").withStyle(ChatFormatting.AQUA).append(Component.literal(String.valueOf(currentElo)).withStyle(ChatFormatting.GOLD)));
            source.sendSystemMessage(Component.literal("Your current Rank: ").withStyle(ChatFormatting.AQUA).append(Component.literal(rank).withStyle(ChatFormatting.GOLD)));

            //now we proceed to open the seasons gui
            SeasonsGUI seasonsGUI = new SeasonsGUI();
            seasonsGUI.open(player);
        } else {
            source.sendSystemMessage(Component.literal("This command can only be run by a player.").withStyle(ChatFormatting.RED));
        }

        return 1;
    }

    public static int banned(CommandSourceStack source) {
        if (source.isPlayer())
        {
            try {
                ServerPlayer player = source.getPlayer();
                if (player == null) {
                    source.sendSystemMessage(Component.literal("An error occurred while fetching your player data. Please try again later.").withStyle(ChatFormatting.RED));
                    return 1;
                }
                //open banned pokemon gui
                BannedPokemonGUI bannedPokemonGUI = new BannedPokemonGUI();
                bannedPokemonGUI.open(player);
            } catch (Exception e)
            {
                e.printStackTrace();
            }

        } else {
            source.sendSystemMessage(Component.literal("This command can only be run by a player.").withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    public static int queue(CommandSourceStack source) {
        if (source.isPlayer()) {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                source.sendSystemMessage(Component.literal("An error occurred while fetching your player data. Please try again later.").withStyle(ChatFormatting.RED));
                return 1;
            }
            source.sendSystemMessage(Component.literal("Fetching your queue status...").withStyle(ChatFormatting.YELLOW
            ));
            //check if in queue
            boolean inQueue = WynautRankUp.instance.matchmakingQueue.isInQueue(player.getUUID());

            //get players in queue within elo range
            WynautRankUp.instance.matchmakingQueue.getPlayersInRange(player.getUUID()).thenAccept(players -> {
                if (players == null || players.isEmpty()) {
                    source.sendSystemMessage(Component.literal("No players found in the queue within your ELO range.").withStyle(ChatFormatting.RED));
                } else {
                    int playerCountInRange = players.size();
                    source.sendSystemMessage(Component.literal("You are currently " + (inQueue ? "" : "not ") + "in the ranked queue.").withStyle(ChatFormatting.AQUA));
                    source.sendSystemMessage(Component.literal("There are " + playerCountInRange + " players in the queue within your ELO range.").withStyle(ChatFormatting.AQUA));
                }
            });

        } else {
            source.sendSystemMessage(Component.literal("This command can only be run by a player.").withStyle(ChatFormatting.RED));
        }

        return 1;
    }

    public static int joinQueue(CommandSourceStack source) {
        try {
            if (source.isPlayer()) {
                ServerPlayer player = source.getPlayer();
                if (player == null) {
                    source.sendSystemMessage(Component.literal("An error occurred while fetching your player data. Please try again later.").withStyle(ChatFormatting.RED));
                    return 1;
                }
                if (WynautRankUp.instance.matchmakingQueue.isInQueue(player.getUUID())) {
                    source.sendSystemMessage(Component.literal("You are already in the ranked queue.").withStyle(ChatFormatting.RED));
                    return 1;
                }
                Optional<List<Pokemon>> activePartyPokemon = BattleUtil.getOptionalActivePartyPokemon(player.getUUID());
                if (activePartyPokemon.isPresent()) {
                    //verify team legality
                    List<Pokemon> team = activePartyPokemon.get();
                    boolean isLegal = WynautRankUp.instance.teamValidator.isTeamLegal(team);
                    if (!isLegal) {
                        source.sendSystemMessage(Component.literal("Your current team is not legal for ranked battles. Please ensure you have a full team of 6 Pokémon and that they comply with all ranked battle rules.").withStyle(ChatFormatting.RED));
                        List<String> teamIllegalityReasons = WynautRankUp.instance.teamValidator.getTeamIllegality(team);
                        teamIllegalityReasons.forEach(reason -> source.sendSystemMessage(Component.literal("- " + reason).withStyle(ChatFormatting.RED)));
                        return 1;
                    }

                    if (BattleRegistry.getBattleByParticipatingPlayerId(player.getUUID()) != null) {
                        source.sendSystemMessage(Component.literal("You are currently in a battle and cannot join the ranked queue.").withStyle(ChatFormatting.RED));
                        return 1;
                    }
                    List<Pokemon> copyedTeam = new ArrayList<>(team);
                    WynautRankUp.instance.matchmakingQueue.addToQueue(player.getUUID(), copyedTeam);
                    source.sendSystemMessage(Component.literal("You have been added to the ranked queue.").withStyle(ChatFormatting.GREEN));
                } else {
                    source.sendSystemMessage(Component.literal("You need to have a full team of 6 Pokémon in your active party to join the ranked queue.").withStyle(ChatFormatting.RED));
                }
            } else {
                source.sendSystemMessage(Component.literal("This command can only be run by a player.").withStyle(ChatFormatting.RED));
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }


        return 1;
    }

    public static int leaveQueue(CommandSourceStack source) {
        if (source.isPlayer()) {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                source.sendSystemMessage(Component.literal("An error occurred while fetching your player data. Please try again later.").withStyle(ChatFormatting.RED));
                return 1;
            }
            if (!WynautRankUp.instance.matchmakingQueue.isInQueue(player.getUUID())) {
                source.sendSystemMessage(Component.literal("You are not in the ranked queue.").withStyle(ChatFormatting.RED));
                return 1;
            }
            WynautRankUp.instance.matchmakingQueue.removeFromQueue(player.getUUID());
            source.sendSystemMessage(Component.literal("You have been removed from the ranked queue.").withStyle(ChatFormatting.GREEN));
        } else {
            source.sendSystemMessage(Component.literal("This command can only be run by a player.").withStyle(ChatFormatting.RED));
        }

        return 1;
    }

    public static int leaderboard(CommandSourceStack source) {
        if (source.isPlayer()) {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                source.sendSystemMessage(Component.literal("An error occurred while fetching your player data. Please try again later.").withStyle(ChatFormatting.RED));
                return 1;
            }

            //get current season id
            String seasonId = WynautRankUp.instance.seasonManager.getCurrentSeasonId();
            LeaderboardGUI leaderboardGUI = new LeaderboardGUI();
            leaderboardGUI.open(player, seasonId);
        } else {
            source.sendSystemMessage(Component.literal("This command can only be run by a player.").withStyle(ChatFormatting.RED));
        }

        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> getCommand() {
        return Commands.literal("ranked")
                .requires(source -> {
                    if (source.isPlayer()) {
                        return PermissionUtil.hasCachedLuckPerms(source, PERMISSION_NODE);
                    } else {
                        return true;
                    }
                })
                .executes(cc -> {
                    // execute help
                    return help(cc.getSource());
                })
                // statistics command
                .then(Commands.literal("help")
                        .executes(cc -> help(cc.getSource()))
                )
                .then(Commands.literal("banned")
                        .executes(cc -> banned(cc.getSource())
                        )
                )

                .then(Commands.literal("stats")
                        .executes(cc -> stats(cc.getSource()))
                )
                // leaderboard command
                .then(Commands.literal("leaderboard")
                        .executes(cc -> leaderboard(cc.getSource()))
                )
                .then(
                        Commands.literal("queue")
                                .executes(cc -> queue(cc.getSource()))
                        .then(
                                Commands.literal("leave")
                                .executes(cc -> leaveQueue(cc.getSource()))
                        )
                        .then(
                                Commands.literal("join")
                                .executes(cc -> joinQueue(cc.getSource()))
                        )
                )
                ;
    }
}
