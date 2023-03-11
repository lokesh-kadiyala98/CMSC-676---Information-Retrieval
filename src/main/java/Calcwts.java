import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.file.Paths;

public class Calcwts {

    static final String FILE_PATH = "src/main/java/";

    static Map<String, Integer> termToDocumentsHashMap = new HashMap<>();
    static Set<String> uniqueWords = new HashSet<>();
    static Set<String> stopWords = new HashSet<>();

    /*
        This method preprocesses the wordFrequencyMap.
        Remove stop-words, words that occur only once in the entire corpus,
        and words of length 1.
     */
    public static int preprocess(Map<String, Integer> wordFrequencyMap) {
        int tokensCount = 0;
        Iterator<Map.Entry<String, Integer>> iterator = wordFrequencyMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();

            if (stopWords.contains(entry.getKey()) || uniqueWords.contains(entry.getKey()) || entry.getKey().length() == 1) {
                iterator.remove();
            } else {
                tokensCount += entry.getValue();
                int frequency = termToDocumentsHashMap.get(entry.getKey()) == null ? 1 : termToDocumentsHashMap.get(entry.getKey()) + 1;
                termToDocumentsHashMap.put(entry.getKey(), frequency);
            }
        }
//        System.out.println(stopWords.contains("an"));
        return tokensCount;
    }

    public static void setUniqueWords() throws IOException {
        File stopWordsFile = new File(FILE_PATH + "/output/sorted_by_frequency.txt");
        Scanner sc = new Scanner(stopWordsFile);
        while (sc.hasNextLine()) {
            String[] line = sc.nextLine().split("\\s+");
            if (Integer.valueOf(line[1]) > 1)
                break;
            uniqueWords.add(line[0]);
        }
    }

    public static void setStopWords() throws IOException {
        File stopWordsFile = new File(FILE_PATH + "stopwords.txt");
        Scanner sc = new Scanner(stopWordsFile);
        while (sc.hasNextLine()) {
            String word = sc.nextLine().replaceAll("[^a-zA-Z ]", " ").toLowerCase().split("\\s+")[0];
            stopWords.add(word);
        }
    }

    public static void calculateTermFrequencies(Map<String, Integer> wordFrequencyMap, int tokensCount, String filePath) throws IOException {
        FileWriter fw = new FileWriter(filePath);
        CSVWriter writer = new CSVWriter(fw);
        writer.writeNext(new String[]{"token", "term_weight"});
        for(Map.Entry<String, Integer> map: wordFrequencyMap.entrySet()) {
            String[] line = {map.getKey(), String.valueOf(map.getValue()), String.valueOf((float)map.getValue() / tokensCount)};
            writer.writeNext(line);
        }
        writer.close();
        fw.close();
    }

    public static void appendTermWeights(Map<String, Float> idfMap, String filePath) throws IOException {
        FileReader fr = new FileReader(filePath);
        CSVReader reader = new CSVReader(fr);
        List<String[]> allLines = reader.readAll();
        int index = 0;

        for (String[] line: allLines) {
            // skip header
            if (index == 0) {
                index++;
                continue;
            }
            // System.out.println(line[0] + " " + filePath);
            String word = line[0];
            String[] newLine = new String[2];
            newLine[0] = line[0];
//            newLine[1] = line[1];
//            newLine[2] = line[2];
//            newLine[3] = String.valueOf(idfMap.get(word));
            newLine[1] = String.valueOf(idfMap.get(word) * Float.parseFloat(line[2]));
            allLines.set(index, newLine);
            index++;
        }
        FileWriter fw = new FileWriter(filePath);
        CSVWriter writer = new CSVWriter(fw);
        writer.writeAll(allLines);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IOException("Argument length 0; required 2 arguments");
        }
        int filesProcessed = 0;
        Set<Integer> timeMapperSet = new HashSet<>(Arrays.asList(10, 20, 30, 40, 80, 100, 200, 300, 400, 500));
        HashMap<Integer, Long> processingTimeHashMap = new LinkedHashMap<>();
        HashMap<String, Float> idfMap = new HashMap<>();

        // dump stop words into a set
        setStopWords();
        // dump unique words into a set
        setUniqueWords();

        File folder = new File(FILE_PATH + args[0]);
        File[] listOfFiles = folder.listFiles();

        // Create output directory for tokens
        String termWeightedTokensOutputPath = FILE_PATH + args[1] + "/term_weighted_tokens/";
        Files.createDirectories(Paths.get(termWeightedTokensOutputPath));

        long[] perPageProcessingTime = new long[listOfFiles.length];
        long programStartTime = System.nanoTime();

        for (File file: listOfFiles) {
            if (file.isFile()) {
                String fileName = file.getName().split("[.]")[0];
                Map<String, Integer> wordFrequencyMap = new HashMap<>();
                long fileStartTime = System.nanoTime();

                Scanner sc = new Scanner(file);
                while (sc.hasNextLine()) {
                    String word = sc.nextLine();
                    Integer frequency = wordFrequencyMap.get(word);
                    frequency = (frequency == null) ? 1 : ++frequency;
                    wordFrequencyMap.put(word, frequency);
                }
                int tokensCount = preprocess(wordFrequencyMap);

                String outputFileName = fileName + ".csv";
                calculateTermFrequencies(wordFrequencyMap, tokensCount, termWeightedTokensOutputPath + outputFileName);

                long fileEndTime = System.nanoTime() - fileStartTime;
                perPageProcessingTime[Integer.parseInt(fileName) - 1] = fileEndTime;

                filesProcessed++;
                if (timeMapperSet.contains(filesProcessed)) {
                    processingTimeHashMap.put(filesProcessed, System.nanoTime() - programStartTime);
                    System.out.printf("Processing time for %d pages is %d \n", filesProcessed, (System.nanoTime() - programStartTime) / 1000000);
                }
            }
        }
        int numberOfDocuments = filesProcessed;
        for (Map.Entry<String, Integer> map: termToDocumentsHashMap.entrySet()) {
            float idfValue = (float) Math.log(numberOfDocuments / map.getValue());
            idfMap.put(map.getKey(), idfValue);
        }

        File termWeightedTokensOutputFile = new File(termWeightedTokensOutputPath);
        listOfFiles = termWeightedTokensOutputFile.listFiles();
        for (File file: listOfFiles) {
            appendTermWeights(idfMap, file.getPath());
        }

        /*
            ------
            Print statistics
            ------
        */
        for (int i=0; i<perPageProcessingTime.length; i++) {
            System.out.print(i+1 + ", ");
        }
        System.out.println();
        for (int i=0; i<perPageProcessingTime.length; i++) {
            System.out.print((float)perPageProcessingTime[i] / 1000000 + ", ");
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

        long programElapsedTime = System.nanoTime() - programStartTime;

        System.out.println("Total elapsed time in millis: " + programElapsedTime/1000000);
    }
}
