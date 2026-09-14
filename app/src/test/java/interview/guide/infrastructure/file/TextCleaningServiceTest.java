package interview.guide.infrastructure.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("文本清理服务单元测试")
class TextCleaningServiceTest {

  private final TextCleaningService service = new TextCleaningService();

  @Test
  @DisplayName("TC-TEXT-001：null 输入返回空字符串")
  void cleanTextShouldReturnEmptyWhenInputIsNull() {
    assertEquals("", service.cleanText(null));
  }

  @Test
  @DisplayName("TC-TEXT-002：空字符串输入返回空字符串")
  void cleanTextShouldReturnEmptyWhenInputIsEmpty() {
    assertEquals("", service.cleanText(""));
  }

  @Test
  @DisplayName("TC-TEXT-003：纯空白输入返回空字符串")
  void cleanTextShouldReturnEmptyWhenInputIsBlank() {
    assertEquals("", service.cleanText("   "));
  }

  @Test
  @DisplayName("TC-TEXT-004：普通文本保持不变")
  void cleanTextShouldKeepPlainTextUnchanged() {
    assertEquals("hello world", service.cleanText("hello world"));
  }

  @Test
  @DisplayName("TC-TEXT-005：CRLF 换行转换为 LF")
  void cleanTextShouldNormalizeCrlfToLf() {
    assertEquals("a\nb", service.cleanText("a\r\nb"));
  }

  @Test
  @DisplayName("TC-TEXT-006：CR 换行转换为 LF")
  void cleanTextShouldNormalizeCrToLf() {
    assertEquals("a\nb", service.cleanText("a\rb"));
  }

  @Test
  @DisplayName("TC-TEXT-007：移除控制字符并保留换行和制表符")
  void shouldRemoveControlCharactersButKeepNewlineAndTab() {
    assertThat(service.cleanText("Java\u0000开发\n项目\t经验"))
        .isEqualTo("Java开发\n项目\t经验");
  }

  @Test
  @DisplayName("TC-TEXT-008：图片扩展名大小写不影响清理")
  void shouldRemoveUppercaseImageFilename() {
    assertThat(service.cleanText("个人简介\nimage12.PNG\nJava开发"))
        .isEqualTo("个人简介\n\nJava开发");
  }

  @Test
  @DisplayName("TC-TEXT-009：截断文本时不破坏 Emoji 字符")
  void shouldNotSplitEmojiWhenTruncating() {
    String result = service.cleanTextWithLimit("A😀B", 2);

    assertThat(result).isEqualTo("A😀");
    assertThat(result.codePoints()).hasSize(2);
  }

  @Test
  @DisplayName("TC-TEXT-010：移除 HTML 标签并还原常见实体")
  void shouldStripHtmlAndDecodeEntities() {
    assertThat(service.stripHtml("<p>Java&nbsp;&amp;&nbsp;<b>Spring</b></p>"))
        .isEqualTo("Java & Spring");
  }
}
