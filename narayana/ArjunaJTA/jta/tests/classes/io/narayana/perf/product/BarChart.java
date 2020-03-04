/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
