package org.example;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Default values
        String mode = "sequential"; // sequential, parallel
        String inputResourceName = "/home/gjore/IdeaProjects/prog3project/src/main/resources/test10(3840-2160).jpg";


        String outputImagePath = "src/main/resources/output4.jpg";
        String operation = "edge"; // edge, blur, sharpen

        // Parse command line arguments
        if (args.length > 0) {
            mode = args[0].toLowerCase();
        }
        if (args.length > 1) {
            inputResourceName = args[1];
        }
        if (args.length > 2) {
            outputImagePath = args[2];
        }
        if (args.length > 3) {
            operation = args[3].toLowerCase();
        }

        // Define kernels for different operations
        double[][] kernel;
        switch (operation) {
            case "blur":
                kernel = new double[][]{
                    {1/9.0, 1/9.0, 1/9.0},
                    {1/9.0, 1/9.0, 1/9.0},
                    {1/9.0, 1/9.0, 1/9.0}
                };
                break;
            case "sharpen":
                kernel = new double[][]{
                    {0, -1, 0},
                    {-1, 5, -1},
                    {0, -1, 0}
                };
                break;
            case "edge":
            default:
                kernel = new double[][]{
                    {0, -1, 0},
                    {-1, 4, -1},
                    {0, -1, 0}
                };
                break;
        }

        try {
            File inputFile = new File(inputResourceName);
            if (!inputFile.exists()) {
                throw new IOException("File not found: " + inputFile.getAbsolutePath());
            }

            BufferedImage inputImage = ImageIO.read(inputFile);

            System.out.println("Processing image: " + inputResourceName);
            System.out.println("Image dimensions: " + inputImage.getWidth() + "x" + inputImage.getHeight());
            System.out.println("Mode: " + mode);

            // Measure execution time
            long startTime = System.nanoTime();
            BufferedImage outputImage;

            switch (mode) {
                case "sequential":
                    outputImage = ConvolutionProcessor.applyConvolution(inputImage, kernel);
                    break;
                case "parallel":
                    outputImage = ConvolutionProcessor.applyConvolutionParallel(inputImage, kernel);
                    break;
                default:
                    System.err.println("Invalid mode: " + mode + ". Use 'sequential' or 'parallel'");
                    return;
            }

            long endTime = System.nanoTime();
            double executionTime = (endTime - startTime) / 1_000_000.0;

            // Write the output image
            ImageIO.write(outputImage, "jpg", new File(outputImagePath));

            System.out.println("Image processed successfully! Check " + outputImagePath);
            System.out.println("Execution time: " + String.format("%.3f", executionTime) + " ms");
            System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());

        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
