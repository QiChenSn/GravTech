package com.qichen.gravtech.manager;

import com.qichen.gravtech.data.custom.GravityFlowBindingData;
import com.qichen.gravtech.entity.blockentity.GravityAnchorEntity;

/**
 * 管理全局引力流
 */

public class GravityFlowManager {
    public static GravityFlowBindingData createNewFlow(GravityAnchorEntity entity1, GravityAnchorEntity entity2){
        //检查是否已经绑定过
        // ...

        if(entity1.getMode()!=entity2.getMode()){
            return entity1.getMode().equals(GravityAnchorEntity.GravityMode.HIGH_GRAVITY)?
                    new GravityFlowBindingData(entity2,entity1):new GravityFlowBindingData(entity1,entity2);
        }
        return null;
    }
}
