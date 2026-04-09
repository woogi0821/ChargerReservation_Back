package com.simplecoding.chargerreservation.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Component
@Log4j2
@RequiredArgsConstructor
public class CommonUtil {

    @Value("${image.upload-dir}")
    private String uploadDir;
    private final MessageSource messageSource;

//    에러메세지 표시
    public String getMessage(String code) {
        return messageSource.getMessage(code, null, Locale.getDefault());
    }

//    서버 유효성 체크
    public void checkBindingResult(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // 로그로 상세 에러
            bindingResult.getAllErrors()
                    .forEach(error -> log.error(error.getDefaultMessage()));

            // 통합 메시지: 유효성 에러가 표시됩니다.
            throw new RuntimeException(getMessage("errors.validation.common"));
        }
    }

    // 서버에 파일 저장
    public void saveFile(MultipartFile file, String  uuid) throws Exception {
        Path folderPath = Paths.get(uploadDir); // uploadDir
        if (!Files.exists(folderPath)) {
            throw new RuntimeException(getMessage("errors.path.not.found"));
        }
        // 파일 저장
        Path uuidPath = folderPath.resolve(uuid);   // uploadDir/uuid
        file.transferTo(uuidPath);
    }

    // 서버에 저장된 파일 삭제
    public void deleteFile(String uuid) {
        Path uuidPath = Paths.get(uploadDir).resolve(uuid);
        try {
            Files.deleteIfExists(uuidPath);          // 있을때만 삭제
        } catch (IOException e) {
            throw new RuntimeException(getMessage("errors.file.delete.fail"), e);
        }
    }

    // 서버에 저장된 파일 읽기
    public byte[] readFile(String uuid) throws Exception {
        // 서버에 저장된 실제 파일 경로
        Path path = Paths.get(uploadDir).resolve(uuid);

        if (!Files.exists(path)) {
            throw new RuntimeException(getMessage("errors.path.not.found")); // 또는 커스텀 예외
        }

        return Files.readAllBytes(path);
    }

    //	다운로드 URL을 만들어주는 메소드
    // 예시) http://localhost:8080/api/download/fileDb/{uuid}
    public String generateUrl(String path, String uuid) {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()               // 기본주소 : http://localhost:8080
                .path("/api/download/"+path+"/{uuid}")  // 경로    : /download/fileDb/{uuid}
                .buildAndExpand(uuid)                   // 파라메터방식: uuid 값넣기
                .toUriString();                         // 위에꺼조합:
    }

    // IP 추출을 위한 헬퍼 메서드
    public String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        String ip = null;

        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) break;
        }

        if (ip == null) ip = request.getRemoteAddr();

        return ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

}
