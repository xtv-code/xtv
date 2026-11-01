package com.xtv.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class PosterArt {
  private static final Font FONT = pickFont();

  private PosterArt() {}

  public static void poster(Path file, String title, String eyebrow, int colorA, int colorB) throws IOException {
    BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    paint(g, 1280, 720, title, eyebrow, colorA, colorB, 64);
    g.dispose();
    write(file, image);
  }

  public static void avatar(Path file, String name, int colorA, int colorB) throws IOException {
    BufferedImage image = new BufferedImage(480, 480, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    String mark = name == null || name.isBlank() ? "·" : name.substring(0, 1);
    paint(g, 480, 480, mark, "", colorA, colorB, 160);
    g.dispose();
    write(file, image);
  }

  private static void paint(Graphics2D g, int width, int height, String title, String eyebrow, int colorA, int colorB, int titleSize) {
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setPaint(new java.awt.GradientPaint(0, 0, new Color(colorA), width, height, new Color(colorB)));
    g.fillRect(0, 0, width, height);
    g.setColor(new Color(0, 0, 0, 70));
    g.fillRect(0, height - height / 3, width, height / 3);
    g.setColor(new Color(255, 255, 255, 210));
    g.setFont(FONT.deriveFont(Font.PLAIN, Math.max(18, titleSize / 3)));
    if (eyebrow != null && !eyebrow.isBlank()) {
      g.drawString(eyebrow, 72, height / 2 - titleSize);
    }
    g.setColor(Color.WHITE);
    g.setFont(FONT.deriveFont(Font.BOLD, titleSize));
    drawWrapped(g, title == null ? "" : title, 72, height / 2, width - 144, titleSize + 12);
  }

  private static void drawWrapped(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight) {
    StringBuilder line = new StringBuilder();
    int offset = 0;
    for (int i = 0; i < text.length(); i++) {
      line.append(text.charAt(i));
      if (g.getFontMetrics().stringWidth(line.toString()) > maxWidth) {
        line.deleteCharAt(line.length() - 1);
        g.drawString(line.toString(), x, y + offset);
        offset += lineHeight;
        line.setLength(0);
        line.append(text.charAt(i));
      }
    }
    g.drawString(line.toString(), x, y + offset);
  }

  private static void write(Path file, BufferedImage image) throws IOException {
    Files.createDirectories(file.getParent());
    ImageIO.write(image, "jpg", file.toFile());
  }

  private static Font pickFont() {
    String[] preferred = {"PingFang SC", "Hiragino Sans GB", "Songti SC", "Noto Sans CJK SC", "Microsoft YaHei", "SansSerif"};
    var available = java.util.Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    for (String name : preferred) {
      if (available.contains(name)) {
        Font font = new Font(name, Font.BOLD, 48);
        if (font.canDisplay('影') || "SansSerif".equals(name)) {
          return font;
        }
      }
    }
    return new Font(Font.SANS_SERIF, Font.BOLD, 48);
  }
}
