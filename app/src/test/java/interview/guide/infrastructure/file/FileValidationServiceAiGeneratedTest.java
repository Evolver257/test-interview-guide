package interview.guide.infrastructure.file;

import interview.guide.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("模块二 AI 生成：文件类型校验补充测试")
class FileValidationServiceAiGeneratedTest {

  private FileValidationService service;

  @BeforeEach
  void setUp() {
    service = new FileValidationService();
  }

  @Test
  @DisplayName("AI-M2-FILE-001：短标识 pdf 可以匹配完整 PDF MIME")
  void shouldAcceptMimeWhenAllowedRuleIsShortToken() {
    assertThatCode(() -> service.validateContentTypeByList(
        "application/pdf", List.of("pdf"), null)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("AI-M2-FILE-002：截断的 MIME 不得被完整 MIME 规则误放行")
  void shouldRejectTruncatedMimeType() {
    assertThatThrownBy(() -> service.validateContentTypeByList(
        "application/p", List.of("application/pdf"), null))
        .isInstanceOf(BusinessException.class)
        .hasMessage("不支持的文件类型: application/p");
  }

  @Test
  @DisplayName("AI-M2-FILE-003：允许类型列表为 null 时明确拒绝")
  void shouldRejectWhenAllowedTypeListIsNull() {
    assertThatThrownBy(() -> service.validateContentTypeByList(
        "application/pdf", null, "类型列表为空"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("类型列表为空");
  }

  @Test
  @DisplayName("AI-M2-FILE-004：空 MIME 类型使用自定义错误信息拒绝")
  void shouldRejectNullContentTypeWithCustomMessage() {
    assertThatThrownBy(() -> service.validateContentTypeByList(
        null, List.of("application/pdf"), "未识别文件类型"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("未识别文件类型");
  }

  @Test
  @DisplayName("AI-M2-FILE-005：完整 MIME 命中后不得执行扩展名兜底")
  void shouldSkipExtensionCheckWhenMimeTypeMatches() {
    AtomicBoolean extensionChecked = new AtomicBoolean(false);

    assertThatCode(() -> service.validateContentType(
        "application/pdf",
        "resume.exe",
        "application/pdf"::equals,
        name -> {
          extensionChecked.set(true);
          return false;
        },
        null
    )).doesNotThrowAnyException();
    assertThat(extensionChecked).isFalse();
  }

  @Test
  @DisplayName("AI-M2-FILE-006：支持 markdown 长扩展名且忽略大小写")
  void shouldAcceptLongMarkdownExtensionIgnoringCase() {
    assertThat(service.isMarkdownExtension("README.MARKDOWN")).isTrue();
  }

  @Test
  @DisplayName("AI-M2-FILE-007：支持 mdown 扩展名且忽略大小写")
  void shouldAcceptMdownExtensionIgnoringCase() {
    assertThat(service.isMarkdownExtension("notes.MDOWN")).isTrue();
  }

  @Test
  @DisplayName("AI-M2-FILE-008：MIME 和扩展名均不合法时返回默认错误")
  void shouldRejectWhenMimeAndExtensionAreBothUnsupported() {
    assertThatThrownBy(() -> service.validateContentType(
        "application/octet-stream",
        "payload.exe",
        service::isKnowledgeBaseMimeType,
        service::isMarkdownExtension,
        null
    )).isInstanceOf(BusinessException.class)
        .hasMessage("不支持的文件类型: application/octet-stream");
  }
}
