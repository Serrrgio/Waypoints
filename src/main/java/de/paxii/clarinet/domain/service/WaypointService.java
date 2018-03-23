package de.paxii.clarinet.domain.service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import de.paxii.clarinet.Wrapper;
import de.paxii.clarinet.domain.model.Point;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class WaypointService {

  private ArrayList<Point> waypointList = new ArrayList<>(10);

  public boolean addWaypoint(String name, BlockPos blockPos) {
    return this.addWaypoint(name, blockPos, 0xffff00);
  }

  public boolean addWaypoint(String name, BlockPos blockPos, int color) {
    return this.addWaypoint(name, blockPos, true, color);
  }

  public boolean addWaypoint(String name, BlockPos blockPos, boolean enabled, int color) {
    return this.addWaypoint(
            name,
            Wrapper.getWorld().provider.getDimensionType().getName(),
            blockPos,
            enabled,
            color
    );
  }

  public boolean addWaypoint(String name, String world, BlockPos blockPos, boolean enabled, int color) {
    if (!this.doesWaypointExist(name)) {
      this.waypointList.add(new Point(
              name,
              world,
              blockPos.getX(),
              blockPos.getY(),
              blockPos.getZ(),
              enabled,
              color,
              this.getServerIP()
      ));

      return true;
    }

    return false;
  }

  public Point getWaypoint(String name) {
    return this.getWaypoints().stream()
            .filter(w -> w.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
  }

  public boolean removeWaypoint(String name) {
    return this.getWaypoints().removeIf(w -> w.getName().equalsIgnoreCase(name));
  }

  public boolean doesWaypointExist(String name) {
    return this.getWaypoints().stream()
            .anyMatch(w -> w.getName().equalsIgnoreCase(name));
  }

  public ArrayList<Point> getWaypoints() {
    return this.waypointList.stream()
            .filter(w -> w.getServerIP().equals(this.getServerIP()))
            .collect(Collectors.toCollection(ArrayList::new));
  }

  public String savePoints() {
    Gson gson = new Gson();

    return gson.toJson(this.waypointList);
  }

  public void loadPoints(String settingString) throws JsonParseException {
    Gson gson = new Gson();

    if (settingString != null && !settingString.trim().isEmpty()) {
      this.waypointList = gson.fromJson(settingString, new TypeToken<ArrayList<Point>>() {
      }.getType());
    }
  }

  private String getServerIP() {
    return Wrapper.getMinecraft().getCurrentServerData() != null ?
            Wrapper.getMinecraft().getCurrentServerData().serverIP.toLowerCase() : "localhost";
  }
}
