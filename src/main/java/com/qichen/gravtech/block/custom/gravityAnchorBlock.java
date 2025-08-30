package com.qichen.gravtech.block.custom;

import com.qichen.gravtech.entity.blockentity.gravityAnchorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.qichen.gravtech.GravTech.PublicLogger;

public class gravityAnchorBlock extends Block implements EntityBlock {
    public gravityAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new gravityAnchorEntity(blockPos,blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType == gravityAnchorEntity.GRAVITY_ANCHOR_ENTITY.get()) {
            // 返回方块实体类的静态tick方法，参数需匹配BlockEntityTicker接口
            return (BlockEntityTicker<T>) (BlockEntityTicker<gravityAnchorEntity>) gravityAnchorEntity::tick;
        }
        return null;
    }
    // 处理玩家右键交互：切换重力模式
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(level.getBlockEntity(pos) instanceof gravityAnchorEntity gravityAnchorEntity) {
            if(!level.isClientSide()&&player.isSecondaryUseActive()) {
                gravityAnchorEntity.switchMode();
                player.sendSystemMessage(Component.translatable(gravityAnchorEntity.getMode()== com.qichen.gravtech.entity.blockentity.gravityAnchorEntity.GravityMode.LOW_GRAVITY?
                        "message.gravtech.gravity_anchor_entity.switch_to_low"
                        :"message.gravtech.gravity_anchor_entity.switch_to_high"));
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    // 处理红石信号
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (!world.isClientSide) { // 只在服务器端处理
            BlockEntity entity = world.getBlockEntity(pos);
            int signal = world.getBestNeighborSignal(pos);
            boolean newState=signal>0;
            if(entity instanceof gravityAnchorEntity gravityAnchorEntity&&gravityAnchorEntity.isActivated()!=newState){
                gravityAnchorEntity.setActivated(newState);
                PublicLogger.info("红石信号"+String.valueOf(signal)+"当前状态"+gravityAnchorEntity.isActivated());
            }
        }
    }

    // 当方块被破坏时清理所有跟踪的实体
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof gravityAnchorEntity gravityAnchorEntity) {
                gravityAnchorEntity.cleanupAllTrackedEntities();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
