package org.example;

import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

public class ConvolutionProcessor {

    public static BufferedImage applyConvolution(BufferedImage inputImage, double[][] kernel) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;

        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double red = 0, green = 0, blue = 0;

                for (int i = 0; i < kernelWidth; i++) {
                    for (int j = 0; j < kernelHeight; j++) {
                        int pixelX = x + i - kernelWidth / 2;
                        int pixelY = y + j - kernelHeight / 2;

                        if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                            int rgb = inputImage.getRGB(pixelX, pixelY);
                            red += ((rgb >> 16) & 0xFF) * kernel[i][j];
                            green += ((rgb >> 8) & 0xFF) * kernel[i][j];
                            blue += (rgb & 0xFF) * kernel[i][j];
                        }
                    }
                }

                int r = clamp((int) red, 0, 255);
                int g = clamp((int) green, 0, 255);
                int b = clamp((int) blue, 0, 255);

                outputImage.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return outputImage;
    }

    public static BufferedImage applyConvolutionParallel(BufferedImage inputImage, double[][] kernel) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;
        int kx = kernelWidth / 2;
        int ky = kernelHeight / 2;

        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

        IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                double red = 0, green = 0, blue = 0;

                for (int j = 0; j < kernelHeight; j++) {
                    int pixelY = clamp(y + j - ky, 0, height - 1);
                    for (int i = 0; i < kernelWidth; i++) {
                        int pixelX = clamp(x + i - kx, 0, width - 1);
                        int rgb = inputImage.getRGB(pixelX, pixelY);
                        double weight = kernel[i][j];
                        red += ((rgb >> 16) & 0xFF) * weight;
                        green += ((rgb >> 8) & 0xFF) * weight;
                        blue += (rgb & 0xFF) * weight;
                    }
                }

                int r = clamp((int) Math.round(red), 0, 255);
                int g = clamp((int) Math.round(green), 0, 255);
                int b = clamp((int) Math.round(blue), 0, 255);

                outputImage.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        });

        return outputImage;
    }

    public static BufferedImage applyMirror(BufferedImage inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int mirroredX = width - 1 - x;
                int rgb = inputImage.getRGB(mirroredX, y);
                outputImage.setRGB(x, y, rgb);
            }
        }

        return outputImage;
    }

    public static BufferedImage applyMirrorParallel(BufferedImage inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

        IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                int mirroredX = width - 1 - x;
                int rgb = inputImage.getRGB(mirroredX, y);
                outputImage.setRGB(x, y, rgb);
            }
        });

        return outputImage;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
