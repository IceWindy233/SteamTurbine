package cn.icewindy.steamturbine.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cn.icewindy.steamturbine.ModCreativeTab;
import cn.icewindy.steamturbine.SteamTurbineMod;
import cn.icewindy.steamturbine.tileentity.TileEntityFlowLimiter;

public class BlockFlowLimiter extends BlockContainer {

    public BlockFlowLimiter() {
        super(Material.iron);
        setBlockName("steamturbine.flow_limiter");
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(ModCreativeTab.INSTANCE);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityFlowLimiter();
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon(SteamTurbineMod.MOD_ID + ":redstone_control");
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
        if (te instanceof TileEntityFlowLimiter) {
            ((TileEntityFlowLimiter) te).setFacing(meta);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;
        player.openGui(SteamTurbineMod.instance, 9, world, x, y, z);
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityFlowLimiter) {
            ((TileEntityFlowLimiter) te).setFacing(world.getBlockMetadata(x, y, z));
        }
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
