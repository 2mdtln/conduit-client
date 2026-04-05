package com.twomdtln.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

class ConduitModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<ConduitMenuScreen> {
        return ConfigScreenFactory { parent -> ConduitMenuScreen(parent) }
    }
}
