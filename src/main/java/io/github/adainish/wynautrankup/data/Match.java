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
package io.github.adainish.wynautrankup.data;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import io.github.adainish.wynautrankup.WynautRankUp;
import io.github.adainish.wynautrankup.util.BattleUtil;
import io.github.adainish.wynautrankup.arenas.Arena;
import io.github.adainish.wynautrankup.util.Location;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class Match
{
    private UUID battleId;
    private final Player player1;
    private final Player player2;
    private transient Arena arena;

    public Match(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Arena getArena() {
        return arena;
    }

    public void setArena(Arena arena) {
        this.arena = arena;
    }

    public UUID startMatch() {
        if (arena != null) {
            arena.setInUse(true);
        }
        UUID battleUUID = BattleUtil.startShowdownBattle(player1, player2);
        setBattleId(battleUUID);
        return battleUUID;
    }

    public void setBattleId(UUID battleId) {
        this.battleId = battleId;
    }

    public UUID getBattleId() {
        return battleId;
    }

    public void endMatch()
    {
        if (battleId != null) {
            PokemonBattle battle = BattleRegistry.getBattle(battleId);
            if (battle != null) {
                battle.end();
            }
        }
        if (arena != null) {
            WynautRankUp.instance.arenaManager.getArena(arena.getName()).setInUse(false);
        }

        Location tpbackLoc = WynautRankUp.instance.generalConfig.tpBackLocation;

        if (tpbackLoc != null) {
            // Ensure teleports happen on the server thread to avoid delayed behavior
            getPlayer1().getOptionalServerPlayer().ifPresent(sp -> {
                MinecraftServer server = sp.getServer();
                if (server != null) {
                    server.execute(() -> tpbackLoc.teleport(sp));
                } else {
                    tpbackLoc.teleport(sp);
                }
            });
            getPlayer2().getOptionalServerPlayer().ifPresent(sp -> {
                MinecraftServer server = sp.getServer();
                if (server != null) {
                    server.execute(() -> tpbackLoc.teleport(sp));
                } else {
                    tpbackLoc.teleport(sp);
                }
            });
        }
    }
}
