package uni.fmi.imagedownsizer;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.awt.image.BufferedImage;

public class Downsizer {
    private double downsizeFactor;
    private File uploadedFile;

    public Downsizer() {}
    public Downsizer(double downsizeFactor, File uploadedFile) {
        this.downsizeFactor = downsizeFactor;
        this.uploadedFile = uploadedFile;
    }
    public void parallelDownsize() throws IOException {
        String productPath = uploadedFile.getAbsolutePath() + Instant.now().toEpochMilli() + "parallel.jpg";

        BufferedImage inputImage = ImageIO.read(uploadedFile);

        int newWidth = (int) (inputImage.getWidth() * downsizeFactor);
        int newHeight = (int) (inputImage.getHeight() * downsizeFactor);

        BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        Thread[] threads = new Thread[4];

        for (int i = 0; i < 4; i++) {
            int quadrant = i;
            threads[i] = new Thread(new Runnable() {
               public void run() {
                   interpolateQuadrant(inputImage, outputImage, newWidth, newHeight, quadrant);
               }
            });
            threads[i].start();
        }

        try {
            for (int i = 0; i < 4; i++) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Bilinear downsizing complete.");

        ImageIO.write(outputImage, "jpg", new File(productPath));
    }

    private void interpolateQuadrant(BufferedImage inputImage, BufferedImage outputImage, int newWidth, int newHeight, int quadrant) {
        int startX, startY, endX, endY;

        switch (quadrant) {
            case 0: // Top-left quadrant
                startX = 0;
                startY = 0;
                endX = newWidth / 2;
                endY = newHeight / 2;
                break;
            case 1: // Top-right quadrant
                startX = newWidth / 2;
                startY = 0;
                endX = newWidth;
                endY = newHeight / 2;
                break;
            case 2: // Bottom-left quadrant
                startX = 0;
                startY = newHeight / 2;
                endX = newWidth / 2;
                endY = newHeight;
                break;
            case 3: // Bottom-right quadrant
                startX = newWidth / 2;
                startY = newHeight / 2;
                endX = newWidth;
                endY = newHeight;
                break;
            default:
                throw new IllegalArgumentException("Invalid quadrant");
        }

        bilinearDownsize(inputImage, startX, startY, endX, endY, outputImage);
    }

    public void consequentialDownsize() throws IOException {
        String productPath = uploadedFile.getAbsolutePath() + Instant.now().toEpochMilli() + ".jpg";

        BufferedImage inputImage = ImageIO.read(uploadedFile);

        int newWidth = (int) (inputImage.getWidth() * downsizeFactor);
        int newHeight = (int) (inputImage.getHeight() * downsizeFactor);

        // Create a new image for the downsized result
        BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        // Perform bilinear downsizing
        bilinearDownsize(inputImage, 0, 0, newWidth, newHeight, outputImage);

        System.out.println("Bilinear downsizing complete.");

        ImageIO.write(outputImage, "jpg", new File(productPath));
    }

    private void bilinearDownsize(BufferedImage inputImage, int startX, int startY, int endX, int endY, BufferedImage outputImage) {
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                // Calculate the corresponding coordinates in the original image
                double srcX = x / downsizeFactor;
                double srcY = y / downsizeFactor;

                // Get the four neighboring pixels
                int x1 = (int) Math.floor(srcX);
                int y1 = (int) Math.floor(srcY);
                int x2 = Math.min(x1 + 1, inputImage.getWidth() - 1);
                int y2 = Math.min(y1 + 1, inputImage.getHeight() - 1);

                // Calculate the interpolation weights
                double weightX = srcX - x1;
                double weightY = srcY - y1;

                // Perform bilinear interpolation
                int rgb1 = inputImage.getRGB(x1, y1);
                int rgb2 = inputImage.getRGB(x2, y1);
                int rgb3 = inputImage.getRGB(x1, y2);
                int rgb4 = inputImage.getRGB(x2, y2);

                int interpolatedRGB = interpolateRGB(rgb1, rgb2, rgb3, rgb4, weightX, weightY);

                // Set the pixel value in the output image
                outputImage.setRGB(x, y, interpolatedRGB);
            }
        }
    }

    private int interpolateRGB(int rgb1, int rgb2, int rgb3, int rgb4, double weightX, double weightY) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        int r3 = (rgb3 >> 16) & 0xFF;
        int g3 = (rgb3 >> 8) & 0xFF;
        int b3 = rgb3 & 0xFF;

        int r4 = (rgb4 >> 16) & 0xFF;
        int g4 = (rgb4 >> 8) & 0xFF;
        int b4 = rgb4 & 0xFF;

        int r = (int) (r1 * (1 - weightX) * (1 - weightY) + r2 * weightX * (1 - weightY) + r3 * (1 - weightX) * weightY + r4 * weightX * weightY);
        int g = (int) (g1 * (1 - weightX) * (1 - weightY) + g2 * weightX * (1 - weightY) + g3 * (1 - weightX) * weightY + g4 * weightX * weightY);
        int b = (int) (b1 * (1 - weightX) * (1 - weightY) + b2 * weightX * (1 - weightY) + b3 * (1 - weightX) * weightY + b4 * weightX * weightY);

        return (r << 16) | (g << 8) | b;
    }

    public double getDownsizeFactor() {
        return downsizeFactor;
    }

    public void setDownsizeFactor(double downsizeFactor) {
        this.downsizeFactor = downsizeFactor;
    }

    public File getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(File uploadedFile) {
        this.uploadedFile = uploadedFile;
    }
}
