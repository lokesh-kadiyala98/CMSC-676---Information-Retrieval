import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.nio.file.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.io.File;

public class Tokenize {
    static final String FILE_PATH = "src/main/java/";

    public static void dumpTokensToFile(String[] words, String filePath) throws IOException {
        // open file for write
        FileWriter fw = new FileWriter(filePath);
        for (String word: words) {
            fw.write(word);
            fw.write("\n");
        }

        fw.close();
    }

    public static void sortByToken(SortedSet<String> sortedSet, Map<String, Integer> hashMap, String filePath) throws IOException {
        FileWriter fw = new FileWriter(filePath);

        for (String word: sortedSet) {
            fw.write(word + " " + hashMap.get(word));
            fw.write("\n");
        }

        fw.close();
    }

    public static void sortByFrequency(Map<String, Integer> sortedHashMap, String filePath) throws IOException {
        FileWriter fw = new FileWriter(filePath);

        for (Map.Entry<String, Integer> map: sortedHashMap.entrySet()) {
            fw.write(map.getKey() + " " + map.getValue());
            fw.write("\n");
        }

        fw.close();
    }

    public static HashMap<String, Integer> sortByValue(Map<String, Integer> hm) {
        List<Map.Entry<String, Integer>> list =
                new LinkedList<>(hm.entrySet());

        Collections.sort(list, Map.Entry.comparingByValue());

        HashMap<String, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        Map<String, Integer> hashMap = new HashMap<>();
        Map<Integer, Long> processingTimeHashMap = new LinkedHashMap<>();
        int filesProcessed = 0;

        if (args.length < 2) {
            throw new IOException("Argument length 0; required 2 arguments");
        }
        File folder = new File(FILE_PATH + args[0]);
        File[] listOfFiles = folder.listFiles();

        // Create output directory for tokens
        String tokensOutputPath = FILE_PATH + args[1] + "/tokens/";
        Files.createDirectories(Paths.get(tokensOutputPath));

        long[] perPageProcessingTime = new long[listOfFiles.length];
        long programStartTime = System.nanoTime();

        for (File file: listOfFiles) {
            if (file.isFile()) {
                filesProcessed++;
                long fileStartTime = System.nanoTime();
                String fileName = file.getName().split("[.]")[0];

                File input = new File(FILE_PATH + "files/" + file.getName());
                Document doc = Jsoup.parse(input, "UTF-8", "");

                String body = doc.body().text();
                String[] words = body.replaceAll("[^a-zA-Z ]", " ").toLowerCase().split("\\s+");
                for (String word: words) {
                    if (!word.isEmpty()) {
                        Integer frequency = hashMap.get(word);
                        frequency = (frequency == null) ? 1 : ++frequency;
                        hashMap.put(word, frequency);
                    }
                }
                dumpTokensToFile(words, tokensOutputPath + fileName + ".txt");

                long fileEndTime = System.nanoTime() - fileStartTime;
                perPageProcessingTime[Integer.parseInt(fileName) - 1] = fileEndTime;

                if (filesProcessed % 50 == 0) {
                    processingTimeHashMap.put(filesProcessed, System.nanoTime() - programStartTime);
                    System.out.printf("Processing time for %d pages is %d \n", filesProcessed, (System.nanoTime() - programStartTime) / 1000000);
                }
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

        SortedSet<String> sortedSet = new TreeSet<>(hashMap.keySet());
        sortByToken(sortedSet, hashMap, FILE_PATH + args[1] + "/sorted_by_token.txt");

        Map<String, Integer> sortedHashMap = sortByValue(hashMap);
        sortByFrequency(sortedHashMap, FILE_PATH + args[1] + "/sorted_by_frequency.txt");

        long programElapsedTime = System.nanoTime() - programStartTime;

        System.out.println("Total elapsed time in millis: " + programElapsedTime/1000000);
    }
}
