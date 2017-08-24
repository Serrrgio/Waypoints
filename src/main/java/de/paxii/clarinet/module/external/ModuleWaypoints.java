package de.paxii.clarinet.module.external;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.sun.org.apache.bcel.internal.generic.POP;
import de.paxii.clarinet.Wrapper;
import de.paxii.clarinet.event.EventHandler;
import de.paxii.clarinet.event.EventPriority;
import de.paxii.clarinet.event.events.game.RenderTickEvent;
import de.paxii.clarinet.event.events.game.StopGameEvent;
import de.paxii.clarinet.event.events.gui.DisplayGuiScreenEvent;
import de.paxii.clarinet.function.ThrowingStringFunction;
import de.paxii.clarinet.module.Module;
import de.paxii.clarinet.module.ModuleCategory;
import de.paxii.clarinet.util.chat.Chat;
import de.paxii.clarinet.util.render.GL11Helper;
import de.paxii.clarinet.util.settings.ClientSettings;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ModuleWaypoints extends Module {
    BlockPos deathPos = null;
    String deathWorld = "";


    public HashMap<String, Point> points;
//    public static Point points;

    public ModuleWaypoints() {
        super("Waypoints", ModuleCategory.RENDER);

        this.setVersion("1.0");
        this.setBuildVersion(16000);
        this.setRegistered(true);
        this.setCommand(true);
        this.setSyntax("waypoint <remove>|<add>");
        this.setEnabled(true);

        loadPoints();
    }

    @Override
    public void onEnable() {
        Wrapper.getEventManager().register(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDisplayGuiScreen(DisplayGuiScreenEvent event) {
        if (event.getGuiScreen() instanceof GuiGameOver) {
            deathPos = Wrapper.getPlayer().getPosition();
            deathWorld = Wrapper.getWorld().provider.getDimensionType().getName();
            setPoint("death", deathPos, 0xFF00FF);
        }
    }

    @Override
    public void onCommand(String[] args) {
        deathPos = Wrapper.getPlayer().getPosition();
        if (args.length < 3) {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                // list waypoints
                Chat.printClientMessage("List of waypoints:");
                for (Map.Entry entry : points.entrySet()) {
                    Point val = (Point) entry.getValue();
                    if (getServerIP().equals(val.severIP)) {
                        Chat.printClientMessage(String.format("%s at %d %d %d (%s).", entry.getKey(), val.x, val.y, val.z, val.enable ? "enable" : "disable"));
                    }
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (args.length == 2) {
                    if (points.get(args[1]) != null) {
                        points.remove(args[1]);
                        Chat.printClientMessage("Waypoint " + args[1] + " remove!");
                    } else {
                        Chat.printClientMessage(String.format("Waypoint %s not found.", args[1]));
                    }
                } else {
                    Chat.printClientMessage("Use: #waypoints remove <waypoint name>");
                }
            } else if (args[0].equalsIgnoreCase("enable")) {
                if (args.length == 2) {
                    if (points.get(args[1]) != null) {
                        Point editP = points.get(args[1]);
                        BlockPos editB = new BlockPos(editP.x, editP.y, editP.z);
                        setPoint(args[1], editB, true, editP.color);
                        Chat.printClientMessage("Waypoint " + args[1] + " enable!");
                    } else {
                        Chat.printClientMessage(String.format("Waypoint %s not found.", args[1]));
                    }
                } else {
                    Chat.printClientMessage("Use: #waypoints enable <waypoint name>");
                }
            } else if (args[0].equalsIgnoreCase("disable")) {
                if (args.length == 2) {
                    if (points.get(args[1]) != null) {
                        Point editP = points.get(args[1]);
                        BlockPos editB = new BlockPos(editP.x, editP.y, editP.z);
                        setPoint(args[1], editB, false, editP.color);
                        Chat.printClientMessage("Waypoint " + args[1] + " disable!");
                    } else {
                        Chat.printClientMessage(String.format("Waypoint %s not found.", args[1]));
                    }
                } else {
                    Chat.printClientMessage("Use: #waypoints disable <waypoint name>");
                }
            } else if (args[0].equalsIgnoreCase("add")) {
                String pName = "";
                if (args.length == 2) {
                    pName = args[1];
                    if (points.get(args[1]) != null) {
                        pName = "";
                        Chat.printClientMessage("Use: Waypoint " + args[1] + " already exists (can on another server).");
                    } else {
                        pName = args[1];
                    }
                } else {
                    int i = 1;
                    while (points.get("point" + i) != null) {
                        i++;
                    }
                    pName = "point" + i;
                }
                if (!pName.equals("")) setPoint(pName, deathPos);
                Chat.printClientMessage(String.format("Waypoint %s add at %d %d %d.", pName, deathPos.getX(), deathPos.getY(), deathPos.getZ()));
            } else {
                Chat.printClientMessage("Unknown subcommand!");
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("addpos")) {
            deathPos = new BlockPos(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            setPoint(args[0], deathPos);
            Chat.printClientMessage(String.format("Waypoint %s add at %d %d %d (server IP %s).", args[0], deathPos.getX(), deathPos.getY(), deathPos.getZ(), getServerIP()));
        } else {
            Chat.printClientMessage("Too few arguments!");
        }
    }

    private void setPoint (String name, BlockPos pos, boolean enable) {
        points.put(name, new Point(Wrapper.getWorld().provider.getDimensionType().getName(), pos.getX(), pos.getY(), pos.getZ(), enable, 0xFFFF00, getServerIP()));
    }

    private void setPoint (String name, BlockPos pos) {
        points.put(name, new Point(Wrapper.getWorld().provider.getDimensionType().getName(), pos.getX(), pos.getY(), pos.getZ(), true, 0xFFFF00, getServerIP()));
    }

    private void setPoint (String name, BlockPos pos, int color) {
        points.put(name, new Point(Wrapper.getWorld().provider.getDimensionType().getName(), pos.getX(), pos.getY(), pos.getZ(), true, color, getServerIP()));
    }

    private void setPoint (String name, BlockPos pos, boolean enable, int color) {
        points.put(name, new Point(Wrapper.getWorld().provider.getDimensionType().getName(), pos.getX(), pos.getY(), pos.getZ(), enable, color, getServerIP()));
    }

    private String getServerIP() {

        return Wrapper.getMinecraft().getCurrentServerData().serverIP;
    }

    @EventHandler
    public void onGlobalRender(RenderTickEvent renderTickEvent) {
        try {
            this.setup(true);
            this.renderWaypoints(renderTickEvent.getRenderPartialTicks());
            this.setup(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderWaypoints(float partialTicks) throws Exception {
        RenderManager renderManager = Wrapper.getMinecraft().getRenderManager();

        Method orientCamera = EntityRenderer.class.getDeclaredMethod("orientCamera", float.class);
        orientCamera.setAccessible(true);
        orientCamera.invoke(Wrapper.getRenderer(), partialTicks);

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
        for (Point value : points.values()) {
            this.renderWaypointsForEntity(startPoint, value);
        }
    }

    private void renderWaypointsForEntity(Vec3d startingPoint, Point curP) {
        if (!(Wrapper.getMinecraft().currentScreen instanceof GuiContainer)) {
            if (curP != null) {
                if (curP.enable && Wrapper.getWorld().provider.getDimensionType().getName().equals(curP.dimension) && getServerIP().equals(curP.severIP)) {
                    Vec3d endPoint = new Vec3d(curP.x, curP.y, curP.z);
                    this.drawLine(startingPoint, endPoint, new Color(curP.color));
                }
            }
        }
    }

    private void drawLine(Vec3d start, Vec3d end, Color lineColor) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexbuffer = tessellator.getBuffer();
        vertexbuffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos(start.x, start.y, start.z).color(
                lineColor.getRed(),
                lineColor.getGreen(),
                lineColor.getBlue(),
                lineColor.getAlpha()
        ).endVertex();
        vertexbuffer.pos(end.x, end.y, end.z).color(
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
    public void onDisable() {
        Wrapper.getEventManager().unregister(this);
    }

    @EventHandler
    public void onStopGame(StopGameEvent event) {
        savePoints();
    }

    private void savePoints() {
//        this.waypointsSaver.saveWaypoints(points);
        File settingFolder = new File(ClientSettings.getClientFolderPath().getValue() + "/");
        File settingsFile = new File(settingFolder, "Waypoints.json");
        settingsFile.delete();

        Gson gson = new Gson();

        String jsonString = gson.toJson(points);

        try {
            settingsFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            PrintWriter pw = new PrintWriter(settingsFile.getAbsolutePath());

            pw.print(jsonString);

            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadPoints() {
        points = new HashMap<String, Point>();
//        points = this.waypointsSaver.loadWaypoints();
        File settingFolder = new File(ClientSettings.getClientFolderPath().getValue() + "/");
        Gson gson = new Gson();
        File settingsFile = new File(settingFolder, "Waypoints.json");
        String jsonString = "";
        try {
            Scanner sc = new Scanner(settingsFile);

            while (sc.hasNextLine()) {
                jsonString += sc.nextLine();
            }
//            Chat.printClientMessage("***jsonString: " + jsonString);
            try {
//                                macro = gson.fromJson(jsonString, Macro.class);
                points = gson.fromJson(jsonString, new TypeToken<HashMap<String, Point>>() {
                }.getType());
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }

            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
