package interview.guide.infrastructure.file;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("文件校验服务单元测试")
class FileValidationServiceTest {

  private FileValidationService service;

  @BeforeEach
  void setUp() {
    service = new FileValidationService();
  }

  @Nested
  @DisplayName("文件基本属性校验")
  class BasicValidationTests {

    @Test
    @DisplayName("TC-FILE-001：空文件被拒绝")
    void shouldRejectEmptyFile() {
      var file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);

      assertThatThrownBy(() -> service.validateFile(file, 10, "简历"))
          .isInstanceOf(BusinessException.class)
          .hasMessage("请选择要上传的简历文件")
          .extracting("code").isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("TC-FILE-002：文件大小等于上限时通过")
    void shouldAcceptFileAtMaximumSize() {
      var file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[10]);

      assertThatCode(() -> service.validateFile(file, 10, "简历")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TC-FILE-003：文件大小超过上限一个字节时被拒绝")
    void shouldRejectFileOneByteOverMaximum() {
      var file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[11]);

      assertThatThrownBy(() -> service.validateFile(file, 10, "简历"))
          .isInstanceOf(BusinessException.class)
          .hasMessage("文件大小超过限制");
    }

    @Test
    @DisplayName("TC-FILE-004：null 文件转换为明确的业务异常")
    void shouldRejectNullFileWithBusinessException() {
      assertThatThrownBy(() -> service.validateFile(null, 10, "简历"))
          .isInstanceOf(BusinessException.class)
          .hasMessage("请选择要上传的简历文件");
    }
  }

  @Nested
  @DisplayName("文件类型校验")
  class ContentTypeValidationTests {

    @Test
    @DisplayName("TC-FILE-005：合法 MIME 类型忽略大小写")
    void shouldAcceptAllowedMimeTypeIgnoringCase() {
      assertThatCode(() -> service.validateContentTypeByList(
          "APPLICATION/PDF", List.of("application/pdf"), null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TC-FILE-006：非法 MIME 类型返回自定义错误信息")
    void shouldRejectUnsupportedMimeTypeWithCustomMessage() {
      assertThatThrownBy(() -> service.validateContentTypeByList(
          "image/png", List.of("application/pdf"), "仅支持PDF"))
          .isInstanceOf(BusinessException.class)
          .hasMessage("仅支持PDF");
    }

    @Test
    @DisplayName("TC-FILE-007：包含 pdf 字样的伪造 MIME 类型不能通过")
    void shouldRejectMimeTypeThatOnlyContainsAllowedText() {
      assertThatThrownBy(() -> service.validateContentTypeByList(
          "application/notpdf", List.of("application/pdf"), null))
          .isInstanceOf(BusinessException.class)
          .hasMessage("不支持的文件类型: application/notpdf");
    }

    @Test
    @DisplayName("TC-FILE-008：MIME 未识别时允许使用合法扩展名兜底")
    void shouldAcceptSupportedFileExtensionAsFallback() {
      assertThatCode(() -> service.validateContentType(
          "application/octet-stream",
          "README.MD",
          service::isKnowledgeBaseMimeType,
          service::isMarkdownExtension,
          null
      )).doesNotThrowAnyException();

      assertThat(service.isMarkdownExtension("README.MD")).isTrue();
    }
  }
}
