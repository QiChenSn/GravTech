package com.qichen.gravtech.datagen;

import com.qichen.gravtech.GravTech;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class LangProvider extends LanguageProvider {
    public LangProvider(PackOutput output, String locale) {
        super(output, GravTech.MODID, locale);
    }

    @Override
    protected void addTranslations() {
    }
}
