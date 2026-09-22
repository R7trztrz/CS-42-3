# FR-38 分支跳转逻辑：设计与校验实现

给要把这部分逻辑移植到队友实现里的人看。范围只覆盖 FR-38（配置分支跳转规则）本身，即"研究者给某道题的某个答案配置跳转目标"这一件事——不含 FR-36/37/39 的其余部分（选题、排序、整体保存），那些内容见 `docs/questionnaire-module.md`。

我这版比较完整的部分是**安全/校验层**：防止研究者通过构造请求配出自相矛盾、指向不存在实体、或导致运行时跳转结果不确定的规则。下面按"数据库怎么兜底"→"应用层怎么校验"拆开讲。

## 1. 数据模型

FR-38 的规则本身存在 `questionnaire_branch_rules` 表里，但依赖两个前提表：`questions`/`question_options`（题库，已有）和 `questionnaire_items`（问卷里启用的题目，FR-36/37 的产物，这里只列必要字段）。

```sql
-- 前提：问卷里启用的题目，跳转规则的 source/target 都指向这张表的行
CREATE TABLE questionnaire_items (
    id UUID NOT NULL,
    questionnaire_id UUID NOT NULL,
    question_id UUID NOT NULL,   -- 指向题库 questions.id
    item_order INTEGER NOT NULL,
    CONSTRAINT pk_questionnaire_items PRIMARY KEY (id)
    -- 其余外键/约束见 questionnaire-module.md，这里从略
);

-- FR-38 本体：一条"选中的答案 -> 跳到哪道题"的规则
CREATE TABLE questionnaire_branch_rules (
    id UUID NOT NULL,
    source_item_id UUID NOT NULL,        -- 哪道题触发
    source_option_id UUID,               -- 触发的选项（单选题）
    source_scale_value INTEGER,          -- 触发的刻度值（打分题）
    target_item_id UUID NOT NULL,        -- 跳转目标

    CONSTRAINT pk_questionnaire_branch_rules PRIMARY KEY (id),

    CONSTRAINT fk_branch_rules_source_item
        FOREIGN KEY (source_item_id) REFERENCES questionnaire_items (id) ON DELETE CASCADE,
    -- target 用 RESTRICT 而不是 CASCADE：删除一个被别的规则当跳转目标的题目
    -- 必须是一次显式、经过校验的操作，不能被别的删除动作顺带带走
    CONSTRAINT fk_branch_rules_target_item
        FOREIGN KEY (target_item_id) REFERENCES questionnaire_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_branch_rules_source_option
        FOREIGN KEY (source_option_id) REFERENCES question_options (id) ON DELETE CASCADE,

    -- 二选一：要么是选项触发，要么是刻度值触发，不能同时为空或同时有值
    CONSTRAINT ck_branch_rules_single_trigger
        CHECK ((source_option_id IS NULL) <> (source_scale_value IS NULL)),
    -- 禁止自己跳自己
    CONSTRAINT ck_branch_rules_no_self_loop
        CHECK (source_item_id <> target_item_id),

    -- 核心：同一道题的同一个选项/同一个刻度值，最多只能配一条规则
    -- 这保证了"给定当前题 + 选中的答案"，跳转结果查表后最多命中一条，
    -- 不用在运行时再做去重/排歧义（对应 NFR-15 确定性要求）
    CONSTRAINT uk_branch_rules_source_option
        UNIQUE (source_item_id, source_option_id),
    CONSTRAINT uk_branch_rules_source_scale_value
        UNIQUE (source_item_id, source_scale_value)
);
```

**为什么这么设计（供对照队友实现时参考）：**

- `source_option_id`/`source_scale_value` 用两个可空列 + `CHECK` 做"二选一"，而不是一个万能的 `trigger_value` 字符串列。好处是外键完整性能直接约束到 `question_options`（选项被删规则自动级联删），坏处是要多判断哪个类型该用哪一列；如果队友的实现用的是单一 `trigger_value` 字段，这部分校验逻辑要相应改成"按题型解析 trigger_value 该按 UUID 还是按整数解析"。
- `target_item_id` 特意用 `RESTRICT` 而不是 `CASCADE`：如果某道题正被别的规则当跳转目标，删除它必须先报错，让研究者显式处理，而不是让它在别的批量删除操作里被静默级联带走。
- 两条 `UNIQUE` 约束是这里唯一"防止跳转结果二义性"的硬保证，如果队友的表结构没有这两条，运行时查表可能查出多条规则，需要在跑 M5 之前补上，否则 NFR-15 不成立。

## 2. 应用层校验逻辑（这是我这版更完整的部分）

保存问卷时（对应 FR-38 部分），对每条跳转规则按顺序做这几层校验，任何一层不过直接 400 拒绝、不落库：

### 2.1 题型限制
只有 `SINGLE_CHOICE`（单选）和 `SCALE`（打分）两种题型允许配跳转规则。`MULTI_CHOICE`（多选）直接拒绝——因为一次可以选多个选项，如果每个选项各配了不同跳转目标，"选中的答案"就不再能唯一决定下一题去哪，会破坏确定性。

```java
if (question.getType() != QuestionType.SINGLE_CHOICE && question.getType() != QuestionType.SCALE) {
    throw new InvalidQuestionnaireDataException(
            "Branch rules are only supported for SINGLE_CHOICE and SCALE questions: " + question.getId()
    );
}
```

