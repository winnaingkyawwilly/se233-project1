package com.project.imagewatermark.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ImageResizeController {

    @FXML
    private ImageView imageView;

    @FXML
    private TextField scaleInput;

    @FXML
    private TextField widthInput;

    @FXML
    private TextField heightInput;

    @FXML
    private Button scaleButton;

    @FXML
    private Button widthButton;

    @FXML
    private Button heightButton;

    @FXML
    private Button saveButton;

    private List<Image> images = new ArrayList<>();
    private List<Image> resizedImages = new ArrayList<>();

    public void initialize(List<Image> images) {
        this.images = images;

        scaleButton.setOnAction(event -> resizeByScale());
        widthButton.setOnAction(event -> resizeByWidth());
        heightButton.setOnAction(event -> resizeByHeight());
        saveButton.setOnAction(event -> saveResizedImages());
    }

    private void resizeByScale() {
        double scaleFactor = Double.parseDouble(scaleInput.getText());
        resizedImages = resizeImages(images, scaleFactor, -1, -1);
        displayResizedImage();
    }

    private void resizeByWidth() {
        int newWidth = Integer.parseInt(widthInput.getText());
        resizedImages = resizeImages(images, -1, newWidth, -1);
        displayResizedImage();
    }

    private void resizeByHeight() {
        int newHeight = Integer.parseInt(heightInput.getText());
        resizedImages = resizeImages(images, -1, -1, newHeight);
        displayResizedImage();
    }

    private List<Image> resizeImages(List<Image> images, double scale, int newWidth, int newHeight) {
        return images.stream()
                .map(image -> resizeImage(image, scale, newWidth, newHeight))
                .collect(Collectors.toList());
    }

    private Image resizeImage(Image image, double scale, int newWidth, int newHeight) {
        int width = (int) (image.getWidth() * scale);
        int height = (int) (image.getHeight() * scale);

        if (newWidth > 0) {
            width = newWidth;
            height = (int) (image.getHeight() * (newWidth / image.getWidth()));
        }

        if (newHeight > 0) {
            height = newHeight;
            width = (int) (image.getWidth() * (newHeight / image.getHeight()));
        }

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        resizedImage.getGraphics().drawImage(bufferedImage, 0, 0, width, height, null);

        return SwingFXUtils.toFXImage(resizedImage, null);
    }

    private void displayResizedImage() {
        if (!resizedImages.isEmpty()) {
            imageView.setImage(resizedImages.get(0));
        }
    }

    private void saveResizedImages() {
        if (!resizedImages.isEmpty()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PNG Images", "*.png"),
                    new FileChooser.ExtensionFilter("JPEG Images", "*.jpg", "*.jpeg"),
                    new FileChooser.ExtensionFilter("ZIP Files", "*.zip")
            );
            fileChooser.setTitle("Save Resized Images");

            // Show the file save dialog
            File selectedFile = fileChooser.showSaveDialog(new Stage());

            if (selectedFile != null) {
                if (selectedFile.getName().toLowerCase().endsWith(".zip")) {
                    saveImagesAsZip(selectedFile);
                } else {
                    saveImagesAsIndividualFiles(selectedFile.getParentFile());
                }
            }
        }
    }

    private void saveImagesAsZip(File zipFile) {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipArchiveOutputStream zos = new ZipArchiveOutputStream(fos)) {

            AtomicInteger counter = new AtomicInteger(0);

            for (Image imgToSave : resizedImages) {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imgToSave, null);

                String fileName = "resized_image_" + counter.getAndIncrement() + ".png";

                ZipArchiveEntry zipEntry = new ZipArchiveEntry(fileName);
                zos.putArchiveEntry(zipEntry);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
                zos.write(byteArrayOutputStream.toByteArray());

                zos.closeArchiveEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveImagesAsIndividualFiles(File saveDirectory) {
        AtomicInteger counter = new AtomicInteger(0);

        for (Image imgToSave : resizedImages) {
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imgToSave, null);

            String fileName = "resized_image_" + counter.getAndIncrement() + ".png";

            File file = new File(saveDirectory, fileName);

            try {
                ImageIO.write(bufferedImage, "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
