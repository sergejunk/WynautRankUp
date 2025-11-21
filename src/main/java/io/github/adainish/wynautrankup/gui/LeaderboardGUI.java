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
package io.github.adainish.wynautrankup.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import io.github.adainish.wynautrankup.WynautRankUp;
import io.github.adainish.wynautrankup.database.PlayerDataManager;
import io.github.adainish.wynautrankup.util.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LeaderboardGUI {

    public void open(ServerPlayer player, String seasonId) {
        WynautRankUp.instance.playerDataManager.getLeaderboardForSeason(seasonId, 100).thenAccept(leaderboard -> {
            List<Button> buttons = new ArrayList<>();
            UUID viewerId = player.getUUID();
            int rank = 1;
            PlayerDataManager.PlayerEloEntry selfEntry = null;
            int selfRank = -1;
            GooeyButton selfButton = null;
            for (PlayerDataManager.PlayerEloEntry entry : leaderboard) {
                String playerName = Util.getPlayerName(entry.playerId());
                boolean isViewer = entry.playerId().equals(viewerId.toString());
                ItemStack icon = Util.getPlayerHead(playerName, UUID.fromString(entry.playerId()));

                String displayName = (isViewer ? "§a" : "§e") + "#" + rank + " §f" + playerName + " §7- §b" + entry.elo();
                List<Component> lore = List.of(
                        Component.literal("§7Ranked: §e" + rank),
                        Component.literal("§7Rank: " + WynautRankUp.instance.rankManager.getRankStringForElo(entry.elo())),
                        Component.literal("§7ELO: §b" + entry.elo())
                );

                GooeyButton button = GooeyButton.builder()
                        .display(icon)
                        .with(DataComponents.CUSTOM_NAME, Component.literal(displayName))
                        .with(DataComponents.LORE, new ItemLore(lore))
                        .build();
                buttons.add(button);

                if (isViewer) {
                    selfEntry = entry;
                    selfRank = rank;
                }
                rank++;
            }

            // Add "Your Position" button in the last slot
            if (selfEntry != null) {
                ItemStack selfIcon = Util.getPlayerHead(player.getName().getString(), viewerId);
                String selfDisplay = "§bYour Position: §a#" + selfRank + " §f" + player.getName().getString() + " §7- §b" + selfEntry.elo();
                List<Component> selfLore = List.of(
                        Component.literal("§7Ranked: §e" + selfRank),
                        Component.literal("§7ELO: §b" + selfEntry.elo()),
                        Component.literal("§7Rank: " + WynautRankUp.instance.rankManager.getRankStringForElo(selfEntry.elo())),
                        Component.literal("§7Click to view your stats!")
                );
                int finalSelfRank = selfRank;
                PlayerDataManager.PlayerEloEntry finalSelfEntry = selfEntry;
                selfButton = GooeyButton.builder()
                        .display(selfIcon)
                        .with(DataComponents.CUSTOM_NAME, Component.literal(selfDisplay))
                        .with(DataComponents.LORE, new ItemLore(selfLore))
                        .onClick((action) -> {
                            // Optionally, show a message or open a detailed stats GUI
                            player.sendSystemMessage(Component.literal("§bYou are ranked §a#" + finalSelfRank + " §bwith §e" + finalSelfEntry.elo() + " ELO§b!"));
                        })
                        .build();
                buttons.add(selfButton);
            }

            ChestTemplate.Builder builder = Util.returnBasicTemplateBuilder();
            if (selfButton != null) {
                builder.set(4, 8, selfButton); // Place in the bottom row, most right slot
            }
            LinkedPage page = PaginationHelper.createPagesFromPlaceholders(
                    builder.build(),
                    buttons,
                    LinkedPage.builder().title(Util.formattedString("§bLeaderboard - Season " + seasonId)).template(builder.build())
            );
            UIManager.openUIForcefully(player, page);
        });
    }

}