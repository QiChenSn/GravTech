package com.qichen.gravtech.datagen;

import com.qichen.gravtech.GravTech;
import com.qichen.gravtech.item.ModItemRegister;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class LangProvider extends LanguageProvider {
    public LangProvider(PackOutput output, String locale) {
        super(output, GravTech.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        add(ModItemRegister.GRAVITY_ANCHOR_ITEM.get(),"引力锚");
        add(ModItemRegister.FORCE_FIELD_CONFIGURATOR_ITEM.get(),"力场配置器");
        add("message.gravtech.gravity_anchor_entity.switch_to_low","切换到低引力模式");
        add("message.gravtech.gravity_anchor_entity.switch_to_high","切换到高引力模式");
    }
}
