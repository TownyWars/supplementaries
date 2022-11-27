package net.mehvahdjukaar.supplementaries.common.block.blocks;

import com.google.common.collect.Maps;
import net.mehvahdjukaar.moonlight.api.client.util.ParticleUtil;
import net.mehvahdjukaar.supplementaries.common.block.ModBlockProperties;
import net.mehvahdjukaar.supplementaries.common.block.tiles.EndermanSkullBlockTile;
import net.mehvahdjukaar.supplementaries.common.utils.BlockUtil;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.Map;

public class EndermanSkullWallBlock extends WallSkullBlock {

    public static final BooleanProperty WATCHED = ModBlockProperties.WATCHED;
    private static final Map<Direction, VoxelShape> AABBS_ANGERY = Maps.newEnumMap(
            Map.of(Direction.NORTH, Block.box(4.0, 4.0, 8.0, 12.0, 12.0 + 6, 16.0),
                    Direction.SOUTH, Block.box(4.0, 4.0, 0.0, 12.0, 12.0 + 6, 8.0),
                    Direction.EAST, Block.box(0.0, 4.0, 4.0, 8.0, 12.0 + 6, 12.0),
                    Direction.WEST, Block.box(8.0, 4.0, 4.0, 16.0, 12.0 + 6, 12.0)));

    public EndermanSkullWallBlock(Properties properties) {
        super(EndermanSkullBlock.TYPE, properties);
        this.registerDefaultState(this.defaultBlockState().setValue(WATCHED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATCHED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(WATCHED) ? AABBS_ANGERY.get(state.getValue(FACING)) : super.getShape(state, level, pos, context);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return BlockUtil.getTicker(type, ModRegistry.ENDERMAN_SKULL_TILE.get(), EndermanSkullBlockTile::tick);
    }

    @Override
    public boolean isSignalSource(@Nonnull BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side) {
        return blockState.getValue(WATCHED) ? 15 : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EndermanSkullBlockTile(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if(state.getValue(WATCHED)) {
            ParticleUtil.spawnParticleOnBlockShape(level, pos, ParticleTypes.PORTAL, UniformInt.of(1, 2),
                    0.5f);
        }
    }
}