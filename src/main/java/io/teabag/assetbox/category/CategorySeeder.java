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

        Category props = categoryRepository.save(new Category("소품", null, 1));
        Category furniture = categoryRepository.save(new Category("가구", props.getId(), 2));
        categoryRepository.save(new Category("의자", furniture.getId(), 3));
        categoryRepository.save(new Category("테이블", furniture.getId(), 3));

        Category character = categoryRepository.save(new Category("캐릭터", null, 1));
        Category humanoid = categoryRepository.save(new Category("휴머노이드", character.getId(), 2));
        categoryRepository.save(new Category("남성", humanoid.getId(), 3));
        categoryRepository.save(new Category("여성", humanoid.getId(), 3));

        Category environment = categoryRepository.save(new Category("환경", null, 1));
        Category indoor = categoryRepository.save(new Category("실내", environment.getId(), 2));
        categoryRepository.save(new Category("방", indoor.getId(), 3));
        categoryRepository.save(new Category("복도", indoor.getId(), 3));
    }
}