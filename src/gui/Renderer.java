package gui;

import model.*;
import util.Constants;
import util.ImageUtil;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

class Renderer {
    private final int HEALTH_BAR_HEIGHT = 5;

    private final Level level;
    private final Camera camera;
    private final Player player;
    private final KeyHandler keyHandler;
    private final LevelView view;
    private final Stroke strichel;
    private final Color staminaBlue;

    Renderer(Level level, Camera camera, Player player, KeyHandler keyHandler, LevelView view) {
        this.level = level;
        this.camera = camera;
        this.player = player;
        this.keyHandler = keyHandler;
        this.view = view;
        strichel = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        staminaBlue = new Color(0, 130, 232);
    }

    void drawPlayer(Graphics2D g2) {
        try {
            BufferedImage image;
            image = ImageUtil.getImage(player.getImagePath());
            int playerX = (int) (player.getX() - image.getWidth() / 2 - camera.getX());
            int playerY = (int) (player.getY() - image.getHeight());
            if (player.getViewingDirection().equals(Direction.RIGHT))
                g2.drawImage(image, playerX, playerY, image.getWidth(), image.getHeight(), null);
            else
                g2.drawImage(image, playerX + image.getWidth(), playerY, -image.getWidth(), image.getHeight(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Color backup = g2.getColor();
        g2.setColor(Color.GREEN);
        int x = (int) (player.getHitbox().getX() - camera.getX());
        int y = (int) (player.getHitbox().getY() - HEALTH_BAR_HEIGHT - 5);
        g2.fillRect(x, y, (int) ((double) player.getHealth() / player.getMaxHealth() * player.getHitbox().getWidth()), HEALTH_BAR_HEIGHT);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, y, (int) player.getHitbox().getWidth(), HEALTH_BAR_HEIGHT);

        g2.setColor(staminaBlue);
        g2.fillRect(x, y - 5, (int) (player.getStamina() / 1000.0 * player.getHitbox().getWidth()), HEALTH_BAR_HEIGHT);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, y - 5, (int) player.getHitbox().getWidth(), HEALTH_BAR_HEIGHT);
        g2.setColor(backup);

        if (keyHandler.debug) {
            Stroke originalStroke = g2.getStroke();
            g2.setStroke(strichel);
            Rectangle2D playerHitbox = player.getHitbox();
            g2.drawRect((int) (playerHitbox.getX() - camera.x), (int) (playerHitbox.getY()),
                    (int) (playerHitbox.getWidth()), (int) (playerHitbox.getHeight()));
            g2.setStroke(originalStroke);
        }
    }

    void drawSword(Graphics2D g2) {
        try {
            BufferedImage image = ImageUtil.getImage("images/sword/sword_giant.png");
            if (keyHandler.strike && !player.isExhausted()) {
                image = ImageUtil.getImage("images/sword/sword_giant_strike.png");
            }

            if (player.getViewingDirection().equals(Direction.RIGHT)) {
                g2.drawImage(image, (int) (player.getSword().getX() - camera.getX()), (int) player.getSword().getY(), null);
            } else {
                g2.drawImage(image, (int) (player.getSword().getX() + player.getSword().getWidth() - camera.getX()),
                        (int) player.getSword().getY(),
                        -image.getWidth(), image.getHeight(), null);
            }
            if (keyHandler.debug) {
                Stroke originalStroke = g2.getStroke();
                g2.setStroke(strichel);
                Rectangle2D.Double rect = new Rectangle2D.Double(player.getSword().getX() - camera.getX(),
                        player.getSword().getY(), player.getSword().getWidth(), player.getSword().getHeight());
                g2.draw(rect);
                g2.setStroke(originalStroke);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void drawBackground(Graphics2D g2) {
        try {
            BufferedImage image = ImageUtil.getImage(level.getBackgroundFilePath());

            double k = view.getHeight() / (double) image.getHeight(); // Skalierungsfaktor
            int width = (int) (k * image.getWidth());
            int height = view.getHeight();

            g2.drawImage(image, -(int) camera.getX(), 0, width, height, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void drawGrounds(Graphics2D g2) {
        for (Ground ground : level.getGrounds()) {
            Rectangle2D.Double rectangle = new Rectangle2D.Double(ground.getHitbox().getX() - camera.getX(),
                    ground.getHitbox().getY(), ground.getHitbox().getWidth(), ground.getHitbox().getHeight());
            g2.drawImage(ground.getImage(), (int) rectangle.getX(), (int) rectangle.getY(), null);
            if (keyHandler.debug) {
                Stroke originalStroke = g2.getStroke();
                g2.setStroke(strichel);
                g2.draw(rectangle);
                g2.setStroke(originalStroke);
            }
        }
    }

    void drawEnemies(Graphics2D g2) {
        for (Enemy enemy : level.getEnemies()) {
            try {
                {
                    BufferedImage image;
                    image = ImageUtil.getImage(enemy.getImagePath());
                    int x = (int) (enemy.getX() - image.getWidth() / 2 - camera.getX());
                    int y = (int) (enemy.getY() - image.getHeight());
                    if (enemy.getViewingDirection().equals(Direction.RIGHT))
                        g2.drawImage(image, x, y, image.getWidth(), image.getHeight(), null);
                    else
                        g2.drawImage(image, x + image.getWidth(), y, -image.getWidth(), image.getHeight(), null);
                }

                {
                    if (enemy.paintHealth()) {
                        Color backup = g2.getColor();
                        g2.setColor(Color.GREEN);
                        int x = (int) (enemy.getHitbox().getX() - camera.getX());
                        int y = (int) (enemy.getHitbox().getY() - HEALTH_BAR_HEIGHT - 5);
                        g2.fillRect(x, y, (int) ((double) enemy.getHealth() / enemy.getMaxHealth() * enemy.getHitbox().getWidth()), HEALTH_BAR_HEIGHT);
                        g2.setColor(Color.BLACK);
                        g2.drawRect(x, y, (int) enemy.getHitbox().getWidth(), HEALTH_BAR_HEIGHT);
                        g2.setColor(backup);
                    }
                }

                if (keyHandler.debug) {
                    Stroke originalStroke = g2.getStroke();
                    g2.setStroke(strichel);
                    Rectangle2D.Double rect = new Rectangle2D.Double(enemy.getHitbox().getX() - camera.getX(),
                            enemy.getHitbox().getY(), enemy.getHitbox().getWidth(), enemy.getHitbox().getHeight());
                    g2.draw(rect);
                    g2.setStroke(originalStroke);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void drawEnemySwords(Graphics2D g2) {
        for (Enemy enemy : level.getEnemies()) {
            if (enemy.hasSword()) {
                try {
                    BufferedImage image = ImageUtil.getImage(enemy.getWeaponImagePath(enemy.isAttacking()));

                    int x = (int) (enemy.getWeapon().getX() - camera.getX()); // - image.getWidth() / 2
                    int y = (int) (enemy.getWeapon().getY());

                    if (enemy.getViewingDirection().equals(Direction.RIGHT))
                        g2.drawImage(image, x, y, image.getWidth(), image.getHeight(), null);
                    else
                        g2.drawImage(image, x + image.getWidth(), y, -image.getWidth(), image.getHeight(), null);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void drawObstacles(Graphics2D g2) {
        for (Obstacle obstacle : level.getObstacles()) {
            try {
                BufferedImage image = ImageUtil.getImage(obstacle.getImagePath());
                int x = (int) (obstacle.getX() - image.getWidth() / 2 - camera.getX());
                int y = (int) (obstacle.getY() - image.getHeight());
                g2.drawImage(image, x, y, image.getWidth(), image.getHeight(), null);

                if (keyHandler.debug) {
                    Stroke originalStroke = g2.getStroke();
                    g2.setStroke(strichel);
                    g2.drawRect(x, y, image.getWidth(), image.getHeight());
                    g2.setStroke(originalStroke);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void drawStaminaBar(Graphics2D g2) {
        /*Rectangle2D staminaMask = new Rectangle2D.Double(view.getWidth() - 220, view.getHeight() - 30, 200, 15);
        Rectangle2D staminaBorder = new Rectangle2D.Double(staminaMask.getX() - 1, staminaMask.getY() - 1, staminaMask.getWidth() + 1, staminaMask.getHeight() + 1);
        Rectangle2D staminaBar = new Rectangle2D.Double(view.getWidth() - 220, view.getHeight() - 30, player.getStamina() / 5, 15);
        g2.setColor(Constants.MENU_BACKGROUND_COLOR);
        g2.fill(staminaMask);
        g2.setColor(Color.WHITE);
        g2.draw(staminaBorder);
        g2.setColor(new Color(34, 62, 98));
        g2.fill(staminaBar);
        g2.setColor(Color.WHITE);
        Font backup = g2.getFont();
        g2.setFont(Constants.DEFAULT_FONT);
        g2.drawString("Ausdauer: " + (int) (player.getStamina() / 10) + "%", view.getWidth() - 215, view.getHeight() - 17);
        g2.setFont(backup);
        g2.setColor(Color.BLACK);*/
    }

    void drawScore(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        Font backup = g2.getFont();
        g2.setFont(Constants.DEFAULT_FONT.deriveFont(Font.BOLD, 24f));
        String s = "Score: " + player.getScore();
        g2.drawString(s, view.getWidth() / 2 - g2.getFontMetrics().stringWidth(s) / 2, 50);
        g2.setFont(backup);
    }

    void drawDebugScreen(Graphics2D g2) {
        String s = Constants.GAME_TITLE + " " + Constants.GAME_VERSION;
        g2.drawString(s, view.getWidth() / 2 - g2.getFontMetrics().stringWidth(s) / 2, 20);

        String perfData = view.getUps() + "\u2009u/s, " + view.getFps() + "\u2009fps";
        g2.drawString(perfData, view.getWidth() - g2.getFontMetrics().stringWidth(perfData) - 20, 20);

        g2.drawString("@(" + player.getX() + "," + player.getY() + ")", 20, 20);
        g2.drawString("velocityX = " + player.getVelocityX(), 20, 40);
        g2.drawString("velocityY = " + player.getVelocityY(), 20, 60);
        g2.drawString("health = " + player.getHealth(), 20, 80);
        g2.drawString("walking = " + player.isWalking(), 20, 100);
        g2.drawString("running = " + player.isRunning(), 20, 120);
        g2.drawString("jumping = " + player.isJumping(), 20, 140);
        g2.drawString("crouching = " + player.isCrouching(), 20, 160);
        g2.drawString("exhausted = " + player.isExhausted(), 20, 180);
        g2.drawString("onGround = " + player.isOnGround(), 20, 200);
    }

    void drawFinishCastle(Graphics2D g2) {
        int x = (int) (level.getLength() - view.getWidth() / 2 - camera.getX()); //TODO Schloss zeichnen

        g2.setColor(Color.YELLOW);
        g2.fillRect(x, 0, 5, view.getHeight());
        g2.setColor(Color.WHITE);
    }
}
