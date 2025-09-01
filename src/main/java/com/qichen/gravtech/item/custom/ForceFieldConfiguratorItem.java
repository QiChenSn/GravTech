package com.qichen.gravtech.item.custom;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ForceFieldConfiguratorItem extends Item {
    public ForceFieldConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if(level.isClientSide())return InteractionResult.FAIL;
        return InteractionResult.PASS;
    }
}
