/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class BarChart {
    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    private Color[] COLORS = new Color [] {
            Color.blue,
            Color.cyan,
            Color.yellow,
            Color.green,
            Color.darkGray,
            Color.red
    };

    public JFreeChart generateChart(String title, String xaxisLabel, String yaxisLabel) {
        JFreeChart chart = ChartFactory.createBarChart(title, xaxisLabel, yaxisLabel,
                dataset, PlotOrientation.VERTICAL, true, false, false);
        CategoryPlot plot = chart.getCategoryPlot();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        chart.setBackgroundPaint(Color.white);

        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinesVisible(true);

        plot.setDomainGridlinePaint(Color.black);
        plot.setRangeGridlinePaint(Color.black);

        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());

        BarRenderer renderer = (BarRenderer)chart.getCategoryPlot().getRenderer();

        for (int i = 0; i < dataset.getRowCount(); i++) {
            Color color = i < COLORS.length ? COLORS[i] : COLORS[0];
            renderer.setSeriesPaint(i, color);
        }

        return chart;
    }

    public void generateImage(String imageFileName, String formatName, String title, String xaxisLable, String yaxisLabel) throws IOException {
        writeImageData(generateChart(title, xaxisLable, yaxisLabel), imageFileName, formatName);
    }

    public void addDataPoint(Double value, String rowKey, String columnKey) {
        dataset.addValue(value, rowKey, columnKey);
    }

    private void writeImageData(JFreeChart chart, String imageFileName) throws IOException {
        ChartUtilities.saveChartAsPNG(new File(imageFileName), chart, 600, 300);
    }

    private void writeImageData(JFreeChart chart, String imageFileName, String formatName) throws IOException {
        BufferedImage objBufferedImage = chart.createBufferedImage(600, 300);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(objBufferedImage, "png", baos);
        byte[] byteArray = baos.toByteArray();
        InputStream in = new ByteArrayInputStream(byteArray);
        BufferedImage image = ImageIO.read(in);
        File outputfile = new File(imageFileName);

        ImageIO.write(image, formatName, outputfile);

        System.out.printf("GENERATED IMAGE FILE TO %s%n", outputfile.getCanonicalPath());
    }
}