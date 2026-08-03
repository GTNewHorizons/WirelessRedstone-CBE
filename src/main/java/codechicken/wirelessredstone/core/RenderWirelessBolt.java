package codechicken.wirelessredstone.core;

import java.util.ArrayList;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.wirelessredstone.core.WirelessBolt.Segment;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderWirelessBolt {

    private static final Frustrum frustum = new Frustrum();

    private static final ResourceLocation glowstoneTexture = new ResourceLocation(
            "wrcbe_core:textures/lightning_glowstone.png");
    private static final ResourceLocation redstoneTexture = new ResourceLocation(
            "wrcbe_core:textures/lightning_redstone.png");

    public static void render(float frame, Entity entity) {
        ArrayList<WirelessBolt> bolts = WirelessBolt.clientboltlist;
        if (bolts.isEmpty()) return;

        double camX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * frame;
        double camY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * frame;
        double camZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * frame;

        ClippingHelperImpl.getInstance();
        frustum.setPosition(camX, camY, camZ);

        double playerX = entity.posX;
        double playerY = entity.posY + entity.getEyeHeight();
        double playerZ = entity.posZ;

        GL11.glPushMatrix();
        RenderUtils.translateToWorldCoords(entity, frame);

        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        final CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        state.setBrightnessInstance(0xF000F0);
        state.changeTexture(glowstoneTexture);
        state.startDrawingInstance(7);
        for (int i = 0; i < bolts.size(); i++) {
            WirelessBolt bolt = bolts.get(i);
            BoltRender cache = bolt.boltCache;
            if (cache == null) bolt.boltCache = cache = BoltRender.create(bolt);
            if (isVisible(bolt)) renderBolt(bolt, cache, 0, playerX, playerY, playerZ);
        }
        state.drawInstance();

        state.changeTexture(redstoneTexture);
        state.startDrawingInstance(7);
        for (int i = 0; i < bolts.size(); i++) {
            WirelessBolt bolt = bolts.get(i);
            BoltRender cache = bolt.boltCache;
            if (cache == null) bolt.boltCache = cache = BoltRender.create(bolt);
            if (isVisible(bolt)) renderBolt(bolt, cache, 1, playerX, playerY, playerZ);
        }
        state.drawInstance();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);

        GL11.glPopMatrix();
    }

    private static boolean isVisible(WirelessBolt bolt) {
        AxisAlignedBB box = bolt.boundingBox;
        double margin = bolt.length;
        return frustum.isBoxInFrustum(
                box.minX - margin,
                box.minY - margin,
                box.minZ - margin,
                box.maxX + margin,
                box.maxY + margin,
                box.maxZ + margin);
    }

    private static void renderBolt(WirelessBolt bolt, BoltRender r, int pass, double camX, double camY, double camZ) {
        Tessellator t = Tessellator.instance;
        float boltage = bolt.particleAge < 0 ? 0 : (float) bolt.particleAge / (float) bolt.particleMaxAge;
        float mainalpha;
        if (pass == 0) mainalpha = (1 - boltage) * 0.4F;
        else mainalpha = 1 - boltage * 0.5F;

        int expandTime = (int) (bolt.length * WirelessBolt.speed);
        int renderstart = (int) ((expandTime / 2 - bolt.particleMaxAge + bolt.particleAge) / (float) (expandTime / 2)
                * bolt.numsegments0);
        int renderend = (int) ((bolt.particleAge + expandTime) / (float) expandTime * bolt.numsegments0);

        for (int i = 0; i < r.count; i++) {
            if (r.segmentNo[i] < renderstart || r.segmentNo[i] > renderend) continue;

            double startX = r.startX[i];
            double startY = r.startY[i];
            double startZ = r.startZ[i];

            double playerX = camX - startX;
            double playerY = camY - startY;
            double playerZ = camZ - startZ;

            double playerMag = Math.sqrt(playerX * playerX + playerY * playerY + playerZ * playerZ);
            double width = 0.025F * (playerMag / 5 + 1) * (1 + r.light[i]) * 0.5F;

            double prevDiffX = r.prevDiffX[i];
            double prevDiffY = r.prevDiffY[i];
            double prevDiffZ = r.prevDiffZ[i];

            double nextDiffX = r.nextDiffX[i];
            double nextDiffY = r.nextDiffY[i];
            double nextDiffZ = r.nextDiffZ[i];

            double crossPrevX = playerY * prevDiffZ - playerZ * prevDiffY;
            double crossPrevY = playerZ * prevDiffX - playerX * prevDiffZ;
            double crossPrevZ = playerX * prevDiffY - playerY * prevDiffX;
            double crossPrevMag = Math
                    .sqrt(crossPrevX * crossPrevX + crossPrevY * crossPrevY + crossPrevZ * crossPrevZ);
            crossPrevX /= crossPrevMag;
            crossPrevY /= crossPrevMag;
            crossPrevZ /= crossPrevMag;

            crossPrevX *= width / r.sinPrev[i];
            crossPrevY *= width / r.sinPrev[i];
            crossPrevZ *= width / r.sinPrev[i];

            double crossNextX = playerY * nextDiffZ - playerZ * nextDiffY;
            double crossNextY = playerZ * nextDiffX - playerX * nextDiffZ;
            double crossNextZ = playerX * nextDiffY - playerY * nextDiffX;
            double crossNextMag = Math
                    .sqrt(crossNextX * crossNextX + crossNextY * crossNextY + crossNextZ * crossNextZ);
            crossNextX /= crossNextMag;
            crossNextY /= crossNextMag;
            crossNextZ /= crossNextMag;

            crossNextX *= width / r.sinNext[i];
            crossNextY *= width / r.sinNext[i];
            crossNextZ *= width / r.sinNext[i];

            double endX = r.endX[i];
            double endY = r.endY[i];
            double endZ = r.endZ[i];

            t.setColorRGBA_F(1, 1, 1, mainalpha * r.light[i]);

            t.addVertexWithUV(endX - crossNextX, endY - crossNextY, endZ - crossNextZ, 0.5, 0);
            t.addVertexWithUV(startX - crossPrevX, startY - crossPrevY, startZ - crossPrevZ, 0.5, 0);
            t.addVertexWithUV(startX + crossPrevX, startY + crossPrevY, startZ + crossPrevZ, 0.5, 1);
            t.addVertexWithUV(endX + crossNextX, endY + crossNextY, endZ + crossNextZ, 0.5, 1);

            if (r.hasNext[i] == 0) {
                double roundEndX = endX + r.diffX[i] * width;
                double roundEndY = endY + r.diffY[i] * width;
                double roundEndZ = endZ + r.diffZ[i] * width;

                t.addVertexWithUV(roundEndX - crossNextX, roundEndY - crossNextY, roundEndZ - crossNextZ, 0, 0);
                t.addVertexWithUV(endX - crossNextX, endY - crossNextY, endZ - crossNextZ, 0.5, 0);
                t.addVertexWithUV(endX + crossNextX, endY + crossNextY, endZ + crossNextZ, 0.5, 1);
                t.addVertexWithUV(roundEndX + crossNextX, roundEndY + crossNextY, roundEndZ + crossNextZ, 0, 1);
            }

            if (r.hasPrev[i] == 0) {
                double roundEndX = startX - r.diffX[i] * width;
                double roundEndY = startY - r.diffY[i] * width;
                double roundEndZ = startZ - r.diffZ[i] * width;

                t.addVertexWithUV(startX - crossPrevX, startY - crossPrevY, startZ - crossPrevZ, 0.5, 0);
                t.addVertexWithUV(roundEndX - crossPrevX, roundEndY - crossPrevY, roundEndZ - crossPrevZ, 0, 0);
                t.addVertexWithUV(roundEndX + crossPrevX, roundEndY + crossPrevY, roundEndZ + crossPrevZ, 0, 1);
                t.addVertexWithUV(startX + crossPrevX, startY + crossPrevY, startZ + crossPrevZ, 0.5, 1);
            }
        }
    }

    static class BoltRender {

        final int count;
        final double[] startX;
        final double[] startY;
        final double[] startZ;
        final double[] endX;
        final double[] endY;
        final double[] endZ;
        final double[] prevDiffX;
        final double[] prevDiffY;
        final double[] prevDiffZ;
        final double[] nextDiffX;
        final double[] nextDiffY;
        final double[] nextDiffZ;
        final double[] diffX;
        final double[] diffY;
        final double[] diffZ;
        final float[] sinPrev;
        final float[] sinNext;
        final float[] light;
        final int[] hasPrev;
        final int[] hasNext;
        final int[] segmentNo;

        BoltRender(WirelessBolt bolt) {
            ArrayList<Segment> segments = bolt.segments;
            count = segments.size();

            startX = new double[count];
            startY = new double[count];
            startZ = new double[count];
            endX = new double[count];
            endY = new double[count];
            endZ = new double[count];
            prevDiffX = new double[count];
            prevDiffY = new double[count];
            prevDiffZ = new double[count];
            nextDiffX = new double[count];
            nextDiffY = new double[count];
            nextDiffZ = new double[count];
            diffX = new double[count];
            diffY = new double[count];
            diffZ = new double[count];
            sinPrev = new float[count];
            sinNext = new float[count];
            light = new float[count];
            hasPrev = new int[count];
            hasNext = new int[count];
            segmentNo = new int[count];

            for (int i = 0; i < count; i++) {
                Segment s = segments.get(i);

                startX[i] = s.startpoint.point.x;
                startY[i] = s.startpoint.point.y;
                startZ[i] = s.startpoint.point.z;
                endX[i] = s.endpoint.point.x;
                endY[i] = s.endpoint.point.y;
                endZ[i] = s.endpoint.point.z;

                prevDiffX[i] = s.prevdiff.x;
                prevDiffY[i] = s.prevdiff.y;
                prevDiffZ[i] = s.prevdiff.z;
                nextDiffX[i] = s.nextdiff.x;
                nextDiffY[i] = s.nextdiff.y;
                nextDiffZ[i] = s.nextdiff.z;

                double rawDiffX = s.diff.x;
                double rawDiffY = s.diff.y;
                double rawDiffZ = s.diff.z;
                double rawDiffMag = Math.sqrt(rawDiffX * rawDiffX + rawDiffY * rawDiffY + rawDiffZ * rawDiffZ);
                diffX[i] = rawDiffX / rawDiffMag;
                diffY[i] = rawDiffY / rawDiffMag;
                diffZ[i] = rawDiffZ / rawDiffMag;

                sinPrev[i] = s.sinprev;
                sinNext[i] = s.sinnext;
                light[i] = s.light;
                hasPrev[i] = s.prev == null ? 0 : 1;
                hasNext[i] = s.next == null ? 0 : 1;
                segmentNo[i] = s.segmentno;
            }
        }

        static BoltRender create(WirelessBolt bolt) {
            return new BoltRender(bolt);
        }
    }
}
