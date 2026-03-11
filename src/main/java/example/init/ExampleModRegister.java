package example.init;

import example.block.TestBlock;
import example.block.blockentity.TestBlockEntity;
import example.entity.Zti;
import example.item.ExampleArmorItem;
import example.item.DeagleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
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
    public static EntityType<Zti> ZTI_ENTITY_TYPE;
    public static BlockItem TEST_BLOCK_ITEM;
    public static DeagleItem DEAGLE_ITEM;

    public static ExampleArmorItem DEFENDER_ARMOR_HELMET;
    public static ExampleArmorItem DEFENDER_ARMOR_CHESTPLATE;
    public static ExampleArmorItem DEFENDER_ARMOR_LEGGINGS;
    public static ExampleArmorItem DEFENDER_ARMOR_BOOTS;

    public static ForgeSpawnEggItem ZTI_SPAWN_EGG;
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

        if (ForgeRegistries.ENTITY_TYPES.equals(registry)) {
            ZTI_ENTITY_TYPE = EntityType.Builder.of(Zti::new, MobCategory.MONSTER)
                    .sized(2.2F, 2.5F)
                    .build(modLoc("zti").toString());
            event.register(ForgeRegistries.ENTITY_TYPES.getRegistryKey(), modLoc("zti"), () -> ZTI_ENTITY_TYPE);
        }

        if (ForgeRegistries.ITEMS.equals(registry)) {
            TEST_BLOCK_ITEM = new BlockItem(TEST_BLOCK, new BlockItem.Properties());
            DEAGLE_ITEM = new DeagleItem();
            DEFENDER_ARMOR_HELMET = new ExampleArmorItem(ArmorItem.Type.HELMET);
            DEFENDER_ARMOR_CHESTPLATE = new ExampleArmorItem(ArmorItem.Type.CHESTPLATE);
            DEFENDER_ARMOR_LEGGINGS = new ExampleArmorItem(ArmorItem.Type.LEGGINGS);
            DEFENDER_ARMOR_BOOTS = new ExampleArmorItem(ArmorItem.Type.BOOTS);
            ZTI_SPAWN_EGG = new ForgeSpawnEggItem(() -> ZTI_ENTITY_TYPE, 0x61554D, 0xD8B076, new Item.Properties());
            event.register(ForgeRegistries.ITEMS.getRegistryKey(), modLoc("test_block_item"), () -> TEST_BLOCK_ITEM);
            event.register(ForgeRegistries.ITEMS.getRegistryKey(), modLoc("deagle"), () -> DEAGLE_ITEM);

            event.register(ForgeRegistries.ITEMS.getRegistryKey(), modLoc("defender_helmet"), () -> DEFENDER_ARMOR_HELMET);
            event.register(ForgeRegistries.ITEMS.getRegistryKey(), modLoc("defender_chestplate"), () -> DEFENDER_ARMOR_CHESTPLATE);
            event.register(ForgeRegistries.ITEMS.getRegistryKey(), modLoc("defender_leggings"), () -> DEFENDER_ARMOR_LEGGINGS);
            event.register(ForgeRegistries.ITEMS.getRegistryKey(), modLoc("defender_boots"), () -> DEFENDER_ARMOR_BOOTS);

            event.register(ForgeRegistries.ITEMS.getRegistryKey(), modLoc("zti_spawn_egg"), () -> ZTI_SPAWN_EGG);
        }

        if (Registries.CREATIVE_MODE_TAB.equals(event.getRegistryKey())) {
            TEST_TAB = CreativeModeTab.builder().title(Component.translatable("item_group.example.name"))
                    .icon(() -> TEST_BLOCK_ITEM.getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(TEST_BLOCK_ITEM);
                        output.accept(DEAGLE_ITEM);
                        output.accept(DEFENDER_ARMOR_HELMET);
                        output.accept(DEFENDER_ARMOR_CHESTPLATE);
                        output.accept(DEFENDER_ARMOR_LEGGINGS);
                        output.accept(DEFENDER_ARMOR_BOOTS);
                        output.accept(ZTI_SPAWN_EGG);
                    }).build();
            event.register(Registries.CREATIVE_MODE_TAB, modLoc("test_tab"), () -> TEST_TAB);
        }
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        if (ZTI_ENTITY_TYPE != null) {
            event.put(ZTI_ENTITY_TYPE, Zti.createAttributes().build());
        }
    }

    public static ResourceLocation modLoc(String name) {
        return new ResourceLocation(MOD_ID, name);
    }
}
