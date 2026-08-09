package ai.novaflow.tool.mcp;

import ai.novaflow.common.exception.BusinessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

final class McpProcessLauncher {

    private McpProcessLauncher() {
    }

    static Process start(McpServerConfig config) throws IOException {
        List<String> commandLine = buildCommandLine(config);
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        if (config.getEnv() != null && !config.getEnv().isEmpty()) {
            processBuilder.environment().putAll(config.getEnv());
        }
        prependNodeToPath(processBuilder);
        try {
            return processBuilder.start();
        } catch (IOException ex) {
            throw wrapStartFailure(config, ex);
        }
    }

    private static List<String> buildCommandLine(McpServerConfig config) {
        String command = config.getCommand().trim();
        List<String> args = config.getArgs() != null ? new ArrayList<>(config.getArgs()) : new ArrayList<>();

        if ("npx".equalsIgnoreCase(command)) {
            Optional<List<String>> viaNode = buildNpxViaNode(args);
            if (viaNode.isPresent()) {
                return viaNode.get();
            }
        }

        List<String> commandLine = new ArrayList<>();
        commandLine.add(resolveExecutable(command));
        commandLine.addAll(args);
        return commandLine;
    }

    private static Optional<List<String>> buildNpxViaNode(List<String> args) {
        Optional<Path> nodeDir = findNodeInstallDir();
        if (nodeDir.isEmpty()) {
            return Optional.empty();
        }
        Path nodeExe = nodeDir.get().resolve(isWindows() ? "node.exe" : "node");
        Path npxCli = nodeDir.get().resolve("node_modules/npm/bin/npx-cli.js");
        if (!Files.isRegularFile(nodeExe) || !Files.isRegularFile(npxCli)) {
            return Optional.empty();
        }
        List<String> commandLine = new ArrayList<>();
        commandLine.add(nodeExe.toAbsolutePath().toString());
        commandLine.add(npxCli.toAbsolutePath().toString());
        commandLine.addAll(args);
        return Optional.of(commandLine);
    }

    private static String resolveExecutable(String command) {
        if (command.contains("/") || command.contains("\\") || command.contains(".")) {
            return command;
        }
        if (isWindows()) {
            String resolved = resolveInPath(command + ".cmd");
            if (resolved != null) {
                return resolved;
            }
            resolved = resolveInPath(command + ".exe");
            if (resolved != null) {
                return resolved;
            }
            if ("npx".equalsIgnoreCase(command) || "npm".equalsIgnoreCase(command)) {
                return command + ".cmd";
            }
            if ("node".equalsIgnoreCase(command)) {
                return command + ".exe";
            }
        }
        String resolved = resolveInPath(command);
        return resolved != null ? resolved : command;
    }

    private static void prependNodeToPath(ProcessBuilder processBuilder) {
        List<String> nodeDirs = collectNodeDirs();
        if (nodeDirs.isEmpty()) {
            return;
        }
        String separator = isWindows() ? ";" : ":";
        String currentPath = processBuilder.environment().getOrDefault("PATH", "");
        String prefix = String.join(separator, nodeDirs);
        if (currentPath.isBlank()) {
            processBuilder.environment().put("PATH", prefix);
            return;
        }
        processBuilder.environment().put("PATH", prefix + separator + currentPath);
    }

    private static List<String> collectNodeDirs() {
        Set<String> dirs = new LinkedHashSet<>();
        addDirIfValid(dirs, System.getenv("NOVAFLOW_NODE_PATH"));
        addDirIfValid(dirs, System.getenv("NODE_HOME"));
        for (String dir : splitPath(System.getenv("PATH"))) {
            if (Files.isRegularFile(Paths.get(dir, isWindows() ? "node.exe" : "node"))
                    || Files.isRegularFile(Paths.get(dir, isWindows() ? "npx.cmd" : "npx"))) {
                dirs.add(dir);
            }
        }
        for (String command : List.of("npx", "node")) {
            locateExecutableParent(command).map(Path::toString).ifPresent(dirs::add);
        }
        return List.copyOf(dirs);
    }

    private static Optional<Path> findNodeInstallDir() {
        for (String dir : collectNodeDirs()) {
            Path nodeDir = Paths.get(dir);
            if (Files.isRegularFile(nodeDir.resolve(isWindows() ? "node.exe" : "node"))) {
                return Optional.of(nodeDir);
            }
        }
        return Optional.empty();
    }

    private static Optional<Path> locateExecutableParent(String command) {
        try {
            ProcessBuilder processBuilder = isWindows()
                    ? new ProcessBuilder("cmd.exe", "/c", "where " + command)
                    : new ProcessBuilder("sh", "-lc", "command -v " + command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Path path = Paths.get(line.trim());
                    if (Files.isRegularFile(path) && path.getParent() != null) {
                        process.waitFor(3, TimeUnit.SECONDS);
                        return Optional.of(path.getParent());
                    }
                }
            }
            process.waitFor(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // fall through
        }
        return Optional.empty();
    }

    private static String resolveInPath(String executable) {
        for (String dir : splitPath(System.getenv("PATH"))) {
            Path candidate = Paths.get(dir, executable);
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return null;
    }

    private static List<String> splitPath(String pathValue) {
        if (pathValue == null || pathValue.isBlank()) {
            return List.of();
        }
        String separator = isWindows() ? ";" : ":";
        List<String> dirs = new ArrayList<>();
        for (String dir : pathValue.split(separator)) {
            if (!dir.isBlank()) {
                dirs.add(dir.trim());
            }
        }
        return dirs;
    }

    private static void addDirIfValid(Set<String> dirs, String dir) {
        if (dir == null || dir.isBlank()) {
            return;
        }
        Path path = Paths.get(dir.trim());
        if (Files.isDirectory(path)) {
            dirs.add(path.toAbsolutePath().toString());
        }
    }

    private static BusinessException wrapStartFailure(McpServerConfig config, IOException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        if (!message.contains("error=2") && !message.contains("No such file")) {
            return new BusinessException("启动 MCP 进程失败: " + message);
        }
        return new BusinessException(
                "无法启动 MCP 命令 \"" + config.getCommand() + "\"，请确认 Node.js 已安装。"
                        + " Windows 可将 Node 安装目录加入系统 PATH，或设置环境变量 NOVAFLOW_NODE_PATH"
                        + "（例如 D:\\Develop\\nodejs），然后重启后端服务。");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
