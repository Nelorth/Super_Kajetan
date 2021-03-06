package gui;

import logic.AIManager;
import logic.CollisionHandler;
import logic.LawMaster;
import model.Camera;
import model.Enemy;
import model.Level;
import model.Player;
import util.Constants;
import util.DBConnection;
import util.SoundUtil;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;


public class LevelView extends AbstractView implements Runnable {
    private final Level level;
    private final Player player;
    private final Camera camera; // Die aktuelle "Kamera"
    private final KeyHandler keyHandler;
    private final CollisionHandler collisionHandler;
    private final AIManager aiManager;
    private final Renderer renderer;
    private final LawMaster lawMaster;
    private JPanel menuPanel;
    private JButton continueButton;
    private JLabel messageLabel;
    private JLabel scoreLabel;

    private boolean running;
    private boolean paused;
    private int ups = 0, fps = 0;

    LevelView(Level level) {
        this.level = level;
        player = new Player(LobbyView.getInstance().getWidth() / 2, Constants.GROUND_LEVEL);
        camera = new Camera(player, this);
        keyHandler = new KeyHandler(player);
        lawMaster = new LawMaster();
        collisionHandler = new CollisionHandler(player, level, keyHandler);
        aiManager = new AIManager(collisionHandler);
        renderer = new Renderer(level, camera, player, keyHandler, this);

        setLayout(new BorderLayout());
        setIgnoreRepaint(true);
        addKeyListener(keyHandler);
        initPauseMenu();
    }

    public void run() {
        running = true;
        SoundUtil.playRandomBackgroundMusic();

        int updateCount = 0;
        int frameCount = 0;

        final int TIME_PER_UPDATE = 1_000_000_000 / Constants.UPDATE_CLOCK;
        long lastTime = System.nanoTime();
        int secondTime = 0;
        int lag = 0;

        while (running) {
            if (!paused) {
                long currentTime = System.nanoTime();
                long elapsedTime = currentTime - lastTime;
                lastTime = currentTime;
                lag += elapsedTime;
                secondTime += elapsedTime;

                if (secondTime > 1_000_000_000) {
                    ups = updateCount;
                    fps = frameCount;
                    secondTime = 0;
                    updateCount = 0;
                    frameCount = 0;
                }

                while (lag >= TIME_PER_UPDATE) {
                    update();
                    updateCount++;
                    lag -= TIME_PER_UPDATE;
                }

                repaint();
                frameCount++;

                // Theoretisch VSync - is aber bissl laggy :(
                /*while (System.nanoTime() - currentTime < 1_000_000_000 / 60) {
                    Thread.yield();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }*/
            } else {
                if (!keyHandler.menu && !player.isDead()) {
                    paused = false;
                    SoundUtil.soundSystem.play(SoundUtil.MUSIC_SOURCE);
                }
                lastTime = System.nanoTime();
            }
        }
        System.out.println(this.getClass().getSimpleName() + " ist raus, Onkel Klaus!");
    }

