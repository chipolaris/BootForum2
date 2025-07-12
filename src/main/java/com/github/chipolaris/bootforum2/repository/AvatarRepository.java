package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public interface AvatarRepository extends JpaRepository<Avatar, Long> {

    /**
     * Finds an Avatar entity by the associated username.
     * @param userName The username to search for.
     * @return an Optional containing the Avatar if found, otherwise empty.
     */
    Optional<Avatar> findByUserName(String userName);

    /**
     * Finds a map of usernames to their corresponding avatar file IDs for a given list of usernames.
     * This method is optimized to fetch only the necessary data and ensures that every username
     * from the input list is present as a key in the returned map.
     *
     * @param userNames A list of usernames to look up.
     * @return A Map where the key is the username and the value is the avatar's file ID,
     *         or null if the user does not have an avatar.
     */
    default Map<String, Long> findAvatarFileIdsByUserNames(List<String> userNames) {
        if (userNames == null || userNames.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. Fetch the map of users who HAVE an avatar
        Map<String, Long> existingAvatars = findRawAvatarData(userNames).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // userName
                        row -> (Long) row[1]    // file.id
                ));

        // 2. Create the final map, ensuring all usernames are present.
        //    Use a standard HashMap which allows null values.
        Map<String, Long> resultMap = new HashMap<>();
        userNames.stream()
                .distinct() // Ensure we don't have issues with duplicate input usernames
                .forEach(username -> resultMap.put(username, existingAvatars.get(username)));

        return resultMap;
    }

    /**
     * Helper method to fetch raw [userName, file.id] pairs for users who have an avatar.
     * It's recommended to use the public default method findAvatarFileIdsByUserNames for a complete map.
     */
    @Query("SELECT a.userName, a.file.id FROM Avatar a WHERE a.userName IN :userNames")
    List<Object[]> findRawAvatarData(@Param("userNames") List<String> userNames);
}