package org.example;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SimpleIndexer {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: SimpleIndexer <index-location> <file-to-index>");
            return;
        }

        try (
                Directory directory = FSDirectory.open(Path.of(args[0]));
                FileReader fr = new FileReader(Path.of(args[1]).toFile());
                BufferedReader br = new BufferedReader(fr);
        ) {
            IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig());
            String line = br.readLine();
            while (line != null) {
                System.out.println(line);
                writer.addDocument(List.of(new TextField("text", line, Field.Store.YES)));
                line = br.readLine();
            }
        }
    }
}
