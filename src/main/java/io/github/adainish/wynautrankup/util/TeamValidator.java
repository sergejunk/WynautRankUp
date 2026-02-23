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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.Items;

import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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
            return false;
        }
        for (Pokemon p : team)
            for (BannedPokemonRule rule : config.bannedPokemon)
                if (matchesRule(p, rule)) {
                    System.out.println("[DEBUG] Illegal Pokémon found: " + p.getSpecies().getName() +
                            " (Form: " + p.getForm().getName() +
                            ", Ability: " + p.getAbility().getName() +
                            ", Aspects: " + p.getAspects() +
                            ", Held Item: "
                            + (p.getHeldItem$common().isEmpty() ? "None"
                                    : BuiltInRegistries.ITEM.getKey(p.getHeldItem$common().getItem()))
                            +
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
        team.forEach(p -> config.bannedPokemon.stream()
                .map(rule -> getIllegalityReasons(p, rule))
                .forEach(reasons::addAll));
        return reasons;
    }

    /**
     * IMPORTANT: Only report reasons if the Pokémon fully matches the rule.
     * Otherwise you get misleading "move banned" messages for combo rules.
     */
    private List<String> getIllegalityReasons(Pokemon p, BannedPokemonRule rule) {
        List<String> reasons = new ArrayList<>();
        if (!matchesRule(p, rule))
            return reasons;

        String name = p.getSpecies().getName();

        if (rule.species != null && !rule.species.isEmpty())
            reasons.add(name + " matches banned species: " + rule.species);

        if (rule.form != null && !rule.form.isEmpty())
            reasons.add(name + " matches banned form: " + rule.form);

        if (rule.ability != null && !rule.ability.isEmpty())
            reasons.add(name + " matches banned ability: " + rule.ability);

        if (rule.aspect != null && !rule.aspect.isEmpty())
            reasons.add(name + " matches banned aspect: " + rule.aspect);

        if (rule.heldItem != null && !rule.heldItem.isEmpty())
            reasons.add(name + " matches banned held item: " + rule.heldItem);

        if (rule.moves != null && !rule.moves.isEmpty())
            reasons.add(name + " matches banned move(s): " + rule.moves);

        return reasons;
    }

    /**
     * Full AND-match:
     * - species (if set)
     * - form (if set)
     * - ability (if set)
     * - aspect (if set)
     * - held item (if set)
     * - moves (if set): Pokémon must contain ALL listed moves
     */
    private boolean matchesRule(Pokemon p, BannedPokemonRule rule) {
        if (rule.species != null && !rule.species.isEmpty()) {
            if (!p.getSpecies().getName().equalsIgnoreCase(rule.species))
                return false;
        }

        if (rule.form != null && !rule.form.isEmpty()) {
            if (!p.getForm().getName().equalsIgnoreCase(rule.form))
                return false;
        }

        if (rule.ability != null && !rule.ability.isEmpty()) {
            if (!p.getAbility().getName().equalsIgnoreCase(rule.ability))
                return false;
        }

        if (rule.aspect != null && !rule.aspect.isEmpty()) {
            String want = rule.aspect.toLowerCase();
            boolean hasAspect = p.getAspects().stream()
                    .map(String::toLowerCase)
                    .anyMatch(a -> a.equals(want) || a.endsWith(":" + want));
            if (!hasAspect)
                return false;
        }

        if (rule.heldItem != null && !rule.heldItem.isEmpty()) {
            ItemStack held = p.getHeldItem$common();
            String resourceId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            if (!resourceId.equalsIgnoreCase(rule.heldItem))
                return false;
        }

        if (rule.moves != null && !rule.moves.isEmpty()) {
            List<String> pokemonMoves = p.getMoveSet().getMoves().stream()
                    .map(Move::getName)
                    .map(String::toLowerCase)
                    .toList();

            List<String> ruleMoves = rule.moves.stream()
                    .map(String::toLowerCase)
                    .toList();

            if (!new HashSet<>(pokemonMoves).containsAll(ruleMoves))
                return false;
        }

        return true;
    }

    public boolean doTeamsMatch(Player player, List<Pokemon> team2) {
        List<Pokemon> team1 = player.getCurrentPartyTeam();
        if (team1.size() != team2.size())
            return false;

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
            if (!found)
                return false;
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
                reasons.add(prefix + "Species mismatch (" + p1.getSpecies().getName() + " vs "
                        + p2.getSpecies().getName() + ")");

            if (!p1.getForm().getName().equalsIgnoreCase(p2.getForm().getName()))
                reasons.add(
                        prefix + "Form mismatch (" + p1.getForm().getName() + " vs " + p2.getForm().getName() + ")");

            if (!p1.getAbility().getName().equalsIgnoreCase(p2.getAbility().getName()))
                reasons.add(prefix + "Ability mismatch (" + p1.getAbility().getName() + " vs "
                        + p2.getAbility().getName() + ")");

            if (p1.getHeldItem$common().getItem() != p2.getHeldItem$common().getItem())
                reasons.add(prefix + "Held item mismatch");

            List<String> moves1 = p1.getMoveSet().getMoves().stream().map(Move::getName).toList();
            List<String> moves2 = p2.getMoveSet().getMoves().stream().map(Move::getName).toList();
            if (moves1.size() != moves2.size() || !new HashSet<>(moves1).containsAll(moves2))
                reasons.add(prefix + "Moves mismatch (" + moves1 + " vs " + moves2 + ")");
        }

        return reasons;
    }

    public List<Button> getGlobalBanButtons() {
        List<Button> buttons = new ArrayList<>();
        if (config == null || config.bannedPokemon == null)
            return buttons;

        for (BannedPokemonRule rule : config.bannedPokemon) {
            if (rule.species != null && !rule.species.isEmpty())
                continue; // nur global

            StringBuilder lore = new StringBuilder();

            if (rule.moves != null && !rule.moves.isEmpty()) {
                lore.append("§e• Globally banned moves:\n");
                for (String move : rule.moves) {
                    String capMove = move.substring(0, 1).toUpperCase() + move.substring(1);
                    lore.append("  §c- ").append(capMove).append("\n");
                }
            } else {
                lore.append("§7• Globally banned moves: None\n");
            }

            if (rule.heldItem != null && !rule.heldItem.isEmpty()) {
                String cleanItem = rule.heldItem.replaceFirst("^(cobblemon:|minecraft:)", "");
                if (!cleanItem.isEmpty())
                    cleanItem = cleanItem.substring(0, 1).toUpperCase() + cleanItem.substring(1);
                lore.append("§e• Globally banned item: §f").append(cleanItem).append("\n");
            }

            if (rule.ability != null && !rule.ability.isEmpty()) {
                String capAbility = rule.ability.substring(0, 1).toUpperCase() + rule.ability.substring(1);
                lore.append("§e• Globally banned ability: §f").append(capAbility).append("\n");
            }

            if (rule.aspect != null && !rule.aspect.isEmpty()) {
                String capAsp = rule.aspect.substring(0, 1).toUpperCase() + rule.aspect.substring(1);
                lore.append("§e• Globally banned aspect: §f").append(capAsp).append("\n");
            }

            if (rule.form != null && !rule.form.isEmpty()) {
                String capForm = rule.form.substring(0, 1).toUpperCase() + rule.form.substring(1);
                lore.append("§e• Globally banned form: §f").append(capForm).append("\n");
            }

            // Wenn wirklich gar nichts gesetzt ist, skip
            boolean hasAny = (rule.moves != null && !rule.moves.isEmpty()) ||
                    (rule.heldItem != null && !rule.heldItem.isEmpty()) ||
                    (rule.ability != null && !rule.ability.isEmpty()) ||
                    (rule.aspect != null && !rule.aspect.isEmpty()) ||
                    (rule.form != null && !rule.form.isEmpty());

            if (!hasAny)
                continue;

            Button b = GooeyButton.builder()
                    .display(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER))
                    .with(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                            net.minecraft.network.chat.Component.literal("§eGlobal Ban"))
                    .with(DataComponents.LORE, new ItemLore(
                            Util.formattedComponentList(
                                    List.of(lore.toString().split("\n")))))
                    .build();

            buttons.add(b);
        }

        return buttons;
    }

    public List<Button> getBannedPokemonButtons() {
        List<Button> buttons = new ArrayList<>();
        if (config == null || config.bannedPokemon == null)
            return buttons;

        for (BannedPokemonRule rule : config.bannedPokemon) {
            if (rule.species == null || rule.species.isEmpty())
                continue;
            if (PokemonSpecies.getByName(rule.species) == null)
                continue;

            Pokemon pokemon = PokemonSpecies.getByName(rule.species).create(100);

            // Apply aspect so the icon renders the variant (e.g., galarian)
            if (rule.aspect != null && !rule.aspect.isEmpty()) {
                pokemon.getAspects().add(rule.aspect.toLowerCase());
            }

            StringBuilder lore = new StringBuilder();

            if (rule.form != null && !rule.form.isEmpty()) {
                String capForm = rule.form.substring(0, 1).toUpperCase() + rule.form.substring(1);
                lore.append("§e• Form: §f").append(capForm).append(" §7(Required for a ban)\n");
            } else {
                lore.append("§7• Form: None\n");
            }

            if (rule.aspect != null && !rule.aspect.isEmpty()) {
                String asp = rule.aspect.substring(0, 1).toUpperCase() + rule.aspect.substring(1);
                lore.append("§e• Aspect: §f").append(asp).append(" §7(Required for a ban)\n");
            } else {
                lore.append("§7• Aspect: None\n");
            }

            if (rule.ability != null && !rule.ability.isEmpty()) {
                String ability = rule.ability.substring(0, 1).toUpperCase() + rule.ability.substring(1);
                lore.append("§e• Ability: §f").append(ability).append(" §7(Required for a ban)\n");
            } else {
                lore.append("§7• Ability: None\n");
            }

            if (rule.heldItem != null && !rule.heldItem.isEmpty()) {
                String cleanItem = rule.heldItem.replaceFirst("^(cobblemon:|minecraft:)", "");
                if (!cleanItem.isEmpty())
                    cleanItem = cleanItem.substring(0, 1).toUpperCase() + cleanItem.substring(1);
                lore.append("§e• Held Item: §f").append(cleanItem).append(" §7(Required for a ban)\n");
            } else {
                lore.append("§7• Held Item: None\n");
            }

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

            // Display name: "PokemonName Aspect" (e.g., "Darmanitan Galarian")
            String displayName = rule.species.substring(0, 1).toUpperCase() + rule.species.substring(1).toLowerCase();
            if (rule.aspect != null && !rule.aspect.isEmpty()) {
                String asp = rule.aspect.substring(0, 1).toUpperCase() + rule.aspect.substring(1).toLowerCase();
                displayName += " " + asp;
            }

            Button button = GooeyButton.builder()
                    .display(Util.pokemonIcon(pokemon))
                    .with(DataComponents.CUSTOM_NAME, Component.literal("§e" + displayName))
                    .with(DataComponents.LORE, new ItemLore(Util.formattedComponentList(
                            lore.toString().isEmpty() ? List.of("Banned") : List.of(lore.toString().split("\n")))))
                    .build();

            buttons.add(button);
        }

        return buttons;
    }

    public List<Button> getGlobalBannedMoveButtons() {
        SortedSet<String> moves = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        if (config == null || config.bannedPokemon == null)
            return List.of();

        for (BannedPokemonRule rule : config.bannedPokemon) {
            boolean isGlobal = rule.species == null || rule.species.isEmpty();
            if (!isGlobal)
                continue;

            if (rule.moves != null)
                moves.addAll(rule.moves);
        }

        return moves.stream()
                .map(m -> GooeyButton.builder()
                        .display(new ItemStack(Items.PAPER))
                        .with(DataComponents.CUSTOM_NAME, Component.literal("§e" + prettify(m)))
                        .with(DataComponents.LORE, new ItemLore(Util.formattedComponentList(
                                List.of("§7Type: §cMove", "§7Globally banned"))))
                        .build())
                .collect(Collectors.toList());
    }

    public List<Button> getGlobalBannedAbilityButtons() {
        SortedSet<String> abilities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        if (config == null || config.bannedPokemon == null)
            return List.of();

        for (BannedPokemonRule rule : config.bannedPokemon) {
            boolean isGlobal = rule.species == null || rule.species.isEmpty();
            if (!isGlobal)
                continue;

            if (rule.ability != null && !rule.ability.isEmpty())
                abilities.add(rule.ability);
        }

        return abilities.stream()
                .map(a -> GooeyButton.builder()
                        .display(new ItemStack(Items.NETHER_STAR))
                        .with(DataComponents.CUSTOM_NAME, Component.literal("§e" + prettify(a)))
                        .with(DataComponents.LORE, new ItemLore(Util.formattedComponentList(
                                List.of("§7Type: §cAbility", "§7Globally banned"))))
                        .build())

                .collect(Collectors.toList());
    }

    public List<Button> getGlobalBannedItemButtons() {
        SortedSet<String> items = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        if (config == null || config.bannedPokemon == null)
            return List.of();

        for (BannedPokemonRule rule : config.bannedPokemon) {
            boolean isGlobal = rule.species == null || rule.species.isEmpty();
            if (!isGlobal)
                continue;

            if (rule.heldItem != null && !rule.heldItem.isEmpty())
                items.add(rule.heldItem);
        }

        return items.stream()
                .map(id -> {

                    // echtes Item holen
                    ItemStack icon = new ItemStack(
                            BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));

                    // Anzeige schöner machen
                    String clean = id.replaceFirst("^(cobblemon:|minecraft:)", "");
                    clean = clean.replace('_', ' ');
                    String name = prettify(clean);

                    return GooeyButton.builder()
                            .display(icon) 
                            .with(DataComponents.CUSTOM_NAME, Component.literal("§e" + name))
                            .with(DataComponents.LORE, new ItemLore(Util.formattedComponentList(
                                    List.of(
                                            "§7Type: §6Held Item",
                                            "§7ID: §f" + id,
                                            "§cGlobally banned"))))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /** macht "shadow tag" / "shadow-tag" / "shadowtag" etwas hübscher */
    private String prettify(String s) {
        if (s == null)
            return "";
        String x = s.trim().replace('_', ' ').replace('-', ' ');
        if (x.isEmpty())
            return x;
        return x.substring(0, 1).toUpperCase() + x.substring(1);
    }
}
