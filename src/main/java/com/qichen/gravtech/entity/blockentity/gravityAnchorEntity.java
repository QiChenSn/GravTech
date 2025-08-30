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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.qichen.gravtech.GravTech.PublicLogger;


public class gravityAnchorEntity extends BlockEntity {

    // 重力模式枚举
    public enum GravityMode {
        LOW_GRAVITY, HIGH_GRAVITY
    }

    // 重力效果常量
    private static final double LOW_GRAVITY_MULTIPLIER = 0.3; // 低重力倍数
    private static final double HIGH_GRAVITY_MULTIPLIER = 2.0; // 高重力倍数
    private static final int EFFECT_DURATION = 2; // 效果持续时间（tick）
    private static final int SOUND_COOLDOWN = 100; // 音效冷却时间
    private static final int PARTICLE_COOLDOWN = 5; // 粒子效果冷却时间
    
    // 属性修饰符UUID
    private static final java.util.UUID SPEED_MODIFIER_UUID = java.util.UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private static final java.util.UUID JUMP_MODIFIER_UUID = java.util.UUID.fromString("87654321-4321-4321-4321-cba987654321");



    //数据存储
    private GravityMode mode;
    private boolean activated;
    private int range;
    
    // 效果跟踪
    private int soundCooldown = 0;
    private int particleCooldown = 0;
    
    // 实体跟踪 - 用于清理离开范围的实体
    private java.util.Set<java.util.UUID> trackedEntities = new java.util.HashSet<>();

