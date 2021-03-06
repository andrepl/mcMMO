package com.gmail.nossr50.skills.gathering;

import java.util.Random;

import org.bukkit.CropState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.config.mods.CustomBlocksConfig;
import com.gmail.nossr50.datatypes.AbilityType;
import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.datatypes.SkillType;
import com.gmail.nossr50.datatypes.mods.CustomBlock;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.runnables.GreenThumbTimer;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.ModChecks;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.Skills;
import com.gmail.nossr50.util.Users;

public class Herbalism {

    private static Random random = new Random();

    /**
     * Activate the Green Terra ability.
     *
     * @param player The player activating the ability
     * @param block The block to be changed by Green Terra
     */
    public static void greenTerra(Player player, Block block) {
        PlayerInventory inventory = player.getInventory();
        boolean hasSeeds = inventory.contains(Material.SEEDS);

        if (!hasSeeds) {
            player.sendMessage("You need more seeds to spread Green Terra");  //TODO: Needs more locale.
        }
        else if (hasSeeds && !block.getType().equals(Material.WHEAT)) {
            inventory.removeItem(new ItemStack(Material.SEEDS));
            player.updateInventory();   // Needed until replacement available
            greenTerraConvert(player, block);
        }
    }

    public static void greenTerraConvert(Player player, Block block) {
        Material type = block.getType();

        if (Misc.blockBreakSimulate(block, player, false)) {
            if (Config.getInstance().getHerbalismGreenThumbSmoothbrickToMossy() && type == Material.SMOOTH_BRICK && block.getData() == 0) {
                block.setTypeIdAndData(block.getTypeId(), (byte) 1, false); //Set type of the brick to mossy, force the client update
            }
            else if (Config.getInstance().getHerbalismGreenThumbDirtToGrass() && type == Material.DIRT) {
                block.setType(Material.GRASS);
            }
            else if (Config.getInstance().getHerbalismGreenThumbCobbleToMossy() && type == Material.COBBLESTONE) {
                block.setType(Material.MOSSY_COBBLESTONE);
                // Don't award double drops to mossified cobblestone
                mcMMO.p.placeStore.setTrue(block);
            }
            else if (Config.getInstance().getHerbalismGreenThumbCobbleWallToMossyWall() && type == Material.COBBLE_WALL) {
                block.setData((byte) 1);
            }
        }
    }

