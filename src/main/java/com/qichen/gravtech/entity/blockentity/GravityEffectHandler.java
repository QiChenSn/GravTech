package com.qichen.gravtech.entity.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 重力效果处理器 - 负责处理所有与重力效果相关的逻辑
 * 将不同类型的实体处理分离到专门的处理器中，提高代码的可读性和可维护性
 */
public class GravityEffectHandler {
    
    // 重力效果常量
    private static final double LOW_GRAVITY_MULTIPLIER = 0.3;
    private static final double HIGH_GRAVITY_MULTIPLIER = 2.0;
    private static final int EFFECT_DURATION = 2;
    
    // 实体处理器
    private final LivingEntityGravityHandler livingEntityHandler;
    private final ItemEntityGravityHandler itemEntityHandler;
    private final ProjectileGravityHandler projectileHandler;
    private final EffectsHandler effectsHandler;
    
    public GravityEffectHandler() {
        this.livingEntityHandler = new LivingEntityGravityHandler();
        this.itemEntityHandler = new ItemEntityGravityHandler();
        this.projectileHandler = new ProjectileGravityHandler();
        this.effectsHandler = new EffectsHandler();
    }
    
    /**
     * 对实体应用重力效果
     */
    public void applyGravityEffect(Entity entity, GravityAnchorEntity.GravityMode mode, GravityAnchorEntity anchor) {
        if (entity instanceof LivingEntity livingEntity) {
            livingEntityHandler.applyEffect(livingEntity, mode, anchor);
        } else if (entity instanceof ItemEntity itemEntity) {
            itemEntityHandler.applyEffect(itemEntity, mode, anchor);
        } else if (entity instanceof Projectile projectile) {
            projectileHandler.applyEffect(projectile, mode, anchor);
        }
    }
    
    /**
     * 播放重力音效
     */
    public void playGravitySound(Level level, BlockPos pos, GravityAnchorEntity.GravityMode mode) {
        effectsHandler.playSound(level, pos, mode);
    }
    
    /**
     * 生成重力粒子效果
     */
    public void spawnGravityParticles(Level level, BlockPos pos, GravityAnchorEntity.GravityMode mode, int range) {
        effectsHandler.spawnParticles(level, pos, mode, range);
    }
    
    /**
     * 生物实体重力处理器
     */
    private static class LivingEntityGravityHandler {
        
        public void applyEffect(LivingEntity entity, GravityAnchorEntity.GravityMode mode, GravityAnchorEntity anchor) {
            switch (mode) {
                case LOW_GRAVITY:
                    applyLowGravityEffect(entity);
                    break;
                case HIGH_GRAVITY:
                    applyHighGravityEffect(entity);
                    break;
            }
        }
        
        private void applyLowGravityEffect(LivingEntity entity) {
            // 1. 减少重力影响
            Vec3 motion = entity.getDeltaMovement();
            if (motion.y < 0) {
                entity.setDeltaMovement(motion.x, motion.y * LOW_GRAVITY_MULTIPLIER, motion.z);
            }
            
            // 2. 添加缓慢下降效果
            if (!entity.onGround() && entity.getDeltaMovement().y < -0.1) {
                entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, EFFECT_DURATION, 0, false, false, true));
            }
            
            // 3. 增加跳跃力
            if (entity instanceof Player player) {
                Vec3 currentMotion = player.getDeltaMovement();
                if (currentMotion.y > 0.1) {
                    player.setDeltaMovement(currentMotion.x, currentMotion.y * 1.1, currentMotion.z);
                }
            }
            
