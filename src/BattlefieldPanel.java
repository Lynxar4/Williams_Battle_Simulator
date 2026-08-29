import java.awt.Color;
import javax.swing.JPanel;
import java.awt.Graphics;

public class BattlefieldPanel extends JPanel {
    private static final int rows = 8;
    private static final int columns = 12;
    private static final int cellSize = 50;

    private Castle teamACastle;
    private Castle teamBCastle;

    public BattlefieldPanel() {
        setBackground(new Color(235, 245, 235));

        teamACastle = new Castle(3, 0, 100);
        teamBCastle = new Castle(3, 11, 100);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
       super.paintComponent(g);

       int gridWidth = columns * cellSize;
       int gridHeight = rows * cellSize;

       int offsetX = (getWidth() - gridWidth) / 2;
       int offsetY = (getHeight() - gridHeight) / 2;

       g.setColor(Color.gray);
       for (int i = 0; i < rows; ++i)
       {
           for (int j = 0; j < columns; ++j)
           {
               int x = j * cellSize + offsetX;
               int y = i * cellSize + offsetY;
               g.drawRect(x, y, cellSize, cellSize);
           }
       }

       drawCastle(g, teamACastle, offsetX, offsetY);
       drawCastle(g, teamBCastle, offsetX, offsetY);
    }

    private void drawCastle(Graphics g, Castle castle, int offsetX, int offsetY)
    {
        int x = castle.getColumn() * cellSize + offsetX;
        int y = castle.getRow() * cellSize + offsetY;

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x, y, cellSize, cellSize);
    }
}
