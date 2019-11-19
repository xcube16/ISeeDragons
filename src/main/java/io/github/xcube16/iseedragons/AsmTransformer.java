package io.github.xcube16.iseedragons;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.common.config.Configuration;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

import javax.annotation.Nullable;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

public class AsmTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {

		// TODO: ok we are messing with multiple classes now... clean this up a bit
		if(transformedName.equals("com.github.alexthe666.iceandfire.item.ItemModAxe") ||
				transformedName.equals("com.github.alexthe666.iceandfire.entity.EntityDragonBase") ||
				transformedName.equals("net.minecraft.advancements.AdvancementManager") ||
				/*transformedName.equals("net.minecraft.advancements.AdvancementRewards$Deserializer")*/
				transformedName.equals("net.minecraftforge.common.ForgeHooks") ||
			    transformedName.equals("com.github.alexthe666.iceandfire.entity.EntityMyrmexEgg")) {

			ISeeDragons.logger.info("ATTEMPTING TO PATCH " + transformedName + "!");

			try {
				ClassReader reader = new ClassReader(bytes);
				ClassNode node = new ClassNode();
				reader.accept(node, 0);

				boolean success = true;
				if (transformedName.equals("com.github.alexthe666.iceandfire.item.ItemModAxe")) {
					success = fixItemModAxe(node);
				} else if (transformedName.equals("com.github.alexthe666.iceandfire.entity.EntityDragonBase")) {
					success = fixEntityDragonBase(node);
					/*if (success) {
						success = fixJawsEscape(node);
					}*/
				} else if (transformedName.equals("net.minecraft.advancements.AdvancementManager")) {
					if (StaticConfig.disableAdvancements) {
						success = nukeAdvancementManager(node);
					}
					if (success && StaticConfig.muteErroringAdvancements) {
						success = muteAdvancementManager(node);
					}
				} else if (transformedName.equals("net.minecraftforge.common.ForgeHooks")) {
					Configuration cfg = new Configuration(Paths.get("config", ISeeDragons.MODID + ".cfg").toFile());
					cfg.load();
					if (cfg.get("general", "muteErroringAdvancements", false).getBoolean()) {
						success = muteForgeHooksLoadAdv(node);
					}
				} else if (transformedName.equals("com.github.alexthe666.iceandfire.entity.EntityMyrmexEgg")) {
					success = fixMyrmexEggDupe(node);
				}/* else if (transformedName.equals("net.minecraft.advancements.AdvancementRewards$Deserializer")) {
					success = fixAdvancementRewards(node);
				}*/ else {
					success = false; // should not happen
				}

				if(success) {
					ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS/* + ClassWriter.COMPUTE_FRAMES can't use, or ku-boom*/);
					//node.accept(writer);
					node.accept(new CheckClassAdapter(writer));
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

	private boolean nukeAdvancementManager(ClassNode node) {
		// find breakBlock() method
		Optional<MethodNode> breakBlockMethod = node.methods.stream()
				.filter(method -> method.name.equals("func_192779_a")) // reload()
				.findFirst();
		if (!breakBlockMethod.isPresent()) {
			// scrap: ISeeDragons.logger.warn("Failed to find func_192777_a() (loadBuiltInAdvancements) method");
			ISeeDragons.logger.warn("Failed to find func_192779_a() (reload) method");
			return false;
		}

		// nuke everything for the lulz
		breakBlockMethod.get().instructions.clear();
		breakBlockMethod.get().localVariables.clear();
		breakBlockMethod.get().tryCatchBlocks.clear();
		// return;
		breakBlockMethod.get().instructions.add(new InsnNode(Opcodes.RETURN));

		return true;
	}

	private boolean fixMyrmexEggDupe(ClassNode node) throws NoSuchMethodException {
		MethodNode attackEntityFrom = findMethod(node, "func_70097_a");

		InsnList deadCheck = new InsnList();
		LabelNode continueLabel = new LabelNode();
		// ... this.
		deadCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
		// ... ...  isDead
		deadCheck.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "field_70128_L", "Z"));
		// if ( ...       ) {
		deadCheck.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel)); // if isDead is false, skip the code in the block

		// return false;
		deadCheck.add(new InsnNode(Opcodes.ICONST_0));
		deadCheck.add(new InsnNode(Opcodes.IRETURN));
		// }
		deadCheck.add(new FrameNode(Opcodes.F_SAME, 3, new Object[] {"com/github/alexthe666/entity/EntityMyrmexEgg", "net/minecraft/util/DamageSource", Opcodes.FLOAT},0, new Object[] {}));
		deadCheck.add(continueLabel);

		// put the check at the start of the method
		attackEntityFrom.instructions.insert(deadCheck);

		return true;
	}

	private boolean muteAdvancementManager(ClassNode node) throws NoSuchMethodException {
		// find loadCustomAdvancements() and loadBuiltInAdvancements() methods
		return muteMethod(findMethod(node, "func_192781_c")) && muteMethod(findMethod(node, "func_192777_a"));
	}

	private boolean muteForgeHooksLoadAdv(ClassNode node) throws NoSuchMethodException {
		return muteMethod(findMethod(node, "lambda$loadAdvancements$0"));
	}

	private boolean muteMethod(MethodNode method) {
		boolean flag = false;
		AbstractInsnNode ip = method.instructions.getFirst();
		while (ip != null) {
			if (ip.getOpcode() == Opcodes.INVOKEINTERFACE) {
				MethodInsnNode call = (MethodInsnNode) ip;
				if (call.owner.equals("org/apache/logging/log4j/Logger") &&
						call.name.equals("error") &&
						call.desc.equals("(Ljava/lang/String;Ljava/lang/Throwable;)V")) {
					method.instructions.insert(call, new InsnNode(Opcodes.POP2));
					method.instructions.insert(call, new InsnNode(Opcodes.POP));
					ip = call.getPrevious();
					method.instructions.remove(call);
					flag = true;
				}
			}

			ip = ip.getNext();
		}
		return flag;
	}

	private static MethodNode findMethod(ClassNode node, String name) throws NoSuchMethodException {
		return findMethod(node, name, null);
	}

	private static MethodNode findMethod(ClassNode node, String name, @Nullable String desc) throws NoSuchMethodException {
		Optional<MethodNode> method = node.methods.stream()
				.filter(m -> m.name.equals(name))
				.filter(m -> desc == null || m.desc.equals(desc))
				.findFirst();
		if (!method.isPresent()) {
			ISeeDragons.logger.warn("Failed to find loadCustomAdvancements() method");
			throw new NoSuchMethodException(node.name + " " + name + " " + desc);
		}
		return method.get();
	}

	/*
	private boolean fixAdvancementRewards(ClassNode node) {
		// find deserialize() method
		Optional<MethodNode> deserializeMethod = node.methods.stream()
				.filter(method -> method.name.equals("deserialize"))
				.filter(method -> method.desc.equals("(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/advancements/AdvancementRewards;"))
				.findFirst();
		if (!deserializeMethod.isPresent()) {
			ISeeDragons.logger.warn("Failed to find deserialize() method");
			return false;
		}

		InsnList instructions = deserializeMethod.get().instructions;

		// find World#destroyBlock() call
		AbstractInsnNode newExcInsn = instructions.getFirst();
		while (newExcInsn != null &&
				(newExcInsn.getOpcode() != Opcodes.NEW ||
						!((TypeInsnNode) newExcInsn).desc.equals("com/google/gson/JsonSyntaxException"))) {
			newExcInsn = newExcInsn.getNext();
		}
		if (newExcInsn == null) {
			ISeeDragons.logger.error("Failed to find NEW JsonSyntaxException instruction");
			return false;
		}

		while (newExcInsn.getNext() != null && newExcInsn.getNext().getOpcode() != Opcodes.ATHROW) {
			instructions.remove(newExcInsn.getNext());
		}
		if (newExcInsn.getNext() == null) {
			ISeeDragons.logger.error("Found NEW JsonSyntaxException, but could not find ATHROW after it");
			return false;
		}
		instructions.remove(newExcInsn.getNext());

		// Find GOTO instruction at the end of the for loop
		AbstractInsnNode gotoInsn = newExcInsn;
		while(gotoInsn.getNext() != null && gotoInsn.getOpcode() != Opcodes.GOTO) {
			gotoInsn = gotoInsn.getNext();
		}
		if (gotoInsn.getNext() == null) {
			ISeeDragons.logger.error("Found NEW JsonSyntaxException, but could not find GOTO after it");
			return false;
		}

		// Remove NEW JsonSyntaxException
		instructions.remove(newExcInsn);

		InsnList hook = new InsnList();
		// ...                  ...                                      aresourcelocation1
		hook.add(new VarInsnNode(Opcodes.ALOAD, 9));
		//                      ISeeDragons.cleanAdvancementRequardsHook(        ...       )
		hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
				"io/github/xcube16/iseedragons/ISeeDragons",
				"cleanAdvancementRequardsHook",
				"([Lnet/minecraft/util/ResourceLocation;)[Lnet/minecraft/util/ResourceLocation;"));
		// aresourcelocation1 = ...                                              ...
		hook.add(new VarInsnNode(Opcodes.ASTORE, 9));

		// insert hook
		instructions.insert(gotoInsn.getNext().getNext().getNext(), hook);

		return true;
	}*/

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

