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
		// TODO: ok we are messing with multiple classes now... clean this up a bit
		if(transformedName.equals("com.github.alexthe666.iceandfire.item.ItemModAxe") ||
				transformedName.equals("com.github.alexthe666.iceandfire.entity.EntityDragonBase")) {

			ISeeDragons.logger.info("ATTEMPTING TO PATCH " + transformedName + "!");

			try {
				ClassReader reader = new ClassReader(bytes);
				ClassNode node = new ClassNode();
				reader.accept(node, 0);

				boolean success;
				if (transformedName.equals("com.github.alexthe666.iceandfire.item.ItemModAxe")) {
					success = fixItemModAxe(node);
				} else {
					success = fixEntityDragonBase(node);
					/*if (success) {
						success = fixJawsEscape(node);
					}*/
				}

				if(success) {
					ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					node.accept(writer);
					//node.accept(new CheckClassAdapter(writer));
					bytes = writer.toByteArray();
					ISeeDragons.logger.info("Patched " + transformedName);
				} else {
					ISeeDragons.logger.error("Failed to patch " + transformedName + "!");
				}

			} catch (Throwable t) {
				ISeeDragons.logger.error("Aborting patch!", t);
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

	private boolean fixEntityDragonBase(ClassNode node) {
		// find breakBlock() method
		Optional<MethodNode> breakBlockMethod = node.methods.stream()
				.filter(method -> method.name.equals("breakBlock"))
				.findFirst();
		if (!breakBlockMethod.isPresent()) {
			ISeeDragons.logger.warn("Failed to find breakBlock() method");
			return false;
		}

		// find World#destroyBlock() call
		AbstractInsnNode breakBlockCall = breakBlockMethod.get().instructions.getFirst();
		while (breakBlockCall != null &&
				(breakBlockCall.getOpcode() != Opcodes.INVOKEVIRTUAL ||
				!((MethodInsnNode) breakBlockCall).name.equals("func_175655_b"))) {
			breakBlockCall = breakBlockCall.getNext();
		}
		if (breakBlockCall == null) {
			ISeeDragons.logger.error("Failed to find func_175655_b (World#destroyBlock()) call");
			return false;
		}

		breakBlockMethod.get().instructions.remove(breakBlockCall.getPrevious()); // delete ICONST_1 (true)

		// call hook method in ISeeDragons to see if the block should drop
		InsnList callHook = new InsnList();

		// ...                                  this.world
		// *world already on stack*
		// ...                                              state
		callHook.add(new VarInsnNode(Opcodes.ALOAD, 5));
		// ... ISeeDragons.dragonBreakBlockHook(   ...    , ...  )
		callHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
				"io/github/xcube16/iseedragons/ISeeDragons",
				"dragonBreakBlockHook",
				"(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z"));
		breakBlockMethod.get().instructions.insertBefore(breakBlockCall, callHook);

		breakBlockMethod.get().instructions.remove(breakBlockCall);

		return true;
	}

	/*
	 * WOW... I am so dumb! ... I was fixing code that never worked!
	 */
	/*private boolean fixJawsEscape(ClassNode node) {
		// find onUpdate() method
		Optional<MethodNode> onUpdateMethod = node.methods.stream()
				.filter(method -> method.name.equals("func_70071_h_"))
				.findFirst();
		if (!onUpdateMethod.isPresent()) {
			ISeeDragons.logger.warn("Failed to find breakBlock() method");
			return false;
		}

		// find Entity#isRiding() call
		AbstractInsnNode isRidingCall = onUpdateMethod.get().instructions.getFirst();
		while (isRidingCall != null &&
				(isRidingCall.getOpcode() != Opcodes.INVOKEVIRTUAL ||
						!((MethodInsnNode) isRidingCall).name.equals("func_70093_af"))) {
			isRidingCall = isRidingCall.getNext();
		}
		if (isRidingCall == null) {
			ISeeDragons.logger.error("Failed to find func_70093_af (Entity#isRiding() call)");
			return false;
		}

		// call hook method in ISeeDragons to see if the block should drop
		InsnList aniCheck = new InsnList();

		// .. this............... .. ...................
		aniCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
		// .. .....getAnimation() .. ...................
		aniCheck.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
				"com/github/alexthe666/iceandfire/entity/EntityDragonBase",
				"getAnimation",
				"()Lnet/ilexiconn/llibrary/server/animation/Animation;"));
		// .. ................... .. ANIMATION_SHAKEPREY
		aniCheck.add(new FieldInsnNode(Opcodes.GETSTATIC,
				"com/github/alexthe666/iceandfire/entity/EntityDragonBase",
				"ANIMATION_SHAKEPREY",
				"Lnet/ilexiconn/llibrary/server/animation/Animation;"));
		// && ................... == ...................
		aniCheck.add(new JumpInsnNode(Opcodes.IFNE, ((JumpInsnNode) isRidingCall.getNext()).label));

		return true;
	}*/
}

