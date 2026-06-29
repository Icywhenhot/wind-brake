package com.delamainsoftware.elytrawindbrake;

import com.delamainsoftware.elytrawindbrake.gui.WindBrakeConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Hooks the config screen into Mod Menu. Mod Menu is optional: this class is only
 * loaded when Mod Menu is installed (it's registered under the "modmenu" entrypoint),
 * so the core mod still runs with nothing but fabric-loader on the classpath.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WindBrakeConfigScreen::new;
    }
}