    /**
     * Check for extra Herbalism drops.
     *
     * @param block The block to check for extra drops
     * @param player The player getting extra drops
     * @param event The event to use for Green Thumb
     * @param plugin mcMMO plugin instance
     */
    public static void herbalismProcCheck(final Block block, Player player, BlockBreakEvent event, mcMMO plugin) {
        if(player == null)
            return;

        final PlayerProfile profile = Users.getProfile(player);
        final int MAX_BONUS_LEVEL = 1000;

        int herbLevel = profile.getSkillLevel(SkillType.HERBALISM);
        int id = block.getTypeId();
        Material type = block.getType();

        Byte data = block.getData();
        Location location = block.getLocation();
        Material mat = null;

        int xp = 0;
        int catciDrops = 0;
        int caneDrops = 0;

        boolean customPlant = false;

        int randomChance = 1000;

        if (player.hasPermission("mcmmo.perks.lucky.herbalism")) {
            randomChance = (int) (randomChance * 0.75);
        }

        switch (type) {
        case BROWN_MUSHROOM:
        case RED_MUSHROOM:
            if (!mcMMO.p.placeStore.isTrue(block)) {
                mat = Material.getMaterial(id);
                xp = Config.getInstance().getHerbalismXPMushrooms();
            }
            break;

        case CACTUS:
            for (int y = 0;  y <= 2; y++) {
                Block b = block.getRelative(0, y, 0);
                if (b.getType().equals(Material.CACTUS)) {
                    mat = Material.CACTUS;
                    if (!mcMMO.p.placeStore.isTrue(b)) {
                        if (herbLevel > MAX_BONUS_LEVEL || random.nextInt(randomChance) <= herbLevel) {
                            catciDrops++;
                        }
                        xp += Config.getInstance().getHerbalismXPCactus();
                    }
                }
            }
            break;

        case CROPS:
            if (data == CropState.RIPE.getData()) {
                mat = Material.WHEAT;
                xp = Config.getInstance().getHerbalismXPWheat();

                if (Permissions.getInstance().greenThumbWheat(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        case MELON_BLOCK:
            if (!mcMMO.p.placeStore.isTrue(block)) {
                mat = Material.MELON;
                xp = Config.getInstance().getHerbalismXPMelon();
            }
            break;

        case NETHER_WARTS:
            if (data == (byte) 0x3) {
                mat = Material.NETHER_STALK;
                xp = Config.getInstance().getHerbalismXPNetherWart();

                if (Permissions.getInstance().greenThumbNetherwart(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        case PUMPKIN:
        case JACK_O_LANTERN:
            if (!mcMMO.p.placeStore.isTrue(block)) {
                mat = Material.getMaterial(id);
                xp = Config.getInstance().getHerbalismXPPumpkin();
            }
            break;

        case RED_ROSE:
        case YELLOW_FLOWER:
            if (!mcMMO.p.placeStore.isTrue(block)) {
                mat = Material.getMaterial(id);
                xp = Config.getInstance().getHerbalismXPFlowers();
            }
            break;

        case SUGAR_CANE_BLOCK:
            for (int y = 0;  y <= 2; y++) {
                Block b = block.getRelative(0, y, 0);
                if (b.getType().equals(Material.SUGAR_CANE_BLOCK)) {
                    mat = Material.SUGAR_CANE;
                    if (!mcMMO.p.placeStore.isTrue(b)) {
                        if (herbLevel > MAX_BONUS_LEVEL || random.nextInt(randomChance) <= herbLevel) {
                            caneDrops++;
                        }
                        xp += Config.getInstance().getHerbalismXPSugarCane();
                    }
                }
            }
            break;

        case VINE:
            if (!mcMMO.p.placeStore.isTrue(block)) {
                mat = type;
                xp = Config.getInstance().getHerbalismXPVines();
            }
            break;

        case WATER_LILY:
            if (!mcMMO.p.placeStore.isTrue(block)) {
                mat = type;
                xp = Config.getInstance().getHerbalismXPLilyPads();
            }
            break;
            
        case COCOA:
            if ((((byte) data) & 0x8) == 0x8) {
                mat = Material.COCOA;
                xp = Config.getInstance().getHerbalismXPCocoa();


                if (Permissions.getInstance().greenThumbCocoa(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        case CARROT:
            if (data == CropState.RIPE.getData()) {
                mat = Material.CARROT;
                xp = Config.getInstance().getHerbalismXPCarrot();


                if (Permissions.getInstance().greenThumbCarrots(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        case POTATO:
            if (data == CropState.RIPE.getData()) {
                mat = Material.POTATO;
                xp = Config.getInstance().getHerbalismXPPotato();

                if (Permissions.getInstance().greenThumbPotatoes(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        default:
            if (Config.getInstance().getBlockModsEnabled() && CustomBlocksConfig.getInstance().customHerbalismBlocks.contains(new ItemStack(block.getTypeId(), 1, (short) 0, block.getData()))) {
                customPlant = true;
                xp = ModChecks.getCustomBlock(block).getXpGain();
            }
            break;
        }

        if (mat == null && !customPlant) {
            return;
        }

        if (Permissions.getInstance().herbalismDoubleDrops(player)) {
            ItemStack is = null;

            if (customPlant) {
                is = new ItemStack(ModChecks.getCustomBlock(block).getItemDrop());
            }
            else {
                if (mat == Material.COCOA) {
                    is = new ItemStack(Material.INK_SACK, 1, (short) 3);
                }
                else if (mat == Material.CARROT) {
                    is = new ItemStack(Material.CARROT_ITEM, 1, (short) 0);
                }
                else if (mat == Material.POTATO) {
                    is = new ItemStack(Material.POTATO_ITEM, 1, (short) 0);
                }
                else {
                    is = new ItemStack(mat);
                }
            }

            if (herbLevel > MAX_BONUS_LEVEL || random.nextInt(randomChance) <= herbLevel) {
                Config configInstance = Config.getInstance();

                switch (type) {
                case BROWN_MUSHROOM:
                    if (configInstance.getBrownMushroomsDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case CACTUS:
                    if (configInstance.getCactiDoubleDropsEnabled()) {
                        Misc.dropItems(location, is, catciDrops);
                    }
                    break;

                case CROPS:
                    if (configInstance.getWheatDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case MELON_BLOCK:
                    if (configInstance.getMelonsDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case NETHER_WARTS:
                    if (configInstance.getNetherWartsDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case PUMPKIN:
                    if (configInstance.getPumpkinsDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case RED_MUSHROOM:
                    if (configInstance.getRedMushroomsDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case SUGAR_CANE_BLOCK:
                    if (configInstance.getSugarCaneDoubleDropsEnabled()) {
                        Misc.dropItems(location, is, caneDrops);
                    }
                    break;

                case VINE:
                    if (configInstance.getVinesDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case WATER_LILY:
                    if (configInstance.getWaterLiliesDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case YELLOW_FLOWER:
                    if (configInstance.getYellowFlowersDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;
                    
                case COCOA:
                    if (configInstance.getCocoaDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case CARROT:
                    if (configInstance.getCarrotDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case POTATO:
                    if (configInstance.getPotatoDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                default:
                    if (customPlant) {
                        CustomBlock customBlock = ModChecks.getCustomBlock(block);
                        int minimumDropAmount = customBlock.getMinimumDropAmount();
                        int maximumDropAmount = customBlock.getMaximumDropAmount();

                        is = customBlock.getItemDrop();

                        if (minimumDropAmount != maximumDropAmount) {
                            Misc.dropItems(location, is, minimumDropAmount);
                            Misc.randomDropItems(location, is, 50, maximumDropAmount - minimumDropAmount);
                        }
                        else {
                            Misc.dropItems(location, is, minimumDropAmount);
                        }
                    }
                    break;
                }
            }
        }

        if(Config.getInstance().getHerbalismAFKDisabled() && player.isInsideVehicle())
            return;

        Skills.xpProcessing(player, profile, SkillType.HERBALISM, xp);
    }

    /**
     * Apply the Green Thumb ability to crops.
     *
     * @param block The block to apply the ability to
     * @param player The player using the ability
     * @param event The event triggering the ability
     * @param plugin mcMMO plugin instance
     */
    private static void greenThumbWheat(Block block, Player player, BlockBreakEvent event, mcMMO plugin) {
        final int MAX_BONUS_LEVEL = 1500;

        PlayerProfile profile = Users.getProfile(player);
        int herbLevel = profile.getSkillLevel(SkillType.HERBALISM);
        PlayerInventory inventory = player.getInventory();
        boolean hasSeeds = false;
        Location location = block.getLocation();
        Material type = block.getType();

        switch(type) {
        case CROPS:
            hasSeeds = inventory.contains(Material.SEEDS);
            break;
        case COCOA:
            // Broken: Requires an update to bukkit to enable seaching for variable-sized ItemStacks.
            hasSeeds = inventory.contains(new ItemStack(Material.INK_SACK, 1, (short) 3), 1);
            break;
        case CARROT:
            hasSeeds = inventory.contains(Material.CARROT_ITEM);
            break;
        case POTATO:
            hasSeeds = inventory.contains(Material.POTATO_ITEM);
            break;
        case NETHER_WARTS:
            hasSeeds = inventory.contains(Material.NETHER_STALK);
            break;
        }

        int randomChance = 1500;

        if (player.hasPermission("mcmmo.perks.lucky.herbalism")) {
            randomChance = (int) (randomChance * 0.75);
        }

        if (hasSeeds && profile.getAbilityMode(AbilityType.GREEN_TERRA) || hasSeeds && (herbLevel > MAX_BONUS_LEVEL || random.nextInt(randomChance) <= herbLevel)) {
            event.setCancelled(true);

            switch(type) {
            case CROPS:
                Misc.dropItem(location, new ItemStack(Material.WHEAT));
                Misc.randomDropItems(location, new ItemStack(Material.SEEDS), 50, 3);
                inventory.removeItem(new ItemStack(Material.SEEDS));
                break;
            case COCOA:
                Misc.dropItem(location, new ItemStack(Material.INK_SACK, 3, (short) 3));
                inventory.removeItem(new ItemStack(Material.INK_SACK, 1, (short) 3));
                break;
            case CARROT:
                Misc.dropItem(location, new ItemStack(Material.CARROT_ITEM));
                Misc.randomDropItems(location, new ItemStack(Material.CARROT_ITEM), 50, 3);
                inventory.removeItem(new ItemStack(Material.CARROT_ITEM));
                break;
            case POTATO:
                Misc.dropItem(location, new ItemStack(Material.POTATO_ITEM));
                Misc.randomDropItems(location, new ItemStack(Material.POTATO_ITEM), 50, 3);
                Misc.randomDropItem(location, new ItemStack(Material.POISONOUS_POTATO), 2);
                inventory.removeItem(new ItemStack(Material.POTATO_ITEM));
                break;
            case NETHER_WARTS:
                Misc.dropItem(location, new ItemStack(Material.NETHER_STALK, 2));
                Misc.randomDropItems(location, new ItemStack(Material.NETHER_STALK), 50, 2);
                inventory.removeItem(new ItemStack(Material.NETHER_STALK));
                break;
            }

            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new GreenThumbTimer(block, profile, type), 1);
            player.updateInventory();   // Needed until replacement available
        }
    }

    /**
     * Apply the Green Thumb ability to blocks.
     *
     * @param is The item in the player's hand
     * @param player The player activating the ability
     * @param block The block being used in the ability
     */
    public static void greenThumbBlocks(ItemStack is, Player player, Block block) {
        final int MAX_BONUS_LEVEL = 1500;

        PlayerProfile profile = Users.getProfile(player);
        int skillLevel = profile.getSkillLevel(SkillType.HERBALISM);
        int seeds = is.getAmount();

        player.setItemInHand(new ItemStack(Material.SEEDS, seeds - 1));

        int randomChance = 1500;

        if (player.hasPermission("mcmmo.perks.lucky.herbalism")) {
            randomChance = (int) (randomChance * 0.75);
        }

        if (skillLevel > MAX_BONUS_LEVEL || random.nextInt(randomChance) <= skillLevel) {
            greenTerraConvert(player, block);
        }
        else {
            player.sendMessage(LocaleLoader.getString("Herbalism.Ability.GTh.Fail"));
        }
    }
}
