package gregtech.api.util;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IElectricItem;
import gregtech.api.capability.impl.ElectricItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.CraftingHelper.ShapedPrimer;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

public class ShapedOreEnergyTransferRecipe extends ShapedOreRecipe {

    private final Predicate<ItemStack> chargePredicate;
    private final boolean transferMaxCharge;

    public ShapedOreEnergyTransferRecipe(ResourceLocation group, @Nonnull ItemStack result, Predicate<ItemStack> chargePredicate, boolean transferMaxCharge, Object... recipe) {
        this(group, result, chargePredicate, transferMaxCharge, CraftingHelper.parseShaped(recipe));
    }

    public ShapedOreEnergyTransferRecipe(ResourceLocation group, @Nonnull ItemStack result, Predicate<ItemStack> chargePredicate, boolean overrideCharge, boolean transferMaxCharge, Object... recipe) {
        this(group, result, chargePredicate, overrideCharge, transferMaxCharge, CraftingHelper.parseShaped(recipe));
    }

    public ShapedOreEnergyTransferRecipe(ResourceLocation group, @Nonnull ItemStack result, Predicate<ItemStack> chargePredicate, boolean transferMaxCharge, ShapedPrimer primer) {
        this(group, result, chargePredicate, true, transferMaxCharge, primer);
    }

    public ShapedOreEnergyTransferRecipe(ResourceLocation group, @Nonnull ItemStack result, Predicate<ItemStack> chargePredicate, boolean overrideCharge, boolean transferMaxCharge, ShapedPrimer primer) {
        super(group, result, primer);
        this.chargePredicate = chargePredicate;
        this.transferMaxCharge = transferMaxCharge;
        if(overrideCharge) {
            fixOutputItemMaxCharge();
        }
    }

    //transfer initial max charge for correct display in JEI
    private void fixOutputItemMaxCharge() {
        long totalMaxCharge = getIngredients().stream()
            .mapToLong(it -> Arrays.stream(it.getMatchingStacks())
                .map(stack -> stack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null))
                .filter(Objects::nonNull)
                .mapToLong(IElectricItem::getMaxCharge)
                .max().orElse(0L)).sum();
        IElectricItem electricItem = output.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
        if (totalMaxCharge > 0L && electricItem instanceof ElectricItem) {
            ((ElectricItem) electricItem).setMaxChargeOverride(totalMaxCharge);
        }
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inventoryCrafting) {
        ItemStack resultStack = super.getCraftingResult(inventoryCrafting);
        chargeStackFromComponents(resultStack, inventoryCrafting, chargePredicate, transferMaxCharge);
        return resultStack;
    }

    public static void chargeStackFromComponents(ItemStack toolStack, IInventory ingredients, Predicate<ItemStack> chargePredicate, boolean transferMaxCharge) {
        IElectricItem electricItem = toolStack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
        long totalMaxCharge = 0L;
        if (electricItem != null && electricItem.getMaxCharge() > 0L) {
            for (int slotIndex = 0; slotIndex < ingredients.getSizeInventory(); slotIndex++) {
                ItemStack stackInSlot = ingredients.getStackInSlot(slotIndex);
                if(!chargePredicate.test(stackInSlot)) {
                    continue;
                }
                IElectricItem batteryItem = stackInSlot.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
                if (batteryItem == null) {
                    continue;
                }
                totalMaxCharge += batteryItem.getMaxCharge();
                long discharged = batteryItem.discharge(Long.MAX_VALUE, Integer.MAX_VALUE, true, true, false);
                electricItem.charge(discharged, Integer.MAX_VALUE, true, false);
                if (discharged > 0L) {
                    ingredients.setInventorySlotContents(slotIndex, stackInSlot);
                }
            }
        }
        if(electricItem instanceof ElectricItem && transferMaxCharge) {
            ((ElectricItem) electricItem).setMaxChargeOverride(totalMaxCharge);
        }
    }
}
