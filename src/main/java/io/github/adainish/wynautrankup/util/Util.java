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

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.authlib.GameProfile;
import io.github.adainish.wynautrankup.WynautRankUp;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class Util
{
    public static String formattedString(String s) {
        return s.replaceAll("&", "§");
    }

    public static Holder<Enchantment> getEnchantment(ResourceKey<Enchantment> enchantmentResourceKey) {
        return WynautRankUp.instance.server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentResourceKey);
    }
    public static List<String> formattedArrayList(List<String> list) {
        return list.stream().map(Util::formattedString).collect(Collectors.toList());
    }

    public static List<Component> formattedComponentList(List<String> s) {
        return s.stream().map(str -> Component.literal(formattedString(str))).collect(Collectors.toList());
    }

    public static GooeyButton filler() {
        return GooeyButton.builder()
                .display(new ItemStack(Items.GRAY_STAINED_GLASS_PANE))
                .build();
    }

    public static ItemStack pokemonIcon(Pokemon pokemon) {
        return PokemonItem.from(pokemon, 1);
    }

    /**
     * Get the player's head as an itemstack
     * @return the player's head as an itemstack
     */
    public static ItemStack getPlayerHead(String userName, UUID uuid) {
        ItemStack skullStack = new ItemStack(Items.PLAYER_HEAD);
        if (userName != null && uuid != null) {
            Optional<GameProfile> gameprofile = WynautRankUp.instance.server.getProfileCache().get(uuid);
            if (gameprofile.isPresent()) {
                skullStack.set(DataComponents.PROFILE, new ResolvableProfile(gameprofile.get()));
            } else {
                // Fallback: create a profile with the given name and uuid
                GameProfile fallbackProfile = new GameProfile(uuid, userName);
                skullStack.set(DataComponents.PROFILE, new ResolvableProfile(fallbackProfile));
            }
        }
        return skullStack;
    }

    public static ChestTemplate.Builder returnBasicTemplateBuilder() {
        ChestTemplate.Builder builder = ChestTemplate.builder(5);
        builder.fill(filler());

        PlaceholderButton placeHolderButton = new PlaceholderButton();
        LinkedPageButton previous = LinkedPageButton.builder()
                .display(new ItemStack(Items.SPECTRAL_ARROW))
                .with(DataComponents.CUSTOM_NAME, TextUtil.parseNativeText("Previous Page"))
                .linkType(LinkType.Previous)
                .build();

        LinkedPageButton next = LinkedPageButton.builder()
                .display(new ItemStack(Items.SPECTRAL_ARROW))
                .with(DataComponents.CUSTOM_NAME, TextUtil.parseNativeText("Next Page"))
                .linkType(LinkType.Next)
                .build();

        builder.set(0, 3, previous)
                .set(0, 5, next)
                .rectangle(1, 1, 3, 7, placeHolderButton);
        return builder;
    }

    public static String getPlayerName(String stringUUID) {
        UUID uuid = UUID.fromString(stringUUID);
        Optional<GameProfile> profile = WynautRankUp.instance.server.getProfileCache().get(uuid);
        if (profile.isPresent()) {
            return profile.get().getName();
        } else {
            return "Unknown";
        }
    }
}
