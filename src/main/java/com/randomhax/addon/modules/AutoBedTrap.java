package com.randomhax.addon.modules;

import com.randomhax.addon.RandomHax;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoBedTrap extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private enum Mode { Front, Look }

    private final Setting<Mode> mode = sg.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Where to place the bed.")
        .defaultValue(Mode.Front)
        .build());

    private final Setting<Double> frontDistance = sg.add(new DoubleSetting.Builder()
        .name("front-distance")
        .description("Blocks in front to try placing at when using Front mode.")
        .defaultValue(1.5)
        .min(0.5).sliderMax(5)
        .visible(() -> mode.get() == Mode.Front)
        .build());

    private final Setting<Double> lookRange = sg.add(new DoubleSetting.Builder()
        .name("look-range")
        .description("Raycast distance for Look mode.")
        .defaultValue(4.5)
        .min(1).sliderMax(6)
        .visible(() -> mode.get() == Mode.Look)
        .build());

    private final Setting<Boolean> autoExplode = sg.add(new BoolSetting.Builder()
        .name("auto-explode")
        .description("Right-click the placed bed immediately to detonate.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> cooldown = sg.add(new IntSetting.Builder()
        .name("cooldown-ticks")
        .description("Delay between attempts (in ticks).")
        .defaultValue(10)
        .min(1).sliderMax(40)
        .build());

    private final Setting<Boolean> requireUnsafeDims = sg.add(new BoolSetting.Builder()
        .name("require-nether-or-end")
        .description("Only run where beds explode (Nether/End).")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> onlyWhenGliding = sg.add(new BoolSetting.Builder()
        .name("only-when-gliding")
        .description("Only place/explode while you are elytra-gliding.")
        .defaultValue(false)
        .build());

    public AutoBedTrap() {
        super(RandomHax.CATEGORY, "AutoBedTrap", "Places and pops beds automatically (Nether/End).");
    }

    private int ticksSince;

    @Override
    public void onActivate() {
        ticksSince = cooldown.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (mc.player == null || mc.world == null) return;
        if (requireUnsafeDims.get() && !bedsExplodeHere()) return;
        if (onlyWhenGliding.get() && !mc.player.isGliding()) return;
        if (ticksSince < cooldown.get()) {
            ticksSince++;
            return;
        }
        FindItemResult bed = InvUtils.findInHotbar(
            Items.WHITE_BED, Items.RED_BED, Items.BLUE_BED, Items.BLACK_BED,
            Items.BROWN_BED, Items.CYAN_BED, Items.GRAY_BED, Items.GREEN_BED,
            Items.LIGHT_BLUE_BED, Items.LIGHT_GRAY_BED, Items.LIME_BED,
            Items.MAGENTA_BED, Items.ORANGE_BED, Items.PINK_BED, Items.PURPLE_BED, Items.YELLOW_BED
        );
        if (!bed.found()) return;

        BlockHitResult placeClick = switch (mode.get()) {
            case Front -> computeFrontPlaceClick(mc.player, frontDistance.get());
            case Look -> computeLookPlaceClick(lookRange.get());
        };
        if (placeClick == null) return;

        Direction facing = placeClick.getSide();
        if (facing == Direction.UP || facing == Direction.DOWN) {
            facing = playerHorizontalFacing(mc.player).getOpposite();
        }
        BlockPos basePos = placeClick.getBlockPos().offset(placeClick.getSide());
        BlockPos headPos = basePos.offset(facing);
        if (!canReplace(basePos) || !canReplace(headPos)) return;

        InvUtils.swap(bed.slot(), true);
        ActionResult ar = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, placeClick);
        if (!ar.isAccepted()) {
            InvUtils.swapBack();
            return;
        }
        ticksSince = 0;
        if (autoExplode.get()) {
            Vec3d hit = Vec3d.ofCenter(basePos);
            BlockHitResult explodeClick = new BlockHitResult(hit, facing, basePos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, explodeClick);
        }
        InvUtils.swapBack();
    }

    private boolean bedsExplodeHere() {
        return mc.world != null && !mc.world.getDimension().bedWorks();
    }

    private boolean canReplace(BlockPos pos) {
        BlockState s = mc.world.getBlockState(pos);
        return s.isAir();
    }

    private Direction playerHorizontalFacing(PlayerEntity p) {
        int dir = MathHelper.floor((p.getYaw() * 4.0f / 360.0f) + 0.5) & 3;
        return switch (dir) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    private BlockHitResult computeFrontPlaceClick(PlayerEntity p, double dist) {
        Direction face = playerHorizontalFacing(p);
        Vec3d eye = p.getEyePos();
        BlockPos support = BlockPos.ofFloored(eye.add(Vec3d.of(face.getVector()).multiply(dist))).down();
        if (!mc.world.getBlockState(support).isSolidBlock(mc.world, support)) return null;
        Vec3d hit = Vec3d.ofCenter(support).add(0, 0.51, 0);
        return new BlockHitResult(hit, Direction.UP, support, false);
    }

    private BlockHitResult computeLookPlaceClick(double range) {
        HitResult hr = mc.player.raycast(range, 0, false);
        if (!(hr instanceof BlockHitResult bhr)) return null;
        BlockPos support = bhr.getBlockPos();
        Direction side = bhr.getSide();
        if (!mc.world.getBlockState(support).isSolidBlock(mc.world, support)) {
            if (side == Direction.UP) {
                BlockPos below = support.down();
                if (!mc.world.getBlockState(below).isSolidBlock(mc.world, below)) return null;
                support = below;
                side = Direction.UP;
            } else {
                return null;
            }
        }
        return new BlockHitResult(bhr.getPos(), side, support, false);
    }
}
