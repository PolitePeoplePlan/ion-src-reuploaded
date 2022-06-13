package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.utils.Categories;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class LegacyMode extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public LegacyMode() {
        super(Categories.Misc, "legacy-mode", "Modifies some client behavior to work on older versions.");
    }

}
