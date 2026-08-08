-- 为已有知识库补全 Qdrant 集合名
UPDATE knowledge_base
SET qdrant_collection = CONCAT('kb_', tenant_id, '_', id)
WHERE qdrant_collection IS NULL OR qdrant_collection = '';
