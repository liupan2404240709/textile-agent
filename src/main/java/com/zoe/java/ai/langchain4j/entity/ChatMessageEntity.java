
package com.zoe.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_messages")
public class ChatMessageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String memoryId;

    private Long userId;

    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}