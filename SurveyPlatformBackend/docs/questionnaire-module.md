# 问卷模块（FR-36 ~ FR-39）设计说明

面向继续做 M5（参与会话与问卷运行，尤其是 UC-31 作答问卷 + 跳转运行时）的队友。本文档只讲清楚"现在有什么、怎么用、接下来要接什么"，不追求穷尽细节，具体以代码 + Javadoc 为准。

代码位置：`src/main/java/com/cs_42_3/surveyplatformbackend/questionnaire/`
迁移文件：`src/main/resources/db/migration/V4__create_questionnaire_tables.sql`

## 1. 这个模块做了什么

研究者在 Dashboard 里编辑问卷时：从题库（`survey` 模块）勾选题目、排顺序、给单选/打分题配置"选了这个答案就跳到第几题"的规则，整体保存。这一版**只做草稿编辑**，不含发布快照（下面第 5 节会说明为什么，以及给未来的接口）。

- FR-36 从题库选择启用题目
- FR-37 调整启用题目顺序与移除
- FR-38 配置分支跳转逻辑
- FR-39 保存问卷（作为一个整体原子保存）
- NFR-15 跳转规则确定性：数据库唯一约束保证"一个答案 → 唯一目标"
- NFR-14 题库联动一致性（部分）：题目被问卷引用后，题库那边锁定编辑/删除

## 2. 数据库设计

一个 study 对应一个 questionnaire（1:1 草稿）；每个 questionnaire 下有若干有序的 item（引用题库里的题目）；每个 item 上可以挂若干跳转规则。

```
studies (1) ──< questionnaires (1) ──< questionnaire_items >── (N:1) questions (题库, V3)
                                              │
                                              └──< questionnaire_branch_rules >── target_item_id 指回 questionnaire_items
```

### `questionnaires`
一个 study 一行（`study_id` 唯一）。`lock_version` 是乐观锁，跟 `studies` 表的用法一致。

### `questionnaire_items`
- `item_order`：显示顺序，也是**没有匹配到任何跳转规则时的默认下一题**（即 `item_order + 1` 对应的那道题）
- `question_id` → `questions.id`，`ON DELETE RESTRICT`：题目被问卷引用期间，题库那边删不掉（对应 NFR-14）

### `questionnaire_branch_rules`
"选中的答案 → 目标题目"的查找表，一行一条规则：
- `source_item_id`：哪道题触发
- `source_option_id` **或** `source_scale_value`：二选一（`CHECK` 约束强制），分别对应单选题的选项 / 打分题的刻度值
- `target_item_id`：跳到哪道题

关键约束（保证 NFR-15 确定性）：
```sql
UNIQUE (source_item_id, source_option_id)       -- 同一题的同一个选项只能配一条规则
UNIQUE (source_item_id, source_scale_value)     -- 同一题的同一个刻度值只能配一条规则
CHECK (source_item_id <> target_item_id)        -- 不能自己跳自己
```
这两个唯一约束是 M5 做跳转运行时可以直接依赖的保证：**给定"当前题 + 选中的答案"，最多命中一条规则**，不需要在运行时再去重/排歧义。

## 3. 核心业务逻辑

### 保存 = 全量替换（不是增量 patch）
`Questionnaire.replaceItems(List<QuestionnaireItemPlan>)` 每次保存都是"清空旧的 item + 规则，按新列表整体重建"，仿照题库那边 `Question.replaceOptions()` 的写法。好处是前端不用先建题目拿到 ID 再回填跳转目标——请求里跳转规则直接用**数组下标**指向目标题目，服务端两遍构建（先建所有 item 拿到实体引用，再挂规则）。

### 谁能配跳转规则
只有 `SINGLE_CHOICE` 和 `SCALE` 两种题型允许配置跳转（`MULTI_CHOICE` 会因为多选而导致目标不唯一，直接在保存时校验拒绝）。校验逻辑在 `QuestionnaireServiceImpl.buildRulePlans`。

### 题库占用保护（NFR-14，当前是"全锁"简化版）
题目一旦被任意问卷的 item 引用，`QuestionServiceImpl.updateQuestion`/`deleteQuestion` 会直接抛 `QuestionInUseException`（409），必须先在问卷里把这道题移除（FR-37）才能改/删。

