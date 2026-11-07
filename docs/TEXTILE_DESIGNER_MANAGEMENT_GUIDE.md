# 纺织设计师与纺织设计项目管理功能说明

## 概述
本模块实现了纺织行业智能体系统的纺织设计师与纺织设计项目管理功能，包括：
1. **纺织设计师档案管理** - 记录设计师基本信息、专业方向（面料/服装/家纺/花型/印染等）、工作经历、设计成果等，构建纺织设计师能力模型
2. **纺织设计项目管理** - 支持设计师维护纺织设计项目，涵盖设计目标、阶段、验收标准与进度监控
3. **纺织设计评审管理** - 评审打分、情感分析、评审报告生成

## 数据库表结构

### 1. textile_designer_profile (纺织设计师档案表)
存储纺织设计师的基本信息、工作经历、设计成果等数据。

**主要字段：**
- `designer_no` - 纺织设计师编号（唯一，例如 TEX001）
- `name` - 纺织设计师姓名
- `specialty` - 专业方向（面料设计、服装设计、家纺设计、花型设计、印染工艺、针织工艺、纺织材料、产业用纺织品等）
- `experience_years` - 从业年限
- `rating` - 综合评分（满分5分）
- `good_rate` - 好评率（百分比）
- `skill_tags` - 技能标签（用于知识图谱）
- `total_projects` - 项目总数
- `total_awards` - 获奖数量

### 2. textile_design_project (纺织设计项目表)
存储纺织设计师的设计项目信息。

**主要字段：**
- `designer_id` - 纺织设计师ID
- `project_name` - 项目名称
- `stage` - 项目阶段（概念提案、面料开发、花型设计、印染打样、大货生产）
- `design_cycle` - 设计周期（天）
- `project_status` - 项目状态（0-草稿，1-待评审，2-已通过，3-已驳回，4-进行中，5-已完成）
- `acceptance_criteria` - 验收标准（克重、色牢度、缩水率、缸差ΔE等）
- `actual_progress` - 实际进度（百分比）
- `design_effect` - 设计成效评估

### 3. textile_design_review (纺织设计评审表)
存储对纺织设计师及其项目的评审数据。

**主要字段：**
- `designer_id` - 纺织设计师ID
- `project_id` - 项目ID
- `overall_score` - 总体评分（1-5分）
- `innovation_score` - 创新性评分
- `usability_score` - 实用性评分
- `processability_score` - 工艺可行性（试纺/试织/印染打样可行性）评分
- `sentiment_type` - 情感类型（positive/neutral/negative）
- `sentiment_score` - 情感分析得分（-1到1）
- `review_content` - 评审内容

### 4. textile_review_appointment (纺织设计评审会议预约表)
存储纺织设计评审会议的预约信息。

**主要字段：**
- `username` - 预约人姓名
- `project_name` - 评审项目名称
- `date` / `time` - 评审日期与时间段
- `reviewer_name` - 评审专家姓名

## 部署步骤

### 1. 执行SQL脚本
按顺序执行以下SQL脚本：

```bash
# 1. 创建表结构
mysql -u root -p llm < create_textile_design_tables.sql

# 2. 插入测试数据
mysql -u root -p llm < insert_textile_designer_test_data.sql
```

或者在MySQL客户端中手动执行：
```sql
source D:\agent\create_textile_design_tables.sql
source D:\agent\insert_textile_designer_test_data.sql
```

> 若从旧版本（工业设计域）升级，请先执行 `migrate_to_textile_design.sql` 完成表与字段重命名。

### 2. 测试数据说明

**纺织设计师档案（10条）：**
- 张伟 - 面料设计师（12年经验，评分4.8）
- 李芳 - 家纺设计师（8年经验，评分4.9）
- 王强 - 服装设计师（11年经验，评分4.7）
- 赵敏 - 印染工艺师（6年经验，评分4.6）
- 刘伟 - 针织工艺专家（15年经验，评分4.9）
- 陈静 - 花型设计师（5年经验，评分4.5）
- 孙涛 - 纺织材料工程师（9年经验，评分4.7）
- 周丽 - 色彩管理师（7年经验，评分4.8）
- 吴刚 - 产业用纺织品设计师（11年经验，评分4.6）
- 郑雪 - 可持续纺织设计师（6年经验，评分4.7）

