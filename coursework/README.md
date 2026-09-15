# 软件测试实践作业 模块一

## 被测对象

本次测试覆盖 AI 面试平台的两个业务范围。

### 简历上传与预处理

- `TextCleaningService`
- `FileValidationService`
- `FileHashService`
- `ResumeUploadService`

### 面试日程管理

- `InterviewScheduleService`

测试代码位于：

```text
app/src/test/java/interview/guide/infrastructure/file/
app/src/test/java/interview/guide/modules/resume/service/
app/src/test/java/interview/guide/modules/interviewschedule/service/
```

## 环境要求

- Windows 11
- JDK 21
- 项目自带 Gradle Wrapper
- 首次运行需要网络下载测试依赖

单元测试使用 Mock 隔离外部依赖，不需要启动 PostgreSQL、Redis、MinIO，也不需要 AI API Key。

## 运行全部测试

在仓库根目录执行：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:test
```

测试报告位置：

```text
app/build/reports/tests/test/index.html
```

JaCoCo 覆盖率报告位置：

```text
app/build/reports/jacoco/test/html/index.html
```

## 当前进度（截至 2026-09-15）

| 范围 | 用例数 | 执行状态 | 缺陷状态 |
| ---- | ------ | -------- | -------- |
| 简历上传与预处理 | 30 条 | 修复前27条通过、3条失败；修复后30条通过 | 3项已修复并完成回归验证 |
| 面试日程管理 | 10 条 | 测试代码已提交，当前因缺少辅助构造方法而未通过编译 | 1项已定位并提交修复，等待回归验证 |
| 合计 | 40 条 | 30条已验证通过，10条待完成编译与回归 | 4项 |

简历上传与预处理模块的阶段覆盖率：

- 指定四个被测类的行覆盖率：85.86%
- 指定四个被测类的分支覆盖率：54.00%

上述覆盖率只针对简历模块的四个类，不代表整个项目，也不包含新增的
`InterviewScheduleService`。日程模块完成编译修复并运行 JaCoCo 后，再补充最终覆盖率。

## 当前待办

面试日程测试提交 `e07e33d` 新增了10条创建、更新和删除场景用例，并修复“删除不存在日程仍返回成功”的问题。目前测试类缺少以下辅助方法，导致 `compileTestJava` 无法完成：

```text
request(String companyName, String position)
entity(Long id, String companyName, String position, InterviewStatus status)
```

下一步由对应成员补全方法，运行 `InterviewScheduleServiceTest`，再执行全部测试并更新最终统计。

## 作业文件

```text
coursework/
├── README.md
├── 模块一测试用例清单.xlsx
├── 模块一测试报告.docx
├── 模块一缺陷报告.docx
├── evidence/
│   ├── module1-before-fix.txt
│   └── module1-after-fix.txt
└── ai-dialogs/
    └── module1-ai-assistance.md
```

小组成员姓名、学号、贡献率和作者字段需要由各成员根据本人 Git 提交记录填写。
新增日程模块测试后，总用例数和最终贡献比例需在测试清单、报告和 PPT 中同步更新。

## 第二轮保留范围

第一轮没有系统覆盖复杂 Unicode 组合、空配置组合、重新下载解析、多依赖连续异常、并发上传和性质测试。模块二可使用 AI 生成不少于 15 条新增用例，再比较用例有效率、行覆盖率、分支覆盖率和新增缺陷数。
