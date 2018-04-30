/**
 * lpsim - 
 * Copyright (c) 2015, Matej Kormuth <http://www.github.com/dobrakmato>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package eu.matejkormuth.bf.tools;

import eu.matejkormuth.bf.compression.BFInputStream;
import eu.matejkormuth.bf.image.ImageFile;
import eu.matejkormuth.lpsim.ChannelUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayDeque;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

@Slf4j
public class BifView {

    @RequiredArgsConstructor
    public static class UIThread extends Thread {

        private final JFrame[] frame;
        private final String input;

        {
            this.setDaemon(true);
        }

        private ArrayDeque<Runnable> tasks = new ArrayDeque<>(2);

        @Override
        public void run() {
            print("Opening...");

            String title = "Loading (";
            title += Paths.get(input).toAbsolutePath().toFile().getAbsolutePath();
            title += ")";

            frame[0] = new MainFrame();
            frame[0].setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame[0].getContentPane().setLayout(new BorderLayout());
            frame[0].setBackground(Color.white);
            frame[0].setTitle(title);
            frame[0].setSize(768, 768 + 3*frame[0].getInsets().top / 4);
            frame[0].setLocationRelativeTo(null);
            frame[0].setVisible(true);
            frame[0].setSize(768, 768 + 3*frame[0].getInsets().top / 4);

            for (; ; ) {
                if (tasks.isEmpty()) {
                    try {
                        synchronized (this) {
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                    }
                } else {
                    Runnable task;
                    synchronized (this) {
                        task = tasks.pop();
                    }
                    try {
                        task.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public void addTask(Runnable task) {
            synchronized (this) {
                tasks.addLast(task);
                this.notifyAll();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String input = args[0];

        System.setProperty("sun.java2d.d3d", "false");

        final MainFrame[] frame = new MainFrame[1];
        UIThread t = new UIThread(frame, input);
        t.start();

        print("Reading...");
        eu.matejkormuth.lpsim.Image original = ImageFile.loadToImage(new BFInputStream(new FileInputStream(Paths.get(input).toAbsolutePath().toFile())));

        JLabel loading = new JLabel("Loading...", SwingConstants.CENTER);

        t.addTask(() -> {
            String title = "";
            title += original.getWidth() + "x" + original.getHeight();
            title += ", " + original.getLayers().size() + " layer(s)";
            title += ", " + original.getLayer().getChannelCount() + " channel(s)";
            title += ", " + original.getLayer().getFormat().getColorSpace();
            title += ", " + original.getLayer().getPixels() + " pixels";
            title += ", " + original.getLayer().getRaster().length + " bytes";

            title += " (";
            title += Paths.get(input).toAbsolutePath().toFile().getAbsolutePath();
            title += ")";
            frame[0].add(loading);
            frame[0].setTitle(title);
        });

        print("Converting...");
        BufferedImage image = open(original);

        t.addTask(() -> {
            frame[0].remove(loading);
            frame[0].add(new ScalablePane(image));
            frame[0].revalidate();
        });

    }

    private static void print(String line) {
        System.out.println(line);
    }

    private static BufferedImage open(eu.matejkormuth.lpsim.Image o) throws IOException {
        BufferedImage img = new BufferedImage(o.getWidth(), o.getHeight(), o.getLayer().getChannelCount() == 4 ? TYPE_INT_ARGB : TYPE_INT_RGB);
        for (int y = 0; y < o.getWidth(); y++) {
            for (int x = 0; x < o.getHeight(); x++) {
                int r = ChannelUtils.red(o).get(x, y) & 0xFF;
                int g = ChannelUtils.hasGreen(o) ? (ChannelUtils.green(o).get(x, y) & 0xFF) : r;
                int b = ChannelUtils.hasBlue(o) ? (ChannelUtils.blue(o).get(x, y) & 0xFF) : r;

                int rgb;
                if (o.getLayer().getChannelCount() != 4) {
                    rgb = (r << 16) | (g << 8) | b;
                } else {
                    int a = ChannelUtils.hasAlpha(o) ? ChannelUtils.alpha(o).get(x, y) & 0xFF : 255;
                    rgb = (a << 24) | (r << 16) | (g << 8) | b;
                }

                img.getRaster().setDataElements(x, y, img.getColorModel().getDataElements(rgb, null));
            }
        }

        return img;
    }

    static class MainFrame extends JFrame {
    }

    public static class ScalablePane extends JPanel {

        private Image master;
        private boolean toFit;
        private Image scaled;

        public ScalablePane(Image master) {
            this(master, true);
        }

        public ScalablePane(Image master, boolean toFit) {
            this.master = master;
            this.setBackground(Color.white);
            setToFit(toFit);
        }

        @Override
        public Dimension getPreferredSize() {
            return master == null ? super.getPreferredSize() : new Dimension(master.getWidth(this), master.getHeight(this));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Image toDraw = null;
            if (scaled != null) {
                toDraw = scaled;
            } else if (master != null) {
                toDraw = master;
            }

            if (toDraw != null) {
                int x = (getWidth() - toDraw.getWidth(this)) / 2;
                int y = (getHeight() - toDraw.getHeight(this)) / 2;
                g.drawImage(toDraw, x, y, this);
            }
        }

        @Override
        public void invalidate() {
            generateScaledInstance();
            super.invalidate();
        }

        public boolean isToFit() {
            return toFit;
        }

        public void setToFit(boolean value) {
            if (value != toFit) {
                toFit = value;
                invalidate();
            }
        }

        protected void generateScaledInstance() {
            scaled = null;
            if (isToFit()) {
                scaled = getScaledInstanceToFit(master, getSize());
            } else {
                scaled = getScaledInstanceToFill(master, getSize());
            }
        }

        protected BufferedImage toBufferedImage(Image master) {
            Dimension masterSize = new Dimension(master.getWidth(this), master.getHeight(this));
            BufferedImage image = createCompatibleImage(masterSize);
            Graphics2D g2d = image.createGraphics();
            g2d.drawImage(master, 0, 0, this);
            g2d.dispose();
            return image;
        }

        public Image getScaledInstanceToFit(Image master, Dimension size) {
            Dimension masterSize = new Dimension(master.getWidth(this), master.getHeight(this));
            return getScaledInstance(
                    toBufferedImage(master),
                    getScaleFactorToFit(masterSize, size),
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                    true);
        }

        public Image getScaledInstanceToFill(Image master, Dimension size) {
            Dimension masterSize = new Dimension(master.getWidth(this), master.getHeight(this));
            return getScaledInstance(
                    toBufferedImage(master),
                    getScaleFactorToFill(masterSize, size),
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                    true);
        }

        public Dimension getSizeToFit(Dimension original, Dimension toFit) {
            double factor = getScaleFactorToFit(original, toFit);
            Dimension size = new Dimension(original);
            size.width *= factor;
            size.height *= factor;
            return size;
        }

        public Dimension getSizeToFill(Dimension original, Dimension toFit) {
            double factor = getScaleFactorToFill(original, toFit);
            Dimension size = new Dimension(original);
            size.width *= factor;
            size.height *= factor;
            return size;
        }

        public double getScaleFactor(int iMasterSize, int iTargetSize) {
            return (double) iTargetSize / (double) iMasterSize;
        }

        public double getScaleFactorToFit(Dimension original, Dimension toFit) {
            double dScale = 1d;
            if (original != null && toFit != null) {
                double dScaleWidth = getScaleFactor(original.width, toFit.width);
                double dScaleHeight = getScaleFactor(original.height, toFit.height);
                dScale = Math.min(dScaleHeight, dScaleWidth);
            }
            return dScale;
        }

        public double getScaleFactorToFill(Dimension masterSize, Dimension targetSize) {
            double dScaleWidth = getScaleFactor(masterSize.width, targetSize.width);
            double dScaleHeight = getScaleFactor(masterSize.height, targetSize.height);

            return Math.max(dScaleHeight, dScaleWidth);
        }

        public BufferedImage createCompatibleImage(Dimension size) {
            return createCompatibleImage(size.width, size.height);
        }

        public BufferedImage createCompatibleImage(int width, int height) {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc == null) {
                gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            }

            BufferedImage image = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            image.coerceData(true);
            return image;
        }

        protected BufferedImage getScaledInstance(BufferedImage img, double dScaleFactor, Object hint, boolean bHighQuality) {
            BufferedImage imgScale = img;
            int iImageWidth = (int) Math.round(img.getWidth() * dScaleFactor);
            int iImageHeight = (int) Math.round(img.getHeight() * dScaleFactor);

            if (dScaleFactor <= 1.0d) {
                imgScale = getScaledDownInstance(img, iImageWidth, iImageHeight, hint, bHighQuality);
            } else {
                imgScale = getScaledUpInstance(img, iImageWidth, iImageHeight, hint, bHighQuality);
            }

            return imgScale;
        }

        protected BufferedImage getScaledDownInstance(BufferedImage img,
                                                      int targetWidth,
                                                      int targetHeight,
                                                      Object hint,
                                                      boolean higherQuality) {

            int type = (img.getTransparency() == Transparency.OPAQUE)
                    ? TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;

            BufferedImage ret = (BufferedImage) img;

            if (targetHeight > 0 || targetWidth > 0) {
                int w, h;
                if (higherQuality) {
                    // Use multi-step technique: start with original size, then
                    // scale down in multiple passes with drawImage()
                    // until the target size is reached
                    w = img.getWidth();
                    h = img.getHeight();
                } else {
                    // Use one-step technique: scale directly from original
                    // size to target size with a single drawImage() call
                    w = targetWidth;
                    h = targetHeight;
                }

                do {
                    if (higherQuality && w > targetWidth) {
                        w /= 2;
                        if (w < targetWidth) {
                            w = targetWidth;
                        }
                    }
                    if (higherQuality && h > targetHeight) {
                        h /= 2;
                        if (h < targetHeight) {
                            h = targetHeight;
                        }
                    }

                    BufferedImage tmp = new BufferedImage(Math.max(w, 1), Math.max(h, 1), type);
                    Graphics2D g2 = tmp.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
                    g2.drawImage(ret, 0, 0, w, h, null);
                    g2.dispose();

                    ret = tmp;
                } while (w != targetWidth || h != targetHeight);
            } else {
                ret = new BufferedImage(1, 1, type);
            }

            return ret;
        }

        protected BufferedImage getScaledUpInstance(BufferedImage img,
                                                    int targetWidth,
                                                    int targetHeight,
                                                    Object hint,
                                                    boolean higherQuality) {

            int type = BufferedImage.TYPE_INT_ARGB;

            BufferedImage ret = (BufferedImage) img;
            int w, h;
            if (higherQuality) {
                // Use multi-step technique: start with original size, then
                // scale down in multiple passes with drawImage()
                // until the target size is reached
                w = img.getWidth();
                h = img.getHeight();
            } else {
                // Use one-step technique: scale directly from original
                // size to target size with a single drawImage() call
                w = targetWidth;
                h = targetHeight;
            }

            do {
                if (higherQuality && w < targetWidth) {
                    w *= 2;
                    if (w > targetWidth) {
                        w = targetWidth;
                    }
                }

                if (higherQuality && h < targetHeight) {
                    h *= 2;
                    if (h > targetHeight) {
                        h = targetHeight;
                    }
                }

                BufferedImage tmp = new BufferedImage(w, h, type);
                Graphics2D g2 = tmp.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
                g2.drawImage(ret, 0, 0, w, h, null);
                g2.dispose();

                ret = tmp;
                tmp = null;
            } while (w != targetWidth || h != targetHeight);
            return ret;
        }
    }
}
