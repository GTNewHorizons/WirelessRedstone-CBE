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
            if (isVisible(bolt)) renderBolt(bolt, 0, playerX, playerY, playerZ);
        }
        state.drawInstance();

        state.changeTexture(redstoneTexture);
        state.startDrawingInstance(7);
        for (int i = 0; i < bolts.size(); i++) {
            WirelessBolt bolt = bolts.get(i);
            if (isVisible(bolt)) renderBolt(bolt, 1, playerX, playerY, playerZ);
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

    private static void renderBolt(WirelessBolt bolt, int pass, double camX, double camY, double camZ) {
        Tessellator t = Tessellator.instance;
        float boltage = bolt.particleAge < 0 ? 0 : (float) bolt.particleAge / (float) bolt.particleMaxAge;
        float mainalpha;
        if (pass == 0) mainalpha = (1 - boltage) * 0.4F;
        else mainalpha = 1 - boltage * 0.5F;

        int expandTime = (int) (bolt.length * WirelessBolt.speed);
        int renderstart = (int) ((expandTime / 2 - bolt.particleMaxAge + bolt.particleAge) / (float) (expandTime / 2)
                * bolt.numsegments0);
        int renderend = (int) ((bolt.particleAge + expandTime) / (float) expandTime * bolt.numsegments0);

        ArrayList<Segment> segments = bolt.segments;
        for (int i = 0; i < segments.size(); i++) {
            Segment rendersegment = segments.get(i);
            if (rendersegment.segmentno < renderstart || rendersegment.segmentno > renderend) continue;

            double startX = rendersegment.startpoint.point.x;
            double startY = rendersegment.startpoint.point.y;
            double startZ = rendersegment.startpoint.point.z;

            double playerX = camX - startX;
            double playerY = camY - startY;
            double playerZ = camZ - startZ;

            double playerMag = Math.sqrt(playerX * playerX + playerY * playerY + playerZ * playerZ);
            double width = 0.025F * (playerMag / 5 + 1) * (1 + rendersegment.light) * 0.5F;

            double prevDiffX = rendersegment.prevdiff.x;
            double prevDiffY = rendersegment.prevdiff.y;
            double prevDiffZ = rendersegment.prevdiff.z;

            double nextDiffX = rendersegment.nextdiff.x;
            double nextDiffY = rendersegment.nextdiff.y;
            double nextDiffZ = rendersegment.nextdiff.z;

            double crossPrevX = playerY * prevDiffZ - playerZ * prevDiffY;
            double crossPrevY = playerZ * prevDiffX - playerX * prevDiffZ;
            double crossPrevZ = playerX * prevDiffY - playerY * prevDiffX;
            double crossPrevMag = Math
                    .sqrt(crossPrevX * crossPrevX + crossPrevY * crossPrevY + crossPrevZ * crossPrevZ);
            crossPrevX /= crossPrevMag;
            crossPrevY /= crossPrevMag;
            crossPrevZ /= crossPrevMag;

            crossPrevX *= width / rendersegment.sinprev;
            crossPrevY *= width / rendersegment.sinprev;
            crossPrevZ *= width / rendersegment.sinprev;

            double crossNextX = playerY * nextDiffZ - playerZ * nextDiffY;
            double crossNextY = playerZ * nextDiffX - playerX * nextDiffZ;
            double crossNextZ = playerX * nextDiffY - playerY * nextDiffX;
            double crossNextMag = Math
                    .sqrt(crossNextX * crossNextX + crossNextY * crossNextY + crossNextZ * crossNextZ);
            crossNextX /= crossNextMag;
            crossNextY /= crossNextMag;
            crossNextZ /= crossNextMag;

            crossNextX *= width / rendersegment.sinnext;
            crossNextY *= width / rendersegment.sinnext;
            crossNextZ *= width / rendersegment.sinnext;

            double endX = rendersegment.endpoint.point.x;
            double endY = rendersegment.endpoint.point.y;
            double endZ = rendersegment.endpoint.point.z;

            t.setColorRGBA_F(1, 1, 1, mainalpha * rendersegment.light);

            t.addVertexWithUV(endX - crossNextX, endY - crossNextY, endZ - crossNextZ, 0.5, 0);
            t.addVertexWithUV(startX - crossPrevX, startY - crossPrevY, startZ - crossPrevZ, 0.5, 0);
            t.addVertexWithUV(startX + crossPrevX, startY + crossPrevY, startZ + crossPrevZ, 0.5, 1);
            t.addVertexWithUV(endX + crossNextX, endY + crossNextY, endZ + crossNextZ, 0.5, 1);

            if (rendersegment.next == null) {
                double diffX = rendersegment.diff.x;
                double diffY = rendersegment.diff.y;
                double diffZ = rendersegment.diff.z;
                double diffMag = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
                diffX /= diffMag;
                diffY /= diffMag;
                diffZ /= diffMag;

                double roundEndX = endX + diffX * width;
                double roundEndY = endY + diffY * width;
                double roundEndZ = endZ + diffZ * width;

                t.addVertexWithUV(roundEndX - crossNextX, roundEndY - crossNextY, roundEndZ - crossNextZ, 0, 0);
                t.addVertexWithUV(endX - crossNextX, endY - crossNextY, endZ - crossNextZ, 0.5, 0);
                t.addVertexWithUV(endX + crossNextX, endY + crossNextY, endZ + crossNextZ, 0.5, 1);
                t.addVertexWithUV(roundEndX + crossNextX, roundEndY + crossNextY, roundEndZ + crossNextZ, 0, 1);
            }

            if (rendersegment.prev == null) {
                double diffX = rendersegment.diff.x;
                double diffY = rendersegment.diff.y;
                double diffZ = rendersegment.diff.z;
                double diffMag = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
                diffX /= diffMag;
                diffY /= diffMag;
                diffZ /= diffMag;

                double roundEndX = startX - diffX * width;
                double roundEndY = startY - diffY * width;
                double roundEndZ = startZ - diffZ * width;

                t.addVertexWithUV(startX - crossPrevX, startY - crossPrevY, startZ - crossPrevZ, 0.5, 0);
                t.addVertexWithUV(roundEndX - crossPrevX, roundEndY - crossPrevY, roundEndZ - crossPrevZ, 0, 0);
                t.addVertexWithUV(roundEndX + crossPrevX, roundEndY + crossPrevY, roundEndZ + crossPrevZ, 0, 1);
                t.addVertexWithUV(startX + crossPrevX, startY + crossPrevY, startZ + crossPrevZ, 0.5, 1);
            }
        }
    }
}
