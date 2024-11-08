package log;

import dataType.ServiceIdType;
import dataType.StatusType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class LogAnalyzer {
    private final LogReader logReader;
    private final LogWriter logWriter;

    private final LogData<String> apiKeyMap = new LogData<>();
    private final LogData<StatusType> statusMap = new LogData<>();
    private final LogData<ServiceIdType> serviceIdMap = new LogData<>();
    private final LogData<LocalDateTime> dateMap = new LogData<>();
    private final LogData<String> browserMap = new LogData<>();

    private final LogParser logParser = new LogParser(apiKeyMap, statusMap, serviceIdMap, dateMap, browserMap);

    public LogAnalyzer(String ipFilePath, String opFilePath) throws IOException {
        this.logReader = new LogReader(ipFilePath);
        this.logWriter = new LogWriter(opFilePath);
    }

    public void readAndParseLog() throws IOException {
        logParser.parseLog(logReader.readLog(new ArrayList<>(), 1000));
    }

    public void analyzeLogData() throws IOException {
        mostApiKey(apiKeyMap);
        codeStatus(statusMap);
        serviceId(serviceIdMap);
        peakTime(dateMap);
        browserType(browserMap);
    }

    public void mostApiKey(LogData<String> apiKeyMap) throws IOException {
        int maxValue = 0;
        String maxKey = null;
        for (Map.Entry<String, Integer> entry : apiKeyMap.getLogParserMap().entrySet()) {
            if (entry.getValue() > maxValue) {
                maxValue = entry.getValue();
                maxKey = entry.getKey();
            }
        }
        logWriter.writeMostApiKey(maxKey);
    }

    public void codeStatus(LogData<StatusType> statusMap) throws IOException {
        List<Map.Entry<StatusType, Integer>> codes = statusMap.getLogParserMap().entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey()) // 키 기준으로 정렬
                .collect(Collectors.toList()); // List로 변환
        logWriter.writeCodeStatus(codes);
    }

    public void serviceId(LogData<ServiceIdType> serviceIdMap) throws IOException {
        // 값(value) 기준으로 정렬하고 상위 3개 항목 추출
        List<Map.Entry<ServiceIdType, Integer>> topThree = serviceIdMap.getLogParserMap().entrySet()
                .stream()  // Map의 엔트리를 Stream으로 변환
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // 값 기준 내림차순 정렬
                .limit(3) // 상위 3개만 가져오기
                .collect(Collectors.toList()); // List로 수집
        logWriter.writeServiceId(topThree);
    }

    public void peakTime(LogData<LocalDateTime> dateMap) throws IOException {
        int maxValue = 0;
        LocalDateTime maxKey = null;
        for (Map.Entry<LocalDateTime, Integer> entry : dateMap.getLogParserMap().entrySet()) {
            if (entry.getValue() > maxValue) {
                maxValue = entry.getValue();
                maxKey = entry.getKey();
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String peakTime = null;
        if (maxKey != null) {
            peakTime = maxKey.format(formatter);
        }
        logWriter.writePeakTime(peakTime);
    }

    public void browserType(LogData<String> browserMap) throws IOException {
        // 전체 사용 횟수 계산
        int totalUsage = browserMap.getLogParserMap().values().stream().mapToInt(Integer::intValue).sum();

        // 각 브라우저의 사용 비율 계산 및 출력
        List<Map.Entry<String, Double>> percentages = browserMap.getLogParserMap().entrySet()
                .stream()
                .map(entry -> {
                    String browser = entry.getKey();
                    int usage = entry.getValue();
                    double percentage = (double) usage / totalUsage * 100;
                    return new AbstractMap.SimpleEntry<>(browser, percentage); // Map.Entry로 반환
                })
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // 비율 기준 내림차순 정렬
                .collect(Collectors.toList());
        logWriter.writeBrowserType(percentages);
    }

    public void close() throws IOException {
        logWriter.close();
    }


}

