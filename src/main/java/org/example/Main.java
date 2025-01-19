package org.example;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Input and output file paths
        String inputImagePath = "src/main/resources/input4.jpg";  // Update with your image path
        String outputImagePath = "src/main/resources/output5.jpg";

        // Define a kernel (Edge Detection)
        double[][] kernel = {
                {0, -1, 0},
                {-1, 4, -1},
                {0, -1, 0}
        };

        try {
            // Read the input image
            BufferedImage inputImage = ImageIO.read(new File(inputImagePath));

            // Apply convolution
            BufferedImage outputImage = ConvolutionProcessor.applyConvolution(inputImage, kernel);

            // Write the output image
            ImageIO.write(outputImage, "jpg", new File(outputImagePath));

            System.out.println("Image processed successfully! Check " + outputImagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
