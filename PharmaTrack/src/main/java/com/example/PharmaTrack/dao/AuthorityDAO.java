package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.Authority;

import java.util.List;

public interface AuthorityDAO {
    Authority findByName(String name);
    Authority findById(Long id);
    List<Authority> findAll();
    Authority save(Authority authority);
}
