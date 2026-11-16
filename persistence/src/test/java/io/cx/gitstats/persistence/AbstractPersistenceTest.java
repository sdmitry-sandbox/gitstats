package io.cx.gitstats.persistence;

import io.cx.gitstats.persistence.utils.PersistenceTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

@PersistenceLayerTest
public abstract class AbstractPersistenceTest {

    @Autowired
    protected PersistenceTestHelper testHelper;

    @AfterEach
    void cleanUp() {
        testHelper.cleanUp();
    }
}
