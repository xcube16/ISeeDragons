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

import io.github.xcube16.iseedragons.ISD;
import io.github.xcube16.iseedragons.asm.ISeeDragonsTransformer;
import io.github.xcube16.iseedragons.asm.Patch;
import io.github.xcube16.iseedragons.asm.PatchResult;
import io.github.xcube16.iseedragons.asm.Patcher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Optional;
import java.util.stream.Collectors;

@Patcher(name = "Ice and Fire Axe fix", config = "IceAndFireAxeFix")
public class PatchIceAndFireAxes {

    /**
     * ItemModAxe has a constant EFFECTIVE_ON that contains a hard-coded list of blocks
     * that the axe works on. This makes it incompatible with mod blocks such as
     * better trees, etc.
     * <p>
     * This fix changes the super class to ItemAxe and removes the hard-coded list.
     */
    @Patch(target = "com.github.alexthe666.iceandfire.item.ItemModAxe",
            desc = "Make ItemModAxe extend ItemAxe and remove EFFECTIVE_ON hard-coded list")
    public static PatchResult fixItemModAxe(ISeeDragonsTransformer tweaker, ClassNode node) {
        // remove EFFECTIVE_ON field
        int fieldCount = node.fields.size();
        node.fields = node.fields.stream().filter(field ->
                !field.name.equals("EFFECTIVE_ON")
        ).collect(Collectors.toList());
        if (fieldCount != node.fields.size() + 1) {
            ISD.logger.error("Failed to remove EFFECTIVE_ON field");
            return PatchResult.NO_MUTATION;
        }

        // change the super class
        node.superName = "net/minecraft/item/ItemAxe";

        // remove <clinit>
        Optional<MethodNode> clinitMethod = node.methods.stream()
                .filter(method -> method.name.equals("<clinit>"))
                .findFirst();
        if (!clinitMethod.isPresent()) {
            ISD.logger.warn("Failed to find <clinit> method");
            return PatchResult.NO_MUTATION;
        }
        node.methods.remove(clinitMethod.get());

        // change ItemTool.super(Item.ToolMaterial, Set<Block>) to ItemAxe.super(Item.ToolMaterial)
        Optional<MethodNode> initMethod = node.methods.stream()
                .filter(method -> method.name.equals("<init>"))
                .findFirst();
        if (!initMethod.isPresent()) {
            ISD.logger.error("Failed to find <init> method");
            return PatchResult.NO_MUTATION;
        }

        AbstractInsnNode getstaticIns = initMethod.get().instructions.getFirst();
        while (getstaticIns != null && getstaticIns.getOpcode() != Opcodes.GETSTATIC) {
            getstaticIns = getstaticIns.getNext();
        }
        if (getstaticIns == null) {
            ISD.logger.error("Failed to find GETSTATIC instruction");
            return PatchResult.NO_MUTATION;
        }

        AbstractInsnNode invokespecialIns = getstaticIns.getNext();
        initMethod.get().instructions.remove(getstaticIns.getPrevious());
        ((FieldInsnNode) getstaticIns).owner = "net/minecraft/item/Item$ToolMaterial";
        ((FieldInsnNode) getstaticIns).name = "DIAMOND"; // this is what 1.8.3 uses
        ((FieldInsnNode) getstaticIns).desc = "Lnet/minecraft/item/Item$ToolMaterial;";

        if (invokespecialIns == null || invokespecialIns.getOpcode() != Opcodes.INVOKESPECIAL) {
            ISD.logger.error("Failed to find INVOKESPECIAL instruction");
            return PatchResult.NO_MUTATION;
        }
        ((MethodInsnNode) invokespecialIns).owner = "net/minecraft/item/ItemAxe";
        ((MethodInsnNode) invokespecialIns).desc = "(Lnet/minecraft/item/Item$ToolMaterial;)V";

        InsnList setToolMaterial = new InsnList();
        // this.
        setToolMaterial.add(new VarInsnNode(Opcodes.ALOAD, 0));
        //      ...          = material;
        setToolMaterial.add(new VarInsnNode(Opcodes.ALOAD, 1));
        //      toolMaterial
        setToolMaterial.add(new FieldInsnNode(Opcodes.PUTFIELD,
                "com/github/alexthe666/iceandfire/item/ItemModAxe",
                "field_77862_b",
                "Lnet/minecraft/item/Item$ToolMaterial;"));
        initMethod.get().instructions.insert(invokespecialIns, setToolMaterial);

        return PatchResult.NO_FLAGS;
    }
}
