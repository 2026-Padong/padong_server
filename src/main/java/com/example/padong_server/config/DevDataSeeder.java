package com.example.padong_server.config;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.menu.entity.Menu;
import com.example.padong_server.domain.menu.repository.MenuRepository;
import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.oauth.repository.UserRepository;
import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowMenu;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import com.example.padong_server.domain.orderFlow.repository.OrderFlowMenuRepository;
import com.example.padong_server.domain.orderFlow.repository.OrderFlowRepository;
import com.example.padong_server.domain.payment.entity.GroupOrder;
import com.example.padong_server.domain.payment.entity.GroupOrderMenu;
import com.example.padong_server.domain.payment.entity.GroupOrderStatus;
import com.example.padong_server.domain.payment.entity.Order;
import com.example.padong_server.domain.payment.entity.OrderMenu;
import com.example.padong_server.domain.payment.entity.OrderStatus;
import com.example.padong_server.domain.payment.entity.Payment;
import com.example.padong_server.domain.payment.entity.PaymentStatus;
import com.example.padong_server.domain.payment.repository.GroupOrderMenuRepository;
import com.example.padong_server.domain.payment.repository.GroupOrderRepository;
import com.example.padong_server.domain.payment.repository.OrderMenuRepository;
import com.example.padong_server.domain.payment.repository.OrderRepository;
import com.example.padong_server.domain.payment.repository.PaymentRepository;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.entity.StoreCategory;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * dev / prod startup 시 mock 데이터 시드.
 * - 가게 6개 (카테고리 다양, weekdayMask 다양, sold-out 일부)
 * - 각 가게별 메뉴 3개 + GroupOrder + OrderFlow(모임) 자동 생성
 * - 모임은 다양한 OrderFlowStatus 분포로 (PENDING / WAITING_APPROVAL / COMPLETED 등)
 * - 시드 가게 이미 있으면 개별 skip — 재실행 안전
 */
