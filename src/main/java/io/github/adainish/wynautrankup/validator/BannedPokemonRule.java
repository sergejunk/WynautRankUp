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
package io.github.adainish.wynautrankup.validator;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class BannedPokemonRule {
    public String species = "";
    public String form = "";
    public Set<String> moves = new HashSet<>();
    public String ability = "";
    public String heldItem = "";
    public String aspect = "";

    public BannedPokemonRule() {

    }

    public BannedPokemonRule(String species, String form, String ability, String heldItem, Set<String> moves) {
        this.species = species;
        this.form = form;
        this.ability = ability;
        this.heldItem = heldItem;
        this.moves = moves;
    }

    public static BannedPokemonRule parse(String ruleStr, List<String> errors) {
        BannedPokemonRule rule = new BannedPokemonRule();
        String[] parts = ruleStr.split(";");
        Species species = null;
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            String key = kv[0].trim();
            String value = kv.length > 1 ? kv[1].trim() : "";

            if (key.equals("species") && value.isEmpty()) {
                errors.add("Missing or empty species in rule: '" + ruleStr + "'");
                continue;
            }

            switch (key) {
                case "species":
                    if (PokemonSpecies.getByIdentifier(ResourceLocation.parse("cobblemon:" + value)) == null) {
                        errors.add("Unknown species: '" + value + "' in rule: '" + ruleStr + "'");
                    }
                    rule.species = value;
                    species = PokemonSpecies.getByIdentifier(ResourceLocation.parse("cobblemon:" + value));
                    break;
                case "form":
                    if (value.isEmpty()) {
                        errors.add("Warning: empty value for 'form' in rule: '" + ruleStr + "'");
                        break;
                    }
                    if (species == null) {
                        errors.add("Species must be defined before form in rule: '" + ruleStr + "'");
                        break;
                    }
                    AtomicBoolean hasForm = new AtomicBoolean(false);
                    species.getForms().forEach(formData -> {
                        if (formData.getName().equals(value)) {
                            hasForm.set(true);
                        }
                    });
                    if (!hasForm.get()) {
                        errors.add("Unknown form: '" + value + "' in rule: '" + ruleStr + "'");
                        break;
                    }
                    rule.form = value;
                    break;
                case "moves":
                    if (value.isEmpty()) {
                        errors.add("Warning: empty value for 'moves' in rule: '" + ruleStr + "'");
                        break;
                    }

                    if (rule.species == null || rule.species.isEmpty()) {
                        rule.moves.add(value);
                        break;
                    }

                    species = PokemonSpecies.getByIdentifier(ResourceLocation.parse("cobblemon:" + rule.species));
                    if (species == null) {
                        errors.add("Species must be defined before moves in rule: '" + ruleStr + "'");
                        break;
                    }

                    boolean canLearn = species.getMoves().getTmMoves().stream()
                            .anyMatch(m -> m.getName().equalsIgnoreCase(value)) ||
                            species.getMoves().getLevelUpMovesUpTo(100).stream()
                                    .anyMatch(m -> m.getName().equalsIgnoreCase(value))
                            ||
                            species.getMoves().getEggMoves().stream().anyMatch(m -> m.getName().equalsIgnoreCase(value))
                            ||
                            species.getMoves().getTutorMoves().stream()
                                    .anyMatch(m -> m.getName().equalsIgnoreCase(value));

                    if (!canLearn) {
                        errors.add("Species '" + rule.species + "' cannot learn move: '" + value + "' in rule: '"
                                + ruleStr + "'");
                        break;
                    }

                    rule.moves.add(value);
                    break;
                case "ability":
                    if (value.isEmpty()) {
                        errors.add("Warning: empty value for 'ability' in rule: '" + ruleStr + "'");
                        break;
                    }
                    
                    if (rule.species == null || rule.species.isEmpty()) {
                        rule.ability = value;
                        break;
                    }

                    if (species == null) {
                        errors.add("Species must be defined before ability in rule: '" + ruleStr + "'");
                        break;
                    }

                    AtomicBoolean hasAbility = new AtomicBoolean(false);
                    species.getAbilities().forEach(abilityData -> {
                        if (abilityData.getTemplate().getName().equalsIgnoreCase(value)) {
                            hasAbility.set(true);
                        }
                    });
                    if (!hasAbility.get()) {
                        errors.add("Unknown ability: '" + value + "' in rule: '" + ruleStr + "'");
                        break;
                    }
                    rule.ability = value;
                    break;
                case "held_items":
                    if (value.isEmpty()) {
                        errors.add("Warning: empty value for 'held_items' in rule: '" + ruleStr + "'");
                        break;
                    }
                    if (BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(value)).isEmpty()) {
                        errors.add("Unknown held item: '" + value + "' in rule: '" + ruleStr + "'");
                        break;
                    }
                    rule.heldItem = value;
                    break;
                case "aspect":
                    if (value.isEmpty()) {
                        errors.add("Warning: empty value for 'aspect' in rule: '" + ruleStr + "'");
                        break;
                    }
                    rule.aspect = value;
                    break;
                default:
                    errors.add(
                            "Unrecognized key: '" + key + "' with value: '" + value + "' in rule: '" + ruleStr + "'");
                    break;
            }
        }
        boolean hasAnyCondition = (rule.species != null && !rule.species.isEmpty()) ||
                (rule.form != null && !rule.form.isEmpty()) ||
                (rule.ability != null && !rule.ability.isEmpty()) ||
                (rule.heldItem != null && !rule.heldItem.isEmpty()) ||
                (rule.aspect != null && !rule.aspect.isEmpty()) ||
                (rule.moves != null && !rule.moves.isEmpty());

        if (!hasAnyCondition) {
            errors.add("Rule has no conditions: '" + ruleStr + "'");
        }
        return rule;
    }

}
