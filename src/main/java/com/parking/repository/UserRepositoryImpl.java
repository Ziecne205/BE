package com.parking.repository;

import com.parking.entity.Role;
import com.parking.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final MongoOperations mongoOperations;

    @Override
    public List<User> findByRole_RoleNameOrderByUserIdAsc(String roleName) {
        // 1) tim cac Role co ten khop (khong phan biet hoa/thuong)
        Query roleQuery = new Query(Criteria.where("roleName")
                .regex(Pattern.compile("^" + Pattern.quote(roleName) + "$", Pattern.CASE_INSENSITIVE)));
        List<Role> roles = mongoOperations.find(roleQuery, Role.class);
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Integer> roleIds = roles.stream().map(Role::getRoleId).toList();

        // 2) tim user co role.$id thuoc danh sach tren
        Query userQuery = new Query(Criteria.where("role.$id").in(roleIds))
                .with(Sort.by(Sort.Direction.ASC, "userId"));
        return mongoOperations.find(userQuery, User.class);
    }
}
