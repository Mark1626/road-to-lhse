package org.example;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SimpleSearcher {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: SimpleSearcher <index-location> <term>");
            return;
        }


        try (
                var fsdir = FSDirectory.open(Paths.get(args[0]));
                DirectoryReader reader = DirectoryReader.open(fsdir)
        ) {
            String searchTerm = args[1];

            IndexSearcher searcher = new IndexSearcher(reader);
            TermQuery termQuery = new TermQuery(new Term("text", searchTerm));
            TopDocs topDocs = searcher.search(termQuery, 10);
            System.out.println("Query " + termQuery + " matched " + topDocs.totalHits + " documents:");
            StoredFields storedFields = reader.storedFields();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                String storedText = storedFields.document(scoreDoc.doc).get("text");
                System.out.println(scoreDoc.score + " - " + scoreDoc.doc + " - " + storedText);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
