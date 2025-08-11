package example.init;

import example.block.TestBlock;
import example.block.blockentity.TestBlockEntity;
import example.item.DeagleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExampleModRegister {
    /**
     * 注册名用 example，方便 build 时排除
     */
    public static final String MOD_ID = "example";

    public static Block TEST_BLOCK;
    public static BlockEntityType<TestBlockEntity> TEST_BLOCK_ENTITY_TYPE;
    public static BlockItem TEST_BLOCK_ITEM;
    public static DeagleItem DEAGLE_ITEM;
    public static CreativeModeTab TEST_TAB;

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        IForgeRegistry<?> registry = event.getForgeRegistry();

        if (ForgeRegistries.BLOCKS.equals(registry)) {
            TEST_BLOCK = new TestBlock();
            event.register(ForgeRegistries.BLOCKS.getRegistryKey(), modLoc("test_block"), () -> TEST_BLOCK);
        }

        if (ForgeRegistries.BLOCK_ENTITY_TYPES.equals(registry)) {
            TEST_BLOCK_ENTITY_TYPE = BlockEntityType.Builder.of(TestBlockEntity::new, TEST_BLOCK).build(null);
            event.register(ForgeRegistries.BLOCK_ENTITY_TYPES.getRegistryKey(), modLoc("test_block_entity_type"), () -> TEST_BLOCK_ENTITY_TYPE);
        }

        if (ForgeRegistries.ITEMS.equals(registry)) {
            TEST_BLOCK_ITEM = new BlockItem(TEST_BLOCK, new BlockItem.Properties());
            DEAGLE_ITEM = new DeagleItem();
            event.register(ForgeRegistries.ITEMS.getRegistryKey(), modLoc("test_block_item"), () -> TEST_BLOCK_ITEM);
            event.register(ForgeRegistries.ITEMS.getRegistryKey(), modLoc("deagle"), () -> DEAGLE_ITEM);
        }

        if (Registries.CREATIVE_MODE_TAB.equals(event.getRegistryKey())) {
            TEST_TAB = CreativeModeTab.builder().title(Component.translatable("item_group.example.name"))
                    .icon(() -> TEST_BLOCK_ITEM.getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(TEST_BLOCK_ITEM);
                        output.accept(DEAGLE_ITEM);
                    }).build();
            event.register(Registries.CREATIVE_MODE_TAB, modLoc("test_tab"), () -> TEST_TAB);
        }
    }

    public static ResourceLocation modLoc(String name) {
        return new ResourceLocation(MOD_ID, name);
    }
}
