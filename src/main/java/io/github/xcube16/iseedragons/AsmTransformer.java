package io.github.xcube16.iseedragons;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.util.Optional;
import java.util.stream.Collectors;

public class AsmTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if(transformedName.equals("com.github.alexthe666.iceandfire.item.ItemModAxe")) {
			ISeeDragons.logger.info("ATTEMPTING TO PATCH " + transformedName + "!");
			ClassReader reader = new ClassReader(bytes);
			ClassNode node = new ClassNode();
			reader.accept(node, 0);
			if(fixItemModAxe(node)) {
				ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
				node.accept(writer);
				//node.accept(new CheckClassAdapter(writer));
				bytes = writer.toByteArray();
				ISeeDragons.logger.info("Patched " + transformedName + " to use ItemAxe as its superclass!");
			} else {
				ISeeDragons.logger.error("Failed to patch " + transformedName + "!");
			}
		}

		return bytes;
	}

	private boolean fixItemModAxe(ClassNode node) {
		// remove EFFECTIVE_ON field
		int fieldCount = node.fields.size();
		node.fields = node.fields.stream().filter(field ->
			!field.name.equals("EFFECTIVE_ON")
		).collect(Collectors.toList());
		if (fieldCount != node.fields.size() + 1) {
			ISeeDragons.logger.error("Failed to remove EFFECTIVE_ON field");
			return false;
		}

		// change the super class
		node.superName = "net/minecraft/item/ItemAxe";

		// remove <clinit>
		Optional<MethodNode> clinitMethod = node.methods.stream()
				.filter(method -> method.name.equals("<clinit>"))
				.findFirst();
		if (!clinitMethod.isPresent()) {
			ISeeDragons.logger.warn("Failed to find <clinit> method");
			return false;
		}
		node.methods.remove(clinitMethod.get());

		// change ItemTool.super(Item.ToolMaterial, Set<Block>) to ItemAxe.super(Item.ToolMaterial)
		Optional<MethodNode> initMethod = node.methods.stream()
				.filter(method -> method.name.equals("<init>"))
				.findFirst();
		if (!initMethod.isPresent()) {
			ISeeDragons.logger.error("Failed to find <init> method");
			return false;
		}

		AbstractInsnNode getstaticIns = initMethod.get().instructions.getFirst();
		while (getstaticIns != null && getstaticIns.getOpcode() != Opcodes.GETSTATIC) {
			getstaticIns = getstaticIns.getNext();
		}
		if (getstaticIns == null) {
			ISeeDragons.logger.error("Failed to find GETSTATIC instruction");
			return false;
		}

		AbstractInsnNode invokespecialIns = getstaticIns.getNext();
		initMethod.get().instructions.remove(getstaticIns.getPrevious());
		((FieldInsnNode) getstaticIns).owner = "net/minecraft/item/Item$ToolMaterial";
		((FieldInsnNode) getstaticIns).name  = "DIAMOND"; // this is what 1.8.3 uses
		((FieldInsnNode) getstaticIns).desc  = "Lnet/minecraft/item/Item$ToolMaterial;";

		if (invokespecialIns == null || invokespecialIns.getOpcode() != Opcodes.INVOKESPECIAL) {
			ISeeDragons.logger.error("Failed to find INVOKESPECIAL instruction");
			return false;
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

		return true;
	}
}

