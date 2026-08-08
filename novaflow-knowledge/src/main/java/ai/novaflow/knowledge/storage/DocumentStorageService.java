package ai.novaflow.knowledge.storage;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentStorageService {

    String store(Long tenantId, Long knowledgeBaseId, String originalFilename, MultipartFile file);

    void delete(String objectPath);

    byte[] load(String objectPath);
}
