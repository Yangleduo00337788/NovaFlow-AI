package ai.novaflow.tool.controller;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.tool.domain.vo.ToolDefinitionVO;
import ai.novaflow.tool.service.ToolDefinitionService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillController {

    private final ToolDefinitionService toolDefinitionService;

    @SaCheckPermission(value = {PermissionCodes.TOOL_READ, PermissionCodes.AGENT_EDIT, PermissionCodes.AGENT_CREATE}, mode = SaMode.OR)
    @GetMapping("/options")
    public ApiResult<List<ToolDefinitionVO>> options(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(toolDefinitionService.listSkillOptions(keyword));
    }

    @SaCheckPermission(value = {PermissionCodes.TOOL_CREATE, PermissionCodes.AGENT_EDIT}, mode = SaMode.OR)
    @PostMapping("/upload")
    public ApiResult<ToolDefinitionVO> upload(@RequestParam("file") MultipartFile file) {
        return ApiResult.ok(toolDefinitionService.uploadSkill(file));
    }

    @SaCheckPermission(value = {PermissionCodes.TOOL_UPDATE, PermissionCodes.AGENT_EDIT}, mode = SaMode.OR)
    @PostMapping("/{id}/upload")
    public ApiResult<ToolDefinitionVO> reupload(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(toolDefinitionService.reuploadSkill(id, file));
    }
}
