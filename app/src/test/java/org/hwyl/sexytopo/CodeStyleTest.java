package org.hwyl.sexytopo;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

public class CodeStyleTest {

    @Test
    public void noJavaFilesHaveTrailingWhitespace() throws IOException {
        Path root = Paths.get("src/main/java");
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        List<String> lines = Files.readAllLines(path);
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            if (!line.isEmpty() && Character.isWhitespace(line.charAt(line.length() - 1))) {
                                violations.add(path + ":" + (i + 1));
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }

        if (!violations.isEmpty()) {
            fail("Trailing whitespace found in:\n" + String.join("\n", violations));
        }
    }
}
