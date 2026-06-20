package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.Minecraft;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.PlayTime;
import top.fpsmaster.modules.statistics.PlayTimeStatistics;
import top.fpsmaster.ui.custom.Component;

import java.util.Locale;

public class PlayTimeComponent extends Component {
    public PlayTimeComponent() {
        super(PlayTime.class);
        x = 0.05f;
        y = 0.15f;
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        PlayTime module = getModule();
        String text = getLabel(module) + formatTime(PlayTimeStatistics.getDisplayMillis(Minecraft.getMinecraft(), module.displayMode.getMode()));

        width = getStringWidth(16, text) + 8;
        height = 16f;

        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(16, text, x + 2, y + 3, module.textColor.getRGB());
    }

    private String getLabel(PlayTime module) {
        String label = module.label.getValue();
        if (label == null || label.trim().isEmpty()) {
            label = FPSMaster.i18n.get("playtime.defaultlabel");
            if ("playtime.defaultlabel".equals(label)) {
                label = "游玩时间：";
            }
            module.label.setValue(label);
        }
        if (!label.endsWith("：") && !label.endsWith(":") && !label.endsWith(" ")) {
            label += "：";
        }
        return label;
    }

    private String formatTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        if (totalSeconds < 60L) {
            return totalSeconds + FPSMaster.i18n.get("playtime.unit.seconds");
        }

        long minutes = totalSeconds / 60L;
        if (minutes < 60L) {
            return minutes + FPSMaster.i18n.get("playtime.unit.minutes");
        }

        long hours = minutes / 60L;
        long remainMinutes = minutes % 60L;
        if (remainMinutes == 0L) {
            return hours + FPSMaster.i18n.get("playtime.unit.hours");
        }
        return String.format(Locale.ROOT, "%d%s%d%s", hours, FPSMaster.i18n.get("playtime.unit.hours"), remainMinutes, FPSMaster.i18n.get("playtime.unit.minutes"));
    }

    private PlayTime getModule() {
        return (PlayTime) mod;
    }
}