### 2.2 触发条件校验（二选一 + 归属/范围检查）
- 请求里 `sourceOptionId`/`sourceScaleValue` 必须**恰好设置一个**（不能都空、也不能都有）
- 题型是 `SINGLE_CHOICE`：必须给 `sourceOptionId`，且这个选项 ID 必须真的属于当前这道题（不能拿别的题目的选项 ID 来配，防止跨题目伪造）
- 题型是 `SCALE`：必须给 `sourceScaleValue`，且数值必须落在这道题自己配置的 `[scaleMin, scaleMax]` 区间内（防止配一个题目本身都选不到的刻度值）

```java
boolean hasOption = ruleRequest.sourceOptionId() != null;
boolean hasScale = ruleRequest.sourceScaleValue() != null;
if (hasOption == hasScale) {
    throw new InvalidQuestionnaireDataException("Exactly one of sourceOptionId or sourceScaleValue is required");
}

if (question.getType() == QuestionType.SINGLE_CHOICE) {
    if (!hasOption) { /* 拒绝：SINGLE_CHOICE 必须给 sourceOptionId */ }
    if (!validOptionIds.contains(ruleRequest.sourceOptionId())) {
        // 拒绝：这个选项不属于当前题目
    }
} else { // SCALE
    if (!hasScale) { /* 拒绝：SCALE 必须给 sourceScaleValue */ }
    int value = ruleRequest.sourceScaleValue();
    if (value < question.getScaleMin() || value > question.getScaleMax()) {
        // 拒绝：超出这道题自己的刻度范围
    }
}
```

`validOptionIds` 是从**当前这道题目当前的活的选项集合**里现查的（`question.getOptions()`），不是信任请求里传来的东西，杜绝越权引用别的题目的选项。

### 2.3 同一题内触发值不能重复
同一道题目下，两条规则不能用同一个 `sourceOptionId` 或同一个 `sourceScaleValue`（应用层这里查重，和数据库的 `UNIQUE` 约束是双保险——应用层给出清晰的 400 错误信息，数据库约束兜底防止应用层校验被绕过时数据仍然一致）：

```java
Set<Object> triggers = new HashSet<>();
for (ruleRequest : ruleRequests) {
    Object trigger = validateTrigger(...);       // 返回校验通过的 optionId 或 scaleValue
    if (!triggers.add(trigger)) {
        throw new InvalidQuestionnaireDataException("Duplicate branch rule trigger ...");
    }
    ...
}
```

### 2.4 跳转目标校验
- `targetPosition` 必须是本次保存请求里 items 数组的合法下标（`0 <= targetPosition < itemCount`）——防止越界引用一个不存在的题目
- `targetPosition` 不能等于当前题目自己的下标——防止自己跳自己（死循环的最简单形式；数据库 `ck_branch_rules_no_self_loop` 也兜底这条）

```java
private void validateTarget(int targetPosition, int sourcePosition, int itemCount) {
    if (targetPosition < 0 || targetPosition >= itemCount) {
        throw new InvalidQuestionnaireDataException("Branch target position " + targetPosition + " is out of range");
    }
    if (targetPosition == sourcePosition) {
        throw new InvalidQuestionnaireDataException("A branch rule cannot target its own item");
    }
}
```

> 注意：这里**没有**校验"多条规则连起来会不会形成更长的循环"（比如 A→B→C→A，每条规则单独看都不是自跳）。当前实现不检测这种情况，运行时如果真的存在环，会导致参与者陷入死循环——如果队友的题量/跳转配置复杂度较高，这是一个值得在移植时一起补的检查点（可以在保存时对 target 关系做一次图遍历判环）。

## 3. 校验顺序小结（方便直接对照移植）

对每一条跳转规则，按下面顺序执行，任意一步失败就整体拒绝这次保存（不会部分落库，因为 FR-39 是整体原子保存）：

```
1. 题目类型是 SINGLE_CHOICE 或 SCALE？否则拒绝
2. sourceOptionId / sourceScaleValue 是否恰好设置了一个？否则拒绝
3. 按题目类型校验触发值：
   - SINGLE_CHOICE: sourceOptionId 是否属于该题目的选项？
   - SCALE: sourceScaleValue 是否落在该题目的 [scaleMin, scaleMax]？
4. 触发值在同一题目下是否已经用过（重复规则）？是则拒绝
5. targetPosition 是否越界（< 0 或 >= 题目总数）？是则拒绝
6. targetPosition 是否等于自己？是则拒绝
```

对应到数据库层面的兜底（即便应用层被绕过，落库也会失败）：
- `ck_branch_rules_single_trigger`（对应第 2 步）
- `fk_branch_rules_source_option`（对应第 3 步的 SINGLE_CHOICE 分支，选项必须真实存在）
- `uk_branch_rules_source_option` / `uk_branch_rules_source_scale_value`（对应第 4 步）
- `fk_branch_rules_target_item` + `ck_branch_rules_no_self_loop`（对应第 5、6 步）

移植到队友实现时，建议至少把「数据库唯一约束 + 二选一 CHECK + 自跳 CHECK」这三条硬约束原样搬过去（不管应用层校验写得多完整，这是最后一道防线），应用层校验逻辑可以按他现有的代码风格重写，但上面 6 步的判断顺序和条件建议保持一致。
