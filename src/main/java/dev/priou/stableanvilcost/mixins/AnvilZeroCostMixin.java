package dev.priou.stableanvilcost.mixins;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public class AnvilZeroCostMixin {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void mayPickup$allowZeroCostPickup(Player p_39023_, boolean p_39024_, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        callbackInfoReturnable.cancel();

        AnvilMenu self = (AnvilMenu) (Object) this;

        callbackInfoReturnable.setReturnValue(p_39023_.getAbilities().instabuild || p_39023_.experienceLevel >= self.getCost());
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    public void createResult$zeroRenameCost(CallbackInfo callbackInfo) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        NonNullList<ItemStack> stacks = self.getItems();
        ItemStack firstStack = stacks.get(0);
        ItemStack secondStack = stacks.get(1);
        ItemStack finalStack = firstStack.copy();

        if (!secondStack.isEmpty()) {
            return;
        }

        String name = self.itemName;
        if (name.isBlank() || name.isEmpty()) {
            if (firstStack.hasCustomHoverName()) {
                finalStack.resetHoverName();
                self.cost.set(0);
            } else {
                return;
            }
        } else {
            if (!name.equals(firstStack.getHoverName().getString())) {
                finalStack.setHoverName(new TextComponent(name).withStyle(finalStack.getHoverName().getStyle()));
                self.cost.set(0);
            } else {
                return;
            }
        }

        callbackInfo.cancel();
        self.resultSlots.setItem(0, finalStack);
    }
}
