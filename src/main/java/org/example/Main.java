package org.example;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Default values
        String mode = "sequential"; // sequential, parallel
        String inputImagePath = "src/main/resources/input4.jpg";
        String outputImagePath = "src/main/resources/output4.jpg";

        // Parse command line arguments
        if (args.length > 0) {
            mode = args[0].toLowerCase();
        }
        if (args.length > 1) {
            inputImagePath = args[1];
        }
        if (args.length > 2) {
            outputImagePath = args[2];
        }

        // Defining kernel (edge Detection)
        double[][] kernel = {
                {0, -1, 0},
                {-1, 4, -1},
                {0, -1, 0}
        };

        try {
            // Read the input image
            BufferedImage inputImage = ImageIO.read(new File(inputImagePath));
            System.out.println("Processing image: " + inputImagePath);
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
            double executionTimeMs = (endTime - startTime) / 1_000_000.0;

            // Write the output image
            ImageIO.write(outputImage, "jpg", new File(outputImagePath));

            System.out.println("Image processed successfully! Check " + outputImagePath);
            System.out.println("Execution time: " + String.format("%.2f", executionTimeMs) + " ms");
            System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());

        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
