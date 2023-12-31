package httpsmanager.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class FileService {

    private FileService() {
    }
    
    public static String loadTextFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            return null;
        }
    }

    public static void saveTextFile(File file, String text) {
        file.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(file)) {
            w.write(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
