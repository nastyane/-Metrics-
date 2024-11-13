package com.vk.itmo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.objectweb.asm.ClassReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java -jar itmo.jar <path_file.jar>");
            return;
        }
        String jarFilePath = args[0];
        Path jar = Path.of(jarFilePath);

        ClassesAggregator aggregator = new ClassesAggregator();

        try (InputStream fileJar = Files.newInputStream(jar)) {
            try (ZipInputStream zipInputStream = new ZipInputStream(fileJar)) {
                ZipEntry ze;
                while ((ze = zipInputStream.getNextEntry()) != null) {


                    if (ze.getName().endsWith(".class")) {
                        byte[] bytes = zipInputStream.readAllBytes();
                        new ClassReader(bytes).accept(aggregator, ClassReader.SKIP_DEBUG);
                    }
                }
            }
        }

        Metrics metrics = aggregator.calculateMetrics();
        System.out.println("Average inheritance: " + metrics.averageInheritance());
        System.out.println("Max inheritance: " + metrics.maxInheritance());
        System.out.println("Average override: " + metrics.averageCountOverride());
        System.out.println("Average field: " + metrics.averageCountField());
        System.out.println("Metric ABC: " + metrics.metricABC());
        try (FileWriter fw = new FileWriter("src/results.json")) {
            GSON.toJson(metrics, fw);
        }
    }
}





