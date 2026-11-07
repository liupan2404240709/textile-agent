package com.zoe.java.ai.langchain4j.util;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.collection.HasCollectionParam;
import lombok.extern.slf4j.Slf4j;

/**
 * Milvus 数据查询工具 - 用于验证数据存储
 */
@Slf4j
public class MilvusDataChecker {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 19530;
        String collectionName = "textile_knowledge";

        MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
                        .build()
        );

        try {
            // 1. 检查集合是否存在
            R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );

            if (!hasCollection.getData()) {
                log.error("❌ 集合 {} 不存在！", collectionName);
                return;
            }

            log.info("✅ 集合 {} 存在", collectionName);
            log.info("📊 请使用 Attu 或其他 Milvus 管理工具查看集合中的详细数据");
            log.info("   或使用以下命令查询:");
            log.info("   docker exec -it milvus-standalone milvus-util -collection {}", collectionName);

        } catch (Exception e) {
            log.error("❌ 检查失败", e);
        } finally {
            milvusClient.close();
        }
    }
}
