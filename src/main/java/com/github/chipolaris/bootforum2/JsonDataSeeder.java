package com.github.chipolaris.bootforum2;

import com.github.chipolaris.bootforum2.dao.dynamic.DynamicDAO;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "prod"})
public class JsonDataSeeder implements CommandLineRunner {

    @Resource
    DynamicDAO dynamicDAO;

    @Override
    public void run(String... args) throws Exception {

        /*DynamicFilterBuilder filterBuilder = new DynamicFilterBuilder().eq("username", "admin");

        if(!dynamicDAO.exists(User.class, filterBuilder.build())) {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = getClass().getResourceAsStream("/seed-data.json");
            if (inputStream == null) {
                throw new FileNotFoundException("seed-data.json not found in resources folder");
            }

            SeedDataDTO seedData = mapper.readValue(inputStream, SeedDataDTO.class);

            for (SeedUserDTO seedUserDTO : seedData.getUsers()) {

            }
        }*/

    }
}

