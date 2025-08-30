package com.qichen.gravtech.item;

import com.qichen.gravtech.GravTech;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GravTech.MODID);
    public static final Supplier<CreativeModeTab> GravTech_TAB = CREATIVE_MODE_TAB.register("gravtech_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + GravTech.MODID + ".item"))
            .icon(() -> new ItemStack(ModItemRegister.GRAVITY_ANCHOR_ITEM.get()))
            .displayItems((params, output) -> {
                output.accept(ModItemRegister.GRAVITY_ANCHOR_ITEM.get());
            })
            .build()
    );
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