**纺织设计项目（9条）：** 覆盖面料开发、家纺、服装、针织、染整工艺与色彩管理等方向
**纺织设计评审（20条）：** 包含评分、评审内容和情感分析数据

## API接口

> 后端实际路径为 `/textile/designer/**`；前端 Vite 代理会把 `/api` 前缀去掉后转发到后端，
> 因此浏览器里访问的是 `/api/textile/designer/**`。直接用 Postman/curl 调试时请使用 `/textile/designer/**`。

### 查询纺织设计师信息

#### 1. 根据姓名查询纺织设计师
```
GET /textile/designer/getByName?name=张伟
```

**响应示例：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "designerNo": "TEX001",
    "name": "张伟",
    "gender": 1,
    "specialty": "面料设计",
    "experienceYears": 12,
    "rating": 4.8,
    "goodRate": 96.50,
    "introduction": "资深面料设计师...",
    "skillTags": "面料设计,织造工艺,功能整理,纱线选型,试纺打样"
  }
}
```

#### 2. 根据纺织设计师编号查询
```
GET /textile/designer/getByNo?designerNo=TEX001
```

#### 3. 查询所有纺织设计师列表
```
GET /textile/designer/list
```

#### 4. 根据专业方向查询
```
GET /textile/designer/getBySpecialty?specialty=面料设计
```

#### 5. 查询纺织设计师的设计项目
```
GET /textile/designer/designProjects?designerId=1
```

#### 6. 查询纺织设计师的设计评审列表
```
GET /textile/designer/reviews?designerId=1
```

#### 7. 查询纺织设计师详细信息（包含档案、设计项目、设计评审）
```
GET /textile/designer/detail?designerId=1
```

## AI智能体查询示例

### 查询张伟的纺织设计师信息
AI智能体可以通过以下方式查询：

1. **直接调用API：**
   ```
   GET http://localhost:8082/textile/designer/getByName?name=张伟
   ```

2. **使用Service层方法：**
   ```java
   TextileDesignerProfile designer = textileDesignerService.getDesignerByName("张伟");
   ```

3. **查询详细信息：**
   ```java
   TextileDesignerService.DesignerDetailVO detail = textileDesignerService.getDesignerDetail(1L);
   ```

### AI 工具（@Tool）
`TextileDesignQueryTools` 提供 5 个可被大模型调用的工具：
- `查询纺织设计师档案` - 按姓名或编号查纺织设计师
- `查询纺织设计项目` - 查纺织设计师的设计项目
- `查询纺织设计评审` - 查纺织设计师的设计评审报告
- `查询纺织设计师完整信息` - 档案 + 项目 + 评审
- `按专业方向查询纺织设计师` - 按专业方向查纺织设计师列表

`TextileReviewAppointmentTools` 提供 3 个评审会议工具：
- `预约纺织设计评审` / `取消纺织设计评审预约` / `查询评审排期`

### 查询结果包含
- **基本信息：** 姓名、性别、学历、专业、联系方式
- **专业能力：** 专业方向、从业年限、资质证书、技能标签
- **设计成果：** 项目总数、获奖数量、综合评分、好评率
- **工作经历：** 详细的工作经历描述
- **设计项目：** 当前和历史纺织设计项目
- **设计评审：** 评审列表、情感分析结果

## 技术栈
- **后端框架：** Spring Boot + MyBatis-Plus
- **数据库：** MySQL 5.5+
- **实体映射：** Lombok
- **API设计：** RESTful风格

## 注意事项
1. 所有表都使用 `is_deleted` 字段实现逻辑删除
2. 时间字段使用 `DATETIME` 类型，兼容MySQL 5.5
3. 评分字段使用 `DECIMAL` 类型保证精度
4. 技能标签使用逗号分隔，便于知识图谱构建
5. 情感分析字段支持AI智能分析功能
6. 编号规则为 `TEX` + 数字（如 TEX001），与工具的正则匹配保持一致

## 扩展功能建议
1. 集成AI情感分析服务，自动分析评审内容
2. 基于技能标签构建纺织设计师知识图谱
3. 实现纺织设计项目智能评审功能
4. 添加项目里程碑自动提醒功能
5. 生成纺织设计评审报告与考核依据
