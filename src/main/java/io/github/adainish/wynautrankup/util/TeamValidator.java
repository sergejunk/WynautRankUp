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
package io.github.adainish.wynautrankup.util;

import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.github.adainish.wynautrankup.data.Player;
import io.github.adainish.wynautrankup.validator.BannedPokemonRule;
import io.github.adainish.wynautrankup.validator.TeamValidationConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TeamValidator {
    private TeamValidationConfig config;

    public void setConfig(TeamValidationConfig cfg) {
        config = cfg.loadFromFile();
    }

    public boolean isTeamLegal(List<Pokemon> team) {
        boolean isLegal = true;
        if (team == null || team.size() != 6 || config == null) {
            if (config == null) {
                System.out.println("[DEBUG] Config is null.");
            }
            if (team == null) {
                System.out.println("[DEBUG] Team is null.");
            }
            System.out.println("[DEBUG] Team is null, not 6 Pokémon, or config is missing.");
            return isLegal;
        }
        for (Pokemon p : team)
            for (BannedPokemonRule rule : config.bannedPokemon)
                if (matchesRule(p, rule)) {
                    System.out.println("[DEBUG] Illegal Pokémon found: " + p.getSpecies().getName() +
                            " (Form: " + p.getForm().getName() +
                            ", Ability: " + p.getAbility().getName() +
                            ", Held Item: " + (p.getHeldItem$common().isEmpty() ? "None" : BuiltInRegistries.ITEM.getKey(p.getHeldItem$common().getItem())) +
                            ", Moves: " + p.getMoveSet().getMoves().stream().map(Move::getName).toList() +
                            ") matches rule: " + rule);
                    isLegal = false;
                }
        return isLegal;
    }

    public List<String> getTeamIllegality(List<Pokemon> team) {
        List<String> reasons = new ArrayList<>();
        if (team == null) {
            reasons.add("Team is null.");
            return reasons;
        }
        if (team.size() != 6) {
            reasons.add("Team must have exactly 6 Pokémon.");
        }
        if (config == null) {
            reasons.add("Validation config is not set up by admins. Please contact them.");
            return reasons;
        }
        team.forEach(p -> config.bannedPokemon.stream().map(rule -> getIllegalityReasons(p, rule)).forEach(reasons::addAll));
        return reasons;
    }

    private List<String> getIllegalityReasons(Pokemon p, BannedPokemonRule rule) {
        List<String> reasons = new ArrayList<>();
        String name = p.getSpecies().getName();
        if (name.equalsIgnoreCase(rule.species)) {
            reasons.add(name + " is a banned species.");
        }
        if (p.getForm().getName().equalsIgnoreCase(rule.form)) {
            reasons.add(name + " has banned form: " + rule.form);
        }
        if (p.getAbility().getName().equalsIgnoreCase(rule.ability)) {
            reasons.add(name + " has banned ability: " + rule.ability);
        }
        if (rule.heldItem != null) {
            ItemStack held = p.getHeldItem$common();
            String resourceId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            if (resourceId.equalsIgnoreCase(rule.heldItem)) {
                reasons.add(name + " is holding banned item: " + rule.heldItem);
            }
        }
        if (!rule.moves.isEmpty()) {
            rule.moves.forEach(move -> {
                boolean found = p.getMoveSet().getMoves().stream().anyMatch(m -> m.getName().equalsIgnoreCase(move));
                if (found) {
                    reasons.add(name + " knows banned move: " + move);
                }
            });
        }
        return reasons;
    }


    private boolean matchesRule(Pokemon p, BannedPokemonRule rule) {
        if (rule.species != null && !rule.species.isEmpty()) {
            if (!p.getSpecies().getName().equalsIgnoreCase(rule.species)) {
                return false;
            }
        }
        if (rule.form != null && !rule.form.isEmpty()) {
            if (!p.getForm().getName().equalsIgnoreCase(rule.form)) {
                return false;
            }
        }
        if (rule.ability != null && !rule.ability.isEmpty()) {
            if (!p.getAbility().getName().equalsIgnoreCase(rule.ability)) {
                return false;
            }
        }
        if (rule.heldItem != null && !rule.heldItem.isEmpty()) {
            ItemStack held = p.getHeldItem$common();
            String resourceId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            if (!resourceId.equalsIgnoreCase(rule.heldItem)) {
                return false;
            }
        }
        if (rule.moves != null && !rule.moves.isEmpty()) {
            List<String> pokemonMoves = p.getMoveSet().getMoves().stream()
                    .map(Move::getName)
                    .map(String::toLowerCase)
                    .toList();
            List<String> ruleMoves = rule.moves.stream()
                    .map(String::toLowerCase)
                    .toList();
            return new HashSet<>(pokemonMoves).containsAll(ruleMoves);
        }
        return true;
    }

    public boolean doTeamsMatch(Player player, List<Pokemon> team2) {
        List<Pokemon> team1 = player.getCurrentPartyTeam();
        if (team1.size() != team2.size()) return false;
        for (Pokemon p1 : team1) {
            boolean found = false;
            for (Pokemon p2 : team2) {
                boolean sameSpecies = p1.getSpecies().getName().equalsIgnoreCase(p2.getSpecies().getName());
                boolean sameForm = p1.getForm().getName().equalsIgnoreCase(p2.getForm().getName());
                boolean sameAbility = p1.getAbility().getName().equalsIgnoreCase(p2.getAbility().getName());
                boolean sameHeldItem = p1.getHeldItem$common().getItem() == p2.getHeldItem$common().getItem();

                List<String> moves1 = p1.getMoveSet().getMoves().stream().map(Move::getName).toList();
                List<String> moves2 = p2.getMoveSet().getMoves().stream().map(Move::getName).toList();
                boolean sameMoves = moves1.size() == moves2.size() && new HashSet<>(moves1).containsAll(moves2);

                if (sameSpecies && sameForm && sameAbility && sameHeldItem && sameMoves) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public List<String> getTeamMismatchReasons(Player player, List<Pokemon> team2) {
        List<String> reasons = new ArrayList<>();
        List<Pokemon> team1 = player.getCurrentPartyTeam();
        if (team1.size() != team2.size()) {
            reasons.add("Team size mismatch: " + team1.size() + " vs " + team2.size());
            return reasons;
        }
        for (int i = 0; i < team1.size(); i++) {
            Pokemon p1 = team1.get(i);
            Pokemon p2 = team2.get(i);
            String prefix = "Slot " + (i + 1) + ": ";
            if (!p1.getSpecies().getName().equalsIgnoreCase(p2.getSpecies().getName()))
                reasons.add(prefix + "Species mismatch (" + p1.getSpecies().getName() + " vs " + p2.getSpecies().getName() + ")");
            if (!p1.getForm().getName().equalsIgnoreCase(p2.getForm().getName()))
                reasons.add(prefix + "Form mismatch (" + p1.getForm().getName() + " vs " + p2.getForm().getName() + ")");
            if (!p1.getAbility().getName().equalsIgnoreCase(p2.getAbility().getName()))
                reasons.add(prefix + "Ability mismatch (" + p1.getAbility().getName() + " vs " + p2.getAbility().getName() + ")");
            if (p1.getHeldItem$common().getItem() != p2.getHeldItem$common().getItem())
                reasons.add(prefix + "Held item mismatch");
            List<String> moves1 = p1.getMoveSet().getMoves().stream().map(Move::getName).toList();
            List<String> moves2 = p2.getMoveSet().getMoves().stream().map(Move::getName).toList();
            if (moves1.size() != moves2.size() || !new HashSet<>(moves1).containsAll(moves2))
                reasons.add(prefix + "Moves mismatch (" + moves1 + " vs " + moves2 + ")");
        }
        return reasons;
    }


    public List<Button> getBannedPokemonButtons() {
        List<Button> buttons = new ArrayList<>();
        if (config == null || config.bannedPokemon == null) {
            return buttons;
        }
        for (BannedPokemonRule rule : config.bannedPokemon) {
            if (rule.species == null) continue; // Species is required to create a Pokemon instance
            if (PokemonSpecies.getByName(rule.species) == null)
                continue;
            Pokemon pokemon = PokemonSpecies.getByName(rule.species).create(100);
            StringBuilder lore = new StringBuilder();
            if (rule.form != null && !rule.form.isEmpty()) {
                String capForm = rule.form.substring(0, 1).toUpperCase() + rule.form.substring(1);
                lore.append("§e• Form: §f").append(capForm).append(" §7(Required for a ban)\n");
            } else {
                lore.append("§7• Form: None\n");
            }


            if (rule.ability != null && !rule.ability.isEmpty()) {
                String ability = rule.ability.substring(0, 1).toUpperCase() + rule.ability.substring(1);
                lore.append("§e• Ability: §f").append(ability).append(" §7(Required for a ban)\n");
            } else {
                lore.append("§7• Ability: None\n");
            }

            if (rule.heldItem != null && !rule.heldItem.isEmpty()) {
                String cleanItem = rule.heldItem.replaceFirst("^(cobblemon:|minecraft:)", "");
                if (!cleanItem.isEmpty()) {
                    cleanItem = cleanItem.substring(0, 1).toUpperCase() + cleanItem.substring(1);
                }
                lore.append("§e• Held Item: §f").append(cleanItem).append(" §7(Required for a ban)\n");
            } else
                lore.append("§7• Held Item: None\n");

            if (rule.moves != null && !rule.moves.isEmpty()) {
                lore.append("§e• Banned Moves:\n");
                for (String move : rule.moves) {
                    String capMove = move.substring(0, 1).toUpperCase() + move.substring(1);
                    lore.append("  §c- ").append(capMove).append(" §7(Required for a ban)\n");
                }
            } else {
                lore.append("§7• Banned Moves: None\n");
            }


            if (lore.isEmpty())
                lore.append("§cThis Pokémon is fully banned.");

            Button button = GooeyButton.builder()
                    .display(Util.pokemonIcon(pokemon))
                    .with(DataComponents.CUSTOM_NAME, Component.literal(
                            rule.species != null && !rule.species.isEmpty()
                                    ? "§e" + rule.species.substring(0, 1).toUpperCase() + rule.species.substring(1).toLowerCase()
                                    : "§cUnknown"
                    ))
                    .with(DataComponents.LORE, new ItemLore(Util.formattedComponentList(
                            lore.toString().isEmpty() ? List.of("Banned") : List.of(lore.toString().split("\n")))))
                    .build();
            buttons.add(button);
        }
        return buttons;
    }
}
