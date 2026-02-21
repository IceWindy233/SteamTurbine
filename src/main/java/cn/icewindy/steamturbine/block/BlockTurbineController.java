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
import cn.icewindy.steamturbine.tileentity.TileEntityTurbineController;

public class BlockTurbineController extends BlockContainer {

    private IIcon iconFront;
    private IIcon iconBack;
    private IIcon iconSide;

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
        iconBack = reg.registerIcon(modId + ":turbine_back");
        iconSide = reg.registerIcon(modId + ":turbine_side");
        blockIcon = iconSide;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        int facing = meta & 7;
        if (side == facing) {
            return iconFront;
        }
        int backSide = getOppositeSide(facing);
        if (side == backSide) {
            return iconBack;
        }
        return iconSide;
    }

    private int getOppositeSide(int side) {
        switch (side) {
            case 0:
                return 1;
            case 1:
                return 0;
            case 2:
                return 3;
            case 3:
                return 2;
            case 4:
                return 5;
            case 5:
                return 4;
            default:
                return 0;
        }
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