            // 4. 添加正面效果
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.JUMP, EFFECT_DURATION, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, EFFECT_DURATION, 0, false, false, true));
        }
        
        private void applyHighGravityEffect(LivingEntity entity) {
            // 1. 增加重力影响
            Vec3 motion = entity.getDeltaMovement();
            if (motion.y < 0) {
                entity.setDeltaMovement(motion.x, motion.y * HIGH_GRAVITY_MULTIPLIER, motion.z);
            }
            
            // 2. 限制跳跃
            if (entity instanceof Player player) {
                Vec3 currentMotion = player.getDeltaMovement();
                if (currentMotion.y > 0.1) {
                    player.setDeltaMovement(currentMotion.x, currentMotion.y * 0.5, currentMotion.z);
                }
            }
            
            // 3. 添加负面效果
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, EFFECT_DURATION, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 0, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, EFFECT_DURATION, 0, false, false, true));
            
            // 4. 高坠落伤害增强
            if (entity.fallDistance > 3.0f) {
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_DURATION, 0, false, false, true));
            }
        }
    }
    
    /**
     * 物品实体重力处理器
     */
    private static class ItemEntityGravityHandler {
        
        public void applyEffect(ItemEntity itemEntity, GravityAnchorEntity.GravityMode mode, GravityAnchorEntity anchor) {
            Vec3 motion = itemEntity.getDeltaMovement();
            Vec3 pos = itemEntity.position();
            Vec3 anchorPos = Vec3.atCenterOf(anchor.getBlockPos());
            
            // 计算距离衰减
            double distance = pos.distanceTo(anchorPos);
            double maxDistance = anchor.getRange() / 2.0;
            double strength = Math.max(0, 1.0 - (distance / maxDistance));
            
            switch (mode) {
                case LOW_GRAVITY:
                    applyLowGravityToItem(itemEntity, motion, strength);
                    break;
                case HIGH_GRAVITY:
                    applyHighGravityToItem(itemEntity, motion, anchorPos, pos, strength);
                    break;
            }
        }
        
        private void applyLowGravityToItem(ItemEntity itemEntity, Vec3 motion, double strength) {
            // 物品缓慢下降
            if (motion.y < 0) {
                itemEntity.setDeltaMovement(motion.x, motion.y * LOW_GRAVITY_MULTIPLIER * strength, motion.z);
            }
            // 添加轻微的向上力
            if (strength > 0.5) {
                itemEntity.setDeltaMovement(motion.x, motion.y + 0.02 * strength, motion.z);
            }
        }
        
        private void applyHighGravityToItem(ItemEntity itemEntity, Vec3 motion, Vec3 anchorPos, Vec3 pos, double strength) {
            // 物品快速下降
            if (motion.y < 0) {
                itemEntity.setDeltaMovement(motion.x, motion.y * HIGH_GRAVITY_MULTIPLIER * strength, motion.z);
            }
            // 向锚点中心吸引
            Vec3 direction = anchorPos.subtract(pos).normalize();
            itemEntity.setDeltaMovement(
                motion.x + direction.x * 0.05 * strength,
                motion.y + direction.y * 0.05 * strength,
                motion.z + direction.z * 0.05 * strength
            );
        }
    }
    
    /**
     * 投射物重力处理器
     */
    private static class ProjectileGravityHandler {
        
        public void applyEffect(Projectile projectile, GravityAnchorEntity.GravityMode mode, GravityAnchorEntity anchor) {
            Vec3 motion = projectile.getDeltaMovement();
            Vec3 pos = projectile.position();
            Vec3 anchorPos = Vec3.atCenterOf(anchor.getBlockPos());
            
            // 计算距离衰减
            double distance = pos.distanceTo(anchorPos);
            double maxDistance = anchor.getRange() / 2.0;
            double strength = Math.max(0, 1.0 - (distance / maxDistance));
            
            // 如果强度太小，不应用效果
            if (strength < 0.1) return;
            
            // 生成轨迹粒子效果
            if (projectile.level() instanceof ServerLevel serverLevel) {
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
        
        private void applyLowGravityToProjectile(Projectile projectile, Vec3 motion, Vec3 anchorPos, Vec3 pos, double strength) {
            Vec3 newMotion = motion;
            
            // 减少重力影响，让飞行物飞得更远更高
            if (motion.y < 0) {
                newMotion = newMotion.multiply(1.0, LOW_GRAVITY_MULTIPLIER, 1.0);
            }
            
            // 根据飞行物类型应用不同效果
            newMotion = applyProjectileSpecificEffects(projectile, newMotion, strength, true);
            
            projectile.setDeltaMovement(newMotion);
        }
        
        private void applyHighGravityToProjectile(Projectile projectile, Vec3 motion, Vec3 anchorPos, Vec3 pos, double strength) {
            Vec3 newMotion = motion;
            
            // 增加重力影响，让飞行物更快下降
            if (motion.y < 0) {
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
            newMotion = applyProjectileSpecificEffects(projectile, newMotion, strength, false);
            
            projectile.setDeltaMovement(newMotion);
        }
        
        private Vec3 applyProjectileSpecificEffects(Projectile projectile, Vec3 motion, double strength, boolean isLowGravity) {
            if (projectile instanceof Arrow) {
                return isLowGravity ? 
                    motion.multiply(1.0 + strength * 0.2, 1.0 + strength * 0.3, 1.0 + strength * 0.2) :
                    motion.multiply(1.0 - strength * 0.1, 1.0 - strength * 0.2, 1.0 - strength * 0.1);
            } else if (projectile instanceof Snowball) {
                return isLowGravity ?
                    motion.multiply(1.0 + strength * 0.3, 1.0 + strength * 0.4, 1.0 + strength * 0.3) :
                    motion.multiply(1.0 - strength * 0.15, 1.0 - strength * 0.25, 1.0 - strength * 0.15);
            } else if (projectile instanceof ThrownEgg) {
                return isLowGravity ?
                    motion.multiply(1.0 + strength * 0.25, 1.0 + strength * 0.35, 1.0 + strength * 0.25) :
                    motion.multiply(1.0 - strength * 0.12, 1.0 - strength * 0.22, 1.0 - strength * 0.12);
            } else if (projectile instanceof ThrownEnderpearl) {
                return isLowGravity ?
                    motion.multiply(1.0 + strength * 0.15, 1.0 + strength * 0.25, 1.0 + strength * 0.15) :
                    motion.multiply(1.0 - strength * 0.08, 1.0 - strength * 0.15, 1.0 - strength * 0.08);
            } else if (projectile instanceof ThrownPotion) {
                return isLowGravity ?
                    motion.multiply(1.0 + strength * 0.2, 1.0 + strength * 0.3, 1.0 + strength * 0.2) :
                    motion.multiply(1.0 - strength * 0.1, 1.0 - strength * 0.2, 1.0 - strength * 0.1);
            } else if (projectile instanceof ThrownTrident) {
                return isLowGravity ?
                    motion.multiply(1.0 + strength * 0.1, 1.0 + strength * 0.2, 1.0 + strength * 0.1) :
                    motion.multiply(1.0 - strength * 0.05, 1.0 - strength * 0.1, 1.0 - strength * 0.05);
            }
            
            // 默认效果
            return isLowGravity ?
                motion.multiply(1.0 + strength * 0.1, 1.0 + strength * 0.2, 1.0 + strength * 0.1) :
                motion.multiply(1.0 - strength * 0.1, 1.0 - strength * 0.2, 1.0 - strength * 0.1);
        }
        
        private void spawnProjectileTrailParticles(ServerLevel serverLevel, Vec3 pos, GravityAnchorEntity.GravityMode mode, double strength) {
            if (strength < 0.3) return;
            
            switch (mode) {
                case LOW_GRAVITY:
                    serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z, 1,
                        0.05, 0.1, 0.05, 0.01
                    );
                    break;
                case HIGH_GRAVITY:
                    serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.SMOKE,
                        pos.x, pos.y, pos.z, 1,
                        0.05, -0.1, 0.05, 0.01
                    );
                    break;
            }
        }
    }
    
    /**
     * 音效和粒子效果处理器
     */
    private static class EffectsHandler {
        
        public void playSound(Level level, BlockPos pos, GravityAnchorEntity.GravityMode mode) {
            if (level instanceof ServerLevel serverLevel) {
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
        
        public void spawnParticles(Level level, BlockPos pos, GravityAnchorEntity.GravityMode mode, int range) {
            if (level instanceof ServerLevel serverLevel) {
                double centerX = pos.getX() + 0.5;
                double centerY = pos.getY() + 0.5;
                double centerZ = pos.getZ() + 0.5;
                
                // 在范围内随机生成粒子
                for (int i = 0; i < 5; i++) {
                    double x = centerX + (level.random.nextDouble() - 0.5) * range;
                    double y = centerY + (level.random.nextDouble() - 0.5) * range;
                    double z = centerZ + (level.random.nextDouble() - 0.5) * range;
                    
                    switch (mode) {
                        case LOW_GRAVITY:
                            serverLevel.sendParticles(
                                net.minecraft.core.particles.ParticleTypes.END_ROD,
                                x, y, z, 1,
                                0.1, 0.2, 0.1, 0.01
                            );
                            break;
                        case HIGH_GRAVITY:
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
    }
}
