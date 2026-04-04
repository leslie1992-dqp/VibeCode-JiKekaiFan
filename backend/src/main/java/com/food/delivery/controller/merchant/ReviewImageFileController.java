package com.food.delivery.controller.merchant;

import com.food.delivery.common.exception.BizException;
import com.food.delivery.impl.merchant.MerchantReviewImageStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class ReviewImageFileController {

    private final MerchantReviewImageStorageService merchantReviewImageStorageService;

    public ReviewImageFileController(MerchantReviewImageStorageService merchantReviewImageStorageService) {
        this.merchantReviewImageStorageService = merchantReviewImageStorageService;
    }

    @GetMapping("/api/v1/files/review-images/{filename:.+}")
    public ResponseEntity<byte[]> getReviewImage(@PathVariable String filename) throws IOException {
        Path path;
        try {
            path = merchantReviewImageStorageService.resolveReviewImagePath(filename);
        } catch (BizException ex) {
            return ResponseEntity.notFound().build();
        }
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(path);
        MediaType mediaType = merchantReviewImageStorageService.probeMediaType(filename);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(bytes);
    }
}
