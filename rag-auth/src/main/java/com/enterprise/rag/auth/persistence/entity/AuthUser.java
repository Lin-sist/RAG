package com.enterprise.rag.auth.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.rag.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`user`")
public class AuthUser extends BaseEntity {

    private String username;
    private String passwordHash;
    private String email;
    private Boolean enabled;
}
