package typhonic.wavestone;

import java.lang.reflect.Method;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;

public class TestReflection {
    public static void main(String[] args) {
        System.out.println("Methods in BlockBehaviour:");
        for (Method m : BlockBehaviour.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " -> " + java.util.Arrays.toString(m.getParameterTypes()));
        }
        System.out.println("Methods in Block:");
        for (Method m : Block.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " -> " + java.util.Arrays.toString(m.getParameterTypes()));
        }
    }
}
