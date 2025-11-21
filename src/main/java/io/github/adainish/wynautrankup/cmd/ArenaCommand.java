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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.adainish.wynautrankup.WynautRankUp;
import io.github.adainish.wynautrankup.arenas.Arena;
import io.github.adainish.wynautrankup.util.Location;
import io.github.adainish.wynautrankup.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Map;

public class ArenaCommand {
    public static final String PERMISSION_NODE = "wynautrankup.admin";


    public static LiteralArgumentBuilder<CommandSourceStack> getCommand() {
        return Commands.literal("wynautarena")
                .requires(source -> {
                    if (source.isPlayer()) {
                        return PermissionUtil.hasCachedLuckPerms(source, PERMISSION_NODE);
                    } else {
                        return true;
                    }
                })
                .then(Commands.literal("help")
                        .executes(ctx -> {
                            Component helpMsg = Component.literal("§e--- WynautArena Commands ---\n")
                                    .append(Component.literal("§a/wynautarena list §7- List all arenas\n"))
                                    .append(Component.literal("§a/wynautarena create <name> <world> §7- Create a new arena\n"))
                                    .append(Component.literal("§a/wynautarena addpos <arena> §7- Add your current position to an arena\n"))
                                    .append(Component.literal("§a/wynautarena editpos <arena> <index> §7- Edit a position in an arena\n"))
                                    .append(Component.literal("§a/wynautarena info <name> §7- Show info about an arena\n"))
                                    .append(Component.literal("§a/wynautarena tp <name> §7- Teleport to the first position of an arena\n"))
                                    .append(Component.literal("§a/wynautarena arenapos <arena> §7- Show clickable teleport positions\n"))
                                    .append(Component.literal("§a/wynautarena tppos <arena> <index> §7- Teleport to a specific position\n"))
                                    .append(Component.literal("§a/wynautarena reload §7- Reload all arenas\n"));
                            ctx.getSource().sendSuccess(() -> helpMsg, false);
                            return 1;
                        })
                )
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            Map<String, Arena> arenas = WynautRankUp.instance.arenaManager.getArenas();
                            if (arenas.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§cNo arenas found."), false);
                            } else {
                                ctx.getSource().sendSuccess(() -> Component.literal("§eArenas: " + String.join(", ", arenas.keySet())), false);
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("world", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            String world = StringArgumentType.getString(ctx, "world");
                                            if (WynautRankUp.instance.arenaManager.getArena(name) != null) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("§cArena already exists: " + name), false);
                                                return 1;
                                            }
                                            Arena arena = new Arena(name, world, new ArrayList<>());
                                            WynautRankUp.instance.arenaManager.addArena(arena);
                                            ctx.getSource().sendSuccess(() -> Component.literal("§aArena created: " + name + " in world " + world), false);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("addpos")
                        .then(Commands.argument("arena", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) {
                                        ctx.getSource().sendSystemMessage(Component.literal("§cThis command can only be run by a player."));
                                        return 1;
                                    }
                                    String arenaName = StringArgumentType.getString(ctx, "arena");
                                    Arena arena = WynautRankUp.instance.arenaManager.getArena(arenaName);
                                    if (arena == null) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("§cArena not found: " + arenaName), false);
                                        return 1;
                                    }
                                    var player = ctx.getSource().getPlayerOrException();
                                    var loc = new Location(
                                            player.level().dimension().location().toString(),
                                            player.getX(), player.getY(), player.getZ(),
                                            player.getXRot(), player.getYHeadRot()
                                    );
                                    arena.addPlayerPosition(loc);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aAdded position to arena: " + arenaName), false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("info")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Arena arena = WynautRankUp.instance.arenaManager.getArena(name);
                                    if (arena == null) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("§cArena not found: " + name), false);
                                    } else {
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§eArena: §a" + arena.getName() +
                                                        "\n§eWorld: §a" + arena.getWorld() +
                                                        "\n§ePlayer Positions: §a" + arena.getPlayerPositions().size() +
                                                        "\n§eIn Use: §a" + arena.isInUse()
                                        ), false);
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("editpos")
                        .then(Commands.argument("arena", StringArgumentType.word())
                                .then(Commands.argument("index", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            if (!ctx.getSource().isPlayer()) {
                                                ctx.getSource().sendSystemMessage(Component.literal("§cThis command can only be run by a player."));
                                                return 1;
                                            }
                                            String arenaName = StringArgumentType.getString(ctx, "arena");
                                            int index = IntegerArgumentType.getInteger(ctx, "index");
                                            Arena arena = WynautRankUp.instance.arenaManager.getArena(arenaName);
                                            if (arena == null) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("§cArena not found: " + arenaName), false);
                                                return 1;
                                            }
                                            if (index < 0 || index >= arena.getPlayerPositions().size()) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("§cInvalid position index."), false);
                                                return 1;
                                            }
                                            var player = ctx.getSource().getPlayerOrException();
                                            var loc = new Location(
                                                    player.level().dimension().location().toString(),
                                                    player.getX(), player.getY(), player.getZ(),
                                                    player.getXRot(), player.getYHeadRot()
                                            );
                                            arena.getPlayerPositions().set(index, loc);
                                            ctx.getSource().sendSuccess(() -> Component.literal("§aEdited position " + index + " in arena: " + arenaName), false);
                                            return 1;
                                        })
                                )
                        )
                )

                //add a teleport command to tp to an arena
                .then(Commands.literal("tp")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) {
                                        ctx.getSource().sendSystemMessage(Component.literal("§cThis command can only be run by a player."));
                                        return 1;
                                    }
                                            String name = StringArgumentType.getString(ctx, "name");
                                            Arena arena = WynautRankUp.instance.arenaManager.getArena(name);
                                            if (arena == null) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("§cArena not found: " + name), false);
                                            } else {
                                                if (arena.getPlayerPositions().isEmpty()) {
                                                    ctx.getSource().sendSuccess(() -> Component.literal("§cArena has no player positions set: " + name), false);
                                                } else {
                                                    //teleport player to first position
                                                    arena.getPlayerPositions().getFirst().teleport(ctx.getSource().getPlayerOrException());
                                                    ctx.getSource().sendSuccess(() -> Component.literal("§aTeleported to arena: " + name), false);
                                                }
                                            }
                                    return 1;
                                }
                                ))
                )

                .then(Commands.literal("arenapos")
                        .then(Commands.argument("arena", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) {
                                        ctx.getSource().sendSystemMessage(Component.literal("§cThis command can only be run by a player."));
                                        return 1;
                                    }
                                    String arenaName = StringArgumentType.getString(ctx, "arena");
                                    Arena arena = WynautRankUp.instance.arenaManager.getArena(arenaName);
                                    if (arena == null) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("§cArena not found: " + arenaName), false);
                                        return 1;
                                    }
                                    if (arena.getPlayerPositions().isEmpty()) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("§cNo positions set for this arena."), false);
                                        return 1;
                                    }
                                    Component message = Component.literal("§ePositions for arena §a" + arenaName + "§e:\n");
                                    for (int i = 0; i < arena.getPlayerPositions().size(); i++) {
                                        int posIndex = i;
                                        int finalI = i;
                                        Component posComponent = Component.literal("§b[TP Pos " + (i + 1) + "] ")
                                                .append(Component.literal("§7(" +
                                                        arena.getPlayerPositions().get(i).getWorld() + " | " +
                                                        String.format("%.1f, %.1f, %.1f",
                                                                arena.getPlayerPositions().get(i).getX(),
                                                                arena.getPlayerPositions().get(i).getY(),
                                                                arena.getPlayerPositions().get(i).getZ()
                                                        ) +
                                                        ")"
                                                ))
                                                .withStyle(style -> style
                                                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                                                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                                                "/wynautarena tppos " + arenaName + " " + posIndex
                                                        ))
                                                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                                                net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                                                Component.literal("§aTeleport to position " + (finalI + 1))
                                                        ))
                                                );
                                        message = message.copy().append(posComponent).append(Component.literal(" "));
                                    }
                                    Component finalMessage = message;
                                    ctx.getSource().sendSuccess(() -> finalMessage, false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("tppos")
                        .then(Commands.argument("arena", StringArgumentType.word())
                                .then(Commands.argument("index", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            if (!ctx.getSource().isPlayer()) {
                                                ctx.getSource().sendSystemMessage(Component.literal("§cThis command can only be run by a player."));
                                                return 1;
                                            }
                                            String arenaName = StringArgumentType.getString(ctx, "arena");
                                            int index = IntegerArgumentType.getInteger(ctx, "index");
                                            Arena arena = WynautRankUp.instance.arenaManager.getArena(arenaName);
                                            if (arena == null) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("§cArena not found: " + arenaName), false);
                                                return 1;
                                            }
                                            if (index < 0 || index >= arena.getPlayerPositions().size()) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("§cInvalid position index."), false);
                                                return 1;
                                            }
                                            arena.getPlayerPositions().get(index).teleport(ctx.getSource().getPlayerOrException());
                                            ctx.getSource().sendSuccess(() -> Component.literal("§aTeleported to position " + (index + 1) + " in arena: " + arenaName), false);
                                            return 1;
                                        })
                                )
                        )
                )

                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            WynautRankUp.instance.arenaManager.reload();
                            ctx.getSource().sendSuccess(() -> Component.literal("§aArenas reloaded!"), false);
                            return 1;
                        })
                );
    }
}
