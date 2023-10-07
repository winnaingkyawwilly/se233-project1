package com.project.imagewatermark.jobs;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class BatchWatermarker extends Task<Void> {
    private ArrayList<Image> imgToSave;
    private File saveDirectory;
    private String outputFormat;

    public BatchWatermarker(ArrayList<Image> image, File saveDirectory, String outputFormat) {
        this.imgToSave = image;
        this.saveDirectory = saveDirectory;
        this.outputFormat = outputFormat;
    }

    @Override
    protected Void call() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        ExecutorCompletionService<Void> executorCompletionService = new ExecutorCompletionService<>(executorService);
        imgToSave = imgToSave.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
        for (int i = 0; i < imgToSave.size(); i++) {
            executorCompletionService.submit(new WatermarkTask(imgToSave.get(i), saveDirectory, outputFormat, i));
        }
        for (int i = 0; i < imgToSave.size(); i++) {
            executorCompletionService.take();
            updateProgress(i + 1, imgToSave.size());
        }

        return null;

    }
}
