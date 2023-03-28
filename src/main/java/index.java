import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.nio.file.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.io.File;

public class index {
    static final String FILE_PATH = "src/main/java/";

    public static double calculateInverseDocumentFrequency(int numberOfDocumentsContainingWord) {
        int TOTAL_NUMBER_OF_DOCUMENTS = 503;

        return (float) TOTAL_NUMBER_OF_DOCUMENTS / numberOfDocumentsContainingWord;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length < 2) {
            throw new IOException("Argument length 0; required 2 arguments");
        }

        // <term, <documentNumber, termFrequency>>
        Map<String, Map<Integer, Integer>> complexMap = new HashMap<>();
        Map<Integer, Long> processingTimeHashMap = new LinkedHashMap<>();
        int filesProcessed = 0;
        File folder = new File(FILE_PATH + args[0]);
        File[] listOfFiles = folder.listFiles();
        int[] numberOfWordsInDocument = new int[listOfFiles.length];
        int totalNumberOfWords = 0;

        // Create output directory for tokens
        String outputPath = FILE_PATH + args[1] + "/phase_3/";
        Files.createDirectories(Paths.get(outputPath));

        long[] perPageProcessingTime = new long[listOfFiles.length];
        long programStartTime = System.nanoTime();

        for (File file: listOfFiles) {
            if (file.isFile()) {
                filesProcessed++;
                long fileStartTime = System.nanoTime();
                int numberOfWords = 0;
                int documentNumber = Integer.parseInt(file.getName().split("[.]")[0]);

                File input = new File(FILE_PATH + "files/" + file.getName());
                Document doc = Jsoup.parse(input, "UTF-8", "");

                // <term, frequency>
                Map<String, Integer> frequencyMap = new HashMap<>();

                String body = doc.body().text();
                String[] words = body.replaceAll("[^a-zA-Z ]", " ").toLowerCase().split("\\s+");
                for (String word: words) {
                    if (!word.isEmpty()) {
                        Integer frequency = frequencyMap.get(word);
                        frequency = (frequency == null) ? 1 : ++frequency;
                        frequencyMap.put(word, frequency);
                        numberOfWords++;
                    }
                }
                numberOfWordsInDocument[documentNumber - 1] = numberOfWords;
                totalNumberOfWords += numberOfWords;
                for (Map.Entry<String, Integer> item: frequencyMap.entrySet()) {
                    Map<Integer, Integer> innerMap = complexMap.get(item.getKey()) == null ? new HashMap<>() : complexMap.get(item.getKey());
                    innerMap.put(documentNumber, item.getValue());
                    complexMap.put(item.getKey(), innerMap);
                }

                long fileEndTime = System.nanoTime() - fileStartTime;
                perPageProcessingTime[documentNumber - 1] = fileEndTime;

                if (filesProcessed % 50 == 0) {
                    processingTimeHashMap.put(filesProcessed, System.nanoTime() - programStartTime);
                    System.out.printf("Processing time for %d pages is %d \n", filesProcessed, (System.nanoTime() - programStartTime) / 1000000);
                }
            }
        }

        FileWriter dictionaryFW = new FileWriter(outputPath + "dictionary_file.txt");
        FileWriter postingsFW = new FileWriter(outputPath + "postings_file.csv");

        int postingFilePointer = 1;
        for (Map.Entry<String, Map<Integer, Integer>> item: complexMap.entrySet()) {
            String term = item.getKey();
            Map<Integer, Integer> innerMap = item.getValue();
//            int termFrequency = (int) innerMap.values().stream().mapToDouble(d -> d).sum();
            double idf = calculateInverseDocumentFrequency(innerMap.size());
            // write term
            dictionaryFW.write(term + "\n");
            // write number of entries in the postings file
            dictionaryFW.write(innerMap.size() + "\n");
            // write posting file pointer
            dictionaryFW.write(postingFilePointer + "\n");
            for (Map.Entry<Integer, Integer> inner: innerMap.entrySet()) {
                int documentNumber = inner.getKey();
                double termFrequency = (double) inner.getValue() / numberOfWordsInDocument[documentNumber - 1];
                postingsFW.write(documentNumber + "," + termFrequency * idf);
                postingsFW.write("\n");
                postingFilePointer++;
            }
        }

        /*
            ------
            Print statistics
            ------

        for (int i=0; i<perPageProcessingTime.size(); i++) {
            System.out.print(i+1 + ", ");
        }
        System.out.println();
        for (int i=0; i<perPageProcessingTime.size(); i++) {
            System.out.print(perPageProcessingTime.get(i) / 1000000 + ", ");
        }
        System.out.println();

        for (Map.Entry<Integer, Long> map: processingTimeHashMap.entrySet()) {
            System.out.print(map.getKey() + ", ");
        }
        System.out.println();
        for (Map.Entry<Integer, Long> map: processingTimeHashMap.entrySet()) {
            System.out.print(map.getValue() / 1000000 + ", ");
        }
        System.out.println();

        */

        long programElapsedTime = System.nanoTime() - programStartTime;

        System.out.println("Total elapsed time in millis: " + programElapsedTime/1000000);
    }
}
