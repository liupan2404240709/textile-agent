package com.zoe.java.ai.langchain4j.util;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

/**
 * Milvus 集合管理工具 - 用于重置和重新创建集合
 */
@Slf4j
public class MilvusCollectionResetter {

    public static void main(String[] args) {
        // Milvus 连接参数
        String host = "localhost";
        int port = 19530;
        String collectionName = "textile_knowledge";
        int dimension = 1024; // text-embedding-v3 实际输出1024维

        // 连接到 Milvus
        MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
                        .build()
        );

        log.info("正在连接到 Milvus: {}:{}", host, port);

        try {
            // 1. 检查集合是否存在
            R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );

            if (hasCollection.getData()) {
                log.warn("发现已存在的集合: {}", collectionName);
                
                // 2. 删除旧集合
                log.info("正在删除旧集合...");
                R<RpcStatus> dropResponse = milvusClient.dropCollection(
                        DropCollectionParam.newBuilder()
                                .withCollectionName(collectionName)
                                .build()
                );
                
                if (dropResponse.getStatus() == 0) {
                    log.info("✅ 旧集合已成功删除");
                } else {
                    log.error("❌ 删除集合失败: {}", dropResponse.getMessage());
                    return;
                }
            } else {
                log.info("集合 {} 不存在，将创建新集合", collectionName);
            }

            // 3. 创建新集合（1024 维）
            log.info("正在创建新集合，维度: {}", dimension);
            
            FieldType fieldType1 = FieldType.newBuilder()
                    .withName("id")
                    .withDataType(DataType.VarChar)
                    .withPrimaryKey(true)
                    .withMaxLength(65535) // 支持UUID字符串
                    .withAutoID(false) // 让LangChain4j管理ID
                    .build();

            FieldType fieldType2 = FieldType.newBuilder()
                    .withName("vector")
                    .withDataType(DataType.FloatVector)
                    .withDimension(dimension)
                    .build();

            FieldType fieldType3 = FieldType.newBuilder()
                    .withName("text")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(65535)
                    .build();

            FieldType fieldType4 = FieldType.newBuilder()
                    .withName("metadata")
                    .withDataType(DataType.JSON)
                    .build();

            CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withShardsNum(2)
                    .addFieldType(fieldType1)
                    .addFieldType(fieldType2)
                    .addFieldType(fieldType3)
                    .addFieldType(fieldType4)
                    .build();

            R<RpcStatus> createResponse = milvusClient.createCollection(createCollectionParam);
            
            if (createResponse.getStatus() == 0) {
                log.info("✅ 新集合已成功创建");
            } else {
                log.error("❌ 创建集合失败: {}", createResponse.getMessage());
                return;
            }

            // 4. 创建索引
            log.info("正在创建向量索引...");
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName("vector")
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam("{\"nlist\":128}")
                    .withSyncMode(Boolean.TRUE)
                    .build();

            R<RpcStatus> indexResponse = milvusClient.createIndex(indexParam);
            
            if (indexResponse.getStatus() == 0) {
                log.info("✅ 索引已成功创建");
            } else {
                log.error("❌ 创建索引失败: {}", indexResponse.getMessage());
                return;
            }

            // 5. 加载集合
            log.info("正在加载集合到内存...");
            R<RpcStatus> loadResponse = milvusClient.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            
            if (loadResponse.getStatus() == 0) {
                log.info("✅ 集合已成功加载到内存");
            } else {
                log.error("❌ 加载集合失败: {}", loadResponse.getMessage());
                return;
            }

            log.info("🎉 Milvus 集合重置完成！");
            log.info("集合名称: {}", collectionName);
            log.info("向量维度: {}", dimension);
            log.info("现在可以重新启动应用并上传文档了");

        } catch (Exception e) {
            log.error("❌ 操作失败", e);
        } finally {
            milvusClient.close();
        }
    }
}
