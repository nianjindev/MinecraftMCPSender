package name.minecraftmcp.mixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import name.minecraftmcp.Minecraftmcp;

@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {
    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(ServerWorld world,DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
    
        if (self instanceof PlayerEntity player) {
            System.out.println(player.getName() + " took " + amount + " damage from " + source.getName());
            Minecraftmcp.sendEventToMCP("player_damaged", source.getName());
        }
    }
}