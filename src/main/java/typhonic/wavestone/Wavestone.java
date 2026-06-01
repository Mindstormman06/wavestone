package typhonic.wavestone;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Class, definitions and registration happens here!
 */
public class Wavestone implements ModInitializer {
	public static final String MOD_ID = "wavestone";


	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Wavestone Loaded!");
	}
}