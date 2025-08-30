package com.qichen.gravtech.datagen;

import com.qichen.gravtech.block.ModBlockRegister;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {


    public ModBlockLootTableProvider(HolderLookup.Provider lookupProvider) {
        // 第一个参数：需要生成战利品表的方块集合（通常传空集，后续通过getKnownBlocks动态获取）
        // 第二个参数：特征标志集（一般使用默认值FeatureFlags.DEFAULT_FLAGS）
        // 第三个参数：注册表查询器（用于获取方块等注册信息）
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
    }

    @Override
    protected void generate() {
        this.dropSelf(ModBlockRegister.GRAVITY_ANCHOR_BLOCK.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // 从方块注册表中获取所有注册的方块并返回
        return ModBlockRegister.BLOCKS.getEntries()
                .stream()
                .map(entry -> (Block) entry.value()) // 转换为Block类型
                .toList();
    }
}
