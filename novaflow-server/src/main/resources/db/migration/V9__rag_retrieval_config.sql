-- 知识库默认检索参数
ALTER TABLE `knowledge_base`
    ADD COLUMN `retrieval_top_k` INT NOT NULL DEFAULT 5 COMMENT '检索 Top-K' AFTER `chunk_overlap`,
    ADD COLUMN `retrieval_score_threshold` DECIMAL(4, 3) NULL COMMENT '相似度阈值（0-1）' AFTER `retrieval_top_k`;
