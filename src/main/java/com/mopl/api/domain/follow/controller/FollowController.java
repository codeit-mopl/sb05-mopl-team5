package com.mopl.api.domain.follow.controller;


import com.mopl.api.domain.follow.dto.request.FollowRequest;
import com.mopl.api.domain.follow.dto.response.FollowDto;
import com.mopl.api.domain.follow.dto.response.FollowResponseByMeDto;
import com.mopl.api.domain.follow.dto.response.FollowResponseCountDto;
import com.mopl.api.domain.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService service;

    @PostMapping
    public ResponseEntity<FollowDto> followAdd(@Valid @RequestBody FollowRequest request) {
        FollowDto followDto = service.addFollow(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(followDto);
    }

    @GetMapping("/followed-by-me")
    public ResponseEntity<FollowResponseByMeDto> followCheck(@RequestParam UUID followeeId) {
        FollowResponseByMeDto followResponseByMeDto = service.checkFollow(followeeId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(followResponseByMeDto);
    }

    @GetMapping("/count")
    public ResponseEntity<FollowResponseCountDto> followCount(@RequestParam UUID followeeId) {
        FollowResponseCountDto followResponseCountDto = service.countFollow(followeeId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(followResponseCountDto);
    }

    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> followRemove(@PathVariable UUID followId) {

        service.followRemove(followId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                             .build();
    }
}