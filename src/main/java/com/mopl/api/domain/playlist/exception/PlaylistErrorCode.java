package com.mopl.api.domain.playlist.exception;

import com.mopl.api.global.config.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {
    PLAYLIST_NOT_FOUND("PL001", "존재하지 않는 플레이리스트입니다.", HttpStatus.NOT_FOUND),
    PLAYLIST_UNAUTHORIZED("PL002", "플레이리스트에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND("PL003", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    CONTENT_NOT_FOUND("PL004", "존재하지 않는 콘텐츠입니다.", HttpStatus.NOT_FOUND),
    CONTENT_ALREADY_EXISTS("PL005", "이미 플레이리스트에 추가된 콘텐츠입니다.", HttpStatus.CONFLICT),
    CONTENT_NOT_IN_PLAYLIST("PL006", "플레이리스트에 존재하지 않는 콘텐츠입니다.", HttpStatus.NOT_FOUND),
    SELF_SUBSCRIPTION("PL007", "자신의 플레이리스트는 구독할 수 없습니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_SUBSCRIPTION("PL008", "이미 구독한 플레이리스트입니다.", HttpStatus.CONFLICT),
    SUBSCRIPTION_NOT_FOUND("PL009", "구독 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}