# 软件测试实践作业 模块一

本作业以 InterviewGuide 为被测项目，针对面试日程管理和简历上传与预处理两个业务范围开展单元测试。测试采用 JUnit 5、Mockito 和 AssertJ 实现，通过 Gradle 一键执行。

## 测试成果

| 项目 | 结果 |
| --- | --- |
| 设计用例 | 40 条 |
| 面试日程管理 | 10 条 |
| 简历上传与预处理 | 30 条 |
| 自动化实现 | 40 条 |
| 有效缺陷 | 3 项，严重程度均为高 |
| 缺陷状态 | 均已修复 |

当前仓库中简历模块30条测试已经完成回归验证。面试日程模块10条测试及产品修复已经提交，测试类的数据构造辅助方法仍待补全；完成后将重新执行40条全量测试并更新最终结果。

## 被测范围

| 被测类 | 测试内容 | 用例数 |
| --- | --- | ---: |
| `InterviewScheduleService` | 日程创建、更新、删除、状态和查询 | 10 |
| `TextCleaningService` | 文本清理、换行、HTML和Unicode安全截断 | 10 |
| `FileValidationService` | 文件空值、大小、MIME类型和扩展名 | 8 |
| `FileHashService` | 字节、文件和输入流的SHA-256计算 | 5 |
| `ResumeUploadService` | 上传编排、重复简历处理和重新分析 | 7 |

本次不覆盖真实 PostgreSQL、Redis、对象存储、外部 AI 接口、HTTP 参数绑定、React 页面交互、并发、性能和生产部署。

## 测试方法

测试综合使用等价类、边界值和场景法，覆盖空值、合法值、非法值、状态变化、时间区间、文件大小、Unicode字符以及完整业务流程。白盒测试根据条件分支、异常转换、短路行为和依赖调用设计。

| 测试方法 | 用例数 | 占比 |
| --- | ---: | ---: |
| 面试日程等价类 | 7 | 17.5% |
| 面试日程场景法 | 3 | 7.5% |
| 简历预处理黑盒测试 | 20 | 50% |
| 简历预处理白盒测试 | 10 | 25% |

Repository、文件存储、解析服务和异步任务通过 Mockito 隔离，因此测试不需要启动数据库、Redis、对象存储或真实 AI 服务。

## 自动化测试

### 环境

- Windows
- JDK 21
- Gradle 8.14
- Spring Boot 4.0.1
- JUnit 5
- Mockito
- AssertJ

### 运行方式

在仓库根目录执行：

```powershell
.\gradlew.bat :app:test --rerun-tasks
```

测试报告：

```text
app/build/reports/tests/test/index.html
```

JaCoCo覆盖率报告：

```text
app/build/reports/jacoco/test/html/index.html
```

### 测试代码

```text
app/src/test/java/interview/guide/modules/interviewschedule/service/InterviewScheduleServiceTest.java
app/src/test/java/interview/guide/infrastructure/file/TextCleaningServiceTest.java
app/src/test/java/interview/guide/infrastructure/file/FileValidationServiceTest.java
app/src/test/java/interview/guide/infrastructure/file/FileHashServiceTest.java
app/src/test/java/interview/guide/modules/resume/service/ResumeUploadServiceTest.java
app/src/test/java/interview/guide/modules/resume/service/ResumeReanalyzeServiceTest.java
```

## 缺陷摘要

| 编号 | 缺陷 | 根因与修复 | 状态 |
| --- | --- | --- | --- |
| DEF-001 | 删除不存在的面试日程仍返回成功 | 删除前没有确认资源存在；修复后先调用 `getByIdOrThrow`，再执行删除 | 已修复 |
| DEF-002 | 文本截断破坏Emoji代理字符 | 原实现按照UTF-16下标截断；修复后使用 `codePointCount` 和 `offsetByCodePoints` 按Unicode码点计算边界 | 已修复 |
| DEF-003 | null文件触发空指针异常 | 原实现未判断null便调用 `isEmpty`；修复后使用短路判断并统一抛出业务异常 | 已修复 |

## 典型用例

- `IG-M1-UT-009`：删除不存在的日程，验证业务异常且不调用删除方法，对应 DEF-001。
- `AIP-UT-009`：文本在Emoji边界截断，验证不会破坏完整Unicode字符，对应 DEF-002。
- `AIP-UT-014`：上传null文件，验证返回统一业务异常而不是空指针异常，对应 DEF-003。

## 小组分工

| 姓名 | 学号 | 主要工作 | 贡献率 |
| --- | --- | --- | ---: |
| 罗文 | M202681090 | 文本清理测试、Unicode边界分析、DEF-002复现与修复记录、成果汇报 | 40% |
| 邱铭曦 | M202681088 | 面试日程需求分析与测试、DEF-001复现与修复记录、合并文档整理 | 30% |
| 陆畅 | M202681089 | 文件校验、文件哈希和上传流程测试、DEF-003复现与修复记录 | 30% |

各成员使用个人 GitHub 账号提交本人负责的代码和文档，个人贡献以 Git 提交记录为依据。

## 作业材料

课程测试说明和交付材料位于 [`coursework/`](coursework/README.md)，包括测试用例清单、测试报告、缺陷报告、执行证据和关键 AI 对话记录。
