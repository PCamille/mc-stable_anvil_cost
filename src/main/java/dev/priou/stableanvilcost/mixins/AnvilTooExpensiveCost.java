package dev.priou.stableanvilcost.mixins;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@OnlyIn(Dist.CLIENT)
@Mixin(AnvilScreen.class)
public class AnvilTooExpensiveCost {
    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 40, ordinal = 0))
    public int maxCostAnvilCost(int base) {
        return 256;
    }
}
