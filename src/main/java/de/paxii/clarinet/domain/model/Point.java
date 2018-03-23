package de.paxii.clarinet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Point {
  private String name;
  private final String world;
  private int x;
  private int y;
  private int z;
  private boolean enabled;
  private int color;
  private String serverIP;
}
