/*
MIT License

Copyright (c) 2020 Charles445

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package io.github.xcube16.iseedragons.asm;

import io.github.xcube16.iseedragons.ISD;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import javax.annotation.Nullable;
import java.nio.file.Paths;

//FoamFix loads these three classes... Seems to work fine?

public class ASMConfig {
    private final Configuration config;

    public ASMConfig(String modName) {
        this.config = new Configuration(
                Paths.get("config").resolve(modName + ".cfg")
                        .toAbsolutePath().toFile(), true);
        this.config.load();
    }

    //Example map strings

    //general.server.roguelike dungeons.ENABLED
    //general.server.battle towers.Change Tower Explosion Owner

    public boolean getBoolean(String id, boolean _default) {
        Property prop = getProperty(id);
        if (prop == null) {
            return _default;
        }

        return prop.getBoolean(_default);
    }

    @Nullable
    public <T> Property getProperty(String s) {
        int i = s.lastIndexOf(Configuration.CATEGORY_SPLITTER);
        if (i < 0) {
            ISD.logger.warn("malformed config key: " + s);
        }

        return this.config.getCategory(s.substring(0, i)).get(s.substring(i + 1));
    }
}
