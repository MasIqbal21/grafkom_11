package com.grafkom.visualrealism;

/**
 * Entry point.
 * Jalankan dengan: mvn compile exec:java
 * atau: mvn package && java -jar target/visual-realism-suite.jar
 *
 * Catatan macOS: tambahkan opsi JVM -XstartOnFirstThread (lihat README.md).
 */
public class Main {
    public static void main(String[] args) {
        new VisualRealismSuite().run();
    }
}
