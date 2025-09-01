package com.qichen.gravtech.item;

import com.qichen.gravtech.GravTech;
import com.qichen.gravtech.block.ModBlockRegister;
import com.qichen.gravtech.item.custom.ForceFieldConfiguratorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItemRegister {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(GravTech.MODID);
    public static final DeferredItem<Item> GRAVITY_ANCHOR_ITEM = ITEMS.register(
            "gravity_anchor_item",
            () -> new BlockItem(ModBlockRegister.GRAVITY_ANCHOR_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredItem<Item> FORCE_FIELD_CONFIGURATOR_ITEM = ITEMS.register(
            "force_field_configurator",
            () -> new ForceFieldConfiguratorItem(new Item.Properties())
    );
    public static void register(IEventBus eventBus){
        ModItemRegister.ITEMS.register(eventBus);
    }
}
