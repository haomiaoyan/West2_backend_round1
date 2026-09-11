import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DWASearch {
    public static void main(String[] args){
        // 判断 args 是否合法
        if (args.length < 2){
            System.err.println("用法: java -jar DWASearch.jar <input.txt> <output.txt>");
            System.exit(1);
        }
        String inputFile = args[0];
        String outputFile = args[1];

        Path inputPath = Paths.get(inputFile);

        try {
            String dataContent = Files.readString(Path.of("data/round-1-diving-2026/data.json"));
            CoreModule core = new CoreModule(dataContent);
            StringBuilder out = new StringBuilder();

            List<String> lines = Files.readAllLines(Path.of(inputFile));
            for (String line : lines) {
                out.append(core.handleLine(line));
            }

            // 输出
            Files.writeString(Path.of(outputFile), out.toString(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
