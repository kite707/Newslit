package com.newslit.backend.support;

import com.oracle.bmc.objectstorage.ObjectStorage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public abstract class IntegrationTestSupport {

    @MockBean
    protected ObjectStorage objectStorage;
}
