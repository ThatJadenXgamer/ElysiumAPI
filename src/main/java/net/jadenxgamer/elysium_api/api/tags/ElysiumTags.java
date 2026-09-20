package net.jadenxgamer.elysium_api.api.tags;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

public class ElysiumTags {

    public static class Blocks {
        public static final TagKey<Block> NON_SOLID_FIRE_SUPPORT = createBlockTag("non_solid_fire_support"); // Blocks in this tag can have soul fire lit on faces that are not solid
        public static final TagKey<Block> FUNGUS_PLANTABLE_ON = createBlockTag("fungus_plantable_on"); // Nether Fungi are Plantable on these Blocks
        public static final TagKey<Block> ROOTS_PLANTABLE_ON = createBlockTag("roots_plantable_on"); // Nether Roots are Plantable on these Blocks
        public static final TagKey<Block> NETHER_SPROUTS_PLANTABLE_ON = createBlockTag("nether_sprouts_plantable_on"); // Nether Sprouts are Plantable on these Blocks
        public static final TagKey<Block> NETHER_WART_PLANTABLE_ON = createBlockTag("nether_wart_plantable_on"); // Nether Warts are Plantable on these Blocks
        public static final TagKey<Block> MULTILAYER_JIGSAW_NON_SOLID_VALID = createBlockTag("multilayer_jigsaw_non_solid_valid"); // Allows MultilayerJigsawStructures to place template on these blocks even if they are non-solid

        private static TagKey<Block> createBlockTag(String name) {
            return TagKey.create(Registries.BLOCK, ElysiumAPI.elysiumPath(name));
        }
    }

    public static class EntityTypes {
        public static final TagKey<EntityType<?>> PIGLINS_AFRAID_OF = createEntityTypeTag("piglins_afraid_of"); // Piglins will flee from mobs in this tag (this use to be in vanilla but mojang removed it for some reason????)

        private static TagKey<EntityType<?>> createEntityTypeTag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, ElysiumAPI.elysiumPath(name));
        }
    }

    public static class DamageTypes {
        public static final TagKey<DamageType> CANT_DAMAGE_ARMOR = createDamageTypeTag("cant_damage_armor"); // DamageTypes in this tag won't take durability away from armor

        private static TagKey<DamageType> createDamageTypeTag(String name) {
            return TagKey.create(Registries.DAMAGE_TYPE, ElysiumAPI.elysiumPath(name));
        }
    }

    public static class Enchantments {
        public static final TagKey<Enchantment> DISABLED = createEnchantmentTag("disabled"); // Completely disables the enchantment from appearing anywhere

        private static TagKey<Enchantment> createEnchantmentTag(String name) {
            return TagKey.create(Registries.ENCHANTMENT, ElysiumAPI.elysiumPath(name));
        }
    }
}