package net.mehvahdjukaar.supplementaries.common.entities.dispenser_minecart;

import net.mehvahdjukaar.supplementaries.common.network.ClientBoundParticlePacket;
import net.mehvahdjukaar.supplementaries.common.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public interface ILevelEventRedirect {

    void supplementaries$setRedirected(boolean redirected, Vec3 entityId);

    static boolean tryRedirect(ILevelEventRedirect serverLevel, Player pPlayer, Vec3 vec3, int pType, BlockPos pPos, int pData) {
        if (pType == 2000) {
            NetworkHandler.CHANNEL.sendToAllClientPlayersInRange((ServerLevel) serverLevel, pPos, 64,
                    new ClientBoundParticlePacket(vec3, ClientBoundParticlePacket.EventType.DISPENSER_MINECART));
            return true;
        }
        return false;
    }
}
