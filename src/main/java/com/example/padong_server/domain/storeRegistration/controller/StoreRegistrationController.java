package com.example.padong_server.domain.storeRegistration.controller;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.storeLike.dto.LikedStoreResponse;
import com.example.padong_server.domain.storeLike.dto.StoreLikeToggleResponse;
import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.storeRegistration.dto.CategoryOption;
import com.example.padong_server.domain.storeRegistration.dto.ShopDetailResponse;
import com.example.padong_server.domain.storeRegistration.dto.ShopSummaryResponse;
import com.example.padong_server.domain.storeRegistration.dto.StoreImageReorderRequest;
import com.example.padong_server.domain.storeRegistration.dto.StoreImageResponse;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationCreateRequest;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationResponse;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationUpdateRequest;
import com.example.padong_server.domain.storeRegistration.dto.StoreSearchCriteria;
import com.example.padong_server.domain.storeRegistration.entity.ShopStatus;
import com.example.padong_server.domain.storeRegistration.entity.StoreCategory;
import com.example.padong_server.domain.storeRegistration.service.StoreImageService;
import com.example.padong_server.domain.storeRegistration.service.StoreRegistrationService;
import com.example.padong_server.global.PageResponse;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "가게", description = "가게 등록·수정·삭제·조회·이미지 관리·좋아요 API.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stores")
public class StoreRegistrationController {

    private final StoreRegistrationService storeRegistrationService;
    private final StoreLikeService storeLikeService;
    private final StoreImageService storeImageService;

    // ─────────────────────────────── 카테고리 ───────────────────────────────

