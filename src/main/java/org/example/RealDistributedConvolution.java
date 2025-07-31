package org.example;

import mpi.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class RealDistributedConvolution {

    public static void main(String[] args) throws Exception {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        final int MASTER = 0;
        BufferedImage inputImage = null;

        // Default to edge detection kernel
        double[][] kernel = {
                {0, -1, 0},
                {-1, 4, -1},
                {0, -1, 0}
        };

        String operation = "Edge Detection";
        long startTime = 0;

        if (rank == MASTER) {
            // Read operation type from file if it exists
            File operationFile = new File("src/main/resources/gui_operation.txt");
            if (operationFile.exists()) {
                try (java.util.Scanner scanner = new java.util.Scanner(operationFile)) {
                    if (scanner.hasNextLine()) {
                        operation = scanner.nextLine().trim();
                        switch (operation.toLowerCase()) {
                            case "blur":
                                operation = "Blur";
                                kernel = new double[][]{
                                        {1/16.0, 2/16.0, 1/16.0},
                                        {2/16.0, 4/16.0, 2/16.0},
                                        {1/16.0, 2/16.0, 1/16.0}
                                };
                                break;
                            case "sharpen":
                                operation = "Sharpen";
                                kernel = new double[][]{
                                        {0, -1, 0},
                                        {-1, 5, -1},
                                        {0, -1, 0}
                                };
                                break;
                            case "edge detection":
                            case "edge":
                            default:
                                operation = "Edge Detection";
                                // Keep default kernel
                                break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not read operation file, using default (Edge Detection)");
                }
            }

            System.out.println("Starting real distributed convolution with " + size + " processes");
            System.out.println("Operation: " + operation);
            startTime = System.nanoTime();

            long setupStartTime = System.nanoTime();

            // Load input image - check for GUI input first, then default
            File inputFile = new File("src/main/resources/gui_input_temp.jpg");
            if (!inputFile.exists()) {
                inputFile = new File("src/main/resources/input4.jpg");
                System.out.println("Using default image: input4.jpg");
            } else {
                System.out.println("Using GUI input image: gui_input_temp.jpg");
            }

            inputImage = ImageIO.read(inputFile);
            int height = inputImage.getHeight();
            int width = inputImage.getWidth();

            System.out.println("Image dimensions: " + width + "x" + height);
            System.out.println("Processing with " + (size - 1) + " worker processes");

            long setupEndTime = System.nanoTime();
            long distributionStartTime = System.nanoTime();

            // Broadcast kernel to all worker processes
            double[] flatKernel = {kernel[0][0], kernel[0][1], kernel[0][2],
                    kernel[1][0], kernel[1][1], kernel[1][2],
                    kernel[2][0], kernel[2][1], kernel[2][2]};
            for (int i = 1; i < size; i++) {
                MPI.COMM_WORLD.Send(flatKernel, 0, 9, MPI.DOUBLE, i, 99);
            }

            // Calculate chunk distribution with ghost cells for proper edge detection
            int kernelRadius = 1; // For 3x3 kernel, we need 1 pixel overlap
            int chunkHeight = height / (size - 1);
            int remainder = height % (size - 1);

            // Send image chunks to worker processes with overlap
            for (int i = 1; i < size; i++) {
                int yStart = (i - 1) * chunkHeight;
                int yEnd = yStart + chunkHeight + (i == size - 1 ? remainder : 0);

                // Add ghost cells (padding) for boundary handling
                int yStartWithPadding = Math.max(0, yStart - kernelRadius);
                int yEndWithPadding = Math.min(height, yEnd + kernelRadius);
                int actualChunkHeight = yEndWithPadding - yStartWithPadding;

                // Calculate padding amounts
                int paddingTop = yStart - yStartWithPadding;
                int paddingBottom = yEndWithPadding - yEnd;
                int validHeight = yEnd - yStart; // Height of the valid region (without padding)

                int[] pixels = new int[width * actualChunkHeight];
                inputImage.getRGB(0, yStartWithPadding, width, actualChunkHeight, pixels, 0, width);

                // Send metadata: [width, actualChunkHeight, yStart, paddingTop, validHeight]
                int[] metadata = {width, actualChunkHeight, yStart, paddingTop, validHeight};
                MPI.COMM_WORLD.Send(metadata, 0, 5, MPI.INT, i, 0);

                // Send pixel data
                MPI.COMM_WORLD.Send(pixels, 0, pixels.length, MPI.INT, i, 1);

                System.out.println("Sent chunk " + i + " (y=" + yStart + "-" + yEnd + ", with padding " + yStartWithPadding + "-" + yEndWithPadding + ") to process " + i);
            }

            long distributionEndTime = System.nanoTime();
            long collectionStartTime = System.nanoTime();

            // Collect processed results
            BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

            for (int i = 1; i < size; i++) {
                // Receive metadata
                int[] receivedMeta = new int[3];
                MPI.COMM_WORLD.Recv(receivedMeta, 0, 3, MPI.INT, i, 2);
                int w = receivedMeta[0];
                int h = receivedMeta[1];
                int yStart = receivedMeta[2];

                // Receive processed pixel data
                int[] processedPixels = new int[w * h];
                MPI.COMM_WORLD.Recv(processedPixels, 0, processedPixels.length, MPI.INT, i, 3);

                // Set processed pixels in output image
                outputImage.setRGB(0, yStart, w, h, processedPixels, 0, w);

                System.out.println("Received processed chunk from process " + i);
            }

            // Save output image and measure time
            ImageIO.write(outputImage, "jpg", new File("src/main/resources/output_real_distributed.jpg"));

            long endTime = System.nanoTime();
            long collectionEndTime = endTime;
            double executionTimeMs = (endTime - startTime) / 1_000_000.0;

            // Calculate timing breakdown
            double setupTimeMs = (setupEndTime - setupStartTime) / 1_000_000.0;
            double distributionTimeMs = (distributionEndTime - distributionStartTime) / 1_000_000.0;
            double collectionTimeMs = (collectionEndTime - collectionStartTime) / 1_000_000.0;

            System.out.println("Real distributed convolution completed successfully!");
            System.out.println("Output saved to: src/main/resources/output_real_distributed.jpg");
            System.out.println("=== DETAILED TIMING BREAKDOWN ===");
            System.out.println("Setup time (image loading):     " + String.format("%8.3f", setupTimeMs) + " ms");
            System.out.println("Distribution time (send chunks): " + String.format("%8.3f", distributionTimeMs) + " ms");
            System.out.println("Collection time (receive results):" + String.format("%8.3f", collectionTimeMs) + " ms");
            System.out.println("Total execution time:           " + String.format("%8.3f", executionTimeMs) + " ms");
            System.out.println("Available processes: " + size);

        } else {
            // Worker process
            System.out.println("Worker process " + rank + " started");

            // Receive kernel from master
            double[] flatKernel = new double[9];
            MPI.COMM_WORLD.Recv(flatKernel, 0, 9, MPI.DOUBLE, MASTER, 99);

            // Reconstruct 3x3 kernel
            kernel = new double[][]{
                    {flatKernel[0], flatKernel[1], flatKernel[2]},
                    {flatKernel[3], flatKernel[4], flatKernel[5]},
                    {flatKernel[6], flatKernel[7], flatKernel[8]}
            };

            System.out.println("Worker " + rank + " received kernel: [" +
                    flatKernel[0] + ", " + flatKernel[1] + ", " + flatKernel[2] + "; " +
                    flatKernel[3] + ", " + flatKernel[4] + ", " + flatKernel[5] + "; " +
                    flatKernel[6] + ", " + flatKernel[7] + ", " + flatKernel[8] + "]");

            // Receive metadata: [width, actualChunkHeight, yStart, paddingTop, validHeight]
            int[] metadata = new int[5];
            MPI.COMM_WORLD.Recv(metadata, 0, 5, MPI.INT, MASTER, 0);
            int width = metadata[0];
            int height = metadata[1]; // This is actualChunkHeight (with padding)
            int yOffset = metadata[2]; // This is yStart (original position)
            int paddingTop = metadata[3];
            int validHeight = metadata[4]; // Height of valid region without padding

            // Receive pixel data
            int[] receivedPixels = new int[width * height];
            MPI.COMM_WORLD.Recv(receivedPixels, 0, receivedPixels.length, MPI.INT, MASTER, 1);

            System.out.println("Process " + rank + " received chunk of size " + width + "x" + height + " (valid: " + width + "x" + validHeight + ")");

            // Create BufferedImage from received pixels (with padding)
            BufferedImage chunk = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            chunk.setRGB(0, 0, width, height, receivedPixels, 0, width);

            // Apply convolution to the full chunk (including padding)
            long chunkStartTime = System.nanoTime();
            BufferedImage processedChunk = ConvolutionProcessor.applyConvolution(chunk, kernel);
            long chunkEndTime = System.nanoTime();

            System.out.println("Process " + rank + " completed convolution in " +
                    String.format("%.3f", (chunkEndTime - chunkStartTime) / 1_000_000.0) + " ms");

            // Extract only the valid region (remove padding) from processed result
            BufferedImage validRegion = processedChunk.getSubimage(0, paddingTop, width, validHeight);

            // Extract processed pixels from valid region only
            int[] resultPixels = new int[width * validHeight];
            validRegion.getRGB(0, 0, width, validHeight, resultPixels, 0, width);

            // Send results back to master (original valid dimensions)
            int[] resultMeta = {width, validHeight, yOffset};
            MPI.COMM_WORLD.Send(resultMeta, 0, 3, MPI.INT, MASTER, 2);
            MPI.COMM_WORLD.Send(resultPixels, 0, resultPixels.length, MPI.INT, MASTER, 3);

            System.out.println("Process " + rank + " sent results back to master");
        }

        MPI.Finalize();
        System.out.println("Process " + rank + " finished");
    }
} 