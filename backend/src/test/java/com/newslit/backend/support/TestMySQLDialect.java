package com.newslit.backend.support;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;

// main의 CustomMySQLDialect는 로컬 전용(gitignore)이라 CI에서도 쓸 수 있게 테스트용으로 따로 둔다
public class TestMySQLDialect extends MySQLDialect {
    public TestMySQLDialect(DialectResolutionInfo info) {
        super(info);
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        typeContributions.getTypeConfiguration()
                .getDdlTypeRegistry()
                .addDescriptor(new DdlTypeImpl(SqlTypes.CLOB, "longtext", this));
    }
}
