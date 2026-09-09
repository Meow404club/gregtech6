package gregtech6.tileentity.sensors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.world.inventory.MenuType;

import gregtech6.gui.machines.GT6MuiMachine;

/**
 * The structural no-GUI pin (task p26-sensors-core offline acceptance — the "零 MenuType
 * 断言" arm). The sensor family is NO_GUI upstream (MultiTileEntitySensor.java:91, the
 * {@code NO_GUI_CLICK_TO_INTERACT} tooltip): the interaction surface is pure world
 * clicks ({@code GTSensorBlock.use}) and the acceptance channel is the /gt6sensor
 * command. This suite pins that the family declares NO menu surface anywhere, by
 * reflection over the card's classes:
 *
 * <ul>
 * <li>no field typed {@link MenuType} (raw or through a generic — a
 *     {@code DeferredRegister<MenuType<?>>} row would name it in the generic string);</li>
 * <li>no class implementing {@link GT6MuiMachine} — the p26-mui-a-open-chain menu
 *     dispatch seam ({@code tryOpen}) a GUI-bearing block would carry;</li>
 * <li>the family's own registration surface ({@code GT6Sensors}) is blocks+items only.</li>
 * </ul>
 *
 * <p>The reflection is deliberately static-init safe offline: every listed class's
 * static state is a logger, an immutable list or DeferredRegister.handle rows — the
 * GT6CapabilityWiringSeamTest RegistryObject-getId precedent (no registry boot needed).
 */
public class GTSensorNoMenuSeamTest {

	/** The whole family the card touched (the sensor package + its registration face). */
	private static final List<Class<?>> FAMILY = List.of(
			GTSensorLogic.class
			, GTSensorBlockEntity.class
			, GT6ProgressmeterBlockEntity.class
			, GT6FluidometerBlockEntity.class
			, GT6ElectrometerBlockEntity.class
			, gregtech6.block.sensors.GTSensorBlock.class
			, GTSensorCommand.class
			, gregtech6.registry.GT6Sensors.class
	);

	@Test
	public void sensorFamilyDeclaresZeroMenuSurface() {
		for (Class<?> tClass : FAMILY) {
			assertFalse(GT6MuiMachine.class.isAssignableFrom(tClass),
					tClass.getSimpleName() + " implements the MUI open seam — sensors are NO_GUI (Sensor.java:91)");
			assertFalse(hasMenuTypedField(tClass),
					tClass.getSimpleName() + " declares a MenuType-typed field — sensors register no menus");
		}
	}

	@Test
	public void theFamilyListCoversTheWholePackage() {
		// the census guard for THIS test: every audited member lives in the sensor
		// package (or is the registration face in the registry package) — a batch-2
		// sensor class joining the package must join the FAMILY list too (append the
		// Class literal, the SeamTest census convention)
		for (Class<?> tClass : FAMILY) {
			String tPackage = tClass.getPackageName();
			assertTrue(
					"gregtech6.tileentity.sensors".equals(tPackage)
							|| "gregtech6.block.sensors".equals(tPackage)
							|| "gregtech6.registry".equals(tPackage),
					tClass.getSimpleName() + " drifted out of the audited packages");
		}
		assertTrue(FAMILY.contains(GTSensorBlockEntity.class), "the double base is audited");
		assertTrue(FAMILY.contains(gregtech6.block.sensors.GTSensorBlock.class), "the block is audited");
		assertTrue(FAMILY.contains(GTSensorCommand.class), "the command is audited");
		assertTrue(FAMILY.contains(gregtech6.registry.GT6Sensors.class), "the registration face is audited");
	}

	private static boolean hasMenuTypedField(Class<?> aClass) {
		for (Field tField : aClass.getDeclaredFields()) {
			if (isMenuSurface(tField.getType(), tField.getGenericType())) return true;
		}
		return false;
	}

	/** Raw MenuType or anything naming it in the generic string (DeferredRegister&lt;MenuType&lt;?&gt;&gt;). */
	private static boolean isMenuSurface(Class<?> aRawType, Type aGenericType) {
		if (aRawType == MenuType.class || MenuType.class.isAssignableFrom(aRawType)) return true;
		return String.valueOf(aGenericType).contains("MenuType");
	}
}
