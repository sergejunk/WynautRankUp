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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.adainish.wynautrankup.WynautRankUp;
import io.github.adainish.wynautrankup.gui.ShopGUI;
import io.github.adainish.wynautrankup.shop.ShopItem;
import io.github.adainish.wynautrankup.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ShopCommand
{
    public static final String PERMISSION_NODE = "wynautrankup.user.season";
    public static final String ADMIN_PERMISSION_NODE = "wynautrankup.admin";

    public static LiteralArgumentBuilder<CommandSourceStack> getShopCommand() {
        return Commands.literal("rankedshop")
                .requires(source -> {
                    if (source.isPlayer()) {
                        return PermissionUtil.hasCachedLuckPerms(source, PERMISSION_NODE);
                    } else {
                        return true;
                    }
                })
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ShopGUI shopGUI = new ShopGUI();
                    shopGUI.open(player);
                    return 1;
                })
                .then(Commands.literal("add")
                        .requires(source -> {
                            if (source.isPlayer()) {
                                return PermissionUtil.hasCachedLuckPerms(source, ADMIN_PERMISSION_NODE);
                            } else {
                                return true;
                            }
                        })
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("price", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String id = StringArgumentType.getString(ctx, "id");
                                            int price = IntegerArgumentType.getInteger(ctx, "price");
                                            ItemStack item = player.getMainHandItem();
                                            ShopItem shopItem = new ShopItem(id, item.getDisplayName().getString(), price, item.copy());
                                            WynautRankUp.instance.shopManager.addItem(shopItem);
                                            WynautRankUp.instance.shopManager.saveToConfig();
                                            ctx.getSource().sendSuccess(() -> Component.literal("Added shop item: " + id), false);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .requires(source -> {
                            if (source.isPlayer()) {
                                return PermissionUtil.hasCachedLuckPerms(source, ADMIN_PERMISSION_NODE);
                            } else {
                                return true;
                            }
                        })
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    WynautRankUp.instance.shopManager.removeItem(id);
                                    WynautRankUp.instance.shopManager.saveToConfig();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Removed shop item: " + id), false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("balance")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int balance = WynautRankUp.instance.playerDataManager.getBalance(String.valueOf(player.getUUID()));
                            ctx.getSource().sendSuccess(() -> Component.literal("Your shop balance: " + balance), false);
                            return 1;
                        })
                )

                .then(Commands.literal("setbalance")
                        .requires(source -> {
                            if (source.isPlayer()) {
                                return PermissionUtil.hasCachedLuckPerms(source, ADMIN_PERMISSION_NODE);
                            } else {
                                return true;
                            }
                        })
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            String playerName = StringArgumentType.getString(ctx, "player");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
                                            if (target == null) {
                                                ctx.getSource().sendFailure(Component.literal("Player not found."));
                                                return 0;
                                            }
                                            WynautRankUp.instance.playerDataManager.setBalance(String.valueOf(target.getUUID()), amount);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Set " + playerName + "'s balance to " + amount), false);
                                            return 1;
                                        })
                                )
                        )
                )

                .then(Commands.literal("addbalance")
                        .requires(source -> {
                            if (source.isPlayer()) {
                                return PermissionUtil.hasCachedLuckPerms(source, ADMIN_PERMISSION_NODE);
                            } else {
                                return true;
                            }
                        })
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            String playerName = StringArgumentType.getString(ctx, "player");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
                                            if (target == null) {
                                                ctx.getSource().sendFailure(Component.literal("Player not found."));
                                                return 0;
                                            }
                                            WynautRankUp.instance.playerDataManager.adjustBalance(String.valueOf(target.getUUID()), amount);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Added " + amount + " to " + playerName + "'s balance."), false);
                                            return 1;
                                        })
                                )
                        )
                )

                .then(Commands.literal("help")
                        .requires(source -> {
                            if (source.isPlayer()) {
                                return PermissionUtil.hasCachedLuckPerms(source, ADMIN_PERMISSION_NODE);
                            } else {
                                return true;
                            }
                        })
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "/shop - Open shop\n" +
                                            "/shop balance - View your balance\n" +
                                            "/shop add <id> <price> - Add item\n" +
                                            "/shop remove <id> - Remove item\n" +
                                            "/shop edit <id> <field> <value> - Edit item\n" +
                                            "/shop setbalance <player> <amount> - Set player balance\n" +
                                            "/shop addbalance <player> <amount> - Add to player balance"
                            ), false);
                            return 1;
                        })
                )
                .then(Commands.literal("edit")
                        .requires(source -> {
                            if (source.isPlayer()) {
                                return PermissionUtil.hasCachedLuckPerms(source, ADMIN_PERMISSION_NODE);
                            } else {
                                return true;
                            }
                        })
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("field", StringArgumentType.word())
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String id = StringArgumentType.getString(ctx, "id");
                                                    String field = StringArgumentType.getString(ctx, "field");
                                                    String value = StringArgumentType.getString(ctx, "value");
                                                    ShopItem item = WynautRankUp.instance.shopManager.getItems().stream()
                                                            .filter(i -> i.getId().equals(id)).findFirst().orElse(null);
                                                    if (item == null) {
                                                        ctx.getSource().sendFailure(Component.literal("Item not found."));
                                                        return 0;
                                                    }
                                                    switch (field.toLowerCase()) {
                                                        case "price":
                                                            item.setPrice(Integer.parseInt(value));
                                                            break;
                                                        case "displayname":
                                                            item.setDisplayName(value);
                                                            break;
                                                        default:
                                                            ctx.getSource().sendFailure(Component.literal("Unknown field."));
                                                            return 0;
                                                    }
                                                    WynautRankUp.instance.shopManager.saveToConfig();
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Edited shop item: " + id), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                );
    }
}
