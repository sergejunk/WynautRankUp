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
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import io.github.adainish.wynautrankup.season.Season;
import io.github.adainish.wynautrankup.season.RewardCriteria;
import io.github.adainish.wynautrankup.season.Condition;
import io.github.adainish.wynautrankup.util.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ItemLore;

import java.util.*;

public class SeasonInfoGUI {
    public void open(ServerPlayer player, Season season, int playerElo, int playerStreak) {
        Set<Integer> usedSlots = new HashSet<>();
        ChestTemplate.Builder builder = ChestTemplate.builder(5); // More rows for space

        // Decorative border
        ItemStack border = new ItemStack(Items.LIGHT_BLUE_STAINED_GLASS_PANE);
        border.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        GooeyButton borderButton = GooeyButton.builder().display(border).build();
        for (int slot = 0; slot < 45; slot++) {
            if (slot < 9 || slot >= 36 || slot % 9 == 0 || slot % 9 == 8) {
                builder.set(slot, borderButton);
                usedSlots.add(slot);
            }
        }

        // Season Info Header (center top)
        ItemStack info = new ItemStack(Items.PAPER);
        info.set(DataComponents.CUSTOM_NAME, Component.literal("§6§l" + season.getName() + " Info"));
        info.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7" + season.getDescription()),
                Component.literal("§8----------------------"),
                Component.literal("§f§lEnds: §c" + season.getRewardDate()),
                Component.literal("§f§lStatus: " + (season.isActive() ? "§aActive" : "§cInactive"))
        )));
        builder.set(13, GooeyButton.builder().display(info).build());
        usedSlots.add(13);
        // Player Progress (left of center)
        ItemStack progress = new ItemStack(Items.EXPERIENCE_BOTTLE);
        progress.set(DataComponents.CUSTOM_NAME, Component.literal("§b§lYour Progress"));
        progress.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§fELO: §e" + playerElo),
                Component.literal("§fWin Streak: §e" + playerStreak)
        )));
        builder.set(11, GooeyButton.builder().display(progress).build());
        usedSlots.add(11);
        int slot = 19; // Start lower for criteria/rewards
        for (RewardCriteria criteria : season.getRewardCriteria()) {
            // Criteria Header
            ItemStack criteriaHeader = new ItemStack(Items.BOOK);
            criteriaHeader.set(DataComponents.CUSTOM_NAME, Component.literal("§d§lCriteria"));
            List<Component> critLore = new ArrayList<>();
            for (Condition cond : criteria.getConditions()) {
                critLore.add(Component.literal("§7Type: §f" + cond.getType()));
                if (cond.getMinValue() != 0 || cond.getMaxValue() != 0)
                    critLore.add(Component.literal("§7Range: §f" + cond.getMinValue() + " - " + cond.getMaxValue()));
                if (cond.getValue() != null && !cond.getValue().isEmpty())
                    critLore.add(Component.literal("§7Value: §f" + cond.getValue()));
            }
            criteriaHeader.set(DataComponents.LORE, new ItemLore(critLore));
            builder.set(slot, GooeyButton.builder().display(criteriaHeader).build());

            // Rewards Header
            ItemStack rewardsHeader = new ItemStack(Items.CHEST);
            rewardsHeader.set(DataComponents.CUSTOM_NAME, Component.literal("§6§lRewards"));
            List<Component> rewardLore = new ArrayList<>();
            for (String desc : criteria.getRewardsDescriptions()) {
                rewardLore.add(Component.literal("§7" + Util.formattedString(desc)));
            }
            rewardsHeader.set(DataComponents.LORE, new ItemLore(rewardLore));
            builder.set(slot + 1, GooeyButton.builder().display(rewardsHeader).build());
            usedSlots.add(slot + 1);
            slot += 2;
            if (slot > 33) break; // Prevent overflow
        }

        GooeyButton backButton = GooeyButton.builder()
                .display(new ItemStack(Items.SPECTRAL_ARROW))
                .with(DataComponents.CUSTOM_NAME, Component.literal("§eBack to Seasons"))
                .with(DataComponents.LORE, new ItemLore(Util.formattedComponentList(List.of("&7Click to return to the Seasons menu"))))
                .onClick(b -> {
                    SeasonsGUI seasonsGUI = new SeasonsGUI();
                    seasonsGUI.open(player);
                })
                .build();

        builder.set(4, 4, backButton);

        // After all content, fill empty slots:
        for (int i = 0; i < 45; i++) {
            if (!usedSlots.contains(i)) {
                builder.set(i, borderButton);
            }
        }



        GooeyPage page = GooeyPage.builder()
                .title(Component.literal("§b§lSeason Details"))
                .template(builder.build())
                .build();
        UIManager.openUIForcefully(player, page);
    }
}
