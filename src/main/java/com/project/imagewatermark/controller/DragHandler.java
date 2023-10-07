package com.project.imagewatermark.controller;

import com.project.imagewatermark.enums.ResizeType;
import com.project.imagewatermark.jobs.BatchResizer;
import com.project.imagewatermark.jobs.BatchWatermarker;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipException;

public class DragHandler {

    @FXML
    private VBox vbox; // Reference to your VBox in FXML
    @FXML
    private Rectangle rectangle;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private ImageView imageView = new ImageView();
    @FXML
    private TextField watermarkInput = new TextField();
    @FXML
    private ColorPicker watermarkColor = new ColorPicker();
    @FXML
    private Slider sizeSlider = new Slider();
    @FXML
    private Slider rotationSlider = new Slider();
    @FXML
    private Slider opacitySlider = new Slider();
    private List<Image> images = new ArrayList<>();
    private ArrayList<File> files = new ArrayList<>();

    private int currentImageIndex = -1;
    protected List<File> img_data;
    ArrayList<Image> toSave = new ArrayList<>();
    private ImageWatermarker watermarker = new ImageWatermarker();

    // Resize Variables

    @FXML
    private TextField resizeValue;
    @FXML
    private Button resizePercentBtn, resizeWidthBtn, resizeHeightBtn;
    @FXML
    private ColorPicker resizeImgBgColor;
    @FXML
    private Slider resizeImgQuality;
    @FXML
    private ProgressBar resizeProgressBar, watermarkProgressBar;
    @FXML
    private ComboBox<String> resizeImgFormat, watermarkOutputFormat;


