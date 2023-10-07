package com.project.imagewatermark.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ImageWatermarker {

    public static List<Image> addWatermarkToImages(List<Image> images, String watermarkText, Color newColor, int newSize, double opacity, double rotationAngleDegrees) {
        return images.parallelStream()
                .map(image -> addWatermark(image, watermarkText, newColor, newSize, opacity, rotationAngleDegrees))
                .collect(Collectors.toList());
    }

    public static Image addWatermark(Image image, String watermarkText, Color newColor, int newSize, double opacity, double rotationAngleDegrees) {
        BufferedImage originalImage = SwingFXUtils.fromFXImage(image, null);
        BufferedImage watermarkedImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        java.awt.Graphics2D graphics = watermarkedImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, null);

        int fontSize = newSize;
        Font javafxFont = Font.font("Arial", FontWeight.BOLD, fontSize);
        java.awt.Font awtFont = new java.awt.Font(
                javafxFont.getFamily(),
                java.awt.Font.PLAIN,
                fontSize
        );

        Color fxColor = newColor;
        java.awt.Color awtColor = new java.awt.Color(
                (float) fxColor.getRed(),
                (float) fxColor.getGreen(),
                (float) fxColor.getBlue(),
                (float) opacity
        );

        graphics.setColor(awtColor);
        graphics.setFont(awtFont);

        int x = (watermarkedImage.getWidth() - graphics.getFontMetrics().stringWidth(watermarkText)) / 2;
        int y = watermarkedImage.getHeight() / 2;

        if (rotationAngleDegrees != 0.0) {
            double centerX = x + graphics.getFontMetrics().stringWidth(watermarkText) / 2.0;
            double centerY = y;
            graphics.translate(centerX, centerY);
            graphics.rotate(Math.toRadians(rotationAngleDegrees));
            graphics.translate(-centerX, -centerY);
        }

        graphics.drawString(watermarkText, x, y);
        graphics.dispose();

        return SwingFXUtils.toFXImage(watermarkedImage, null);
    }

    public static void saveWatermarkedImages(List<Image> images, File outputDirectory) {
        AtomicInteger counter = new AtomicInteger(0);

        images.forEach(image -> {
            File outputFile = new File(outputDirectory, "watermarked_image_" + counter.getAndIncrement() + ".png");

            try {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(bufferedImage, "png", outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