> 这是刻意简化的 v1 规则：现在是"只要在用就整体锁死"，不是只锁"会影响跳转规则的那部分改动"。原因是 `Question.replaceOptions()` 每次编辑都会整批删掉重建选项（哪怕文本没变），旧的 `question_options.id` 会失效，进而级联删掉指向它的跳转规则——与其精细判断"这次编辑是否安全"，v1 选择直接全锁，后续如果需要放开（比如允许改题干文字），要先让 `replaceOptions` 支持按需保留未变的选项行。

跨模块实现方式是一个端口/适配器：`survey.service.QuestionUsageGuard`（接口，声明在 survey）由 `questionnaire.service.implementation.QuestionUsageGuardImpl` 实现，避免 `survey` 反向依赖 `questionnaire`。

## 4. API

Base path：`/api/studies/{studyId}/questionnaire`，需要 `RESEARCHER` 角色 + JWT。

### `GET /api/studies/{studyId}/questionnaire`
拿当前草稿。study 存在但还没保存过问卷时，返回 `id: null` 的空结构（不会自动建库表行），第一次 `PUT` 才真正建行。

### `PUT /api/studies/{studyId}/questionnaire`
整体替换。请求体：

```jsonc
{
  "items": [
    {
      "questionId": "uuid",
      "branchRules": [
        { "sourceOptionId": "uuid", "sourceScaleValue": null, "targetPosition": 2 }
        // sourceOptionId / sourceScaleValue 二选一；targetPosition 是本次请求 items 数组的下标
      ]
    }
  ]
}
```

返回体 `QuestionnaireResponse`：包含每个 item 的 `position`（= 数据库里的 `item_order`）、内嵌的完整题目内容（`QuestionResponse`，复用 survey 模块的 DTO，免得前端再查一次题库）、以及每条跳转规则的 `targetItemId` + `targetPosition`。

具体字段定义见 `questionnaire.api.dto` 包，出错时统一走 `QuestionnaireExceptionHandler`（400/401/403/404，`QuestionnaireErrorResponse`）。

## 5. 给 M5 的接口点

M5（UC-31 作答问卷 + 跳转运行时）需要的运行时算法，就是照着 `questionnaire_branch_rules` 这张查找表走一遍，伪代码：

```
输入：当前 item、参与者提交的答案（选中的 option / 打分值）
1. 按 (source_item_id = 当前item, source_option_id/source_scale_value = 选中的答案) 查 questionnaire_branch_rules
2. 命中一条（保证最多一条，见第 2 节唯一约束）→ 跳到 rule.target_item_id
3. 没命中 → 走默认顺序：questionnaire_items 里 item_order 紧接着的下一个
4. 已经是最后一题 → 问卷结束（交给 FR-46 会话状态判定）
```

这段逻辑目前**没有对应的 service 方法**（草稿编辑阶段不需要执行跳转，只需要保存规则），M5 要新建一个类似 `QuestionnaireRuntimeService`/`SurveyFlowEngine` 的东西来做这件事，直接读 `questionnaire_items` + `questionnaire_branch_rules`（可以复用 `QuestionnaireRepository`/`QuestionnaireItemRepository`，或者按需加新的查询方法）。

### 发布快照（还没做，先别依赖草稿表）
现在 `questionnaire_items.question_id` 指向的是**活的**题库题目，研究者还在编辑草稿时改题库会实时反映。等 Study 发布功能（FR-14/FR-15，M2 范畴，目前也还没实现）做出来后，需要在发布那一刻把当时引用的题目内容冻结一份快照，之后题库怎么改都不影响已发布问卷——这样 M5 的参与端运行时应该读**发布快照**，而不是直接读草稿表 `questionnaire_items`。这部分目前只有设计思路，没有代码，动 M5 之前建议先和后端同学对一下发布快照具体怎么接。

## 6. 相关文件速查

| 文件 | 作用 |
|---|---|
| `questionnaire/domain/Questionnaire.java` | 聚合根，`replaceItems` 全量替换入口 |
| `questionnaire/domain/QuestionnaireItem.java` | 一道启用题目 |
| `questionnaire/domain/QuestionnaireBranchRule.java` | 一条跳转规则 |
| `questionnaire/service/implementation/QuestionnaireServiceImpl.java` | 所有校验逻辑（题目归属、跳转合法性、去重、越界检查） |
| `questionnaire/api/QuestionnaireController.java` | REST 入口 |
| `survey/service/QuestionUsageGuard.java` | 题库占用保护的接口定义 |
| `db/migration/V4__create_questionnaire_tables.sql` | 表结构 + 约束，注释里解释了每个约束为什么这么设计 |
