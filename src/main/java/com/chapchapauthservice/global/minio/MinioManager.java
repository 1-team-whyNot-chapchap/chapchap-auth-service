package com.chapchapauthservice.global.minio;


import com.chapchapauthserivce.global.error.custom.business.FileManagedException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MinioManager {
    private final MinioConfig minioConfig;
    private final MinioClient minioClient;

    /**
     * 파일 확장자 추출 및 파일 검증
     * @param file
     * @return 확장자(소문자)
     */
    public String extractExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileManagedException("파일 업로드 실패: 파일없음");
        }
        String fileName = file.getOriginalFilename();
        if(fileName == null || !fileName.contains(".")) {
            throw new FileManagedException("파일 업로드 실패: 파일명 이상");
        }

        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        // 허용 확장자 검증
        if(!minioConfig.allowImageExtensions().contains("image/" + fileExtension)){
            throw new FileManagedException("파일 업로드 실패: 허용하지 않는 확장자");
        }

        return fileExtension;
    }

    public String generateFileName() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate now =LocalDate.now();
        return now.format(dateFormatter) + "_" + UUID.randomUUID();
    }

    public String generateObjectKey(MultipartFile file) {
        Path path = Path.of(minioConfig.minioProfilePath(), this.generateFileName() + "." + this.extractExtension(file));
        return path.toString().replace(File.separator, "/");
    }

    public void uploadFile(String objectKey, MultipartFile file) {
        try(InputStream inputStream = file.getInputStream()) {
            minioClient
                .putObject(
                    PutObjectArgs.builder()
                        .bucket(minioConfig.minioBucket())  // 파일이 저장될 MinIO의 버킷명
                        .object(objectKey)  // 버킷 내부에서 관리될 전체 저장 경로
                        .stream(
                            inputStream,  // 업로드할 파일의 InputStream
                            file.getSize(),  // 업로드할 파일의 크기
                            -1  // 업로드시 패킷 크기(-1은 MinIO SDK가 적절하게 조절해서 전송)
                        )
                        .contentType(file.getContentType())  // 파일의 MIME 타입
                        .build()
                );
        } catch (Exception e) {
            throw new FileManagedException("파일 업로드 실패: MinIo 업로드 실패, " + objectKey + "\n" + e.getMessage());
        }
    }

    public String createMinioObjectUri(String objectKey){
        Path path = Path.of(minioConfig.minioBucket(), objectKey);
        return String.format(
            "%s/%s",
            minioConfig.minioEndpoint(),
            path.toString().replace(File.separator, "/")
        );
    }

}
