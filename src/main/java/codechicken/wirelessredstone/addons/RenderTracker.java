package codechicken.wirelessredstone.addons;

import static codechicken.lib.math.MathHelper.sin;
import static codechicken.lib.math.MathHelper.todeg;

import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import codechicken.core.ClientUtils;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCModelLibrary;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.TextureUtils;
import codechicken.lib.render.uv.IconTransformation;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.SwapYZ;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.wirelessredstone.core.RedstoneEther;

public class RenderTracker extends RenderEntity implements IItemRenderer {

    private static final CCModel model;
    private static final Vector3 Y_AXIS = new Vector3(0, 1, 0);

    static {
        model = CCModel.parseObjModels(new ResourceLocation("wrcbe_addons", "models/tracker.obj"), 7, new SwapYZ())
                .get("Tracker");
        model.apply(new Translation(0, 0.1875, 0));
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    public void renderTracker(int freq) {
        GL11.glDisable(GL11.GL_LIGHTING);

        TextureUtils.bindAtlas(0);
        final CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        state.startDrawingInstance(7);
        state.setColourInstance(0xFFFFFFFF);
        model.render(new IconTransformation(Blocks.obsidian.getIcon(0, 0)));
        state.drawInstance();

        Matrix4 pearlMat = CCModelLibrary.getRenderMatrix(
                0,
                0.44 + RedstoneEther.getSineWave(ClientUtils.getRenderTime(), 7) * 0.02,
                0,
                new Rotation(RedstoneEther.getRotation(ClientUtils.getRenderTime(), freq), Y_AXIS),
                0.04);

        CCRenderState.changeTexture("wrcbe_core:textures/hedronmap.png");
        state.startDrawingInstance(4);
        state.setColourInstance(freq == 0 ? 0xC0C0C0FF : 0xFFFFFFFF);
        CCModelLibrary.icosahedron4.render(pearlMat);
        state.drawInstance();

        GL11.glEnable(GL11.GL_LIGHTING);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float f, float f1) {
        GL11.glPushMatrix();

        EntityWirelessTracker tracker = (EntityWirelessTracker) entity;
        if (tracker.isAttachedToEntity()) {

            Vector3 relVec = tracker.getRotatedAttachment();

            final double posX = tracker.attachedEntity.lastTickPosX
                    + (tracker.attachedEntity.posX - tracker.attachedEntity.lastTickPosX) * f1
                    + relVec.x
                    - RenderManager.renderPosX;
            final double posY = tracker.attachedEntity.lastTickPosY
                    + (tracker.attachedEntity.posY - tracker.attachedEntity.lastTickPosY) * f1
                    + tracker.attachedEntity.height / 2
                    - tracker.attachedEntity.yOffset
                    - tracker.height
                    + relVec.y
                    - RenderManager.renderPosY;
            final double posZ = tracker.attachedEntity.lastTickPosZ
                    + (tracker.attachedEntity.posZ - tracker.attachedEntity.lastTickPosZ) * f1
                    + relVec.z
                    - RenderManager.renderPosZ;

            GL11.glTranslated(posX, posY, posZ);

            final double axisX = -relVec.z;
            final double axisY = 0;
            final double axisZ = relVec.x;
            // leaving this code commented for understand,
            // but it can be simplified to what is below for speed
            // Vector3 yAxis = new Vector3(0, 1, 0);
            // double angle = -(relVec.angle(yAxis) * todeg);
            final double angle = -(Math.acos(relVec.normalize().y) * todeg);
            GL11.glRotatef((float) angle, (float) axisX, (float) axisY, (float) axisZ);

        } else if (tracker.item) {
            GL11.glTranslated(x, y + 0.2, z);
            double bob = sin(ClientUtils.getRenderTime() / 10) * 0.1;
            double rotate = ClientUtils.getRenderTime() / 20 * todeg;

            GL11.glRotatef((float) rotate, 0, 1, 0);
            GL11.glTranslated(0, bob + 0.2, 0);
        }
        GL11.glTranslated(0, -0.2, 0);
        renderTracker(tracker.freq);
        GL11.glPopMatrix();
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        switch (type) {
            case ENTITY:
                GL11.glScaled(1.9, 1.9, 1.9);
                renderTracker(item.getItemDamage());
                break;
            case EQUIPPED:
            case EQUIPPED_FIRST_PERSON:
                GL11.glPushMatrix();
                GL11.glTranslated(0.4, 0.3, 0.5);
                GL11.glScaled(2, 2, 2);
                renderTracker(item.getItemDamage());
                GL11.glPopMatrix();
                break;
            case INVENTORY:
                GL11.glTranslated(0, -0.7, 0);
                GL11.glScalef(2, 2, 2);
                renderTracker(item.getItemDamage());
                break;
        }
    }
}
