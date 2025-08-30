package com.qichen.gravtech.block;

import com.qichen.gravtech.GravTech;
import com.qichen.gravtech.block.custom.gravityAnchorBlock;
import com.qichen.gravtech.entity.blockentity.gravityAnchorEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockRegister {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(GravTech.MODID);
    public static final DeferredBlock<gravityAnchorBlock> GRAVITY_ANCHOR_BLOCK =
            BLOCKS.register("gravity_anchor_block", () -> new gravityAnchorBlock(BlockBehaviour.Properties.of()
                    .destroyTime(3.0f)
                    .explosionResistance(10.0f)
                    .sound(SoundType.ANCIENT_DEBRIS)
                    .lightLevel(state -> 10)));

    public static void register(IEventBus eventBus){
        ModBlockRegister.BLOCKS.register(eventBus);
        gravityAnchorEntity.BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
