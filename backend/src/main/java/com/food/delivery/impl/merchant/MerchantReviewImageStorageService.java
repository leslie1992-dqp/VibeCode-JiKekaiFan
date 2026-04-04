package com.food.delivery.impl.merchant;

import com.food.delivery.common.exception.BizException;
import com.food.delivery.config.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class MerchantReviewImageStorageService {

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9._-]{0,200}$");
    private static final Set<String> ALLOW_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final AppProperties appProperties;
    private Path reviewImageDir;

    public MerchantReviewImageStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() throws IOException {
        String dir = appProperties.getUpload().getDir();
        Path base = Paths.get(dir);
        if (!base.isAbsolute()) {
            base = Paths.get(System.getProperty("user.dir")).resolve(dir);
        }
        reviewImageDir = base.resolve("review-images");
        Files.createDirectories(reviewImageDir);
    }

    public Path resolveReviewImagePath(String filename) {
        if (!StringUtils.hasText(filename) || !SAFE_NAME.matcher(filename).matches()) {
            throw new BizException(40404, "file not found");
        }
        Path p = reviewImageDir.resolve(filename).normalize();
        if (!p.startsWith(reviewImageDir)) {
            throw new BizException(40404, "file not found");
        }
        return p;
    }

    public MediaType probeMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    /**
     * 保存上传文件，返回可访问的完整 URL（供前端 img src）
     */
    public List<String> saveAndBuildUrls(List<MultipartFile> files, HttpServletRequest request) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new BizException(40001, "请选择图片");
        }
        if (files.size() > 6) {
            throw new BizException(40001, "单次最多上传 6 张图片");
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String ext = extensionOf(file.getOriginalFilename(), file.getContentType());
            if (ext == null) {
                throw new BizException(40001, "仅支持 jpg/png/gif/webp 图片");
            }
            String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = reviewImageDir.resolve(name);
            Files.copy(file.getInputStream(), target);
            urls.add(buildPublicUrl(request, name));
        }
        if (urls.isEmpty()) {
            throw new BizException(40001, "请选择图片");
        }
        return urls;
    }

    private static String buildPublicUrl(HttpServletRequest request, String filename) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);
        if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
            sb.append(":").append(port);
        }
        sb.append("/api/v1/files/review-images/").append(filename);
        return sb.toString();
    }

    private static String extensionOf(String originalName, String contentType) {
        String ext = null;
        if (StringUtils.hasText(originalName)) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                ext = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }
        if (ext == null || !ALLOW_EXT.contains(ext)) {
            if (contentType != null) {
                if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                    ext = "jpg";
                } else if (contentType.contains("png")) {
                    ext = "png";
                } else if (contentType.contains("gif")) {
                    ext = "gif";
                } else if (contentType.contains("webp")) {
                    ext = "webp";
                }
            }
        }
        if (ext == null || !ALLOW_EXT.contains(ext)) {
            return null;
        }
        return ext;
    }
}
