package com.project.imagewatermark.jobs;

import com.project.imagewatermark.enums.ResizeType;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatchResizer extends Task<Void> {
    private ArrayList<File> imageFiles;
    private ResizeType resizeType;
    private int resizeValue;
    private File outputDir;
    private Color backgroundColor;
    private String outputFormat;
    private double imgQuality;

    public BatchResizer(ArrayList<File> imageFiles, ResizeType resizeType, int resizeValue, File outputDir, Color backgroundColor, String outputFormat, double imgQuality) {
        this.imageFiles = imageFiles;
        this.resizeType = resizeType;
        this.resizeValue = resizeValue;
        this.outputDir = outputDir;
        this.backgroundColor = backgroundColor;
        this.outputFormat = outputFormat;
        this.imgQuality = imgQuality;
    }

    @Override
    protected Void call() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        ExecutorCompletionService<Void> executorCompletionService = new ExecutorCompletionService<>(executorService);

        imageFiles.stream().forEach(imageFile -> {
            executorCompletionService.submit(new ResizeTask(imageFile, resizeType, resizeValue, outputDir, backgroundColor, outputFormat, imgQuality));
        });


        for (int i = 0; i < imageFiles.size(); i++) {
            executorCompletionService.take();
            updateProgress(i + 1, imageFiles.size());
        }

        executorService.close();
        return null;
    }
}
