package dev.priou.stableanvilcost;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber
public class AnvilEvent {
    public static int sumFrom1(int max) {
        int sum = 0;
        for (int i = 0; i <= max; ++i) {
            sum += i;
        }
        return sum;
    }

    public static int getEnchantCost(Map<Enchantment, Integer> finalEnchantments, Map<Enchantment, Integer> firstEnchantments, Map<Enchantment, Integer> secondEnchantments, boolean isSameObject) {
        int cost = 0;
        int nbCurses = 0;

        for (Enchantment enchantment : finalEnchantments.keySet()) {
            if (enchantment.isCurse()) {
                nbCurses++;
            } else {
                int finalLevel = finalEnchantments.get(enchantment);
                int firstLevel = firstEnchantments.getOrDefault(enchantment, 0);
                int secondLevel = secondEnchantments.getOrDefault(enchantment, 0);

                double rarity = switch (enchantment.getRarity()) {
                    case COMMON -> 1;
                    case UNCOMMON -> 2;
                    case RARE -> 4;
                    case VERY_RARE -> 8;
                };

                if (firstLevel == secondLevel && finalLevel != firstLevel) {
                    //fusion same level -> cost depends on level and rarity
                    cost += sumFrom1(firstLevel) * rarity;
                } else if (isSameObject) {
                    //fusion same item -> cost depends on level
                    cost += finalLevel;
                } else {
                    //fusion with a book
                    if (secondLevel == 0) {
                        //enchant already on the item -> cost depends on level
                        cost += finalLevel;
                    } else {
                        if (secondLevel > enchantment.getMaxLevel()) {
                            //curse's ancient tomes
                            cost += 30;
                        }
                        //enchant on the book -> cost depends on the level and the rarity
                        cost += sumFrom1(finalLevel) - sumFrom1(firstLevel) * rarity/3.0;
                    }
                }
            }
        }

        cost += sumFrom1(Math.min(finalEnchantments.keySet().size() - nbCurses - 1, 0));
        return Math.max(1, (int) (cost - (cost * 0.1 * nbCurses)));
    }

    @SubscribeEvent
    public void onAnvilEvent(AnvilUpdateEvent e) {
        ItemStack firstStack = e.getLeft();
        ItemStack secondStack = e.getRight();
        ItemStack resultStack = e.getOutput();
        boolean isChanged = false;

        if (resultStack.isEmpty()) {
            ItemStack finalStack = firstStack.copy();
            int cost = 0;

            boolean isRepairWithMaterial = firstStack.isDamaged() && firstStack.getItem().isValidRepairItem(firstStack, secondStack);
            boolean isRepairWithSameItem = firstStack.isDamaged() && firstStack.is(secondStack.getItem());
            if (isRepairWithMaterial) {
                int repairMaterialCost = 0;
                int maxDamage = firstStack.getMaxDamage();
                int currentDamage = firstStack.getDamageValue();
                int repairPerMaterial = (int) (maxDamage * 0.25) + 1;
                int newDamage = currentDamage;

                for (int i = 0; i < secondStack.getCount() && newDamage > 0; ++i) {
                    repairMaterialCost++;
                    newDamage -= repairPerMaterial;
                }
                if (newDamage < 0) {
                    newDamage = 0;
                }
                finalStack.setDamageValue(newDamage);
                e.setMaterialCost(repairMaterialCost);
                isChanged = true;
            } else if (isRepairWithSameItem) {
                int maxDamage = firstStack.getMaxDamage();
                int currentDamage = firstStack.getDamageValue();
                int secondDamage = secondStack.getDamageValue();
                int newDamage = maxDamage - (int) (((maxDamage - currentDamage) + (maxDamage - secondDamage)) * 1.2);

                if (newDamage < 0) {
                    newDamage = 0;
                }

                finalStack.setDamageValue(newDamage);
                isChanged = true;
            }

            boolean isEnchantWithBook = secondStack.getItem() == Items.ENCHANTED_BOOK && !EnchantedBookItem.getEnchantments(secondStack).isEmpty();
            boolean isEnchantWithSameItem = firstStack.is(secondStack.getItem()) && secondStack.isEnchanted();
            if (isEnchantWithBook || isEnchantWithSameItem) {
                boolean alwaysLegal = e.getPlayer().getAbilities().instabuild || firstStack.is(Items.ENCHANTED_BOOK);

                Map<Enchantment, Integer> firstEnchants = EnchantmentHelper.getEnchantments(firstStack);
                Map<Enchantment, Integer> secondEnchants = EnchantmentHelper.getEnchantments(secondStack);
                Map<Enchantment, Integer> finalEnchants = EnchantmentHelper.getEnchantments(finalStack);
                int enchantability = firstStack.getItemEnchantability(); //TODO: use it?

                boolean isLegal = true;
                for (Enchantment enchant : secondEnchants.keySet()) {
                    if (isLegal) {
                        int firstLevel = firstEnchants.getOrDefault(enchant, 0);
                        int secondLevel = secondEnchants.get(enchant);
                        int finalLevel = firstLevel == secondLevel ? Math.min(firstLevel + 1, enchant.getMaxLevel()) : Math.max(firstLevel, secondLevel);
                        isLegal = alwaysLegal || enchant.canEnchant(firstStack);

                        for (Enchantment enchantment : firstEnchants.keySet()) {
                            if (enchant != enchantment && !enchant.isCompatibleWith(enchantment)) {
                                isLegal = false;
                            }
                        }
                        finalEnchants.put(enchant, finalLevel);
                    }
                }

                if (!isLegal) {
                    return;
                }
                EnchantmentHelper.setEnchantments(finalEnchants, finalStack);
                cost += getEnchantCost(finalEnchants, firstEnchants, secondEnchants, isEnchantWithSameItem);
                isChanged = true;
            }

            if (e.getName() == null || e.getName().isBlank() || e.getName().isEmpty()) {
                if (firstStack.hasCustomHoverName()) {
                    finalStack.resetHoverName();
                    isChanged = true;
                }
            } else if (!e.getName().equals(firstStack.getHoverName().getString())) {
                finalStack.setHoverName(new TextComponent(e.getName()));
                isChanged = true;
            }

            if (isChanged) {
                e.setCost(cost);
                e.setOutput(finalStack);
            }
        }
    }
}
