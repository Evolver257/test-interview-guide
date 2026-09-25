package interview.guide.modules.resume.service;

import interview.guide.common.config.AppConfigProperties;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.file.FileStorageService;
import interview.guide.infrastructure.file.FileValidationService;
import interview.guide.modules.resume.listener.AnalyzeStreamProducer;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("模块二 AI 生成：简历重新分析补充测试")
class ResumeReanalyzeAiGeneratedTest {

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
  }

  @Test
  @DisplayName("AI-M2-REANALYZE-001：缓存文本为空时重新下载解析并写回缓存")
  void shouldDownloadAndCacheTextWhenCachedTextIsBlank() {
    ResumeEntity resume = resume(201L, "resume.pdf", "storage-201");
    resume.setResumeText("   ");
    resume.setAnalyzeStatus(AsyncTaskStatus.FAILED);
    resume.setAnalyzeError("上次分析失败");
    when(resumeRepository.findById(201L)).thenReturn(Optional.of(resume));
    when(parseService.downloadAndParseContent("storage-201", "resume.pdf"))
        .thenReturn("重新解析的简历文本");

    service.reanalyze(201L);

    assertThat(resume.getResumeText()).isEqualTo("重新解析的简历文本");
    assertThat(resume.getAnalyzeStatus()).isEqualTo(AsyncTaskStatus.PENDING);
    assertThat(resume.getAnalyzeError()).isNull();
    verify(resumeRepository).save(resume);
    verify(analyzeStreamProducer).sendAnalyzeTask(201L, "重新解析的简历文本");
  }

  @Test
  @DisplayName("AI-M2-REANALYZE-002：重新解析仍为空时返回解析失败且不发送任务")
  void shouldRejectWhenDownloadedTextIsStillBlank() {
    ResumeEntity resume = resume(202L, "scan.pdf", "storage-202");
    resume.setResumeText(null);
    when(resumeRepository.findById(202L)).thenReturn(Optional.of(resume));
    when(parseService.downloadAndParseContent("storage-202", "scan.pdf")).thenReturn("\n\t");

    assertThatThrownBy(() -> service.reanalyze(202L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("无法获取简历文本内容")
        .extracting("code").isEqualTo(ErrorCode.RESUME_PARSE_FAILED.getCode());
    verify(resumeRepository, never()).save(any());
    verify(analyzeStreamProducer, never()).sendAnalyzeTask(any(), any());
  }

  @Test
  @DisplayName("AI-M2-REANALYZE-003：缓存文本存在时不得访问文件存储解析")
  void shouldNotDownloadWhenCachedTextExists() {
    ResumeEntity resume = resume(203L, "resume.docx", "storage-203");
    resume.setResumeText("已有文本");
    when(resumeRepository.findById(203L)).thenReturn(Optional.of(resume));

    service.reanalyze(203L);

    verify(parseService, never()).downloadAndParseContent(any(), any());
    verify(analyzeStreamProducer).sendAnalyzeTask(203L, "已有文本");
  }

  private ResumeEntity resume(Long id, String filename, String storageKey) {
    ResumeEntity entity = new ResumeEntity();
    entity.setId(id);
    entity.setOriginalFilename(filename);
    entity.setStorageKey(storageKey);
    return entity;
  }
}