    @Operation(summary = "가게 카테고리 옵션 조회", description = "등록·수정 dropdown 용 enum 옵션. 인증 불필요.")
    @GetMapping("/categories")
    public ResponseEntity<ResponseDTO<List<CategoryOption>>> getCategories() {
        List<CategoryOption> options =
                Arrays.stream(StoreCategory.values()).map(CategoryOption::from).toList();
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "가게 카테고리 조회 성공", options));
    }

    // ─────────────────────────────── CRUD ───────────────────────────────

    @Operation(summary = "가게 등록 (JSON)", description = "JSON body 로 가게 등록. 이미지는 별도 endpoint 사용.")
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<StoreRegistrationResponse>> createStore(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StoreRegistrationCreateRequest request) {
        StoreRegistrationResponse response =
                storeRegistrationService.createStore(userDetails.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDTO.res(HttpStatus.CREATED, "가게 등록 성공", response));
    }

    @Operation(
            summary = "가게 등록 (multipart 통합)",
            description =
                    "multipart/form-data. `request` part (application/json) + `thumbnail` (optional) + "
                            + "`images` (optional, 다중). 한 번 호출로 가게 + 썸네일 + 갤러리 등록.")
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO<StoreRegistrationResponse>> createStoreWithAssets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestPart("request") StoreRegistrationCreateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        com.example.padong_server.domain.oauth.entity.User user = userDetails.getUser();
        StoreRegistrationResponse created = storeRegistrationService.createStore(user, request);
        Long storeId = created.getId();
        Long userId = user.getId();

        if (thumbnail != null && !thumbnail.isEmpty()) {
            storeImageService.replaceThumbnail(storeId, userId, thumbnail);
        }
        if (images != null) {
            for (MultipartFile img : images) {
                if (img != null && !img.isEmpty()) {
                    storeImageService.addImage(storeId, userId, img);
                }
            }
        }

        StoreRegistrationResponse finalResponse =
                storeRegistrationService.getStore(storeId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDTO.res(HttpStatus.CREATED, "가게 등록 성공 (이미지 포함)", finalResponse));
    }

    @Operation(summary = "가게 부분 수정", description = "본인 소유 가게만 가능. null 필드는 미변경 (PATCH).")
    @SecurityRequirement(name = "bearer-jwt")
    @PatchMapping("/{storeId}")
    public ResponseEntity<ResponseDTO<StoreRegistrationResponse>> patchStore(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storeId,
            @Valid @RequestBody StoreRegistrationUpdateRequest request) {
        StoreRegistrationResponse response =
                storeRegistrationService.patchStore(storeId, userDetails.getUserId(), request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "가게 수정 성공", response));
    }

    @Operation(
            summary = "가게 삭제 (soft delete)",
            description = "본인 소유 가게만. 진행 중 공구 있으면 409 STORE_HAS_ACTIVE_GROUP_ORDER.")
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storeId) {
        storeRegistrationService.deleteStore(storeId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────── 조회 ───────────────────────────────

    @Operation(summary = "가게 목록 (검색·필터·페이징)")
    @GetMapping
    public ResponseEntity<ResponseDTO<PageResponse<ShopSummaryResponse>>> searchStores(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String adminDongCode,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<ShopStatus> status,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) Boolean likedOnly,
            @ParameterObject Pageable pageable) {
        StoreSearchCriteria criteria =
                new StoreSearchCriteria(adminDongCode, q, status, category, likedOnly);
        Long currentUserId = userDetails == null ? null : userDetails.getUserId();
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "가게 목록 조회 성공",
                        storeRegistrationService.search(criteria, pageable, currentUserId)));
    }

    @Operation(summary = "가게 상세 조회")
    @GetMapping("/{storeId}")
    public ResponseEntity<ResponseDTO<ShopDetailResponse>> getStoreDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storeId) {
        Long currentUserId = userDetails == null ? null : userDetails.getUserId();
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "가게 상세 조회 성공",
                        storeRegistrationService.getStoreDetail(storeId, currentUserId)));
    }

    @Operation(summary = "무작위 가게 목록", description = "size 미입력 시 3개. 최대 20.")
    @GetMapping("/random")
    public ResponseEntity<ResponseDTO<List<ShopSummaryResponse>>> getRandomStores(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false, defaultValue = "3") int size) {
        Long currentUserId = userDetails == null ? null : userDetails.getUserId();
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "무작위 가게 조회 성공",
                        storeRegistrationService.getRandomStores(size, currentUserId)));
    }

    @Operation(summary = "내가 등록한 가게 (ADMIN)", description = "마이페이지 점주용. 최신순.")
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/mine")
    public ResponseEntity<ResponseDTO<PageResponse<StoreRegistrationResponse>>> getMyStores(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "내 가게 목록 조회 성공",
                        storeRegistrationService.getMyStores(userDetails.getUserId(), pageable)));
    }

    // ─────────────────────────────── 좋아요 ───────────────────────────────

    @Operation(summary = "가게 좋아요 토글")
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/likes")
    public ResponseEntity<ResponseDTO<StoreLikeToggleResponse>> toggleLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long storeId) {
        StoreLikeToggleResponse response =
                storeLikeService.toggleLike(storeId, userDetails.getUserId());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "가게 좋아요 토글 성공", response));
    }

    @Operation(summary = "내가 좋아요한 가게 (커서)")
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/likes/me")
    public ResponseEntity<
                    ResponseDTO<com.example.padong_server.global.CursorPageResponse<LikedStoreResponse>>>
            getMyStoreLikes(
                    @AuthenticationPrincipal CustomUserDetails userDetails,
                    @Parameter(description = "직전 페이지 마지막 likeId", example = "42")
                            @RequestParam(required = false)
                            Long cursor,
                    @Parameter(description = "페이지 크기", example = "20")
                            @RequestParam(required = false, defaultValue = "20")
                            int size,
                    @Parameter(description = "가게명 검색", example = "베이커리")
                            @RequestParam(required = false)
                            String q) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "내 좋아요 가게 조회 성공",
                        storeLikeService.getMyLikes(userDetails.getUserId(), cursor, size, q)));
    }

    // ─────────────────────────────── 썸네일 ───────────────────────────────

    @Operation(summary = "썸네일 업로드/교체", description = "본인 소유 가게만. 기존 썸네일 있으면 S3 에서 삭제 후 교체.")
    @SecurityRequirement(name = "bearer-jwt")
    @PutMapping(value = "/{storeId}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO<String>> uploadThumbnail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storeId,
            @RequestPart("file") MultipartFile file) {
        String url = storeImageService.replaceThumbnail(storeId, userDetails.getUserId(), file);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "썸네일 업로드 성공", url));
    }

    @Operation(summary = "썸네일 제거", description = "본인 소유 가게만. thumbnailUrl=null 로 변경.")
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{storeId}/thumbnail")
    public ResponseEntity<Void> deleteThumbnail(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long storeId) {
        storeImageService.removeThumbnail(storeId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────── 갤러리 ───────────────────────────────

    @Operation(summary = "갤러리 이미지 목록", description = "sort_order 오름차순. 인증 불필요.")
    @GetMapping("/{storeId}/images")
    public ResponseEntity<ResponseDTO<List<StoreImageResponse>>> listImages(
            @PathVariable Long storeId) {
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "갤러리 조회 성공", storeImageService.list(storeId)));
    }

    @Operation(summary = "갤러리 이미지 추가", description = "본인 소유 가게만. sort_order 는 자동 (마지막).")
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping(value = "/{storeId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO<StoreImageResponse>> addImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storeId,
            @RequestPart("file") MultipartFile file) {
        StoreImageResponse response =
                storeImageService.addImage(storeId, userDetails.getUserId(), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDTO.res(HttpStatus.CREATED, "갤러리 이미지 추가 성공", response));
    }

    @Operation(summary = "갤러리 이미지 삭제", description = "본인 소유 가게만. 삭제 후 sort_order 자동 압축.")
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{storeId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storeId,
            @PathVariable Long imageId) {
        storeImageService.removeImage(storeId, imageId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "갤러리 순서 변경", description = "ids 배열의 순서대로 sort_order 재할당.")
    @SecurityRequirement(name = "bearer-jwt")
    @PatchMapping("/{storeId}/images/order")
    public ResponseEntity<ResponseDTO<List<StoreImageResponse>>> reorderImages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storeId,
            @Valid @RequestBody StoreImageReorderRequest request) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "갤러리 순서 변경 성공",
                        storeImageService.reorder(
                                storeId, userDetails.getUserId(), request.ids())));
    }
}