    private void update() {
        // 1. Reset
        player.reset();

        // 2. Input Handling
        keyHandler.process();

        // 3. General Gravitation
        lawMaster.applyGravitation(player);
        for (Enemy enemy : level.getEnemies())
            lawMaster.applyGravitation(enemy);

        // 4. Ausdauerverbrauch
        lawMaster.updateStamina(player);
        lawMaster.regenerate(player);

        // 5. Kollision - zuerst in x- dann in y-Richtung
        collisionHandler.forPlayer();

        // Test
        aiManager.handleAI(level, player);

        // 6. Score reduzieren
        if (Math.random() < 0.005) {
            player.addScore(-1);
        }

        // 7. Änderungen vornehmen
        player.move();
        camera.move();

        if (!hasFocus())
            keyHandler.clear();

        if (keyHandler.menu) {
            paused = true;
            SoundUtil.soundSystem.pause(SoundUtil.MUSIC_SOURCE);
        }

        if (keyHandler.menu || player.isDead() || (player.getX() + getWidth() / 2 > level.getLength() && player.getY() < 1000)) {
            if (!keyHandler.menu) {
                SoundUtil.soundSystem.stop(SoundUtil.MUSIC_SOURCE);
                SoundUtil.soundSystem.cull(SoundUtil.MUSIC_SOURCE);
                if (player.isDead()) {
                    messageLabel.setText("Du bist tot.");
                    SoundUtil.playEffect("death");
                } else {
                    player.addScore(level.getBasescore());
                    messageLabel.setText("Du hast gewonnen!");
                    SoundUtil.playEffect("victory");
                }
                messageLabel.setVisible(true);
                scoreLabel.setText("Score: " + player.getScore());
                scoreLabel.setVisible(true);
                continueButton.setVisible(false);
                running = false;
                try {
                    String date = new SimpleDateFormat("#yyyy-MM-dd#").format(new java.util.Date());
                    String query = String.format("SELECT * FROM %s WHERE %s = '%s';",
                            Constants.DB_TABLE, Constants.DB_COLLUM_NAME, MainMenuView.getInstance().getCurrentName());
                    ResultSet highScoreSet = DBConnection.getInstance().query(query);

                    if (highScoreSet.next()) {
                        if (player.getScore() > highScoreSet.getInt(Constants.DB_COLLUM_SCORE)) {
                            String update = String.format("UPDATE %s SET %s = %d, %s = %s WHERE %s = '%s';",
                                    Constants.DB_TABLE,
                                    Constants.DB_COLLUM_SCORE, player.getScore(),
                                    Constants.DB_COLLUM_DATE, date,
                                    Constants.DB_COLLUM_NAME, MainMenuView.getInstance().getCurrentName());
                            DBConnection.getInstance().update(update);
                        }
                    } else {
                        String insert = String.format("INSERT INTO %s (%s, %s, %s) VALUES ('%s', %d, %s);",
                                Constants.DB_TABLE,
                                Constants.DB_COLLUM_NAME, Constants.DB_COLLUM_SCORE, Constants.DB_COLLUM_DATE,
                                MainMenuView.getInstance().getCurrentName(), player.getScore(), date);
                        DBConnection.getInstance().update(insert);
                    }
                    highScoreSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                repaint();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // 0. Reset
        g2.clearRect(0, 0, getWidth(), getHeight());

        // 1. Background
        renderer.drawBackground(g2);
        if (running)
            renderer.drawFinishCastle(g2);

        // 2. Grounds

        renderer.drawGrounds(g2);

        // 3. Enemies
        renderer.drawEnemies(g2);
        renderer.drawEnemySwords(g2);

        // 4. Obstacles
        renderer.drawObstacles(g2);

        // 5. Player
        if (running) {
            renderer.drawPlayer(g2);
            renderer.drawSword(g2);
        }

        // 6. Score
        if (running)
            renderer.drawScore(g2);

        // 7. Debug Screen
        if (keyHandler.debug) {
            renderer.drawDebugScreen(g2);
        }

        //8. Pausen- und Game-Over-Menü
        if (keyHandler.menu || player.isDead() || (player.getX() + getWidth() / 2 > level.getLength() && player.getY() < 1000)) {
            menuPanel.setVisible(true);
            g2.setColor(new Color(0, 0, 0, 0.8f));
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else
            menuPanel.setVisible(false);
    }

    public void refresh() {
        setFocusable(true);
        requestFocusInWindow();
    }

    private void initPauseMenu() {
        menuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 0, 5, 0);

        Font buttonFont = Constants.DEFAULT_FONT.deriveFont(24f);

        messageLabel = new JLabel("Du bist tod.");
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setFont(Constants.DEFAULT_FONT.deriveFont(32f));
        messageLabel.setVisible(false);
        menuPanel.add(messageLabel, constraints);
        int i = constraints.insets.bottom;
        constraints.insets.set(constraints.insets.top, constraints.insets.left, 50, constraints.insets.right);
        scoreLabel = new JLabel();
        scoreLabel.setForeground(Color.YELLOW);
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scoreLabel.setFont(buttonFont);
        scoreLabel.setVisible(false);
        menuPanel.add(scoreLabel, constraints);
        constraints.insets.bottom = i;

        continueButton = new WoodenButton("Fortsetzen");
        continueButton.setBackground(Constants.BUTTON_COLOR);
        continueButton.setPreferredSize(Constants.DEFAULT_BUTTON_SIZE);
        continueButton.setFont(buttonFont);
        continueButton.addActionListener(a -> {
            paused = false;
            keyHandler.menu = false;
            SoundUtil.soundSystem.play(SoundUtil.MUSIC_SOURCE);
        });

        WoodenButton backButton = new WoodenButton("Zurück zur Lobby");
        backButton.setBackground(Constants.BUTTON_COLOR);
        backButton.setFont(buttonFont);
        backButton.setPreferredSize(Constants.DEFAULT_BUTTON_SIZE);
        backButton.addActionListener(a -> {
            paused = false;
            running = false;
            SoundUtil.soundSystem.stop(SoundUtil.MUSIC_SOURCE);
            SoundUtil.soundSystem.cull(SoundUtil.MUSIC_SOURCE);
            SoundUtil.soundSystem.stop("death");
            SoundUtil.soundSystem.cull("death");
            SoundUtil.soundSystem.stop("victory");
            SoundUtil.soundSystem.cull("victory");
            MainFrame.getInstance().changeTo(LobbyView.getInstance());
        });

        menuPanel.add(continueButton, constraints);
        menuPanel.add(backButton, constraints);
        menuPanel.setOpaque(false);
        menuPanel.setVisible(false);

        add(menuPanel, BorderLayout.CENTER);
    }

    int getUps() {
        return ups;
    }

    int getFps() {
        return fps;
    }
}