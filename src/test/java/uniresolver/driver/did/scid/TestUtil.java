package uniresolver.driver.did.scid;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class TestUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static String readResourceString(String resourceName) {
        try {
            return Files.readString(Paths.get(TestUtil.class.getResource(resourceName).toURI()), StandardCharsets.UTF_8);
        } catch (URISyntaxException | IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    static Map<String, Object> readResourceJson(String resourceName) throws IOException {
        return (Map<String, Object>) objectMapper.readValue(readResourceString(resourceName), Map.class);
    }
}
