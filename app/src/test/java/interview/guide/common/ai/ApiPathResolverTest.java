package interview.guide.common.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AI 服务路径解析测试")
class ApiPathResolverTest {

  @Test
  @DisplayName("IG-M2-AI-017：识别末尾的版本路径及尾斜杠")
  void recognizesVersionAtEndWithOrWithoutSlash() {
    assertThat(ApiPathResolver.baseUrlContainsVersion("https://ai.example.com/v1")).isTrue();
    assertThat(ApiPathResolver.baseUrlContainsVersion(" https://ai.example.com/v2/ ")).isTrue();
  }

  @Test
  @DisplayName("IG-M2-AI-018：识别带字母后缀的版本路径")
  void recognizesVersionWithSuffix() {
    assertThat(ApiPathResolver.baseUrlContainsVersion("https://ai.example.com/v1beta")).isTrue();
    assertThat(ApiPathResolver.baseUrlContainsVersion("https://ai.example.com/v12alpha")).isTrue();
  }

  @Test
  @DisplayName("IG-M2-AI-019：非末尾版本、空值和空白不误判")
  void ignoresNonTrailingVersionAndEmptyInput() {
    assertThat(ApiPathResolver.baseUrlContainsVersion("https://ai.example.com/v1/chat/completions"))
        .isFalse();
    assertThat(ApiPathResolver.baseUrlContainsVersion("https://ai.example.com/version1")).isFalse();
    assertThat(ApiPathResolver.baseUrlContainsVersion(null)).isFalse();
    assertThat(ApiPathResolver.baseUrlContainsVersion("  ")).isFalse();
  }

  @Test
  @DisplayName("IG-M2-AI-020：去除尾斜杠并处理null")
  void stripsTrailingSlashesAndHandlesNull() {
    assertThat(ApiPathResolver.stripTrailingSlashes(" https://ai.example.com/// "))
        .isEqualTo("https://ai.example.com");
    assertThat(ApiPathResolver.stripTrailingSlashes("///")).isEmpty();
    assertThat(ApiPathResolver.stripTrailingSlashes(null)).isEmpty();
  }
}
