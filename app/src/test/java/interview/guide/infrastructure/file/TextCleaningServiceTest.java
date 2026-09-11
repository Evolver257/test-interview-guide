package interview.guide.infrastructure.file;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextCleaningServiceTest {

    private final TextCleaningService service = new TextCleaningService();

    @Test
    void cleanTextShouldReturnEmptyWhenInputIsNull() {
        assertEquals("", service.cleanText(null));
    }

    @Test
    void cleanTextShouldReturnEmptyWhenInputIsEmpty() {
        assertEquals("", service.cleanText(""));
    }

    @Test
    void cleanTextShouldReturnEmptyWhenInputIsBlank() {
        assertEquals("", service.cleanText("   "));
    }

    @Test
    void cleanTextShouldKeepPlainTextUnchanged() {
        assertEquals("hello world", service.cleanText("hello world"));
    }

    @Test
    void cleanTextShouldNormalizeCrlfToLf() {
        assertEquals("a\nb", service.cleanText("a\r\nb"));
    }

    @Test
    void cleanTextShouldNormalizeCrToLf() {
        assertEquals("a\nb", service.cleanText("a\rb"));
    }
}