package com.qichen.gravtech.data;

import com.mojang.serialization.Codec;
import com.qichen.gravtech.GravTech;
import com.qichen.gravtech.item.ModItemRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponent {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, GravTech.MODID);

    // 配置器储存绑定信息
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> BIND_POS =
            DATA_COMPONENTS.register("bind_pos", () ->
                    DataComponentType.<BlockPos>builder()
                            .persistent(BlockPos.CODEC)
                            .networkSynchronized(BlockPos.STREAM_CODEC)
                            .build()
            );

    public static void register(IEventBus eventBus){
        DATA_COMPONENTS.register(eventBus);
    }
}
