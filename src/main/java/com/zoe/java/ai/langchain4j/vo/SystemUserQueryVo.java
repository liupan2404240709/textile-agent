package com.zoe.java.ai.langchain4j.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "员工查询实体")
public class SystemUserQueryVo {

    @Schema(description= "员工姓名")
    private String name;

    @Schema(description= "手机号码")
    private String phone;
}
