package com.github.chipolaris.bootforum2.dto;

import java.util.List;

public class SeedDataDTO {
    private List<SeedUserDTO> users;
    public List<SeedUserDTO> getUsers() { return users; }
    public void setUsers(List<SeedUserDTO> users) { this.users = users; }
}
