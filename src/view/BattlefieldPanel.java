package view;

import java.util.List;
import controller.PlacementRule;
import entity.Castle;
import entity.Team;
import entity.interactables.TileEffect;
import entity.troops.Troop;
import entity.troops.melee.Cavalry;
import entity.troops.melee.Infantry;
import entity.troops.ranged.Archer;
import entity.troops.support.Healer;
import entity.troops.support.Priest;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.JLabel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class BattlefieldPanel extends JPanel implements Scrollable {
    private static final int PADDING = 8;
    private static final int SIDEBAR_GAP = 18;
    private static final int SIDEBAR_WIDTH = 148;
    private static final int MIN_TILE_SIZE = 30;

    private static final long DEATH_FULL_OPACITY_DURATION = 1000;
    private static final long DEATH_ANIMATION_DURATION = 4000;

    private static final Color HEAL_GLOW_OUTER = new Color(255, 215, 20, 75);
    private static final Color HEAL_GLOW_INNER = new Color(255, 235, 70, 120);
    private static final Color HEAL_GLOW_CENTRE = new Color(255, 250, 150, 170);

    private final Image infantryASprite = sprite("Infantry_A");
    private final Image infantryBSprite = sprite("Infantry_B");
    private final Image archerASprite = sprite("Archer_A");
    private final Image archerBSprite = sprite("Archer_B");
    private final Image cavalryASprite = sprite("Cavalry_A");
    private final Image cavalryBSprite = sprite("Cavalry_B");
    private final Image priestASprite = sprite("Priest_A");
    private final Image priestBSprite = sprite("Priest_B");

    private final Image infantryADeadSprite = GrayFilter.createDisabledImage(infantryASprite);
    private final Image infantryBDeadSprite = GrayFilter.createDisabledImage(infantryBSprite);
    private final Image archerADeadSprite = GrayFilter.createDisabledImage(archerASprite);
    private final Image archerBDeadSprite = GrayFilter.createDisabledImage(archerBSprite);
    private final Image cavalryADeadSprite = GrayFilter.createDisabledImage(cavalryASprite);
    private final Image cavalryBDeadSprite = GrayFilter.createDisabledImage(cavalryBSprite);
    private final Image priestADeadSprite = GrayFilter.createDisabledImage(priestASprite);
    private final Image priestBDeadSprite = GrayFilter.createDisabledImage(priestBSprite);

    private final Image castleSprite = sprite("Castle");
    private final Image castle75Sprite = sprite("Castle_75");
    private final Image castle50Sprite = sprite("Castle_50");
    private final Image castle25Sprite = sprite("Castle_25");
    private final Image castleDestroyedSprite = sprite("Castle_Destroyed");

    private final Image coinSprite = sprite("Coin");
    private final Image hillSprite = sprite("Hill");
    private final Image trapSprite = sprite("Trap");

    // one visitor per job, both reused for every effect drawn or hovered
    private final TileEffectSpriteVisitor spriteVisitor = new TileEffectSpriteVisitor(hillSprite, coinSprite, trapSprite);
    private final TileEffectDescriptionVisitor descriptionVisitor = new TileEffectDescriptionVisitor();

    private int rows;
    private int columns;
    private Team teamA;
    private Team teamB;
    private Castle castleA;
    private Castle castleB;
    private List<TileEffect> tileEffects = List.of();
    private boolean placementEnabled;
    private Troop.Team placementTeam = Troop.Team.teamA;
    private BiConsumer<Integer, Integer> placementHandler = (row, column) -> {
    };
    private BiConsumer<Troop, MouseEvent> troopMenuHandler = (troop, event) -> {
    };
    private Troop movingTroop;
    private int hoverRow = -1;
    private int hoverColumn = -1;
    private boolean fitWholeBoard;
    private JPanel battlefieldView;
    private final JLabel latestEvent = new JLabel(" ");
    private final Timer animationTimer;
    private ArmyHealthSummary teamAHealth = ArmyHealthSummary.empty();
    private ArmyHealthSummary teamBHealth = ArmyHealthSummary.empty();
    private final ArmyHealthMonitor healthMonitor = new ArmyHealthMonitor(summaries -> {
        teamAHealth = summaries.teamA();
        teamBHealth = summaries.teamB();
        repaintView();
    });
    private PlacementRule placementRule = (team, row, column) -> false;

    /* ---- CONSTRUCTOR ---- */
    public BattlefieldPanel() {
        setBackground(SimulationTheme.BACKGROUND);
        setToolTipText("");

        animationTimer = new Timer(100, e -> {
            if (hasActiveDeathAnimation()) {
                repaint();
            }
        });
        animationTimer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showTroopMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showTroopMenu(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (placementEnabled && SwingUtilities.isLeftMouseButton(e)) {
                    placementHandler.accept(rowFor(e.getY()), columnFor(e.getX()));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearHover();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (hasBoard()) {
                    hoverRow = rowFor(e.getY());
                    hoverColumn = columnFor(e.getX());
                    repaint();
                }
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                clearHover();
            }
        });
    }


    private void showTroopMenu(MouseEvent e) {
        if (!e.isPopupTrigger() || !placementEnabled || !getGridBounds().contains(e.getPoint())) {
            return;
        }
        int row = rowFor(e.getY());
        int column = columnFor(e.getX());
        allTroops().filter(troop -> !troop.isDestroyed() && troop.isAt(row, column)).findFirst().ifPresent(troop -> troopMenuHandler.accept(troop, e));
    }

    public void setTroopMenuHandler(BiConsumer<Troop, MouseEvent> handler) {
        troopMenuHandler = handler;
    }

    public void setMovingTroop(Troop troop) {
        movingTroop = troop;
        setCursor(Cursor.getPredefinedCursor(troop == null ? Cursor.DEFAULT_CURSOR : Cursor.CROSSHAIR_CURSOR));
        repaint();
    }

    /* Keep team information fixed while the battlefield can scroll. */
    public JPanel createView() {
        battlefieldView = new JPanel(new BorderLayout(SIDEBAR_GAP, 0));
        battlefieldView.setBackground(SimulationTheme.BACKGROUND);
        battlefieldView.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        battlefieldView.add(teamSummary(true, "TEAM A", SimulationTheme.TEAM_A), BorderLayout.WEST);
        battlefieldView.add(teamSummary(false, "TEAM B", SimulationTheme.TEAM_B), BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(this);
        scrollPane.setBorder(BorderFactory.createLineBorder(SimulationTheme.BORDER));
        scrollPane.getViewport().setBackground(SimulationTheme.BACKGROUND);
        battlefieldView.add(scrollPane, BorderLayout.CENTER);

        latestEvent.setFont(SimulationTheme.BODY);
        latestEvent.setForeground(SimulationTheme.MUTED);
        latestEvent.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        battlefieldView.add(latestEvent, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::centreView);
        return battlefieldView;
    }

    private JPanel teamSummary(boolean isTeamA, String title, Color accent) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (!hasBoard()) {
                    return; // before the first showBoard there's nothing to draw
                }
                // read the fields NOW, so we always get the latest board
                Team team = isTeamA ? teamA : teamB;
                Castle castle = isTeamA ? castleA : castleB;

                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawSidebar(g, 0, 0, getHeight() - 3, team, castle, title, accent);
                g.dispose();
            }
        };

        card.setOpaque(false);
        card.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 443));
        card.setToolTipText("<html>Health is remaining HP / starting HP, including defeated groups.<br>" + "Healthy: above 70%; wounded: 30-70%; critical: below 30%; defeated: 0 HP.<br>" + "Health categories count groups, not individual soldiers.</html>");

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(card, BorderLayout.NORTH);

        return column;
    }

    public void setFitWholeBoard(boolean fit) {
        fitWholeBoard = fit;
        clearHover();
        revalidate();
        SwingUtilities.invokeLater(this::centreView);
    }

    private void centreView() {
        if (getParent() instanceof JViewport viewport) {
            viewport.setViewPosition(new Point(Math.max(0, (getWidth() - viewport.getWidth()) / 2), Math.max(0, (getHeight() - viewport.getHeight()) / 2)));
        }
    }

    public void refresh() {
        if (hasBoard()) healthMonitor.request(teamA, teamB);
        repaintView();
    }

    private void repaintView() {
        if (battlefieldView != null) {
            battlefieldView.repaint();
        } else {
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return !hasBoard() ? new Dimension(800, 680) : new Dimension(columns * MIN_TILE_SIZE + 2 * PADDING, rows * MIN_TILE_SIZE + 2 * PADDING);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(800, 680);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return fitWholeBoard || getParent() instanceof JViewport viewport && viewport.getWidth() >= getPreferredSize().width;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return fitWholeBoard || getParent() instanceof JViewport viewport && viewport.getHeight() >= getPreferredSize().height;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.HORIZONTAL ? cellWidth() : cellHeight();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.HORIZONTAL ? Math.max(cellWidth(), visible.width - cellWidth()) : Math.max(cellHeight(), visible.height - cellHeight());
    }

    /* Drawing and mouse input share these bounds, including after a resize. */
    public Rectangle getGridBounds() {
        if (!hasBoard()) {
            return new Rectangle();
        }

        int availableWidth = Math.max(1, getWidth() - 2 * PADDING);
        int availableHeight = Math.max(1, getHeight() - 2 * PADDING);

        int width = Math.max(1, availableWidth / columns) * columns;

        int height = Math.max(1, availableHeight / rows) * rows;

        return new Rectangle((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
    }

    private int cellWidth() {
        return getGridBounds().width / columns;
    }

    private int cellHeight() {
        return getGridBounds().height / rows;
    }

    public Rectangle getTileBounds(int row, int column) {
        Rectangle board = getGridBounds();
        int width = cellWidth();
        int height = cellHeight();

        return new Rectangle(board.x + column * width, board.y + row * height, width, height);
    }

    private int columnFor(int mouseX) {
        return Math.floorDiv(mouseX - getGridBounds().x, cellWidth());
    }

    private int rowFor(int mouseY) {
        return Math.floorDiv(mouseY - getGridBounds().y, cellHeight());
    }

    private boolean isWithinGrid(int row, int column) {
        return row >= 0 && row < rows && column >= 0 && column < columns;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (!hasBoard()) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        Rectangle board = getGridBounds();

        drawGrid(g, board);
        drawHover(g, board);

        for (TileEffect effect : tileEffects) {
            drawTileEffect(g, board, effect);
        }

        drawCastle(g, board, castleA, SimulationTheme.TEAM_A);
        drawCastle(g, board, castleB, SimulationTheme.TEAM_B);

        allTroops().sorted(Comparator.comparingInt(Troop::getRow)).forEach(troop -> drawTroop(g, board, troop));

        if (movingTroop != null && placementEnabled) {
            Rectangle tile = getTileBounds(movingTroop.getRow(), movingTroop.getColumn());
            g.setColor(SimulationTheme.TEAM_A);
            g.setStroke(new BasicStroke(3f));
            g.drawRect(tile.x + 2, tile.y + 2, Math.max(1, tile.width - 4), Math.max(1, tile.height - 4));
            g.setStroke(new BasicStroke(1f));
        }
        g.dispose();
    }

    private void drawGrid(Graphics2D g, Rectangle board) {
        int width = cellWidth();
        int height = cellHeight();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                boolean left = column < columns / 2;
                boolean alternate = (row + column) % 2 == 0;

                g.setColor(left ? new Color(226, alternate ? 235 : 232, 224) : new Color(235, alternate ? 234 : 231, 220));

                g.fillRect(board.x + column * width, board.y + row * height, width, height);
            }
        }

        for (int column = 0; column <= columns; column++) {
            g.setColor(column % 5 == 0 ? new Color(176, 190, 176) : new Color(205, 214, 201));

            int x = board.x + column * width;
            g.drawLine(x, board.y, x, board.y + board.height);
        }

        for (int row = 0; row <= rows; row++) {
            g.setColor(row % 5 == 0 ? new Color(176, 190, 176) : new Color(205, 214, 201));

            int y = board.y + row * height;
            g.drawLine(board.x, y, board.x + board.width, y);
        }

        g.setColor(new Color(148, 160, 137));

        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 5f}, 0f));

        int middle = board.x + board.width / 2;
        g.drawLine(middle, board.y, middle, board.y + board.height);

        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(153, 170, 151));
        g.drawRect(board.x, board.y, board.width, board.height);
    }

    private void drawHover(Graphics2D g, Rectangle board) {
        if (!placementEnabled || !isWithinGrid(hoverRow, hoverColumn)) {
            return;
        }

        // Ask the controller. The panel doesn't know WHY a tile is valid, only the answer.
        boolean valid = placementRule.isValid(placementTeam, hoverRow, hoverColumn);

        g.setColor(valid ? new Color(63, 127, 211, 100) : new Color(207, 72, 61, 110));
        Rectangle tile = getTileBounds(hoverRow, hoverColumn);
        g.fillRect(tile.x, tile.y, tile.width, tile.height);
    }

    private void drawTileEffect(Graphics2D g, Rectangle board, TileEffect effect) {

        // the effect picks its own sprite through the visitor, so no type
        // checks here
        Image image = effect.accept(spriteVisitor);

        if (image != null) {
            drawSprite(g, board, effect.getRow(), effect.getColumn(), image);
        }
    }

    private void drawSprite(Graphics2D g, Rectangle board, int row, int column, Image image) {

        Rectangle tile = getTileBounds(row, column);
        int inset = Math.max(1, Math.min(tile.width, tile.height) / 15);

        drawImageFitted(g, image, tile.x + tile.width / 2, tile.y + tile.height - inset, Math.max(1, tile.width - 2 * inset), Math.max(1, tile.height - 2 * inset));
    }

    private void drawImageFitted(Graphics2D g, Image image, int centreX, int bottomY, int maxWidth, int maxHeight) {

        double scale = Math.min((double) maxWidth / image.getWidth(this), (double) maxHeight / image.getHeight(this));

        int width = Math.max(1, (int) Math.round(image.getWidth(this) * scale));

        int height = Math.max(1, (int) Math.round(image.getHeight(this) * scale));

        g.drawImage(image, centreX - width / 2, bottomY - height, width, height, this);
    }

    private void drawCastle(Graphics2D g, Rectangle board, Castle castle, Color accent) {

        Rectangle firstTile = getTileBounds(castle.getRow(), castle.getColumn());

        Rectangle lastTile = getTileBounds(castle.getRow() + castle.getSize() - 1, castle.getColumn() + castle.getSize() - 1);

        Rectangle castleBounds = new Rectangle(firstTile.x, firstTile.y, lastTile.x + lastTile.width - firstTile.x, lastTile.y + lastTile.height - firstTile.y);

        g.setColor(accent);

        g.fillRect(castleBounds.x + 1, castleBounds.y + 1, Math.max(1, castleBounds.width - 2), Math.max(1, castleBounds.height - 2));

        Image sprite;

        if (castle.getHealth() <= 0) {
            sprite = castleDestroyedSprite;
        } else if (castle.getHealth() <= 250) {
            sprite = castle25Sprite;
        } else if (castle.getHealth() <= 500) {
            sprite = castle50Sprite;
        } else if (castle.getHealth() <= 750) {
            sprite = castle75Sprite;
        } else {
            sprite = castleSprite;
        }

        int inset = Math.max(1, Math.min(castleBounds.width, castleBounds.height) / 15);

        drawImageFitted(g, sprite, castleBounds.x + castleBounds.width / 2, castleBounds.y + castleBounds.height - inset, Math.max(1, castleBounds.width - 2 * inset), Math.max(1, castleBounds.height - 2 * inset));
    }

    private void drawTroop(Graphics2D g, Rectangle board, Troop troop) {

        boolean dead = troop.isDestroyed();

        if (dead && isDeathAnimationFinished(troop)) {
            return;
        }

        Graphics2D troopGraphics = (Graphics2D) g.create();

        if (dead) {
            troopGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getDeathOpacity(troop)));
        }

        Rectangle tile = getTileBounds(troop.getRow(), troop.getColumn());

        Color accent;

        if (dead) {
            accent = new Color(105, 105, 105);
        } else {
            accent = troop.getTeam() == Troop.Team.teamA ? SimulationTheme.TEAM_A : SimulationTheme.TEAM_B;
        }

        troopGraphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55));

        troopGraphics.fillRect(tile.x + 1, tile.y + 1, Math.max(1, tile.width - 2), Math.max(1, tile.height - 2));

        if (!dead && troop.wasRecentlyHealed()) {
            drawHealGlow(troopGraphics, tile);
        }

        // Every group shows its count. Wider tiles also show the troop's initial.
        String label = (tile.width >= 48 ? troop.getClass().getSimpleName().substring(0, 1) : "") + troop.getGroupSize();

        int fontSize = Math.min(10, Math.max(6, Math.min(tile.width, tile.height) / 3));

        troopGraphics.setFont(new Font("SansSerif", Font.BOLD, fontSize));

        while (troopGraphics.getFontMetrics().stringWidth(label) > tile.width - 4 && fontSize > 6) {

            troopGraphics.setFont(new Font("SansSerif", Font.BOLD, --fontSize));
        }

        FontMetrics metrics = troopGraphics.getFontMetrics();

        int labelWidth = Math.min(tile.width - 2, metrics.stringWidth(label) + 4);

        int labelHeight = Math.min(tile.height - 2, metrics.getHeight());

        int inset = Math.max(1, Math.min(tile.width, tile.height) / 15);

        int figureLeft = tile.x + inset;

        int figureWidth = tile.width - 2 * inset;

        boolean labelBesideFigure = figureWidth - labelWidth - 2 >= 8;

        if (labelBesideFigure) {
            figureLeft += labelWidth + 2;
            figureWidth -= labelWidth + 2;
        }

        Image troopSprite = dead ? getDeadTroopSprite(troop) : getTroopSprite(troop);

        drawImageFitted(troopGraphics, troopSprite, figureLeft + figureWidth / 2, tile.y + tile.height - inset, Math.max(1, figureWidth), Math.max(1, tile.height - 2 * inset));

        if (!dead && troop instanceof Healer healer && healer.hasRecentlyHealed()) {

            drawBookGlow(troopGraphics, troop, tile, figureLeft, figureWidth);
        }

        int labelY = labelBesideFigure ? tile.y + 1 : tile.y + tile.height - labelHeight - 1;

        Graphics2D badge = (Graphics2D) troopGraphics.create();

        badge.clipRect(tile.x + 1, tile.y + 1, Math.max(1, tile.width - 2), Math.max(1, tile.height - 2));

        badge.setColor(accent);

        badge.fillRect(tile.x + 1, labelY, labelWidth, labelHeight);

        badge.setColor(Color.WHITE);

        badge.drawString(label, tile.x + 3, labelY + metrics.getAscent());

        badge.dispose();
        troopGraphics.dispose();
    }

    private float getDeathOpacity(Troop troop) {
        long timeSinceDeath = System.currentTimeMillis() - troop.getDeathTime();

        if (timeSinceDeath <= DEATH_FULL_OPACITY_DURATION) {
            return 1.0f;
        }

        long fadeTime = timeSinceDeath - DEATH_FULL_OPACITY_DURATION;

        long fadeDuration = DEATH_ANIMATION_DURATION - DEATH_FULL_OPACITY_DURATION;

        return Math.max(0.0f, 1.0f - (float) fadeTime / fadeDuration);
    }

    private boolean isDeathAnimationFinished(Troop troop) {
        return troop.isDestroyed() && System.currentTimeMillis() - troop.getDeathTime() >= DEATH_ANIMATION_DURATION;
    }

    private boolean hasActiveDeathAnimation() {
        if (!hasBoard()) {
            return false;
        }
        return allTroops().anyMatch(troop -> troop.isDestroyed() && !isDeathAnimationFinished(troop));
    }

    private Image getTroopSprite(Troop troop) {
        boolean isTeamA = troop.getTeam() == Troop.Team.teamA;

        if (troop instanceof Cavalry) {
            return isTeamA ? cavalryASprite : cavalryBSprite;
        } else if (troop instanceof Archer) {
            return isTeamA ? archerASprite : archerBSprite;
        } else if (troop instanceof Priest) {
            return isTeamA ? priestASprite : priestBSprite;
        } else if (troop instanceof Infantry) {
            return isTeamA ? infantryASprite : infantryBSprite;
        }

        return isTeamA ? infantryASprite : infantryBSprite;
    }

    private Image getDeadTroopSprite(Troop troop) {
        boolean isTeamA = troop.getTeam() == Troop.Team.teamA;

        if (troop instanceof Cavalry) {
            return isTeamA ? cavalryADeadSprite : cavalryBDeadSprite;
        } else if (troop instanceof Archer) {
            return isTeamA ? archerADeadSprite : archerBDeadSprite;
        } else if (troop instanceof Priest) {
            return isTeamA ? priestADeadSprite : priestBDeadSprite;
        } else if (troop instanceof Infantry) {
            return isTeamA ? infantryADeadSprite : infantryBDeadSprite;
        }

        return isTeamA ? infantryADeadSprite : infantryBDeadSprite;
    }

    private void drawHealGlow(Graphics2D g, Rectangle tile) {

        g.setColor(HEAL_GLOW_OUTER);

        g.fillOval(tile.x + 1, tile.y + 1, Math.max(1, tile.width - 2), Math.max(1, tile.height - 2));

        g.setColor(HEAL_GLOW_INNER);

        g.fillOval(tile.x + 3, tile.y + 3, Math.max(1, tile.width - 6), Math.max(1, tile.height - 6));

        g.setColor(HEAL_GLOW_CENTRE);

        g.drawOval(tile.x + 2, tile.y + 2, Math.max(1, tile.width - 5), Math.max(1, tile.height - 5));
    }

    private void drawBookGlow(Graphics2D g, Troop troop, Rectangle tile, int figureLeft, int figureWidth) {

        int size = Math.max(6, Math.min(tile.width, tile.height) / 2);

        int centreX = figureLeft + (troop.getTeam() == Troop.Team.teamA ? figureWidth * 3 / 4 : figureWidth / 4);

        int centreY = tile.y + tile.height * 3 / 5;

        Graphics2D glow = (Graphics2D) g.create();

        glow.clipRect(tile.x + 1, tile.y + 1, Math.max(1, tile.width - 2), Math.max(1, tile.height - 2));

        glow.setColor(HEAL_GLOW_OUTER);

        glow.fillOval(centreX - size / 2, centreY - size / 2, size, size);

        glow.setColor(HEAL_GLOW_INNER);

        glow.fillOval(centreX - size / 3, centreY - size / 3, Math.max(1, size * 2 / 3), Math.max(1, size * 2 / 3));

        glow.setColor(HEAL_GLOW_CENTRE);

        glow.fillOval(centreX - size / 6, centreY - size / 6, Math.max(1, size / 3), Math.max(1, size / 3));

        glow.dispose();
    }

    private void drawSidebar(Graphics2D g, int x, int y, int availableHeight, Team team, Castle castle, String title, Color accent) {

        int width = SIDEBAR_WIDTH;
        int height = Math.min(availableHeight, 440);

        g.setColor(new Color(221, 227, 233));

        g.fillRoundRect(x, y + 3, width, height, 14, 14);

        g.setColor(Color.WHITE);

        g.fillRoundRect(x, y, width, height, 14, 14);

        g.setColor(accent);

        g.fillRoundRect(x + 14, y + 16, width - 28, 30, 10, 10);

        g.setFont(SimulationTheme.LABEL);
        g.setColor(Color.WHITE);

        centred(g, title, x, width, y + 36);

        g.setColor(SimulationTheme.MUTED);

        centred(g, "AVAILABLE GOLD", x, width, y + 73);

        g.setColor(SimulationTheme.TEXT);

        g.setFont(new Font("SansSerif", Font.BOLD, 27));

        centred(g, "$" + team.getBudget(), x, width, y + 108);

        g.setColor(SimulationTheme.BORDER);

        g.drawLine(x + 16, y + 125, x + width - 16, y + 125);

        ArmyHealthSummary health = team == teamA ? teamAHealth : teamBHealth;

        sidebarStat(g, "Groups", Integer.toString(health.livingGroups()), x, y + 153, width);

        sidebarStat(g, "Soldiers", Integer.toString(health.soldiers()), x, y + 182, width);

        sidebarStat(g, "Castle HP", Integer.toString(Math.max(0, castle.getHealth())), x, y + 211, width);

        if (height >= 420) {
            g.setColor(SimulationTheme.BORDER);

            g.drawLine(x + 16, y + 232, x + width - 16, y + 232);

            g.setFont(SimulationTheme.LABEL);
            g.setColor(accent);

            centred(g, "ARMY HEALTH", x, width, y + 259);

            sidebarStat(g, "Health", health.groupCount() == 0 ? "-" : String.format(Locale.ROOT, "%.0f%%", health.healthPercent()), x, y + 284, width);
            sidebarStat(g, "Healthy", Integer.toString(health.healthy()), x, y + 313, width);
            sidebarStat(g, "Wounded", Integer.toString(health.wounded()), x, y + 342, width);
            sidebarStat(g, "Critical", Integer.toString(health.critical()), x, y + 371, width);
            sidebarStat(g, "Defeated", Integer.toString(health.dead()), x, y + 400, width);
            g.setFont(SimulationTheme.LABEL);
            g.setColor(SimulationTheme.MUTED);
            centred(g, "Counts are groups", x, width, y + 425);
        }
    }

    private void sidebarStat(Graphics2D g, String label, String value, int x, int baseline, int width) {

        g.setFont(SimulationTheme.BODY);
        g.setColor(SimulationTheme.MUTED);

        g.drawString(label, x + 16, baseline);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));

        g.setColor(SimulationTheme.TEXT);

        g.drawString(value, x + width - 16 - g.getFontMetrics().stringWidth(value), baseline);
    }

    private void centred(Graphics2D g, String text, int x, int width, int baseline) {

        FontMetrics metrics = g.getFontMetrics();

        g.drawString(text, x + (width - metrics.stringWidth(text)) / 2, baseline);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (!hasBoard()) {
            return null;
        }

        int row = rowFor(e.getY());

        int column = columnFor(e.getX());

        if (!isWithinGrid(row, column)) {
            return null;
        }

        Troop troop = allTroops().filter(group -> group.isAt(row, column)).filter(group -> !group.isDestroyed() || !isDeathAnimationFinished(group)).findFirst().orElse(null);

        if (troop != null) {
            return troop.getClass().getSimpleName() + " | " + (troop.getTeam() == Troop.Team.teamA ? "Team A" : "Team B") + " | Soldiers: " + troop.getGroupSize() + " | HP: " + troop.getGroupHealth() + "/" + troop.getMaxHealth();
        }

        // nothing standing here, so report the tile effect if there is one
        final int hoverRowFinal = row;
        final int hoverColumnFinal = column;

        TileEffect effect = tileEffects.stream().filter(tileEffect -> tileEffect.isAt(hoverRowFinal, hoverColumnFinal)).findFirst().orElse(null);

        if (effect != null) {
            return effect.accept(descriptionVisitor);
        }

        return "Row " + (row + 1) + ", column " + (column + 1);
    }

    private Image sprite(String name) {
        File file = new File("assets/" + name + "_Sprite.png");

        try {
            BufferedImage image = ImageIO.read(file);

            if (image == null) {
                throw new IOException("Unsupported image format");
            }

            // Ignore transparent padding when scaling. The original PNG is left untouched.
            int left = image.getWidth();
            int top = image.getHeight();
            int right = -1;
            int bottom = -1;

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) > 16) {
                        left = Math.min(left, x);
                        top = Math.min(top, y);
                        right = Math.max(right, x);
                        bottom = Math.max(bottom, y);
                    }
                }
            }

            return right < left ? image : image.getSubimage(left, top, right - left + 1, bottom - top + 1);

        } catch (IOException e) {
            throw new IllegalStateException("Cannot load sprite: " + file + ". Run from the project root.", e);
        }
    }

    private void clearHover() {
        hoverRow = -1;
        hoverColumn = -1;
        repaint();
    }
    public void showBoard(int rows, int columns, Team teamA, Team teamB,
                          Castle castleA, Castle castleB, List<TileEffect> tileEffects) {
        this.rows = rows;
        this.columns = columns;
        this.teamA = teamA;
        this.teamB = teamB;
        this.castleA = castleA;
        this.castleB = castleB;
        this.tileEffects = tileEffects;
        healthMonitor.reset();
        teamAHealth = ArmyHealthSummary.empty();
        teamBHealth = ArmyHealthSummary.empty();
        revalidate();   // the preferred size depends on rows/columns
        refresh();
    }

    public void setPlacementEnabled(boolean enabled) {
        placementEnabled = enabled;
        if (!enabled) {
            clearHover();
        }
        repaint();
    }

    public void showLatestEvent(String text) {
        // a single space keeps the label's height, so the layout doesn't jump
        latestEvent.setText(text == null || text.isEmpty() ? " " : text);
    }

    public void setPlacementHandler(BiConsumer<Integer, Integer> handler) {

        this.placementHandler = handler;
    }

    public void setPlacementTeam(Troop.Team team) {
        this.placementTeam = team;
        repaint();
    }

    // True once showBoard has been called.
    private boolean hasBoard() {
        return teamA != null;
    }

    // Every troop on the board, both teams.
    private Stream<Troop> allTroops() {
        return Stream.concat(teamA.getArmy().stream(), teamB.getArmy().stream());
    }

    public void setPlacementRule(PlacementRule rule) {
        placementRule = rule;
        repaint(); // redraw the hover with the new rule
    }
}