@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private static final int WEEK_ALL = 0b1111111; // 월~일
    private static final int WEEK_WEEKDAY = 0b0011111; // 월~금
    private static final int WEEK_WEEKEND = 0b1100000; // 토,일

    private final UserRepository userRepository;
    private final AdminDongRepository adminDongRepository;
    private final StoreRegistrationRepository storeRepository;
    private final MenuRepository menuRepository;
    private final GroupOrderRepository groupOrderRepository;
    private final GroupOrderMenuRepository groupOrderMenuRepository;
    private final OrderFlowRepository orderFlowRepository;
    private final OrderFlowMenuRepository orderFlowMenuRepository;
    private final OrderRepository orderRepository;
    private final OrderMenuRepository orderMenuRepository;
    private final PaymentRepository paymentRepository;
    private final JdbcTemplate jdbcTemplate;

    /** seed 도중 모임 참여자로 쓸 사용자 풀. run() 진입 시 채워짐. */
    private List<User> participantPool = List.of();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        runMigrations();

        // 사장 (ADMIN) — OAuth 로그인 user 우선, 없으면 mock 생성
        User owner = userRepository.findAll().stream().findFirst().orElse(null);
        if (owner == null) {
            owner = seedMockOwner();
            log.info("[DevDataSeeder] mock owner 생성 — id={}", owner.getId());
        }
        // 일반 사용자 10명 mock (참여자 시드용). 기존에 있으면 skip
        List<User> mockUsers = seedMockUsers(10);
        AdminDong fallbackDong = adminDongRepository.findAll().stream().findFirst().orElse(null);
        if (fallbackDong == null) {
            log.warn("[DevDataSeeder] no admin_dong — skip seeding (행정동 마스터 시드 필요)");
            return;
        }

        Set<String> existingNames = storeRepository.findAll().stream()
                .map(Store::getName)
                .collect(java.util.stream.Collectors.toSet());

        log.info(
                "[DevDataSeeder] seeding (existing={}, mockUsers={}): owner={}",
                existingNames.size(),
                mockUsers.size(),
                owner.getId());
        int inserted = 0;
        this.participantPool = mockUsers;

        // 1. 연희제빵소 — 베이커리, 평일+주말, 모집 중 (정원 5중 1)
        inserted += seedStore(new SeedSpec(
                existingNames, owner,
                findDong("서대문구", "연희동", fallbackDong),
                "연희제빵소", StoreCategory.BAKERY, "매일 아침 굽는 동네 빵집",
                "서울 서대문구 연희로 11길 24 1층",
                "02-3142-8200", 37.5685, 126.9275, WEEK_ALL,
                LocalTime.of(8, 0), LocalTime.of(22, 0),
                List.of(
                        menu("크루아상", 4500, false),
                        menu("통밀 식빵", 5500, false),
                        menu("플랫화이트", 3800, false)),
                OrderFlowStatus.PENDING, plusHours(24), 5, 1));

        // 2. 홍대 로스터리 — 카페, 매일, 모집 중·정원 1자리 남음(closingSoon)
        //    핸드드립 sold-out 표시
        inserted += seedStore(new SeedSpec(
                existingNames, owner,
                findDong("마포구", "서교동", fallbackDong),
                "홍대 로스터리", StoreCategory.CAFE_DESSERT, "직접 로스팅한 원두로 내린 핸드드립",
                "서울 마포구 와우산로 78 2층",
                "02-322-1234", 37.5563, 126.9237, WEEK_ALL,
                LocalTime.of(11, 0), LocalTime.of(23, 0),
                List.of(
                        menu("핸드드립 (에티오피아)", 5500, true),
                        menu("아이스 라떼", 4800, false),
                        menu("티라미수", 6000, false)),
                OrderFlowStatus.PENDING, plusHours(6), 8, 7));

        // 3. 신림 김밥집 — 분식, 평일만, 모집 중
        inserted += seedStore(new SeedSpec(
                existingNames, owner,
                findDong("관악구", "신림동", fallbackDong),
                "신림 김밥집", StoreCategory.SNACK, "고시생이 사랑한 30년 노포 김밥",
                "서울 관악구 신림로 280",
                "02-873-4567", 37.4842, 126.9292, WEEK_WEEKDAY,
                LocalTime.of(7, 0), LocalTime.of(21, 0),
                List.of(
                        menu("참치 김밥", 3800, false),
                        menu("라볶이", 5500, false),
                        menu("계란말이 김밥", 4200, false)),
                OrderFlowStatus.PENDING, plusHours(12), 10, 3));

        // 4. 종로 골목식당 — 한식, 매일, 모집 중
        inserted += seedStore(new SeedSpec(
                existingNames, owner,
                findDong("종로구", "통인동", fallbackDong),
                "종로 골목식당", StoreCategory.KOREAN, "삼청동 한옥집 점심 정식",
                "서울 종로구 자하문로 7길 8",
                "02-735-9911", 37.5780, 126.9695, WEEK_ALL,
                LocalTime.of(11, 30), LocalTime.of(21, 0),
                List.of(
                        menu("한정식 코스", 22000, false),
                        menu("된장찌개 정식", 10500, false),
                        menu("불고기 정식", 13500, false)),
                OrderFlowStatus.PENDING, plusHours(30), 6, 0));

        // 5. 강남 스시오마카세 — 일식, 매일, 정원 가득 → 사장 승인 대기
        //    딸기 바스크 sold-out
        inserted += seedStore(new SeedSpec(
                existingNames, owner,
                findDong("강남구", "역삼동", fallbackDong),
                "강남 스시오마카세", StoreCategory.JAPANESE, "런치 오마카세 13피스 + 우니 1피스",
                "서울 강남구 테헤란로 12길 34 B1",
                "02-555-1212", 37.4979, 127.0276, WEEK_ALL,
                LocalTime.of(12, 0), LocalTime.of(22, 0),
                List.of(
                        menu("런치 오마카세 13P", 75000, false),
                        menu("우니 단품", 22000, false),
                        menu("따끈한 사케 1잔", 7000, false)),
                OrderFlowStatus.WAITING_APPROVAL, plusHours(3), 4, 4));

        // 6. 이태원 치즈케이크 — 디저트, 주말 매장, 완료된 모임 (히스토리용)
        inserted += seedStore(new SeedSpec(
                existingNames, owner,
                findDong("용산구", "이태원동", fallbackDong),
                "이태원 치즈케이크", StoreCategory.CAFE_DESSERT, "수제 바스크 치즈케이크 전문",
                "서울 용산구 이태원로 200",
                "02-794-7777", 37.5345, 126.9947, WEEK_WEEKEND,
                LocalTime.of(13, 0), LocalTime.of(22, 0),
                List.of(
                        menu("바스크 치즈케이크 1조각", 6500, false),
                        menu("바스크 치즈케이크 홀(미니)", 30000, false),
                        menu("아이스 아메리카노", 3800, false)),
                OrderFlowStatus.COMPLETED, plusHours(-12), 5, 5));

        log.info("[DevDataSeeder] done: inserted={} new stores (+ menus + group orders + order flows)", inserted);

        backfillParticipantsForExistingStores();
    }

    /**
     * 마이그레이션 후 menus.price = 0 으로 박힌 옛 시드 메뉴들 가격 보강.
     * 시드 spec 의 메뉴 이름 → 가격 매핑. 매칭 없으면 그대로.
     */
    private void backfillMenuPrices() {
        int updated = jdbcTemplate.update(
                """
                UPDATE menus SET price = CASE menu_info
                    WHEN '크루아상' THEN 4500
                    WHEN '통밀 식빵' THEN 5500
                    WHEN '식빵' THEN 5500
                    WHEN '플랫화이트' THEN 3800
                    WHEN '아메리카노' THEN 3800
                    WHEN '핸드드립 (에티오피아)' THEN 5500
                    WHEN '아이스 라떼' THEN 4800
                    WHEN '티라미수' THEN 6000
                    WHEN '참치 김밥' THEN 3800
                    WHEN '라볶이' THEN 5500
                    WHEN '계란말이 김밥' THEN 4200
                    WHEN '한정식 코스' THEN 22000
                    WHEN '된장찌개 정식' THEN 10500
                    WHEN '불고기 정식' THEN 13500
                    WHEN '런치 오마카세 13P' THEN 75000
                    WHEN '우니 단품' THEN 22000
                    WHEN '따끈한 사케 1잔' THEN 7000
                    WHEN '바스크 치즈케이크 1조각' THEN 6500
                    WHEN '바스크 치즈케이크 홀(미니)' THEN 30000
                    WHEN '아이스 아메리카노' THEN 3800
                    ELSE price
                END
                WHERE price = 0 OR price IS NULL
                """);
        if (updated > 0) log.info("[DevDataSeeder] menus.price 보강 {}건", updated);
        backfillOrderPrices();
    }

    /**
     * orders.total_price = 0 / order_menu.price = 0 인 옛 row 들을 menus.price 기준으로 보강.
     * menus.price backfill 직후 호출 — order_menu.price 먼저, 그 합으로 orders.total_price.
     */
    private void backfillOrderPrices() {
        int omUpdated = jdbcTemplate.update(
                """
                UPDATE order_menu om
                JOIN menus m ON m.id = om.menu_id
                SET om.price = m.price * om.quantity
                WHERE (om.price IS NULL OR om.price = 0) AND m.price > 0
                """);
        int oUpdated = jdbcTemplate.update(
                """
                UPDATE orders o
                JOIN (
                    SELECT order_id, SUM(price) AS total
                    FROM order_menu
                    GROUP BY order_id
                ) sums ON sums.order_id = o.id
                SET o.total_price = sums.total
                WHERE (o.total_price IS NULL OR o.total_price = 0) AND sums.total > 0
                """);
        if (omUpdated > 0 || oUpdated > 0) {
            log.info(
                    "[DevDataSeeder] 가격 보강 — order_menu.price {}건, orders.total_price {}건",
                    omUpdated,
                    oUpdated);
        }
    }

    /**
     * 옛 시드 가게 — seedStore 는 이름 일치로 skip 했지만 OrderFlow / OrderFlowMenu 행이 없음.
     * 각 가게의 메뉴를 묶어 PENDING OrderFlow 1개씩 생성. 이미 있으면 skip.
     */
    private void backfillOrderFlows() {
        int created = 0;
        for (Store store : storeRepository.findAll()) {
            // 이미 OrderFlow 가 있으면 skip
            var existing = orderFlowRepository
                    .findTopByStoreIdAndStatusInOrderByIdDesc(
                            store.getId(),
                            java.util.List.of(
                                    OrderFlowStatus.PENDING,
                                    OrderFlowStatus.WAITING_APPROVAL,
                                    OrderFlowStatus.APPROVED,
                                    OrderFlowStatus.READY,
                                    OrderFlowStatus.COMPLETED,
                                    OrderFlowStatus.REJECTED));
            if (existing.isPresent()) continue;

            GroupOrder go = groupOrderRepository
                    .findTopByStoreIdOrderByIdDesc(store.getId())
                    .orElse(null);
            if (go == null) continue;
            List<Menu> menus = menuRepository.findAllByStoreId(store.getId());
            if (menus.isEmpty()) continue;

            OrderFlowStatus status =
                    go.getStatus() == GroupOrderStatus.OPEN
                            ? (go.getCurrentParticipants() >= go.getMaxParticipants()
                                    ? OrderFlowStatus.WAITING_APPROVAL
                                    : OrderFlowStatus.PENDING)
                            : OrderFlowStatus.COMPLETED;

            OrderFlow flow = orderFlowRepository.save(OrderFlow.builder()
                    .store(store)
                    .menu(menus.get(0))
                    .status(status)
                    .recruitmentStart(LocalDateTime.now().minusHours(2))
                    .recruitmentDeadline(go.getRecruitmentDeadline())
                    .minOrderPerPerson(1)
                    .paymentMethod("CARD")
                    .maxParticipants(go.getMaxParticipants())
                    .currentParticipants(go.getCurrentParticipants())
                    .build());
            for (int i = 0; i < menus.size(); i++) {
                Menu m = menus.get(i);
                orderFlowMenuRepository.save(OrderFlowMenu.builder()
                        .orderFlow(flow)
                        .menu(m)
                        .sortOrder(i)
                        .menuInfoSnapshot(m.getName())
                        .priceSnapshot(m.getPrice() == null ? 0 : m.getPrice())
                        .build());
            }
            created++;
        }
        if (created > 0) log.info("[DevDataSeeder] OrderFlow backfill — {}개 가게", created);
    }

    /**
     * picsum.photos 의 seed 기반 placeholder. 가게 이름 슬러그 → 일관된 이미지.
     * 600x400 비율은 카드/상세 양쪽에 자연스럽게 들어감.
     */
    private String placeholderThumbnail(String storeName) {
        String slug = storeName == null
                ? "store"
                : storeName.replaceAll("\\s+", "-")
                        .replaceAll("[^0-9A-Za-z\\u3131-\\u318E\\uAC00-\\uD7A3-]", "");
        return "https://picsum.photos/seed/" + slug + "/600/400";
    }

    /**
     * 기존 시드 가게의 active GroupOrder 에 PAID Order 가 부족하면 mock user 로 채워 넣음.
     * (재기동 시 가게는 skip 됐지만 참여자 데이터가 비어있을 때 보강.) thumbnailUrl null 인 가게도 보강.
     */
    private void backfillParticipantsForExistingStores() {
        // 옛 시드 가게의 thumbnailUrl 이 null 이면 picsum 으로 채움
        jdbcTemplate.update(
                """
                UPDATE store_registrations
                SET thumbnail_url = CONCAT('https://picsum.photos/seed/store-', id, '/600/400')
                WHERE thumbnail_url IS NULL OR thumbnail_url = ''
                """);

        backfillMenuPrices();
        backfillOrderFlows();
        if (participantPool.isEmpty()) return;
        int totalAdded = 0;
        for (Store store : storeRepository.findAll()) {
            GroupOrder go = groupOrderRepository
                    .findTopByStoreIdOrderByIdDesc(store.getId())
                    .orElse(null);
            if (go == null) continue;
            int existing = orderRepository.findPaidByGroupOrderId(go.getId()).size();
            int target = Math.max(go.getCurrentParticipants(), 0);
            if (existing >= target) continue;

            List<Menu> menus = menuRepository.findAllByStoreId(store.getId());
            if (menus.isEmpty()) continue;
            int missing = target - existing;
            seedParticipants(store, go, menus, missing);
            totalAdded += missing;
        }
        if (totalAdded > 0) {
            log.info("[DevDataSeeder] 참여자 backfill — {}건 추가", totalAdded);
        }
    }

    private int seedStore(SeedSpec s) {
        if (s.existingNames.contains(s.name)) {
            log.info("[DevDataSeeder] '{}' already exists, skip", s.name);
            return 0;
        }

        Store store = storeRepository.save(Store.builder()
                .adminDong(s.dong)
                .name(s.name)
                .address(s.address)
                .phoneNumber(s.phoneNumber)
                .openTime(s.openTime)
                .closeTime(s.closeTime)
                .category(s.category)
                .description(s.description)
                .latitude(s.latitude)
                .longitude(s.longitude)
                .weekdayMask(s.weekdayMask)
                .thumbnailUrl(placeholderThumbnail(s.name))
                .owner(s.owner)
                .build());

        // GroupOrder — 결제·공구 흐름 호환용
        GroupOrder groupOrder = groupOrderRepository.save(GroupOrder.builder()
                .minOrderAmount(0)
                .currentAmount(0)
                .currentParticipants(s.currentParticipants)
                .maxParticipants(s.maxParticipants)
                .recruitmentDeadline(s.deadline)
                .status(toGroupOrderStatus(s.orderFlowStatus))
                .store(store)
                .build());

        // Menu + GroupOrderMenu 묶음
        List<Menu> menus = new java.util.ArrayList<>();
        for (MenuSpec spec : s.menus) {
            Menu menu = menuRepository.save(Menu.builder()
                    .store(store)
                    .name(spec.name())
                    .price(spec.price())
                    .soldOut(spec.soldOut())
                    .build());
            menus.add(menu);
            groupOrderMenuRepository.save(GroupOrderMenu.builder()
                    .soldOut(spec.soldOut()).groupOrder(groupOrder).menu(menu).build());
        }

        // OrderFlow — 모임 도메인. 메뉴 묶음 스냅샷 보존.
        OrderFlow flow = orderFlowRepository.save(OrderFlow.builder()
                .store(store)
                .menu(menus.get(0))
                .status(s.orderFlowStatus)
                .recruitmentStart(LocalDateTime.now().minusHours(2))
                .recruitmentDeadline(s.deadline)
                .minOrderPerPerson(1)
                .paymentMethod("CARD")
                .maxParticipants(s.maxParticipants)
                .currentParticipants(s.currentParticipants)
                .build());
        for (int i = 0; i < menus.size(); i++) {
            Menu m = menus.get(i);
            orderFlowMenuRepository.save(OrderFlowMenu.builder()
                    .orderFlow(flow)
                    .menu(m)
                    .sortOrder(i)
                    .menuInfoSnapshot(m.getName())
                    .priceSnapshot(m.getPrice())
                    .build());
        }

        seedParticipants(store, groupOrder, menus, s.currentParticipants);
        return 1;
    }

    /** currentParticipants 명만큼 mock User 가 첫 메뉴 1개 주문한 것으로 시드. */
    private void seedParticipants(
            Store store, GroupOrder groupOrder, List<Menu> menus, int count) {
        if (count <= 0 || participantPool.isEmpty() || menus.isEmpty()) return;
        Menu defaultMenu = menus.get(0);
        int n = Math.min(count, participantPool.size());
        for (int i = 0; i < n; i++) {
            User u = participantPool.get(i);
            // 사장 본인이 본인 가게에 참여하는 케이스는 피함
            if (u.getId().equals(store.getOwner().getId())) continue;

            Order order = orderRepository.save(Order.builder()
                    .user(u)
                    .groupOrder(groupOrder)
                    .store(store)
                    .totalPrice(defaultMenu.getPrice())
                    .orderStatus(OrderStatus.PAID)
                    .paymentStatus(PaymentStatus.PAID)
                    .build());
            order.assignOrderNumber(java.time.LocalDate.now());

            orderMenuRepository.save(OrderMenu.builder()
                    .order(order)
                    .menu(defaultMenu)
                    .quantity(1)
                    .price(defaultMenu.getPrice())
                    .build());

            Payment payment = Payment.builder()
                    .order(order)
                    .paymentId(java.util.UUID.randomUUID().toString())
                    .totalAmount((long) defaultMenu.getPrice())
                    .isTest(true)
                    .status(PaymentStatus.PAID)
                    .paymentMethod("card")
                    .paidAt(LocalDateTime.now().minusMinutes(30L * (i + 1)))
                    .build();
            paymentRepository.save(payment);
        }
    }

    private GroupOrderStatus toGroupOrderStatus(OrderFlowStatus flow) {
        return switch (flow) {
            case PENDING, WAITING_APPROVAL, APPROVED, READY -> GroupOrderStatus.OPEN;
            case COMPLETED, REJECTED -> GroupOrderStatus.CLOSED;
        };
    }

    private AdminDong findDong(String districtName, String adminDongName, AdminDong fallback) {
        return adminDongRepository.findByCityNameAndDistrictNameAndAdminDongName(
                        "서울특별시", districtName, adminDongName)
                .orElse(fallback);
    }

    private LocalDateTime plusHours(int hours) {
        return LocalDateTime.now().plusHours(hours);
    }

    private MenuSpec menu(String name, int price, boolean soldOut) {
        return new MenuSpec(name, price, soldOut);
    }

    private record MenuSpec(String name, int price, boolean soldOut) {}

    private record SeedSpec(
            Set<String> existingNames,
            User owner,
            AdminDong dong,
            String name,
            StoreCategory category,
            String description,
            String address,
            String phoneNumber,
            double latitude,
            double longitude,
            int weekdayMask,
            LocalTime openTime,
            LocalTime closeTime,
            List<MenuSpec> menus,
            OrderFlowStatus orderFlowStatus,
            LocalDateTime deadline,
            int maxParticipants,
            int currentParticipants) {}

    // ─────────────────────────────── 사용자 시드 ───────────────────────────────

    /** OAuth 로그인이 한 번도 안 된 경우 사장 1명을 mock 으로 만들어 줌. */
    private User seedMockOwner() {
        User u = User.createKakaoMember(
                -1L, "padong-owner@example.local", "파동사장", null);
        u.completeSignUp(Role.ADMIN, null, null);
        u.approve();
        return userRepository.save(u);
    }

    /** 모임 참여자 시드용 mock USER 10명 (kakaoId -1001 ~ -1010). 이미 있으면 그대로 재사용. */
    private List<User> seedMockUsers(int count) {
        List<User> result = new java.util.ArrayList<>();
        String[] nicknames = {
            "지민", "수아", "민준", "서연", "도윤", "지유", "예준", "하은", "주원", "채원"
        };
        for (int i = 0; i < count; i++) {
            long kakaoId = -1001L - i;
            String nickname = nicknames[i % nicknames.length] + (i / nicknames.length == 0 ? "" : String.valueOf(i / nicknames.length));
            User existing = userRepository.findAll().stream()
                    .filter(u -> u.getKakaoId() != null && u.getKakaoId().equals(kakaoId))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                result.add(existing);
                continue;
            }
            User u = User.createKakaoMember(
                    kakaoId, "padong-user-" + (i + 1) + "@example.local", nickname, null);
            u.completeSignUp(Role.USER, null, null);
            result.add(userRepository.save(u));
        }
        return result;
    }

    // ─────────────────────────────── 마이그레이션 ───────────────────────────────

    private void runMigrations() {
        migrateRoadDetailAddressToSingle();
        migrateCategoryEnum();
        migrateDefaultWeekdayMask();
        migrateMenuColumns();
        migrateOrderFlowStatus();
    }

    /** order_flows.status legacy 9개 값 → 6개 enum 정리. */
    private void migrateOrderFlowStatus() {
        if (!tableExists("order_flows")) return;
        int updated = jdbcTemplate.update(
                """
                UPDATE order_flows SET status = CASE status
                    WHEN 'RECRUITING'        THEN 'PENDING'
                    WHEN 'CLOSING'           THEN 'PENDING'
                    WHEN 'PENDING_FULL'      THEN 'WAITING_APPROVAL'
                    WHEN 'PREPARING'         THEN 'APPROVED'
                    WHEN 'READY_FOR_PICKUP'  THEN 'READY'
                    WHEN 'PICKUP_COMPLETED'  THEN 'COMPLETED'
                    WHEN 'CANCELED'          THEN 'REJECTED'
                    ELSE status
                END
                WHERE status IN (
                    'RECRUITING','CLOSING','PENDING_FULL',
                    'PREPARING','READY_FOR_PICKUP','PICKUP_COMPLETED','CANCELED'
                )
                """);
        if (updated > 0) log.info("[DevDataSeeder] order_flows.status legacy → 6-enum {}건", updated);
    }

    private void migrateMenuColumns() {
        if (!columnExists("menus", "discount_price") && !columnExists("menus", "original_price")) {
            if (!columnExists("menus", "sold_out")) {
                jdbcTemplate.execute(
                        "ALTER TABLE menus ADD COLUMN sold_out TINYINT(1) NOT NULL DEFAULT 0");
            }
            return;
        }
        if (!columnExists("menus", "price")) {
            jdbcTemplate.execute("ALTER TABLE menus ADD COLUMN price INT");
        }
        if (columnExists("menus", "discount_price")) {
            jdbcTemplate.update("UPDATE menus SET price = discount_price WHERE price IS NULL");
            jdbcTemplate.execute("ALTER TABLE menus DROP COLUMN discount_price");
        }
        if (columnExists("menus", "original_price")) {
            jdbcTemplate.update("UPDATE menus SET price = original_price WHERE price IS NULL");
            jdbcTemplate.execute("ALTER TABLE menus DROP COLUMN original_price");
        }
        for (String col : new String[] {"pickup_available_time", "recruitment_deadline", "payment_method"}) {
            if (columnExists("menus", col)) {
                jdbcTemplate.execute("ALTER TABLE menus DROP COLUMN " + col);
            }
        }
        if (!columnExists("menus", "sold_out")) {
            jdbcTemplate.execute(
                    "ALTER TABLE menus ADD COLUMN sold_out TINYINT(1) NOT NULL DEFAULT 0");
        }
        log.info("[DevDataSeeder] menus 컬럼 단순화 완료");
    }

    /** 기존 가게의 weekday_mask 가 0 이면 127 보정. */
    private void migrateDefaultWeekdayMask() {
        int updated = jdbcTemplate.update(
                "UPDATE store_registrations SET weekday_mask = 127 WHERE weekday_mask = 0");
        if (updated > 0) log.info("[DevDataSeeder] weekday_mask 0 → 127 보정 {}건", updated);
    }

    /** road_address + detail_address → address 단일. */
    private void migrateRoadDetailAddressToSingle() {
        if (!columnExists("store_registrations", "road_address")) return;
        if (!columnExists("store_registrations", "address")) {
            jdbcTemplate.execute("ALTER TABLE store_registrations ADD COLUMN address VARCHAR(500)");
        }
        jdbcTemplate.update(
                """
                UPDATE store_registrations
                SET address = TRIM(CONCAT(
                    COALESCE(road_address, ''),
                    CASE WHEN detail_address IS NOT NULL AND detail_address <> ''
                         THEN CONCAT(' ', detail_address) ELSE '' END))
                WHERE address IS NULL OR address = ''
                """);
        jdbcTemplate.execute("ALTER TABLE store_registrations DROP COLUMN road_address");
        if (columnExists("store_registrations", "detail_address")) {
            jdbcTemplate.execute("ALTER TABLE store_registrations DROP COLUMN detail_address");
        }
        log.info("[DevDataSeeder] road_address + detail_address → address 마이그레이션 완료");
    }

    private void migrateCategoryEnum() {
        int updated = jdbcTemplate.update(
                """
                UPDATE store_registrations SET category = CASE category
                    WHEN '베이커리' THEN 'BAKERY'
                    WHEN '카페'     THEN 'CAFE_DESSERT'
                    WHEN '디저트'   THEN 'CAFE_DESSERT'
                    WHEN '분식'     THEN 'SNACK'
                    WHEN '한식'     THEN 'KOREAN'
                    WHEN '일식'     THEN 'JAPANESE'
                    WHEN '중식'     THEN 'CHINESE'
                    WHEN '양식'     THEN 'WESTERN'
                    WHEN '아시안'   THEN 'ASIAN'
                    WHEN '치킨'     THEN 'CHICKEN'
                    WHEN '피자'     THEN 'PIZZA'
                    WHEN '버거'     THEN 'BURGER'
                    WHEN '샐러드'   THEN 'SALAD'
                    WHEN '주점'     THEN 'BAR'
                    WHEN '바'       THEN 'BAR'
                    WHEN '식료품'   THEN 'GROCERY'
                    ELSE category
                END
                WHERE category IS NOT NULL
                """);
        if (updated > 0) log.info("[DevDataSeeder] legacy category {}건 정정", updated);
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """,
                Integer.class, table, column);
        return count != null && count > 0;
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """,
                Integer.class, table);
        return count != null && count > 0;
    }
}
