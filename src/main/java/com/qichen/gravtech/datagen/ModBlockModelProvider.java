package com.qichen.gravtech.datagen;

import com.qichen.gravtech.GravTech;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockModelProvider extends BlockModelProvider {
    public ModBlockModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, GravTech.MODID, helper);
    }

    @Override
    protected void registerModels() {
    }
}
