package com.qichen.gravtech.entity.blockentity;

import com.qichen.gravtech.GravTech;
import com.qichen.gravtech.block.ModBlockRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.qichen.gravtech.GravTech.PublicLogger;


public class gravityAnchorEntity extends BlockEntity {

    // 重力模式枚举
    public enum GravityMode {
        LOW_GRAVITY, HIGH_GRAVITY
    }

    //数据存储
    //默认低重力模式
    private GravityMode mode;
    private boolean activated;

    public gravityAnchorEntity(BlockPos pos, BlockState blockState) {
        super(GRAVITY_ANCHOR_ENTITY.get(), pos, blockState);
        this.mode=GravityMode.LOW_GRAVITY;
        this.activated=true;
    }

    // 在模组初始化类中定义注册器
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GravTech.MODID);

    public static final Supplier<BlockEntityType<gravityAnchorEntity>> GRAVITY_ANCHOR_ENTITY = BLOCK_ENTITY_TYPES.register(
            "gravity_anchor_entity",
            () -> BlockEntityType.Builder.of(
                    gravityAnchorEntity::new, // 实体构造函数
                    ModBlockRegister.GRAVITY_ANCHOR_BLOCK.get() // 关联的方块
            ).build(null)
    );

    public static int ticker=0;
    public static void tick(Level level, BlockPos pos, BlockState state, gravityAnchorEntity be) {
        if(!be.isActivated())return;
        ticker++;
        if(ticker%20==0){
            PublicLogger.info("111");
            ticker=0;
        }
    }

    // 获取当前模式
    public GravityMode getMode() {
        return this.mode;
    }

    // 设置模式
    public void setMode(GravityMode newMode) {
        this.mode = newMode;
        setChanged(); // 标记Entity变化，触发保存
        // 通知客户端数据已更新
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // 切换模式
    public void switchMode() {
        this.mode = (this.mode == GravityMode.LOW_GRAVITY) ? GravityMode.HIGH_GRAVITY : GravityMode.LOW_GRAVITY;
        setChanged(); // 标记变化
        // 通知客户端数据已更新
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    public boolean isActivated() {
        return this.activated;
    }

    public void setActivated(boolean activated) {
        if(this.activated==activated)return;
        this.activated = activated;
        setChanged();
        // 通知客户端数据已更新
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // 保存NBT数据（模式状态）
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("mode", this.mode.name()); // 保存模式为字符串
        tag.putBoolean("activated",this.activated);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if(tag.contains("mode")){
            this.mode=GravityMode.valueOf(tag.getString("mode")); // 加载模式
        }
        if(tag.contains("activated")){
            this.activated=tag.getBoolean("activated");
        }
    }

    // 网络同步方法 - 获取更新包
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // 网络同步方法 - 获取更新标签
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    // 网络同步方法 - 处理客户端接收到的数据包
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        if (pkt.getTag() != null) {
            loadAdditional(pkt.getTag(), registries);
        }
    }
}
