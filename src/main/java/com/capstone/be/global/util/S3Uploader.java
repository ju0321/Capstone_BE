package com.capstone.be.global.util;

import com.capstone.be.domain.file.error.FileErrorCode;
import com.capstone.be.global.exception.CustomException;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3에 파일을 업로드하는 공용 컴포넌트.
 * 원본 파일명 앞에 UUID를 붙여서 저장하기 때문에, 같은 이름의 파일이 여러 번
 * 업로드되어도 S3에서 서로 덮어쓰지 않는다. (예: 아몬드1_3.jpg -> 3f2504e0-...-아몬드1_3.jpg)
 */
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.region}")
    private String region;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(FileErrorCode.EMPTY_FILE);
        }

        String storedFileName = buildStoredFileName(file.getOriginalFilename());

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storedFileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | SdkException e) {
            throw new CustomException(FileErrorCode.UPLOAD_FAILED);
        }

        return buildFileUrl(storedFileName);
    }

    /**
     * upload()로 올렸던 파일을 URL 기준으로 S3에서 삭제한다.
     * (예: 프로필 이미지 교체 시 기존 이미지를 정리하는 용도)
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        String storedFileName = extractStoredFileName(fileUrl);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storedFileName)
                    .build());
        } catch (SdkException e) {
            throw new CustomException(FileErrorCode.DELETE_FAILED);
        }
    }

    // 원본 파일명 앞에 UUID를 붙여서 저장용 파일명을 만든다. (예: 3f2504e0-...-아몬드1_3.jpg)
    private String buildStoredFileName(String originalFilename) {
        String safeOriginalName = (originalFilename == null || originalFilename.isBlank())
                ? "file"
                : originalFilename;
        return UUID.randomUUID() + "_" + safeOriginalName;
    }

    private String buildFileUrl(String storedFileName) {
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, storedFileName);
    }

    // buildFileUrl()의 역연산: "https://{bucket}.s3.{region}.amazonaws.com/{storedFileName}"에서 key만 추출한다.
    private String extractStoredFileName(String fileUrl) {
        int idx = fileUrl.indexOf(".amazonaws.com/");
        if (idx == -1) {
            throw new CustomException(FileErrorCode.DELETE_FAILED);
        }
        return fileUrl.substring(idx + ".amazonaws.com/".length());
    }
}
