package autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** generating an icon image from the name of the app */
public class ApplicationIconGenerator {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new ApplicationIconGenerator()::runDemo);
    }

    public void runDemo() {
        JFrame frame = new JFrame("App Icon");
        {
            JPanel pane = new JPanel() {
                private static final long serialVersionUID = 1L;
                @Override
                public void paintComponent(Graphics g) {
                    g.clearRect(0, 0, getWidth(), getHeight());
                    g.translate(20, 20);
                    ApplicationIconGenerator.this.paint((Graphics2D) g);
                    setAppIcon(frame);
                }
            };
            pane.setPreferredSize(new Dimension(256, 256));
            JPanel mainPane = new JPanel(new BorderLayout());
            JTextField nameField = new JTextField(30);
            nameField.addActionListener(e -> {
                this.names = Arrays.asList(nameField.getText().split("\\s+"));
                pane.repaint();
            });
            mainPane.add(nameField, BorderLayout.NORTH);
            mainPane.add(pane, BorderLayout.CENTER);
            frame.setContentPane(mainPane);
        }
        frame.pack();
        frame.setVisible(true);

    }

    protected float width = 128;
    protected float height = 128;

    protected List<String> names;

    public ApplicationIconGenerator() {
    }

    public ApplicationIconGenerator(float width, float height, List<String> names) {
        this.width = width;
        this.height = height;
        this.names = names;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public List<String> getNames() {
        return names;
    }

    public void setAppIcon(JFrame frame, String name) {
        setNames(Arrays.asList(name.split("\\s+")));
        setAppIcon(frame);
    }

    public void setAppIcon(JFrame frame) {
        BufferedImage img = getImage();
        frame.setIconImage(img);
        try {
            Class<?> taskBarType = Class.forName("java.awt.Taskbar");
            Object taskBar = taskBarType.getMethod("getTaskbar").invoke(null);
            taskBarType.getMethod("setIconImage", Image.class).invoke(taskBar, img);
        } catch (Exception ex) {
            //java8 on macOS?
            setMacApplicationIcon(frame, img);
        }
    }


    public void setMacApplicationIcon(JFrame frame, Image image) {
        try {
            Class<?> appType = Class.forName("com.apple.eawt.Application");
            Object app = appType.getMethod("getApplication").invoke(null);
            appType.getMethod("setDockIconImage", Image.class).invoke(app, image);
        } catch (Exception e) {
        }
    }

    public BufferedImage getImage() {
        BufferedImage img = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = img.createGraphics();
        paint(g);
        g.dispose();
        return img;
    }

    public void paint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle2D.Float iconFrame = new Rectangle2D.Float(0, 0, width, height);

        float hue = 0.57f;
        if (names != null) {
            hue = getNameColorHue(String.join(" ", names));
        }

        Rectangle2D.Float outerRect = buildRectSmaller(iconFrame, iconFrame.width * 0.1f, iconFrame.height * 0.1f);
        Paint fillPaint = buildRadialGradientPaint(outerRect.x + outerRect.width * 0.2f, outerRect.y + outerRect.height * 0.2f,
                (outerRect.width + outerRect.height) / 2.0f, hue, 0.35f, 0.9f, 0.3f, 0.85f);
        {
            Path2D outerRectPath = buildRoundRect(true, true, true, true, outerRect, 0.15f);
            drawShadow(g, 7, outerRectPath, 0.4f);

            g.setPaint(fillPaint);
            g.fill(outerRectPath);
        }

        Rectangle2D.Float interRect = buildRectSmaller(outerRect, outerRect.width * 0.015f, outerRect.height * 0.015f);
        {
            Path2D interRectPath = buildRoundRect(true, true, true, true, interRect, 0.15f);

            g.setPaint(Color.white);
            g.fill(interRectPath);
        }

        Rectangle2D.Float fillRect = buildRectSmaller(interRect, interRect.width * 0.03f, outerRect.height * 0.03f);
        float fillRectHeightP = 0.3f;
        Rectangle2D.Float fillRect2 = new Rectangle2D.Float(fillRect.x, fillRect.y + fillRect.height * fillRectHeightP,
                fillRect.width, fillRect.height * (1 - fillRectHeightP));
        {

            Path2D.Float fillRect2Path = buildFill(fillRect2.x, fillRect2.y, fillRect2.width, fillRect2.height, fillRect2.width * 0.15f);

            float curvePointX = fillRect2.x + fillRect2.width * 0.7f;
            float curvePointY = fillRect2.y - fillRect2.height * 0.3f;

            //top-right to curvePoint
            fillRect2Path.append(new QuadCurve2D.Float(
                    maxX(fillRect2), fillRect2.y,
                    curvePointX + (maxX(fillRect) - curvePointX) * 0.2f, fillRect2.y - (fillRect2.y - curvePointY) * 0.5f,
                    curvePointX, curvePointY), true);
            //curvePoint to top-left
            fillRect2Path.append(new QuadCurve2D.Float(
                    curvePointX, curvePointY,
                    fillRect2.x + (curvePointX - fillRect2.x) * 0.5f , fillRect2.y,
                    fillRect2.x, fillRect2.y), true);

            fillRect2Path.closePath();

            g.setPaint(fillPaint);
            g.fill(fillRect2Path);
        }

        Rectangle2D.Float nameRect = buildRectSmaller(fillRect2, fillRect2.width * 0.05f, fillRect2.width * 0.01f);
        {
            if (names != null) {
                NameWordBounds words = new NameWordBounds(g, names);

                NameLinePattern pattern = layout(words.w, words.h, nameRect.width / nameRect.height);

                Font font = UIManagerUtil.getInstance().getLabelFont();
                g.setFont(font);

                float fontSizeP = Math.min(nameRect.width / pattern.getWidth(), nameRect.height / pattern.getHeight()) * 0.9f;
                g.setFont(words.metrics.getFont().deriveFont(fontSizeP * words.metrics.getFont().getSize2D()));
                NameWordBounds wordsFit = new NameWordBounds(g, words.names);

                List<Rectangle2D.Float> layouts = pattern.layout(nameRect.x, nameRect.y, wordsFit.w, wordsFit.h);
                g.setPaint(Color.white);

                for (int i = 0, l = wordsFit.names.size() ; i < l; ++i) {
                    Rectangle2D.Float rect = layouts.get(i);
                    g.drawString(wordsFit.names.get(i), rect.x, rect.y - nameRect.height * 0.05f);
                }
            }
        }
    }

    public float getNameColorHue(String name) {
        int sum = 0;
        for (char ch : name.toCharArray()) {
            sum += ch;
        }
        sum %= 300;
        return ((float) sum) / 300f;
    }

    /** bounds of words with a graphics context */
    public static class NameWordBounds {
        public List<String> names;
        public List<Rectangle2D> bounds;
        public float[] w;
        public float h;
        public FontMetrics metrics;

        public NameWordBounds(Graphics2D g, List<String> names) {
            this.names = names;
            metrics = g.getFontMetrics();
            float addW = (float) metrics.getStringBounds("m", g).getWidth() * 0.25f;
            bounds = names.stream()
                    .map(n -> metrics.getStringBounds(n, g))
                    .collect(Collectors.toList());
            w = new float[bounds.size()];
            h = 0;
            for (int i = 0, l = bounds.size(); i < l; ++i) {
                w[i] = (float) bounds.get(i).getWidth() + addW;
                h = Math.max((float) bounds.get(i).getHeight(), h);
            }
        }
    }

    public static NameLinePattern layout(float[] w, float h, float requiredRatio) {
        List<NameLinePattern> pats = new ArrayList<>();
        pats.add(new NameLinePattern().add(0, true));
        for (int i = 1, l = w.length; i < l; ++i) {
            List<NameLinePattern> nextPats = new ArrayList<>();
            for (NameLinePattern p : pats) {
                nextPats.add(p.copy().add(i, false));
                nextPats.add(p.copy().add(i, true));
            }
            pats = nextPats;
        }

        for (NameLinePattern p : pats) {
            p.setSize(w, h);
        }
        pats.sort(Comparator.comparing(e -> e.getRatioDiff(requiredRatio)));
//        System.err.printf("req-r=%.3f\n", requiredRatio);
//        pats.forEach(e -> System.err.printf("d=%.3f, %s\n", e.getRatioDiff(requiredRatio), e));

        return pats.get(0);
    }

    /** a pattern of line wrapping in order to find optimal layout:
     *     indices are word indices of lines e.g. [[0,1,2], [3,4], ...] */
    public static class NameLinePattern {
        List<List<Integer>> indices = new ArrayList<>();
        double width;
        double height;
        double ratio;

        public NameLinePattern() {
        }

        public List<List<Integer>> getIndices() {
            return indices;
        }

        public float getWidth() {
            return (float) width;
        }

        public float getHeight() {
            return (float) height;
        }

        public List<Rectangle2D.Float> layout(float x, float y, float[] ws, float h) {
            List<Rectangle2D.Float> ret = new ArrayList<>();
            float lineY = y + h;
            for (List<Integer> line : indices) {
                float lineX = x;
                for (int i : line) {
                    ret.add(new Rectangle2D.Float(lineX, lineY, ws[i], h));
                    lineX += ws[i];
                }
                lineY += h;
            }
            return ret;
        }

        public double getRatio() {
            return ratio;
        }

        public double getRatioDiff(float requiredRatio) {
            return Math.abs(Math.abs(ratio) - requiredRatio);
        }

        public void setSize(float[] w, float h) {
            for (List<Integer> i : indices) {
                width = Math.max(width, i.stream()
                        .mapToDouble(idx -> w[idx])
                        .sum());
            }
            height = indices.size() * h;
            ratio = width / height;
        }

        public NameLinePattern add(int i, boolean nextLine) {
            if (indices.isEmpty() || nextLine){
                indices.add(new ArrayList<>());
            }
            indices.get(indices.size() - 1).add(i);
            return this;
        }

        public NameLinePattern copy() {
            NameLinePattern p = new NameLinePattern();
            p.indices.addAll(indices);
            if (!p.indices.isEmpty()) {
                p.indices.remove(p.indices.size() - 1);
                p.indices.add(new ArrayList<>(indices.get(indices.size() - 1)));
            }
            return p;
        }

        @Override
        public String toString() {
            return String.format("r=%.3f: %s", ratio, indices.toString());
        }
    }

    private float maxX(Rectangle2D.Float r) {
        return r.x + r.width;
    }

    private float maxY(Rectangle2D.Float r) {
        return r.y + r.height;
    }

    public void drawShadow(Graphics2D g2, int shadowWidth, Shape shape, float alpha) {
        for (int i = shadowWidth; i > 0; i-=1) {
            float pct = ((float) shadowWidth - i) / ((float) shadowWidth);
            g2.setColor(new Color(0.5f, 0.5f, 0.5f, pct * 0.5f * alpha));
            g2.setStroke(new BasicStroke(i));
            g2.draw(shape);
        }
    }

    public RadialGradientPaint buildRadialGradientPaint(float x, float y, float r, float hue, float sat1, float br1, float sat2, float br2) {
        return new RadialGradientPaint(x, y, r, new float[] {0, 1}, new Color[] {
                Color.getHSBColor(hue, sat1, br1),
                Color.getHSBColor(hue, sat2, br2),
        });
    }

    public Color getHSBColor(float hue, float sat, float br, float alpha) {
        Color c = Color.getHSBColor(hue, sat, br);
        float[] fs = c.getRGBColorComponents(null);
        return new Color(fs[0], fs[1], fs[2], alpha);
    }

    public Rectangle2D.Float buildRectSmaller(Rectangle2D.Float base, float w, float h) {
        return new Rectangle2D.Float(base.x + w, base.y + h, base.width - w * 2f, base.height - h * 2f);
    }

    public Path2D.Float buildRoundRect(boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight,
                                       Rectangle2D.Float base, float arcP) {
        return buildRoundRect(topLeft, topRight, bottomLeft, bottomRight, base.x, base.y, base.width, base.height, base.width * arcP);
    }

    public Path2D.Float buildRoundRect(boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight,
                                       float x, float y, float w, float h, float arc) {
        Path2D.Float p = new Path2D.Float();
        if (topLeft) {
            arc(p, x, y, arc, arc, 90);
        } else {
            line(p, x + arc, y, x, y);
            line(p, x, y, x, y + arc);
        }
        line(p, x, y + arc, x, y + h - arc);
        if (bottomLeft) {
            arc(p, x, y + h - arc, arc, arc, 180);
        } else {
            line(p, x, y + h - arc, x, y + h);
            line(p, x, y + h, x + arc, y + h);
        }
        line(p, x + arc, y + h, x + w - arc, y + h);
        if (bottomRight) {
            arc(p, x + w - arc, y + h - arc, arc, arc, 270);
        } else {
            line(p, x + w - arc, y + h, x + w, y + h);
            line(p, x + w, y + h, x + w, y + h - arc);
        }
        line(p, x + w, y + h - arc, x + w, y + arc);
        if (topRight) {
            arc(p, x + w - arc, y, arc, arc, 0);
        } else {
            line(p, x + w, y + arc, x + w, y);
            line(p, x + w, y, x + w - arc, y);
        }
        line(p, x + w - arc, y, x + arc, y);
        p.closePath();
        return p;
    }
    private void arc(Path2D.Float p, float x, float y, float w, float h, float as) {
        p.append(new Arc2D.Float(x, y, w, h, as, 90, Arc2D.OPEN), true);
    }
    private void line(Path2D.Float p, float a, float b, float c, float d) {
        p.append(new Line2D.Float(a, b, c, d), true);
    }

    private Path2D.Float buildFill(float x, float y, float w, float h, float arc) {
        Path2D.Float p = new Path2D.Float();
        //left-line
        line(p, x, y, x, y + h - arc);
        //bottom-left-corner
        arc(p, x, y + h - arc, arc, arc, 180);
        //bottom-line
        line(p, x + arc, y + h, x + w - arc, y + h);
        //bottom-right-corner
        arc(p, x + w - arc, y + h - arc, arc, arc, 270);
        //right-line
        line(p, x + w, y + h - arc, x + w, y);
        return p;
    }
}
