package com.cleaner.util;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;

public class LogoGenerator {

    public static Image generateLogo(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Background circle with gradient
        LinearGradient bgGradient = new LinearGradient(0, 0, size, size, false, null,
            new Stop(0, Color.web("#1f883d")),
            new Stop(1, Color.web("#2ea043"))
        );
        gc.setFill(bgGradient);
        gc.fillOval(2, 2, size - 4, size - 4);

        // White brush/broom icon
        gc.setFill(Color.WHITE);

        // Handle
        double handleWidth = size * 0.1;
        double handleHeight = size * 0.4;
        double handleX = (size - handleWidth) / 2;
        double handleY = size * 0.15;
        gc.fillRoundRect(handleX, handleY, handleWidth, handleHeight, 3, 3);

        // Brush head
        double brushWidth = size * 0.4;
        double brushHeight = size * 0.2;
        double brushX = (size - brushWidth) / 2;
        double brushY = handleY + handleHeight - 5;
        gc.fillRoundRect(brushX, brushY, brushWidth, brushHeight, 5, 5);

        // Bristles
        double bristleY = brushY + brushHeight;
        double bristleHeight = size * 0.15;
        double bristleWidth = size * 0.04;
        double bristleStartX = brushX + brushWidth * 0.1;
        double bristleSpacing = brushWidth / 5;

        for (int i = 0; i < 5; i++) {
            double x = bristleStartX + i * bristleSpacing;
            double h = bristleHeight + (i % 2 == 0 ? 5 : -3);
            gc.fillRoundRect(x, bristleY, bristleWidth, h, 1, 1);
        }

        // Sparkles
        gc.setFill(Color.WHITE);
        double sparkleSize = size * 0.04;
        gc.fillOval(size * 0.2, size * 0.25, sparkleSize, sparkleSize);
        gc.fillOval(size * 0.75, size * 0.3, sparkleSize * 0.7, sparkleSize * 0.7);
        gc.fillOval(size * 0.8, size * 0.2, sparkleSize * 1.2, sparkleSize * 1.2);

        // Star shape
        drawStar(gc, size * 0.22, size * 0.4, sparkleSize * 1.5);
        drawStar(gc, size * 0.78, size * 0.45, sparkleSize * 1.2);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = new WritableImage(size, size);
        canvas.snapshot(params, image);
        return image;
    }

    private static void drawStar(GraphicsContext gc, double x, double y, double size) {
        double[] points = new double[10];
        for (int i = 0; i < 5; i++) {
            double angle = i * 72 - 90;
            double r = i % 2 == 0 ? size : size / 2;
            points[i * 2] = x + r * Math.cos(Math.toRadians(angle));
            points[i * 2 + 1] = y + r * Math.sin(Math.toRadians(angle));
        }
        gc.fillPolygon(
            new double[]{points[0], points[2], points[4], points[6], points[8]},
            new double[]{points[1], points[3], points[5], points[7], points[9]},
            5
        );
    }
}