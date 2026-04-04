package com.food.delivery.controller.user;

import com.food.delivery.common.exception.BizException;
import com.food.delivery.impl.user.UserAvatarStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class UserAvatarFileController {

    private final UserAvatarStorageService userAvatarStorageService;

    public UserAvatarFileController(UserAvatarStorageService userAvatarStorageService) {
        this.userAvatarStorageService = userAvatarStorageService;
    }

    @GetMapping("/api/v1/files/user-avatars/{filename:.+}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String filename) throws IOException {
        Path path;
        try {
            path = userAvatarStorageService.resolveAvatarPath(filename);
        } catch (BizException ex) {
            return ResponseEntity.notFound().build();
        }
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(path);
        MediaType mediaType = userAvatarStorageService.probeMediaType(filename);
        return ResponseEntity.ok().contentType(mediaType).body(bytes);
    }
}
