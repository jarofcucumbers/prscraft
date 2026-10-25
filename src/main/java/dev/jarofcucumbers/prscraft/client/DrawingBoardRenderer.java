package dev.jarofcucumbers.prscraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
/** Horizontal runs reduce a blank board to one quad per row; palette is shared with the editor. */
public final class DrawingBoardRenderer implements BlockEntityRenderer<DrawingBoardEntity>{
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"textures/block/chalk_surface.png");
    public DrawingBoardRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(DrawingBoardEntity board,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        byte[] pixels=board.pixels();pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(-board.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot()));
        var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));var matrix=pose.last();
        for(int y=0;y<128;y++)for(int x=0;x<128;){int start=x,color=pixels[y*128+x];while(x<128&&pixels[y*128+x]==color)x++;
            float left=-.4375f+start*.875f/128,right=-.4375f+x*.875f/128,top=.4375f-y*.875f/128,bottom=.4375f-(y+1)*.875f/128,z=.126f;
            int rgb=BoardPixels.color(color);
            consumer.addVertex(matrix,left,top,z).setColor(rgb).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,0,0,1);
            consumer.addVertex(matrix,right,top,z).setColor(rgb).setUv(1,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,0,0,1);
            consumer.addVertex(matrix,right,bottom,z).setColor(rgb).setUv(1,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,0,0,1);
            consumer.addVertex(matrix,left,bottom,z).setColor(rgb).setUv(0,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,0,0,1);
        }pose.popPose();
    }
}
