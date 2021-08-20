package io.github.xcube16.iseedragons.client;

import io.github.xcube16.iseedragons.DefaultProxy;
import net.minecraft.client.settings.GameSettings;

public class ClientProxy extends DefaultProxy {

    public void setBrightness(float min, float max) {
        super.setBrightness(min, max);

        GameSettings.Options.GAMMA.valueMin = min;
        GameSettings.Options.GAMMA.valueMax = max;
    }
}
