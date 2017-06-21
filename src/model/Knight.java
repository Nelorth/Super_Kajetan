package model;

import util.ImageUtil;
import util.Point;

import java.io.IOException;

public class Knight extends Enemy {
    public Knight(Point position) {
        this.position = position;
    }

    public void loadImage() {
        try {
            ImageUtil.getImage("images");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
