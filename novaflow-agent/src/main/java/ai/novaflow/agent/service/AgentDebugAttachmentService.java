package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.vo.DebugAttachmentVO;
import ai.novaflow.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class AgentDebugAttachmentService {

    private static final int MAX_BYTES = 64 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "md", "json", "csv", "log");

    public DebugAttachmentVO parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("附件大小不能超过 64KB");
        }

        String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "attachment.txt";
        String extension = extractExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 txt、md、json、csv、log 文本附件");
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
            if (!StringUtils.hasText(content)) {
                throw new BusinessException("附件内容为空");
            }
            return DebugAttachmentVO.builder()
                    .fileName(fileName)
                    .content(content)
                    .contentLength(content.length())
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("附件解析失败: " + e.getMessage());
        }
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}
