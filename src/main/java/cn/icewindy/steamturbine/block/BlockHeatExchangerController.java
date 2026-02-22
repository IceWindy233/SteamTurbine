package cn.icewindy.steamturbine.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cn.icewindy.steamturbine.ModCreativeTab;
import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.tileentity.TileEntityHeatExchangerController;

public class BlockHeatExchangerController extends BlockContainer {

    private IIcon iconFront;
    private IIcon iconSide;

    public BlockHeatExchangerController() {
        super(Material.iron);
        setBlockName("steamturbine.heat_exchanger_controller");
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(ModCreativeTab.INSTANCE);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityHeatExchangerController();
    }

    private IIcon iconFrontFormed;
    private IIcon iconFrontActive;

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        iconFront = reg.registerIcon(SteamTurbineMod.MOD_ID + ":heat_exchanger_controller_front");
        iconSide = reg.registerIcon(SteamTurbineMod.MOD_ID + ":heat_exchanger_controller_side");
        iconFrontFormed = reg.registerIcon(SteamTurbineMod.MOD_ID + ":overlayheat_exchanger_controller_front");
        iconFrontActive = reg.registerIcon(SteamTurbineMod.MOD_ID + ":overlay_heat_exchanger_controller_front_active");
        blockIcon = iconSide;
    }

    public IIcon getIconFrontFormed() {
        return iconFrontFormed;
    }

    public IIcon getIconFrontActive() {
        return iconFrontActive;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (meta == 0) {
            return side == 4 ? iconFront : iconSide;
        }
        int facing = meta & 7;
        return side == facing ? iconFront : iconSide;
    }

    @Override
    public IIcon getIcon(net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
        int meta = world.getBlockMetadata(x, y, z);
        int facing = meta & 7;

        if (cn.icewindy.steamturbine.util.HeatExchangerValidator.isPartOfFormed(x, y, z)) {
            return cn.icewindy.steamturbine.registry.ModBlocks.heatExchangerCasing.getIcon(side, 0);
        }
        return getIcon(side, meta);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int facing = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int meta;
        switch (facing) {
            case 0:
                meta = 2;
                break;
            case 1:
                meta = 5;
                break;
            case 2:
                meta = 3;
                break;
            case 3:
                meta = 4;
                break;
            default:
                meta = 2;
                break;
        }
        world.setBlockMetadataWithNotify(x, y, z, meta, 2);

        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityHeatExchangerController) {
            ((TileEntityHeatExchangerController) te).setFacing(meta);
            ((TileEntityHeatExchangerController) te).recheckStructure();
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        if (world.isRemote) return;
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityHeatExchangerController) {
            ((TileEntityHeatExchangerController) te).recheckStructure();
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityHeatExchangerController)) return true;

        TileEntityHeatExchangerController controller = (TileEntityHeatExchangerController) te;
        controller.recheckStructure();
        for (String line : controller.getInfoLines()) {
            player.addChatMessage(new ChatComponentText(line));
        }
        return true;
    }

    @Override
    public int getRenderType() {
        return 0;
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return true;
    }
}
