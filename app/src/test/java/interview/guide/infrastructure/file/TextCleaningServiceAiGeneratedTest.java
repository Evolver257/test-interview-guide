package interview.guide.infrastructure.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("模块二 AI 生成：文本清理补充测试")
class TextCleaningServiceAiGeneratedTest {

  private final TextCleaningService service = new TextCleaningService();

  @Test
  @DisplayName("AI-M2-TEXT-001：移除带查询参数且扩展名大写的图片链接")
  void shouldRemoveUppercaseImageUrlWithQueryString() {
    String input = "简历\nHTTPS://cdn.example.com/PHOTO.PNG?token=abc\n项目经验";

    assertThat(service.cleanText(input)).isEqualTo("简历\n\n项目经验");
  }

  @Test
  @DisplayName("AI-M2-TEXT-002：移除大小写混合的 file 协议路径")
  void shouldRemoveFileProtocolUrlIgnoringCase() {
    assertThat(service.cleanText("FILE:///tmp/resume.pdf\nJava开发"))
        .isEqualTo("Java开发");
  }

  @Test
  @DisplayName("AI-M2-TEXT-003：移除由多种符号组成的分隔线")
  void shouldRemoveMixedSymbolSeparatorLine() {
    assertThat(service.cleanText("技能\n-_*==\nJava"))
        .isEqualTo("技能\n\nJava");
  }

  @Test
  @DisplayName("AI-M2-TEXT-004：连续多个空行最多保留一个空行")
  void shouldCompressConsecutiveBlankLines() {
    assertThat(service.cleanText("A\n\n\n\nB")).isEqualTo("A\n\nB");
  }

  @Test
  @DisplayName("AI-M2-TEXT-005：清理每行末尾的空格和制表符")
  void shouldTrimTrailingWhitespaceOnEveryLine() {
    assertThat(service.cleanText("Java  \nSpring\t\n项目   "))
        .isEqualTo("Java\nSpring\n项目");
  }

  @Test
  @DisplayName("AI-M2-TEXT-006：单行清理合并混合换行和连续空白")
  void shouldCollapseMixedWhitespaceIntoSingleLine() {
    assertThat(service.cleanToSingleLine("  Java\r\n\tSpring   Boot  "))
        .isEqualTo("Java Spring Boot");
  }

  @Test
  @DisplayName("AI-M2-TEXT-007：HTML 清理还原引号和尖括号实体")
  void shouldDecodeQuoteAndAngleBracketEntities() {
    assertThat(service.stripHtml("<div>A&lt;B &quot;x&quot; &apos;y&apos;</div>"))
        .isEqualTo("A<B \"x\" 'y'");
  }

  @Test
  @DisplayName("AI-M2-TEXT-008：最大长度为零时返回空字符串")
  void shouldReturnEmptyWhenMaximumLengthIsZero() {
    assertThat(service.cleanTextWithLimit("A😀B", 0)).isEmpty();
  }
}
