package ai.novaflow.knowledge.storage;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.knowledge.config.StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioDocumentStorageService implements DocumentStorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    @PostConstruct
    public void ensureBucket() {
        try {
            String bucket = storageProperties.getBucket();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO bucket initialization skipped: {}", e.getMessage());
        }
    }

    @Override
    public String store(Long tenantId, Long knowledgeBaseId, String originalFilename, MultipartFile file) {
        String objectPath = buildObjectPath(tenantId, knowledgeBaseId, originalFilename);
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .object(objectPath)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return objectPath;
        } catch (Exception e) {
            throw new BusinessException("文件上传失败，请确认 MinIO 服务已启动: " + e.getMessage());
        }
    }

    @Override
    public void delete(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .object(objectPath)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete object {}: {}", objectPath, e.getMessage());
        }
    }

    @Override
    public byte[] load(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            throw new BusinessException("文件路径无效");
        }
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(storageProperties.getBucket())
                .object(objectPath)
                .build())) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new BusinessException("读取文件失败: " + e.getMessage());
        }
    }

    private String buildObjectPath(Long tenantId, Long knowledgeBaseId, String originalFilename) {
        String safeName = originalFilename == null ? "file" : originalFilename.replaceAll("[\\\\/:*?\"<>|]", "_");
        return "knowledge/" + tenantId + "/" + knowledgeBaseId + "/" + UUID.randomUUID() + "_" + safeName;
    }
}
