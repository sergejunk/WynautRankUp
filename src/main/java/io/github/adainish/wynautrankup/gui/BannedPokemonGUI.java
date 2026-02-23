package io.github.adainish.wynautrankup.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import io.github.adainish.wynautrankup.WynautRankUp;
import io.github.adainish.wynautrankup.util.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BannedPokemonGUI {

    public void open(ServerPlayer player) {
        UIManager.openUIForcefully(player, getBannedPokemonPage());
    }

    public LinkedPage getBannedPokemonPage() {
        ChestTemplate.Builder builder = Util.returnBasicTemplateBuilder();

        builder.set(0, 0,
                GooeyButton.builder()
                        .display(new ItemStack(Items.BOOK))
                        .with(DataComponents.CUSTOM_NAME, Component.literal("§eGlobal Bans"))
                        .onClick(e -> new GlobalBansGUI().open(e.getPlayer()))
                        .build()
        );

        return PaginationHelper.createPagesFromPlaceholders(
                builder.build(),
                WynautRankUp.instance.teamValidator.getBannedPokemonButtons(),
                LinkedPage.builder()
                        .title(Util.formattedString("&4Banned Pokémon"))
                        .template(builder.build())
        );
    }
}
