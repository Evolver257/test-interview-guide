package interview.guide.modules.resume.service;

import interview.guide.common.config.AppConfigProperties;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.file.FileStorageService;
import interview.guide.infrastructure.file.FileValidationService;
import interview.guide.modules.interview.model.ResumeAnalysisResponse;
import interview.guide.modules.resume.listener.AnalyzeStreamProducer;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("简历上传服务单元测试")
class ResumeUploadServiceTest {

  @Mock
  private ResumeParseService parseService;
  @Mock
  private FileStorageService storageService;
  @Mock
  private ResumePersistenceService persistenceService;
  @Mock
  private FileValidationService fileValidationService;
  @Mock
  private AnalyzeStreamProducer analyzeStreamProducer;
  @Mock
  private ResumeRepository resumeRepository;

  private ResumeUploadService service;
  private MockMultipartFile file;

  @BeforeEach
  void setUp() {
    AppConfigProperties config = new AppConfigProperties();
    config.setAllowedTypes(List.of("application/pdf", "text/plain"));
    service = new ResumeUploadService(
        parseService,
        storageService,
        persistenceService,
        config,
        fileValidationService,
        analyzeStreamProducer,
        resumeRepository
    );
    file = new MockMultipartFile(
        "file", "resume.txt", "text/plain", "Java简历".getBytes(StandardCharsets.UTF_8));
  }

  @Nested
  @DisplayName("上传与分析")
  class UploadTests {

    @Test
    @DisplayName("TC-UPLOAD-001：新简历完成解析、存储、入库和任务发送")
    void shouldUploadNewResumeAndSendAnalyzeTask() {
      ResumeEntity saved = resume(101L, "resume.txt", "key-101", "http://file/101");
      when(parseService.detectContentType(file)).thenReturn("text/plain");
      when(persistenceService.findExistingResume(file)).thenReturn(Optional.empty());
      when(parseService.parseResume(file)).thenReturn("Java简历");
      when(storageService.uploadResume(file)).thenReturn("key-101");
      when(storageService.getFileUrl("key-101")).thenReturn("http://file/101");
      when(persistenceService.saveResume(file, "Java简历", "key-101", "http://file/101"))
          .thenReturn(saved);

      Map<String, Object> result = service.uploadAndAnalyze(file);
      Map<?, ?> resumeResult = (Map<?, ?>) result.get("resume");

      assertThat(result).containsEntry("duplicate", false);
      assertThat(resumeResult.get("id")).isEqualTo(101L);
      assertThat(resumeResult.get("analyzeStatus")).isEqualTo("PENDING");
      verify(analyzeStreamProducer).sendAnalyzeTask(101L, "Java简历");
    }

    @Test
    @DisplayName("TC-UPLOAD-002：重复简历存在分析结果时直接返回历史结果")
    void shouldReturnExistingAnalysisForDuplicateResume() {
      ResumeEntity existing = resume(102L, "old.pdf", "old-key", "http://file/old");
      ResumeAnalysisResponse analysis = new ResumeAnalysisResponse(
          80, null, "摘要", List.of("结构清晰"), List.of(), "原文");
      when(parseService.detectContentType(file)).thenReturn("text/plain");
      when(persistenceService.findExistingResume(file)).thenReturn(Optional.of(existing));
      when(persistenceService.getLatestAnalysisAsDTO(102L)).thenReturn(Optional.of(analysis));

      Map<String, Object> result = service.uploadAndAnalyze(file);

      assertThat(result).containsEntry("duplicate", true).containsEntry("analysis", analysis);
      verify(parseService, never()).parseResume(any());
      verify(storageService, never()).uploadResume(any());
    }

    @Test
    @DisplayName("TC-UPLOAD-003：重复简历无分析结果时返回当前状态")
    void shouldReturnCurrentStatusWhenDuplicateHasNoAnalysis() {
      ResumeEntity existing = resume(103L, "old.pdf", null, null);
      existing.setAnalyzeStatus(AsyncTaskStatus.FAILED);
      when(parseService.detectContentType(file)).thenReturn("text/plain");
      when(persistenceService.findExistingResume(file)).thenReturn(Optional.of(existing));
      when(persistenceService.getLatestAnalysisAsDTO(103L)).thenReturn(Optional.empty());

      Map<String, Object> result = service.uploadAndAnalyze(file);
      Map<?, ?> resumeResult = (Map<?, ?>) result.get("resume");

      assertThat(result).containsEntry("duplicate", true);
      assertThat(resumeResult.get("id")).isEqualTo(103L);
      assertThat(resumeResult.get("analyzeStatus")).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("TC-UPLOAD-004：无法解析出文本时停止存储和入库")
    void shouldStopWhenParsedTextIsBlank() {
      when(parseService.detectContentType(file)).thenReturn("text/plain");
      when(persistenceService.findExistingResume(file)).thenReturn(Optional.empty());
      when(parseService.parseResume(file)).thenReturn("   ");

      assertThatThrownBy(() -> service.uploadAndAnalyze(file))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("无法从文件中提取文本内容")
          .extracting("code").isEqualTo(ErrorCode.RESUME_PARSE_FAILED.getCode());
      verify(storageService, never()).uploadResume(any());
      verify(persistenceService, never()).saveResume(any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-UPLOAD-005：文件类型校验失败后不再查询重复文件")
    void shouldStopAfterContentTypeValidationFailure() {
      when(parseService.detectContentType(file)).thenReturn("image/png");
      BusinessException error = new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件类型");
      org.mockito.Mockito.doThrow(error).when(fileValidationService)
          .validateContentTypeByList(eq("image/png"), any(), any());

      assertThatThrownBy(() -> service.uploadAndAnalyze(file)).isSameAs(error);
      verify(persistenceService, never()).findExistingResume(any());
    }
  }

  private ResumeEntity resume(Long id, String filename, String storageKey, String storageUrl) {
    ResumeEntity entity = new ResumeEntity();
    entity.setId(id);
    entity.setOriginalFilename(filename);
    entity.setStorageKey(storageKey);
    entity.setStorageUrl(storageUrl);
    entity.setAnalyzeStatus(AsyncTaskStatus.PENDING);
    return entity;
  }
}
