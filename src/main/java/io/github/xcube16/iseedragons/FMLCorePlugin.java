package io.github.xcube16.iseedragons;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions(value={"io.github.xcube16.iseedragons"})
@IFMLLoadingPlugin.SortingIndex(1001)
public class FMLCorePlugin implements IFMLLoadingPlugin {

	@Override
	public String[] getASMTransformerClass() {
		return new String[] {
				"io.github.xcube16.iseedragons.AsmTransformer",
		};
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		// ignored
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}
