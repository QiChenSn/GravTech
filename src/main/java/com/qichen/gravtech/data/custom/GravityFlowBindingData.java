package com.qichen.gravtech.data.custom;

import com.qichen.gravtech.entity.blockentity.GravityAnchorEntity;

/**
 * 储存单个引力流数据
 */
public class GravityFlowBindingData {
    private GravityAnchorEntity highAnchor;
    private GravityAnchorEntity lowAnchor;

    public GravityFlowBindingData(GravityAnchorEntity lowAnchor, GravityAnchorEntity highAnchor) {
        this.lowAnchor = lowAnchor;
        this.highAnchor = highAnchor;
    }
}
