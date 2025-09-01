package com.qichen.gravtech.item.custom;

import com.qichen.gravtech.data.custom.GravityFlowBindingData;
import com.qichen.gravtech.entity.blockentity.GravityAnchorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import static com.qichen.gravtech.data.ModDataComponent.BIND_POS;
import static com.qichen.gravtech.manager.GravityFlowManager.createNewFlow;

public class ForceFieldConfiguratorItem extends Item {
    public ForceFieldConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack item = context.getItemInHand();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(clickedPos);
        if(level.isClientSide())return InteractionResult.FAIL;
        if(!player.isSecondaryUseActive())return InteractionResult.PASS;
        if(!(blockEntity instanceof GravityAnchorEntity clickedEntity)){
            item.set(BIND_POS,null);
            player.sendSystemMessage(Component.literal("清除绑定信息"));
            return InteractionResult.SUCCESS;
        }
        BlockPos bindPos = item.get(BIND_POS);
        if(bindPos==null){
            item.set(BIND_POS,clickedPos);
            player.sendSystemMessage(Component.literal("首个锚点已绑定"));
            return InteractionResult.SUCCESS;
        }
        if(bindPos.equals(clickedPos)){
            player.sendSystemMessage(Component.literal("无法绑定自身"));
            return InteractionResult.FAIL;
        }
        // 确保两处坐标不同,尝试绑定
        GravityFlowBindingData flowBindingData = createNewFlow((GravityAnchorEntity) level.getBlockEntity(bindPos), clickedEntity);
        player.sendSystemMessage(Component.literal(flowBindingData==null?"绑定失败":"绑定成功"));
        // 无论是否成功都清除item数据
        item.set(BIND_POS,null);
        return InteractionResult.PASS;
    }
}
