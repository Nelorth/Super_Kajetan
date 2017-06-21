package model;

import util.ImageUtil;
import util.Point;

import java.awt.geom.Rectangle2D;
import java.io.IOException;

public class Player extends Entity {
    private final double PLAYER_WIDTH = 100;
    private final double PLAYER_HEIGHT = 300;

    public Player(Point position) {
        this.position = position;
        hitbox = new Rectangle2D.Double(position.getX() - PLAYER_WIDTH / 2, position.getY() - PLAYER_HEIGHT, PLAYER_WIDTH, PLAYER_HEIGHT);
    }

    /**
     * Getter-Methode für die aktuelle Spielerposition
     *
     * @return Position des Spielers als Point
     */
    public Point getPosition() {
        return position;
    }

    /**
     * lädt Bild aus images/...
     */
    public void loadImage() {
        try {
            ImageUtil.getImage("images");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
