package net.mehvahdjukaar.supplementaries.blocks;

import io.netty.buffer.Unpooled;
import net.mehvahdjukaar.supplementaries.gui.NoticeBoardContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.FurnaceTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class NoticeBoardBlock extends Block {
    public static final DirectionProperty FACING = HorizontalBlock.HORIZONTAL_FACING;
    public static final BooleanProperty HAS_BOOK = BlockStateProperties.HAS_BOOK;
    public NoticeBoardBlock(Properties properties) {
        super(properties);
        this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH).with(HAS_BOOK, false));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_BOOK);
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.toRotation(state.get(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
                                             BlockRayTraceResult hit) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        // itemstack.getItem() instanceof WrittenBookItem && player.abilities.allowEdit;
        if (tileentity instanceof NoticeBoardBlockTile) {
            ItemStack itemstack = player.getHeldItem(handIn);
            NoticeBoardBlockTile te = (NoticeBoardBlockTile) tileentity;
            boolean flag = itemstack.getItem() instanceof DyeItem && player.abilities.allowEdit;
            boolean flag2 = (te.isEmpty() && (te.canInsertItem(0, itemstack, null)));
            boolean flag3 = (player.isSneaking() && !te.isEmpty());

            boolean server = !worldIn.isRemote();

            //insert Item
            if (flag2) {
                if(server){
                    ItemStack it = itemstack.copy();
                    it.setCount(1);
                    NonNullList<ItemStack> stacks = NonNullList.withSize(1, it);
                    te.setItems(stacks);
                    te.markDirty();
                    worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0F,
                            worldIn.rand.nextFloat() * 0.10F + 0.95F);
                }
                if (!player.isCreative()) {
                    itemstack.shrink(1);
                }

                return ActionResultType.SUCCESS;
            }
            // change color
            else if (flag) {
                if(te.setTextColor(((DyeItem) itemstack.getItem()).getDyeColor())){
                    if (!player.isCreative()) {
                        itemstack.shrink(1);
                    }
                    if(server){
                        te.markDirty();
                    }
                    return ActionResultType.SUCCESS;
                }
            }
            //pop item
            else if (flag3) {
                if(server){
                    ItemStack it = te.removeStackFromSlot(0);
                    BlockPos newpos = pos.add(state.get(FACING).getDirectionVec());
                    ItemEntity drop = new ItemEntity(worldIn, newpos.getX() + 0.5, newpos.getY() + 0.5, newpos.getZ() + 0.5, it);
                    worldIn.addEntity(drop);
                    te.markDirty();
                }
                return ActionResultType.SUCCESS;
            }
            //open gui
            else if (player instanceof ServerPlayerEntity) {
                //this.getContainer ?
                TileEntity te1 = worldIn.getTileEntity(pos);
                if (te1 instanceof NoticeBoardBlockTile) {
                    player.openContainer((INamedContainerProvider)tileentity);
                }
                return ActionResultType.SUCCESS;
            }

        } else {
            return ActionResultType.PASS;
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public INamedContainerProvider getContainer(BlockState state, World worldIn, BlockPos pos) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        return tileEntity instanceof INamedContainerProvider ? (INamedContainerProvider) tileEntity : null;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new NoticeBoardBlockTile();
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (stack.hasDisplayName()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity instanceof NoticeBoardBlockTile) {
                ((NoticeBoardBlockTile) tileentity).setCustomName(stack.getDisplayName());
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean moving) {
        //for culling text
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof NoticeBoardBlockTile) {
            ((NoticeBoardBlockTile)te).updateTextVisibility(state, world, pos, fromPos);
        }
        super.neighborChanged(state, world, pos, neighborBlock, fromPos, moving);
    }

    @Override
    public boolean eventReceived(BlockState state, World world, BlockPos pos, int eventID, int eventParam) {
        super.eventReceived(state, world, pos, eventID, eventParam);
        TileEntity tileentity = world.getTileEntity(pos);
        return tileentity != null && tileentity.receiveClientEvent(eventID, eventParam);
    }

    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity tileentity = world.getTileEntity(pos);
            if (tileentity instanceof NoticeBoardBlockTile) {
                InventoryHelper.dropInventoryItems(world, pos, (NoticeBoardBlockTile) tileentity);
                world.updateComparatorOutputLevel(pos, this);
            }
            super.onReplaced(state, world, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(BlockState blockState, World world, BlockPos pos) {
        TileEntity tileentity = world.getTileEntity(pos);
        if (tileentity instanceof NoticeBoardBlockTile)
            return Container.calcRedstoneFromInventory((NoticeBoardBlockTile) tileentity);
        else
            return 0;
    }
}