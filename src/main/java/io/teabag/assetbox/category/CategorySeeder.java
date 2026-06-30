package io.teabag.assetbox.category;

import io.teabag.assetbox.category.domain.Category;
import io.teabag.assetbox.category.repository.CategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class CategorySeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        Category character = save("캐릭터", null, 1);
        Category humanoid = save("인간형", character.getId(), 2);
        saveLeaves(humanoid, "전사", "마법사", "NPC", "빌런");
        Category creature = save("생물 & 크리처", character.getId(), 2);
        saveLeaves(creature, "동물", "몬스터", "신화 생물", "로봇");

        Category background = save("배경 & 환경", null, 1);
        Category indoor = save("실내 공간", background.getId(), 2);
        saveLeaves(indoor, "주거 공간", "상업 공간", "던전 & 지하");
        Category outdoor = save("실외 공간", background.getId(), 2);
        saveLeaves(outdoor, "도시 & 거리", "자연 환경", "유적 & 폐허");
        Category fantasySf = save("판타지 & SF", background.getId(), 2);
        saveLeaves(fantasySf, "마법 세계", "우주 & 행성", "사이버펑크");

        Category props = save("소품 & 오브젝트", null, 1);
        Category furniture = save("가구 & 인테리어", props.getId(), 2);
        saveLeaves(furniture, "의자 & 소파", "테이블 & 책상", "조명");
        Category foodGoods = save("음식 & 잡화", props.getId(), 2);
        saveLeaves(foodGoods, "음식 & 음료", "일상 용품", "책 & 문구");
        Category electronics = save("전자기기", props.getId(), 2);
        saveLeaves(electronics, "컴퓨터 & 모바일", "가전제품", "게임기기");

        Category nature = save("자연 & 식물", null, 1);
        Category plants = save("식물", nature.getId(), 2);
        saveLeaves(plants, "나무", "꽃 & 풀", "수중 식물");
        Category terrain = save("지형 & 지물", nature.getId(), 2);
        saveLeaves(terrain, "바위 & 절벽", "물 & 강", "지형 타일");

        Category architecture = save("건축 & 구조물", null, 1);
        Category modern = save("현대 건축", architecture.getId(), 2);
        saveLeaves(modern, "주거 건물", "상업 빌딩", "다리 & 도로");
        Category historicalFantasy = save("역사 & 판타지", architecture.getId(), 2);
        saveLeaves(historicalFantasy, "성 & 요새", "신전 & 유적", "탑 & 탑문");
        Category futureSf = save("SF & 미래", architecture.getId(), 2);
        saveLeaves(futureSf, "우주선 내부", "기지 & 시설", "에너지 구조물");

        Category vehicle = save("차량 & 탈것", null, 1);
        Category groundVehicle = save("지상 차량", vehicle.getId(), 2);
        saveLeaves(groundVehicle, "자동차", "오토바이", "전차 & 군용");
        Category aircraftSpace = save("항공 & 우주", vehicle.getId(), 2);
        saveLeaves(aircraftSpace, "비행기", "헬리콥터", "우주선");
        Category maritime = save("해상", vehicle.getId(), 2);
        saveLeaves(maritime, "선박", "잠수함");

        Category weapon = save("무기 & 장비", null, 1);
        Category melee = save("근접 무기", weapon.getId(), 2);
        saveLeaves(melee, "검 & 도끼", "창 & 지팡이", "둔기");
        Category ranged = save("원거리 무기", weapon.getId(), 2);
        saveLeaves(ranged, "활 & 석궁", "총기류", "마법 도구");
        Category armor = save("방어구 & 액세서리", weapon.getId(), 2);
        saveLeaves(armor, "갑옷 & 방패", "헬멧", "장신구");

        Category material = save("머티리얼 & 텍스처", null, 1);
        Category pbr = save("PBR 머티리얼", material.getId(), 2);
        saveLeaves(pbr, "금속", "나무 & 석재", "직물 & 가죽");
        Category stylized = save("스타일라이즈드", material.getId(), 2);
        saveLeaves(stylized, "툰 셰이딩", "핸드페인티드", "픽셀 아트");
        Category hdr = save("환경맵 & HDR", material.getId(), 2);
        saveLeaves(hdr, "실내 HDR", "야외 HDR", "스튜디오 HDR");
    }

    private Category save(String name, Long parentId, int depth) {
        return categoryRepository.save(new Category(name, parentId, depth));
    }

    private void saveLeaves(Category parent, String... names) {
        for (String name : names) {
            save(name, parent.getId(), 3);
        }
    }
}
