package io.github.adainish.wynautrankup.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import io.github.adainish.wynautrankup.WynautRankUp;
import io.github.adainish.wynautrankup.util.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GlobalBansGUI {

    public enum Category {
        MOVES, ABILITIES, ITEMS
    }

    public void open(ServerPlayer player) {
        UIManager.openUIForcefully(player, getPage(Category.MOVES));
    }

    public LinkedPage getPage(Category category) {
        ChestTemplate.Builder builder = Util.returnBasicTemplateBuilder();

        // Tab: zurück zu Pokémon bans
        builder.set(0, 0,
                GooeyButton.builder()
                        .display(new ItemStack(Items.TOTEM_OF_UNDYING))
                        .with(DataComponents.CUSTOM_NAME, Component.literal("§ePokémon Bans"))
                        .onClick(e -> new BannedPokemonGUI().open(e.getPlayer()))
                        .build());

        // Sub-tabs: Moves / Abilities / Items
        Item razorClaw = BuiltInRegistries.ITEM.get(ResourceLocation.parse("cobblemon:razor_claw"));
        builder.set(0, 2, tabButton("Moves", razorClaw, Category.MOVES, category));
        builder.set(0, 4, tabButton("Abilities", Items.NETHER_STAR, Category.ABILITIES, category));
        Item leftovers = BuiltInRegistries.ITEM.get(ResourceLocation.parse("cobblemon:leftovers"));
        builder.set(0, 6, tabButton("Items", leftovers, Category.ITEMS, category));

        var buttons = switch (category) {
            case MOVES -> WynautRankUp.instance.teamValidator.getGlobalBannedMoveButtons();
            case ABILITIES -> WynautRankUp.instance.teamValidator.getGlobalBannedAbilityButtons();
            case ITEMS -> WynautRankUp.instance.teamValidator.getGlobalBannedItemButtons();
        };

        String title = switch (category) {
            case MOVES -> "&4Global Bans &7- &eMoves";
            case ABILITIES -> "&4Global Bans &7- &eAbilities";
            case ITEMS -> "&4Global Bans &7- &eHeld Items";
        };

        return PaginationHelper.createPagesFromPlaceholders(
                builder.build(),
                buttons,
                LinkedPage.builder()
                        .title(Util.formattedString(title))
                        .template(builder.build()));
    }

    private GooeyButton tabButton(String name, Item icon, Category target, Category current) {
        String prefix = (target == current) ? "§a" : "§e";
        ItemStack stack = new ItemStack(icon);
        stack.remove(DataComponents.LORE);
        return GooeyButton.builder()
                .display(stack)
                .with(DataComponents.CUSTOM_NAME, Component.literal(prefix + name))
                .onClick(e -> UIManager.openUIForcefully(e.getPlayer(), getPage(target)))
                .build();
    }
}