    public gravityAnchorEntity(BlockPos pos, BlockState blockState) {
        super(GRAVITY_ANCHOR_ENTITY.get(), pos, blockState);
        this.mode=GravityMode.LOW_GRAVITY;
        this.activated=true;
        this.range=10;
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

    public static void tick(Level level, BlockPos pos, BlockState state, gravityAnchorEntity be) {
        if(!be.isActivated()) return;
        
        // 更新冷却时间
        if(be.soundCooldown > 0) be.soundCooldown--;
        if(be.particleCooldown > 0) be.particleCooldown--;
        
        int range = be.getRange();
        // 定义作用范围AABB
        AABB area = new AABB(pos.getX() - range/2, pos.getY() - range/2, pos.getZ() - range/2,
                pos.getX() + range/2, pos.getY() + range/2, pos.getZ() + range/2);
        
        // 获取范围内所有实体
        var entities = level.getEntitiesOfClass(Entity.class, area, e -> true);
        boolean hasLivingEntities = false;
        
        // 存储当前范围内的实体UUID
        java.util.Set<java.util.UUID> currentEntityUUIDs = new java.util.HashSet<>();
        
        for(Entity entity : entities) {
            if(entity instanceof LivingEntity livingEntity) {
                hasLivingEntities = true;
                currentEntityUUIDs.add(livingEntity.getUUID());
                be.trackedEntities.add(livingEntity.getUUID());
                applyGravityEffect(livingEntity, be.getMode(), be);
            } else if(entity instanceof ItemEntity itemEntity) {
                applyItemGravityEffect(itemEntity, be.getMode(), be);
            } else if(entity instanceof Projectile projectile) {
                // 处理飞行物实体
                applyProjectileGravityEffect(projectile, be.getMode(), be);
            }
        }
        
        // 清理离开范围的实体的重力效果
        cleanupEntitiesOutsideRange(level, be, currentEntityUUIDs);
        
        // 播放音效和粒子效果
        if(hasLivingEntities && be.soundCooldown <= 0) {
            playGravitySound(level, pos, be.getMode());
            be.soundCooldown = SOUND_COOLDOWN;
        }
        
        if(be.particleCooldown <= 0) {
            spawnGravityParticles(level, pos, be.getMode(), range);
            be.particleCooldown = PARTICLE_COOLDOWN;
        }
    }

    // 应用重力效果
    private static void applyGravityEffect(LivingEntity entity, GravityMode mode, gravityAnchorEntity anchor) {
        switch (mode) {
            case LOW_GRAVITY:
                applyLowGravityEffect(entity, anchor);
                break;
            case HIGH_GRAVITY:
                applyHighGravityEffect(entity, anchor);
                break;
        }
    }

    // 应用低重力效果
    private static void applyLowGravityEffect(LivingEntity entity, gravityAnchorEntity anchor) {
        // 1. 减少重力影响
        Vec3 motion = entity.getDeltaMovement();
        if(motion.y < 0) { // 只在下降时减少重力
            entity.setDeltaMovement(motion.x, motion.y * LOW_GRAVITY_MULTIPLIER, motion.z);
        }
        
        // 2. 添加缓慢下降效果
        if(!entity.onGround() && entity.getDeltaMovement().y < -0.1) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, EFFECT_DURATION, 0, false, false, true));
        }
        
        // 3. 增加跳跃力 - 通过检测Y轴速度变化来判断是否在跳跃
        if(entity instanceof Player player) {
            Vec3 currentMotion = player.getDeltaMovement();
            if(currentMotion.y > 0.1) { // 检测向上的运动
                player.setDeltaMovement(currentMotion.x, currentMotion.y * 1.1, currentMotion.z);
            }
        }
        
        // 4. 添加速度提升效果
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, 1, false, false, true));
        
        // 5. 添加跳跃提升效果
        entity.addEffect(new MobEffectInstance(MobEffects.JUMP, EFFECT_DURATION, 1, false, false, true));
        
        // 6. 添加视觉反馈 - 发光效果
        entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, EFFECT_DURATION, 0, false, false, true));
    }

    // 应用高重力效果
    private static void applyHighGravityEffect(LivingEntity entity, gravityAnchorEntity anchor) {
        // 1. 增加重力影响
        Vec3 motion = entity.getDeltaMovement();
        if(motion.y < 0) { // 只在下降时增加重力
            entity.setDeltaMovement(motion.x, motion.y * HIGH_GRAVITY_MULTIPLIER, motion.z);
        }
        
        // 2. 添加缓慢效果
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION, 1, false, false, true));
        
        // 3. 限制跳跃 - 通过检测Y轴速度变化来判断是否在跳跃
        if(entity instanceof Player player) {
            Vec3 currentMotion = player.getDeltaMovement();
            if(currentMotion.y > 0.1) { // 检测向上的运动
                player.setDeltaMovement(currentMotion.x, currentMotion.y * 0.5, currentMotion.z);
            }
        }
        
        // 4. 添加挖掘疲劳效果
        entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, EFFECT_DURATION, 1, false, false, true));
        
        // 5. 添加视觉反馈 - 发光效果
        entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, EFFECT_DURATION, 0, false, false, true));
        
        // 6. 如果从高处掉落，增加伤害
        if(entity.fallDistance > 3.0f) {
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_DURATION, 0, false, false, true));
        }
        
        // 7. 添加虚弱效果
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 0, false, false, true));
    }

    // 应用物品重力效果
    private static void applyItemGravityEffect(ItemEntity itemEntity, GravityMode mode, gravityAnchorEntity anchor) {
        Vec3 motion = itemEntity.getDeltaMovement();
        Vec3 pos = itemEntity.position();
        Vec3 anchorPos = Vec3.atCenterOf(anchor.getBlockPos());
        
        // 计算距离衰减
        double distance = pos.distanceTo(anchorPos);
        double maxDistance = anchor.getRange() / 2.0;
        double strength = Math.max(0, 1.0 - (distance / maxDistance));
        
        switch (mode) {
            case LOW_GRAVITY:
                // 物品缓慢下降，可能向上漂浮
                if(motion.y < 0) {
                    itemEntity.setDeltaMovement(motion.x, motion.y * LOW_GRAVITY_MULTIPLIER * strength, motion.z);
                }
                // 添加轻微的向上力
                if(strength > 0.5) {
                    itemEntity.setDeltaMovement(motion.x, motion.y + 0.02 * strength, motion.z);
                }
                break;
            case HIGH_GRAVITY:
                // 物品快速下降
                if(motion.y < 0) {
                    itemEntity.setDeltaMovement(motion.x, motion.y * HIGH_GRAVITY_MULTIPLIER * strength, motion.z);
                }
                // 向锚点中心吸引
                Vec3 direction = anchorPos.subtract(pos).normalize();
                itemEntity.setDeltaMovement(
                    motion.x + direction.x * 0.05 * strength,
                    motion.y + direction.y * 0.05 * strength,
                    motion.z + direction.z * 0.05 * strength
                );
                break;
        }
    }

    // 应用飞行物重力效果
    private static void applyProjectileGravityEffect(Projectile projectile, GravityMode mode, gravityAnchorEntity anchor) {
        Vec3 motion = projectile.getDeltaMovement();
        Vec3 pos = projectile.position();
        Vec3 anchorPos = Vec3.atCenterOf(anchor.getBlockPos());
        
        // 计算距离衰减
        double distance = pos.distanceTo(anchorPos);
        double maxDistance = anchor.getRange() / 2.0;
        double strength = Math.max(0, 1.0 - (distance / maxDistance));
        
        // 如果强度太小，不应用效果
        if(strength < 0.1) return;
        
        // 为飞行物添加视觉反馈 - 发光效果
        if(projectile.level() instanceof ServerLevel serverLevel) {
            // 生成飞行物轨迹粒子效果
            spawnProjectileTrailParticles(serverLevel, pos, mode, strength);
        }
        
        switch (mode) {
            case LOW_GRAVITY:
                applyLowGravityToProjectile(projectile, motion, anchorPos, pos, strength);
                break;
            case HIGH_GRAVITY:
                applyHighGravityToProjectile(projectile, motion, anchorPos, pos, strength);
                break;
        }
    }

    // 对飞行物应用低重力效果
    private static void applyLowGravityToProjectile(Projectile projectile, Vec3 motion, Vec3 anchorPos, Vec3 pos, double strength) {
        Vec3 newMotion = motion;
        
        // 减少重力影响，让飞行物飞得更远更高
        if(motion.y < 0) {
            newMotion = newMotion.multiply(1.0, LOW_GRAVITY_MULTIPLIER, 1.0);
        }
        
        // 根据飞行物类型应用不同效果
        if(projectile instanceof Arrow arrow) {
            // 箭矢：减少重力，增加飞行距离
            newMotion = newMotion.multiply(1.0 + strength * 0.2, 1.0 + strength * 0.3, 1.0 + strength * 0.2);
        } else if(projectile instanceof Snowball snowball) {
            // 雪球：增加投掷距离，减少下降速度
            newMotion = newMotion.multiply(1.0 + strength * 0.3, 1.0 + strength * 0.4, 1.0 + strength * 0.3);
        } else if(projectile instanceof ThrownEgg egg) {
            // 鸡蛋：类似雪球效果
            newMotion = newMotion.multiply(1.0 + strength * 0.25, 1.0 + strength * 0.35, 1.0 + strength * 0.25);
        } else if(projectile instanceof ThrownEnderpearl enderpearl) {
            // 末影珍珠：减少重力影响，增加投掷距离
            newMotion = newMotion.multiply(1.0 + strength * 0.15, 1.0 + strength * 0.25, 1.0 + strength * 0.15);
        } else if(projectile instanceof ThrownPotion potion) {
            // 药水：增加投掷距离
            newMotion = newMotion.multiply(1.0 + strength * 0.2, 1.0 + strength * 0.3, 1.0 + strength * 0.2);
        } else if(projectile instanceof ThrownTrident trident) {
            // 三叉戟：减少重力影响
            newMotion = newMotion.multiply(1.0 + strength * 0.1, 1.0 + strength * 0.2, 1.0 + strength * 0.1);
        }
        
        projectile.setDeltaMovement(newMotion);
    }

    // 对飞行物应用高重力效果
    private static void applyHighGravityToProjectile(Projectile projectile, Vec3 motion, Vec3 anchorPos, Vec3 pos, double strength) {
        Vec3 newMotion = motion;
        
        // 增加重力影响，让飞行物更快下降
        if(motion.y < 0) {
            newMotion = newMotion.multiply(1.0, HIGH_GRAVITY_MULTIPLIER, 1.0);
        }
        
        // 向锚点中心吸引
        Vec3 direction = anchorPos.subtract(pos).normalize();
        double attractionForce = 0.08 * strength;
        
        newMotion = newMotion.add(
            direction.x * attractionForce,
            direction.y * attractionForce,
            direction.z * attractionForce
        );
        
        // 根据飞行物类型应用不同效果
        if(projectile instanceof Arrow arrow) {
            // 箭矢：增加重力，减少飞行距离，向锚点弯曲
            newMotion = newMotion.multiply(1.0 - strength * 0.1, 1.0 - strength * 0.2, 1.0 - strength * 0.1);
        } else if(projectile instanceof Snowball snowball) {
            // 雪球：快速下降，向锚点吸引
            newMotion = newMotion.multiply(1.0 - strength * 0.15, 1.0 - strength * 0.25, 1.0 - strength * 0.15);
        } else if(projectile instanceof ThrownEgg egg) {
            // 鸡蛋：类似雪球效果
            newMotion = newMotion.multiply(1.0 - strength * 0.12, 1.0 - strength * 0.22, 1.0 - strength * 0.12);
        } else if(projectile instanceof ThrownEnderpearl enderpearl) {
            // 末影珍珠：增加重力，向锚点吸引
            newMotion = newMotion.multiply(1.0 - strength * 0.08, 1.0 - strength * 0.15, 1.0 - strength * 0.08);
        } else if(projectile instanceof ThrownPotion potion) {
            // 药水：快速下降
            newMotion = newMotion.multiply(1.0 - strength * 0.1, 1.0 - strength * 0.2, 1.0 - strength * 0.1);
        } else if(projectile instanceof ThrownTrident trident) {
            // 三叉戟：增加重力影响
            newMotion = newMotion.multiply(1.0 - strength * 0.05, 1.0 - strength * 0.1, 1.0 - strength * 0.05);
        }
        
        projectile.setDeltaMovement(newMotion);
    }

    // 生成飞行物轨迹粒子效果
    private static void spawnProjectileTrailParticles(ServerLevel serverLevel, Vec3 pos, GravityMode mode, double strength) {
        // 只在强度较高时生成粒子，避免过多粒子影响性能
        if(strength < 0.3) return;
        
        switch (mode) {
            case LOW_GRAVITY:
                // 低重力：生成向上漂浮的粒子
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z, 1,
                    0.05, 0.1, 0.05, 0.01
                );
                break;
            case HIGH_GRAVITY:
                // 高重力：生成向下坠落的粒子
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    pos.x, pos.y, pos.z, 1,
                    0.05, -0.1, 0.05, 0.01
                );
                break;
        }
    }

    // 播放重力音效
    private static void playGravitySound(Level level, BlockPos pos, GravityMode mode) {
        if(level instanceof ServerLevel serverLevel) {
            switch (mode) {
                case LOW_GRAVITY:
                    serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.3f, 1.5f);
                    break;
                case HIGH_GRAVITY:
                    serverLevel.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.2f, 0.8f);
                    break;
            }
        }
    }

    // 生成重力粒子效果
    private static void spawnGravityParticles(Level level, BlockPos pos, GravityMode mode, int range) {
        if(level instanceof ServerLevel serverLevel) {
            double centerX = pos.getX() + 0.5;
            double centerY = pos.getY() + 0.5;
            double centerZ = pos.getZ() + 0.5;
            
            // 在范围内随机生成粒子
            for(int i = 0; i < 5; i++) {
                double x = centerX + (level.random.nextDouble() - 0.5) * range;
                double y = centerY + (level.random.nextDouble() - 0.5) * range;
                double z = centerZ + (level.random.nextDouble() - 0.5) * range;
                
                switch (mode) {
                    case LOW_GRAVITY:
                        // 向上漂浮的粒子
                        serverLevel.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.END_ROD,
                            x, y, z, 1,
                            0.1, 0.2, 0.1, 0.01
                        );
                        break;
                    case HIGH_GRAVITY:
                        // 向下坠落的粒子
                        serverLevel.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.SMOKE,
                            x, y, z, 1,
                            0.1, -0.2, 0.1, 0.01
                        );
                        break;
                }
            }
        }
    }

    // 清理实体上的重力效果
    public static void clearGravityEffects(LivingEntity entity) {
        // 移除相关效果
        entity.removeEffect(MobEffects.GLOWING);
        entity.removeEffect(MobEffects.SLOW_FALLING);
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.MOVEMENT_SPEED);
        entity.removeEffect(MobEffects.JUMP);
        entity.removeEffect(MobEffects.DIG_SLOWDOWN);
        entity.removeEffect(MobEffects.WEAKNESS);
        entity.removeEffect(MobEffects.DAMAGE_BOOST);
    }

    // 清理离开范围的实体的重力效果
    private static void cleanupEntitiesOutsideRange(Level level, gravityAnchorEntity anchor, java.util.Set<java.util.UUID> currentEntityUUIDs) {
        // 创建需要清理的实体UUID集合
        java.util.Set<java.util.UUID> entitiesToCleanup = new java.util.HashSet<>(anchor.trackedEntities);
        entitiesToCleanup.removeAll(currentEntityUUIDs);
        
        // 清理离开范围的实体
        for(java.util.UUID entityUUID : entitiesToCleanup) {
            if(level instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(entityUUID);
                if(entity instanceof LivingEntity livingEntity) {
                    clearGravityEffects(livingEntity);
                }
            }
        }
        
        // 更新跟踪的实体集合
        anchor.trackedEntities.retainAll(currentEntityUUIDs);
    }

    // 当重力锚被破坏时清理所有跟踪的实体
    public void cleanupAllTrackedEntities() {
        if(level instanceof ServerLevel serverLevel) {
            for(java.util.UUID entityUUID : trackedEntities) {
                Entity entity = serverLevel.getEntity(entityUUID);
                if(entity instanceof LivingEntity livingEntity) {
                    clearGravityEffects(livingEntity);
                }
            }
            trackedEntities.clear();
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

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
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
        tag.putBoolean("activated", this.activated);
        tag.putInt("range", this.range);
        tag.putInt("soundCooldown", this.soundCooldown);
        tag.putInt("particleCooldown", this.particleCooldown);
        
        // 保存跟踪的实体UUID列表
        var trackedEntitiesTag = new net.minecraft.nbt.ListTag();
        for(java.util.UUID uuid : this.trackedEntities) {
            trackedEntitiesTag.add(net.minecraft.nbt.NbtUtils.createUUID(uuid));
        }
        tag.put("trackedEntities", trackedEntitiesTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if(tag.contains("mode")){
            this.mode = GravityMode.valueOf(tag.getString("mode")); // 加载模式
        }
        if(tag.contains("activated")){
            this.activated = tag.getBoolean("activated");
        }
        if(tag.contains("range")){
            this.range = tag.getInt("range");
        }
        if(tag.contains("soundCooldown")){
            this.soundCooldown = tag.getInt("soundCooldown");
        }
        if(tag.contains("particleCooldown")){
            this.particleCooldown = tag.getInt("particleCooldown");
        }
        
        // 加载跟踪的实体UUID列表
        this.trackedEntities.clear();
        if(tag.contains("trackedEntities")) {
            var trackedEntitiesTag = tag.getList("trackedEntities", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
            for(int i = 0; i < trackedEntitiesTag.size(); i++) {
                this.trackedEntities.add(net.minecraft.nbt.NbtUtils.loadUUID(trackedEntitiesTag.get(i)));
            }
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
