/*
MIT License

Copyright (c) 2020 Charles445

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
package io.github.xcube16.iseedragons.asm;

import io.github.xcube16.iseedragons.ISD;
import io.github.xcube16.iseedragons.ISeeDragons;
import io.github.xcube16.iseedragons.asm.helper.ASMHelper;
import io.github.xcube16.iseedragons.asm.patch.*;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiFunction;

public class ISeeDragonsTransformer implements IClassTransformer {
    private final Map<String, List<PatchMeta>> transformMap = new HashMap<>();

    private static final ASMConfig config = new ASMConfig(ISeeDragons.MODID);

    public ISeeDragonsTransformer() {
        ISD.logger.info("Adding Patchers");

        addPatcher(PatchDragonItemLag.class);
        addPatcher(PatchIceAndFireAxes.class);
        addPatcher(PatchLogSpam.class);
        addPatcher(PatchMyrmexEggDupe.class);
        addPatcher(PatchNukeAdvancements.class);
        addPatcher(PatchSerpentSpawning.class);
        addPatcher(PatchDragonDismount.class);
        addPatcher(HookStructureGenerator.class);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        //Check for patches
        if (transformMap.containsKey(transformedName)) {
            ISD.logger.info("ATTEMPTING TO PATCH  " + transformedName + "!");
            PatchResult result = PatchResult.NO_MUTATION;

            ClassNode clazzNode = ASMHelper.readClassFromBytes(basicClass);

            for (PatchMeta manager : transformMap.get(transformedName)) {
                try {
                    ISD.logger.info("Applying patch [" + manager.name + "] " + manager.desc);
                    result = result.add(manager.patch.apply(this, clazzNode));
                } catch (Exception e) {
                    // TODO: If there was an error, clazzNode may have been mutated into an undefined state.
                    ISD.logger.error("Failed at patch " + manager.name);
                    ISD.logger.error("Failed to write " + transformedName);
                    e.printStackTrace();
                    return basicClass;
                }
            }

            //TODO verbose
            if (result.isMutated()) {
                ISD.logger.info("Writing class " + transformedName + " with flags " + result);
                return ASMHelper.writeClassToBytes(clazzNode, result.getFlags());
            } else {
                ISD.logger.info("All patches for class " + transformedName + " were cancelled, skipping...");
                return basicClass;
            }
        }

        return basicClass;
    }

    public void addPatcher(Class<?> clazz) {
        @Nullable
        Patcher patcher = clazz.getAnnotation(Patcher.class);
        if (patcher == null) {
            throw new IllegalArgumentException(clazz.getName() + " does not have an @Patcher annotation");
        }

        if (!patcher.config().isEmpty() && !config.getBoolean("general.asm." + patcher.config(), patcher.defaultEnable())) {
            ISD.logger.info("ASM patcher: [" + patcher.name() + "] disabled");
            return;
        }

        for (Method m : clazz.getDeclaredMethods()) {
            @Nullable
            Patch patch = m.getAnnotation(Patch.class);
            if (patch != null) {
                if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers()) ||
                        !Arrays.equals(m.getParameterTypes(), new Class[]{ISeeDragonsTransformer.class, ClassNode.class}) ||
                        !m.getReturnType().equals(PatchResult.class)) {
                    throw new IllegalArgumentException(clazz.getName() + "#" + m.getName() + " is not declared correctly to be a @Patch");
                }

                addPatch(patch.target(),
                        patcher.name().equals("") ? clazz.getSimpleName() : patcher.name(),
                        patch.desc(),
                        (tweaker, clazzNode) -> {
                            try {
                                return (PatchResult) m.invoke(null, tweaker, clazzNode);
                            } catch (ReflectiveOperationException e) {
                                // We sanitized the method already
                                throw new RuntimeException("This shouldn't have happened (blame xcube)", e);
                            }
                        });
            }
        }
        ISD.logger.info("ASM patcher: [" + patcher.name() + "] enabled");
    }

    public void addPatch(String target, String name, String desc, BiFunction<ISeeDragonsTransformer, ClassNode, PatchResult> patch) {
        this.transformMap.computeIfAbsent(target, t -> new ArrayList<>())
                .add(new PatchMeta(name, desc, patch));
    }

    public void addPatch(String target, BiFunction<ISeeDragonsTransformer, ClassNode, PatchResult> patch) {
        this.addPatch(target, "<anonymous>", "", patch);
    }

    private static final class PatchMeta {

        final String name;
        final String desc;
        final BiFunction<ISeeDragonsTransformer, ClassNode, PatchResult> patch;

        private PatchMeta(String name, String desc, BiFunction<ISeeDragonsTransformer, ClassNode, PatchResult> patch) {
            this.name = name;
            this.desc = desc;
            this.patch = patch;
        }
    }
}
