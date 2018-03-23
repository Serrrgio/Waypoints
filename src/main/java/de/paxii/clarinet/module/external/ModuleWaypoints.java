package de.paxii.clarinet.module.external;

import com.google.gson.JsonParseException;

import de.paxii.clarinet.Wrapper;
import de.paxii.clarinet.domain.model.Point;
import de.paxii.clarinet.domain.service.WaypointService;
import de.paxii.clarinet.event.EventHandler;
import de.paxii.clarinet.event.events.game.RenderTickEvent;
import de.paxii.clarinet.event.events.gui.DisplayGuiScreenEvent;
import de.paxii.clarinet.function.ThrowingStringFunction;
import de.paxii.clarinet.module.Module;
import de.paxii.clarinet.module.ModuleCategory;
import de.paxii.clarinet.util.chat.Chat;
import de.paxii.clarinet.util.render.GL11Helper;
import de.paxii.clarinet.util.settings.type.ClientSettingString;

import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ModuleWaypoints extends Module {

  private WaypointService waypointService;

  public ModuleWaypoints() {
    super("Waypoints", ModuleCategory.RENDER);

    this.setVersion("1.0");
    this.setBuildVersion(16000);
    this.setDisplayedInGui(false);
    this.setCommand(true);
    this.setRegistered(true);
    this.setEnabled(true);
    this.setSyntax("waypoints <list/add/addpos/enabled/disable/remove>");
    this.setDescription("Waypoints");

    this.waypointService = new WaypointService();
  }

  @Override
  public void onStartup() {
    try {
      String waypointSetting = this.getValueOrDefault("waypoints", String.class, null);
      this.waypointService.loadPoints(waypointSetting);
    } catch (JsonParseException exception) {
      exception.printStackTrace();
    }
  }

  @EventHandler
  public void onDisplayGuiScreen(DisplayGuiScreenEvent event) {
    if (event.getGuiScreen() instanceof GuiGameOver) {
      this.waypointService.addWaypoint("death", Wrapper.getPlayer().getPosition(), 0xff0000);
    }
  }

  @Override
  public void onCommand(String[] args) {
    BlockPos currentPosition = Wrapper.getPlayer().getPosition();

    if (args.length > 0) {
      if (args[0].equalsIgnoreCase("list")) {
        ArrayList<Point> currentWaypoints = this.waypointService.getWaypoints();
        if (!currentWaypoints.isEmpty()) {
          this.sendClientMessage("List of waypoints:");
          currentWaypoints.forEach(waypoint -> Chat.printChatMessage(String.format("%s at %d %d %d (%s).",
                  waypoint.getName(),
                  waypoint.getX(),
                  waypoint.getY(),
                  waypoint.getZ(),
                  waypoint.isEnabled() ? "enabled" : "disable"
          )));
        } else {
          this.sendClientMessage("No Waypoints available for this server.");
        }
      } else if (args[0].equalsIgnoreCase("add")) {
        String waypointName;
        if (args.length >= 2) {
          waypointName = args[1];
        } else {
          int i = 0;
          for (; this.waypointService.doesWaypointExist("Waypoint " + i); i++) {
          }
          waypointName = "Waypoint " + i;
        }
        if (!waypointName.trim().isEmpty()) {
          // TODO: Add Support for colors
          if (this.waypointService.addWaypoint(waypointName, currentPosition)) {
            this.sendClientMessage(String.format("Waypoint %s was added at %d %d %d.",
                    waypointName,
                    currentPosition.getX(),
                    currentPosition.getY(),
                    currentPosition.getZ()
            ));
          } else {
            this.sendClientMessage(String.format("Waypoint %s does already exist.", waypointName));
          }
        }
      } else if (args[0].equalsIgnoreCase("addpos")) {
        if (args.length >= 5) {
          currentPosition = new BlockPos(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
          this.waypointService.addWaypoint(args[1], currentPosition);
          this.sendClientMessage(String.format("Waypoint %s was added at %d %d %d.",
                  args[1],
                  currentPosition.getX(),
                  currentPosition.getY(),
                  currentPosition.getZ()
          ));
        } else {
          this.sendClientMessage("Usage: waypoints add <waypoint name>");
        }
      } else if (args[0].equalsIgnoreCase("remove")) {
        if (args.length >= 2) {
          if (this.waypointService.removeWaypoint(args[1])) {
            this.sendClientMessage("Waypoint " + args[1] + " was removed.");
          } else {
            this.sendClientMessage(String.format("Waypoint %s not found.", args[1]));
          }
        } else {
          this.sendClientMessage("Usage: waypoints remove <waypoint name>");
        }
      } else if (args[0].equalsIgnoreCase("enabled") || args[0].equalsIgnoreCase("disable")) {
        if (args.length >= 2) {
          Point waypoint = this.waypointService.getWaypoint(args[1]);
          if (waypoint != null) {
            waypoint.setEnabled(args[0].equalsIgnoreCase("enabled"));
            this.sendClientMessage(String.format("Waypoint %s was %s.", args[1], waypoint.isEnabled() ? "enabled" : "disabled"));
          } else {
            this.sendClientMessage(String.format("Waypoint %s not found.", args[1]));
          }
        } else {
          this.sendClientMessage("Usage: waypoints enabled <waypoint name>");
        }
      } else {
        this.sendClientMessage("Unknown subcommand!");
      }
    } else {
      this.sendClientMessage("Too few arguments!");
    }
  }

  @EventHandler
  public void onGlobalRender(RenderTickEvent renderTickEvent) {
    try {
      this.setup(true);
      this.renderWaypoints(renderTickEvent.getRenderPartialTicks());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      this.setup(false);
    }
  }

  private void renderWaypoints(float partialTicks) throws Exception {
    if (Wrapper.getMinecraft().currentScreen instanceof GuiContainer
            || this.waypointService.getWaypoints().isEmpty()) {
      return;
    }

    Method orientCamera = EntityRenderer.class.getDeclaredMethod("orientCamera", float.class);
    orientCamera.setAccessible(true);
    orientCamera.invoke(Wrapper.getRenderer(), partialTicks);

    RenderManager renderManager = Wrapper.getMinecraft().getRenderManager();
    ThrowingStringFunction<Double> renderPosition = (position) -> {
      Field renderPos = RenderManager.class.getDeclaredField("renderPos" + position);
      renderPos.setAccessible(true);
      return renderPos.getDouble(renderManager);
    };
    double[] startingPosition = new double[]{
            renderPosition.apply("X"), renderPosition.apply("Y"), renderPosition.apply("Z")
    };
    GL11.glTranslated(-startingPosition[0], -startingPosition[1], -startingPosition[2]);
    Vec3d startPoint = Wrapper.getPlayer().getLook(partialTicks)
            .addVector(0, Wrapper.getPlayer().getEyeHeight(), 0)
            .addVector(startingPosition[0], startingPosition[1], startingPosition[2]);

    this.waypointService.getWaypoints().stream()
            .filter(Point::isEnabled)
            .filter(w -> w.getWorld().equals(Wrapper.getWorld().provider.getDimensionType().getName()))
            .forEach(w -> this.renderWaypointsForEntity(startPoint, w));
  }

  private void renderWaypointsForEntity(Vec3d startingPoint, Point waypoint) {
    Vec3d endPoint = new Vec3d(waypoint.getX(), waypoint.getY(), waypoint.getZ());
    this.drawLine(startingPoint, endPoint, new Color(waypoint.getColor()));
  }

  private void drawLine(Vec3d start, Vec3d end, Color lineColor) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder vertexBuffer = tessellator.getBuffer();
    vertexBuffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
    vertexBuffer.pos(start.x, start.y, start.z).color(
            lineColor.getRed(),
            lineColor.getGreen(),
            lineColor.getBlue(),
            lineColor.getAlpha()
    ).endVertex();
    vertexBuffer.pos(end.x, end.y, end.z).color(
            lineColor.getRed(),
            lineColor.getGreen(),
            lineColor.getBlue(),
            lineColor.getAlpha()
    ).endVertex();
    tessellator.draw();
  }

  private void setup(boolean enable) {
    if (enable) {
      GL11.glPushMatrix();
      GL11.glLoadIdentity();
      GL11Helper.enableDefaults();
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDepthMask(false);
      GlStateManager.depthMask(false);
      GlStateManager.disableTexture2D();
      GlStateManager.disableLighting();
      GlStateManager.disableCull();
      GlStateManager.disableBlend();
      GL11.glLineWidth(1.0F);
    } else {
      GL11Helper.disableDefaults();
      GlStateManager.enableTexture2D();
      GlStateManager.enableLighting();
      GlStateManager.enableCull();
      GlStateManager.disableBlend();
      GlStateManager.depthMask(true);
      GL11.glDepthMask(true);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_BLEND);
      GL11.glEnable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
      GL11.glPopMatrix();
    }
  }

  @Override
  public void onShutdown() {
    this.getModuleSettings().put(
            "waypoints", new ClientSettingString("waypoints", this.waypointService.savePoints())
    );
  }

}
