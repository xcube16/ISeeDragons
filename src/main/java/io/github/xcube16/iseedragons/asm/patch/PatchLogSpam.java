/*
MIT License

Copyright (c) 2021 xcube16

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
package io.github.xcube16.iseedragons.asm.patch;

import io.github.xcube16.iseedragons.asm.ISeeDragonsTransformer;
import io.github.xcube16.iseedragons.asm.Patch;
import io.github.xcube16.iseedragons.asm.PatchResult;
import io.github.xcube16.iseedragons.asm.Patcher;
import org.objectweb.asm.tree.ClassNode;

import static io.github.xcube16.iseedragons.asm.helper.PatchHelper.findMethod;
import static io.github.xcube16.iseedragons.asm.helper.PatchHelper.muteMethod;

/**
 * Mutes errors caused by broken advancements (can be used to stop log spam when recipes are tweaked)
 */
@Patcher(name = "Stop Log Spammers", config = "STFU")
public class PatchLogSpam {

    @Patch(target = "net.minecraft.advancements.AdvancementManager")
    public static PatchResult muteAdvancementManager(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        // find loadCustomAdvancements() and loadBuiltInAdvancements() methods
        return muteMethod(findMethod(node, "func_192781_c"))
                .add(muteMethod(findMethod(node, "func_192777_a")));
    }

    @Patch(target = "net.minecraftforge.common.ForgeHooks")
    public static PatchResult muteForgeHooksLoadAdv(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        return muteMethod(findMethod(node, "lambda$loadAdvancements$0"));
    }
}