    @FXML
    public void initialize() {
        // Set up drag-and-drop events for the VBox
        rectangle.setOnDragOver(this::handleDragOver);
        rectangle.setOnDragExited(this::handleDragExited);
        rectangle.setOnDragDropped(this::handleDragDrop);

        // Set up navigation button actions
        prevButton.setOnAction(event -> showPreviousImage());
        nextButton.setOnAction(event -> showNextImage());

        // Bind the slider value change event to the updateWatermarkPreview method
        sizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateWatermarkPreview());
        rotationSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateWatermarkPreview());
        opacitySlider.valueProperty().addListener((observable, oldValue, newValue) -> updateWatermarkPreview());


        // Resize Section

        resizePercentBtn.setOnAction(event -> resize(ResizeType.PERCENTAGE));
        resizeWidthBtn.setOnAction(event -> resize(ResizeType.WIDTH));
        resizeHeightBtn.setOnAction(event -> resize(ResizeType.HEIGHT));
        resizeImgFormat.getItems().addAll("PNG", "JPEG");
        resizeImgFormat.getSelectionModel().selectFirst();


        watermarkOutputFormat.getItems().addAll("PNG", "JPEG");
        watermarkOutputFormat.getSelectionModel().selectFirst();


    }

    private void resize(ResizeType resizeType) {
        int value = Integer.parseInt(resizeValue.getText());
        Color bgColor = resizeImgBgColor.getValue();
        double quality = resizeImgQuality.getValue() / 100.0f;
        String imageFormat = resizeImgFormat.getValue();


        DirectoryChooser directoryChooser = new DirectoryChooser();

        File selectedDirectory = directoryChooser.showDialog(new Stage());

        if (selectedDirectory != null) {
            BatchResizer batchResizer = new BatchResizer(this.files, resizeType, value, selectedDirectory, bgColor, imageFormat, quality);
            resizeProgressBar.progressProperty().bind(batchResizer.progressProperty());
            new Thread(batchResizer).start();
        }
    }

    @FXML
    public void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.ANY);

            // Change the background color when a file is dragged over the VBox
            rectangle.setStyle("-fx-background-color: lightblue;");
        }
    }

    @FXML
    public void handleDragExited(DragEvent event) {
        // Reset the background color when the drag operation exits
        rectangle.setStyle("-fx-background-color: transparent;");
    }

    @FXML
    public void handleDragDrop(DragEvent event) {
        List<File> files = event.getDragboard().getFiles();
        img_data = event.getDragboard().getFiles();
        images.clear(); // Clear previous images
        currentImageIndex = -1;

        for (File file : files) {
            if (file.isFile()) {
                if (isSupportedImageFile(file)) {
                    Image img = null;
                    try {
                        img = new Image(new FileInputStream(file));
                        images.add(img);
                        toSave.add(img);
                        // Copy file to temp directory
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (isZipFile(file)) {
                    // Handle ZIP files
                    List<Image> extractedImages = null;
                    try {
                        extractedImages = extractImagesFromZip(file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (!extractedImages.isEmpty()) {
                        images.addAll(extractedImages);
                        toSave.addAll(extractedImages);

                    }
                }
            }
        }

        // Show the first image, if available
        showNextImage();

        // Reset the background color after dropping files
        vbox.setStyle("-fx-background-color: transparent;");

        // Update the watermark preview
        updateWatermarkPreview();
    }

    private boolean isZipFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".zip");
    }

    private List<Image> extractImagesFromZip(File zipFile) throws IOException {
        List<Image> extractedImages = new ArrayList<>();
        String tmpDir = System.getProperty("java.io.tmpdir") + "photo-edit/unzip/"
                + zipFile.getName().replace(".zip", "") + "/";
        Path tmpDirFile = Paths.get(tmpDir);
        if (Files.exists(tmpDirFile)) {
            Stream<Path> walk = Files.walk(tmpDirFile);
            walk.map(Path::toFile).forEach(File::delete);
            walk.close();
        }
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new FileInputStream(zipFile))) {
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                File file = new File(entry.getName());
                Path tempFilePath = Paths.get(tmpDir, entry.getName());

                File tempFile = tempFilePath.toFile();
                if (entry.isDirectory()) {
                    tempFile.mkdirs();
                } else {
                    File parent = tempFile.getParentFile();
                    if (!parent.isDirectory()) {
                        parent.mkdirs();
                    }
                }
                Files.copy(zis, tempFilePath);
                if (isSupportedImageFile(file)) {
                    BufferedImage bufferedImage = ImageIO.read(tempFile);
                    Image img = SwingFXUtils.toFXImage(bufferedImage, null);
                    extractedImages.add(img);
                    files.add(tempFile);
                }
            }
        } catch (ZipException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid ZIP file").showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error reading ZIP file").showAndWait();
        }
        return extractedImages;
    }


    private boolean isSupportedImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png");
    }

    private void showPreviousImage() {
        if (!toSave.isEmpty()) {
            currentImageIndex = (currentImageIndex - 1 + toSave.size()) % toSave.size();
            updateImageView();
        }
    }

    private void showNextImage() {
        if (!toSave.isEmpty()) {
            currentImageIndex = (currentImageIndex + 1) % toSave.size();
            updateImageView();
        }
    }

    private void updateImageView() {
        if (currentImageIndex >= 0 && currentImageIndex < toSave.size()) {
            Image img = toSave.get(currentImageIndex);
            imageView.setImage(img);
            vbox.getChildren().clear();
            vbox.getChildren().add(imageView);
        }
    }

    public List<File> getImg_data() {
        return img_data;
    }

    @FXML
    private void updateWatermarkPreview() {
        Color newColor = watermarkColor.getValue();
        int newSize = (int) sizeSlider.getValue();
        String watermarkText = watermarkInput.getText();
        double newOpacity = opacitySlider.getValue();
        double rotationAngle = rotationSlider.getValue();

        if (!watermarkText.isEmpty() && !images.isEmpty()) {
            List<Image> watermarkedImages = new ArrayList<>();

            for (Image img : images) {
                Image watermarkedImage = watermarker.addWatermark(img, watermarkText, newColor, newSize, newOpacity, rotationAngle);
                watermarkedImages.add(watermarkedImage);
            }
            toSave.clear();
            toSave.addAll(watermarkedImages);

            // Display the watermarked images
            imageView.setImage(watermarkedImages.get(currentImageIndex));
            vbox.getChildren().clear();
            vbox.getChildren().add(imageView);
        }
    }

    @FXML
    private void saveImagesAsPngJpg() {
        if (!toSave.isEmpty()) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Save Watermarked Images");
            File selectedDirectory = directoryChooser.showDialog(new Stage());

            if (selectedDirectory != null) {
                // Save as individual PNG or JPG files
                Task<Void> batchWatermarkTask = new BatchWatermarker(toSave, selectedDirectory, watermarkOutputFormat.getValue());
                watermarkProgressBar.progressProperty().bind(batchWatermarkTask.progressProperty());

                (new Thread(batchWatermarkTask)).start();
            }
        }
    }

    private String getFileExtension(String fileName) {
        return FilenameUtils.getExtension(fileName);
    }

    private void saveImagesAsIndividualFiles(File saveDirectory) {
        for (int i = 0; i < toSave.size(); i++) {
            Image imgToSave = toSave.get(i);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imgToSave, null);

            BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            newBufferedImage.createGraphics().drawImage(bufferedImage, null, 0, 0);

            // Determine the file name based on the image format
            String imageFormat = getFileExtension(saveDirectory.getName());
            String fileName = "watermarked_image_" + i + "." + imageFormat;

            File file = new File(saveDirectory.getParentFile(), fileName);

            try {
                ImageIO.write(newBufferedImage, imageFormat, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
