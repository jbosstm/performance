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
package io.narayana.perf.jmh;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BMParser {
    BufferedReader reader;

    public BMParser() {
    }

    public BMParser(String fname) throws IOException {
        reader = new BufferedReader(new FileReader(fname));
        // read the header
        reader.readLine();
    }

    public Benchmark nextBenchmark() throws IOException {
        return parseBenchmark(reader.readLine());
    }

    public Benchmark parseBenchmark(String line) throws IOException {
        Benchmark bm = new Benchmark();

        if (line == null)
            return null;

        String[] values = line.split( "," );

        bm.benchmark = stripQuotes(values[0]);
        bm.mode = stripQuotes(values[1]);
        bm.threads = Double.valueOf(stripQuotes(values[2])).intValue();
        bm.samples = Double.valueOf(stripQuotes(values[3])).intValue();
        bm.score = Double.valueOf(stripQuotes(values[4]));
        if (!values[5].equals( "NaN" ) )
            bm.scoreError = Double.valueOf(values[5]);

        bm.unit = stripQuotes(values[6]);

        return bm;
    }

    public static Map<String, Benchmark> readBenchmarks(String fname) throws IOException {
        BMParser parser = new BMParser(fname);
        Map<String, Benchmark> benchmarks = new HashMap<>();
        Benchmark bm = parser.nextBenchmark();

        while (bm != null) {
            if (!bm.benchmark.contains(":·stack")) {
                benchmarks.put(bm.benchmark, bm);
            }
            bm = parser.nextBenchmark();
        }

        return benchmarks;
    }

    private String stripQuotes( String quotedStr ) {
        return quotedStr.replace( "\"", "" );
    }
}
