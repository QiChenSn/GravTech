package com.qichen.gravtech.datagen;

import com.qichen.gravtech.GravTech;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ChineseLangProvider extends LanguageProvider {
    public ChineseLangProvider(PackOutput output) {
        super(output, GravTech.MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {

    }
} 