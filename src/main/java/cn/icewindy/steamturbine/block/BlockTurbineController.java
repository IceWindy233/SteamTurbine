package cn.icewindy.steamturbine.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cn.icewindy.steamturbine.ModCreativeTab;
import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.registry.ModBlocks;
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;

public class BlockTurbineController extends BlockContainer {

    private IIcon iconFront;
    private IIcon iconSide;

    public static IIcon rotorBaseBG;
    public static IIcon rotorBaseRing;
    public static IIcon rotorIdle;
    public static IIcon rotorSpinning;

    public static IIcon overlayInput;
    public static IIcon overlayOutput;
    public static IIcon overlayDynamo;
    public static IIcon overlayRedstone;

    public BlockTurbineController() {
        super(Material.iron);
        setBlockName("steamturbine.turbine_controller");
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(ModCreativeTab.INSTANCE);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityTurbineController();
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        String modId = SteamTurbineMod.MOD_ID;
        iconFront = reg.registerIcon(modId + ":turbine_front");
        // Back side will also use side texture per user request
        iconSide = reg.registerIcon(modId + ":turbine_side");
        blockIcon = iconSide;

        rotorBaseBG = reg.registerIcon(modId + ":rotor/base_bg");
        rotorBaseRing = reg.registerIcon(modId + ":rotor/base_ring");
        rotorIdle = reg.registerIcon(modId + ":rotor/rotor_idle");
        rotorSpinning = reg.registerIcon(modId + ":rotor/rotor_spinning");

        overlayInput = reg.registerIcon(modId + ":overlay_input_hatch");
        overlayOutput = reg.registerIcon(modId + ":overlay_output_hatch");
        overlayDynamo = reg.registerIcon(modId + ":overlay_dynamo_hatch");
        overlayRedstone = reg.registerIcon(modId + ":overlay_redstone_control");
    }

    @Override
    public IIcon getIcon(net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
        int meta = world.getBlockMetadata(x, y, z);
        int facing = meta & 7;

        if (cn.icewindy.steamturbine.util.MultiblockValidator.isPartOfFormed(x, y, z)) {
            return ModBlocks.turbineCasing.getIcon(side, 0);
        }
        return getIcon(side, meta);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        // Meta 为 0 通常代表物品形态
        if (meta == 0) {
            // 改为 Side 4 (West)，在物品栏渲染中通常显示在左侧
            return (side == 4) ? iconFront : iconSide;
        }
        int facing = meta & 7;
        return (side == facing) ? iconFront : iconSide;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int facing = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int meta;
        switch (facing) {
            case 0:
                meta = 2;
                break; // North
            case 1:
                meta = 5;
                break; // East
            case 2:
                meta = 3;
                break; // South
            case 3:
                meta = 4;
                break; // West
            default:
                meta = 2;
                break;
        }
        world.setBlockMetadataWithNotify(x, y, z, meta, 2);

        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityTurbineController) {
            ((TileEntityTurbineController) te).setFacing(meta);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityTurbineController) {
                // Open GUI, ID 0
                player.openGui(SteamTurbineMod.instance, 0, world, x, y, z);
            }
        }
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityTurbineController) {
                ((TileEntityTurbineController) te).recheckStructure();
            }
        }
    }

    @Override
    public int getRenderType() {
        return 0; // Use standard block render type
    }

    @Override
    public boolean isOpaqueCube() {
        return true; // Block is solid, not transparent
    }

    @Override
    public boolean renderAsNormalBlock() {
        return true; // Render as standard cube
    }

    @Override
    public int getLightValue() {
        return 0; // No light emission
    }
}
