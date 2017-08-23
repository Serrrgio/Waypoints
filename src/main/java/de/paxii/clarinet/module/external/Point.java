package de.paxii.clarinet.module.external;

public class Point {
    //    public final String name;
    public String dimension;
    public int x;
    public int y;
    public int z;
    public boolean enable;
    public int color;
    public String severIP;

    public Point(String deathWorld, int x, int y, int z, boolean b, int i, String s ) {
        this.dimension = deathWorld;
        this.x = x;
        this.y = y;
        this.z = z;
        this.enable = b;
        this.color = i;
        this.severIP = s;
    }
}
