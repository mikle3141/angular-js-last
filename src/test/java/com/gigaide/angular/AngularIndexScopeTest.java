package com.gigaide.angular;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AngularIndexScopeTest extends AngularLightFixtureTestCase {

    @Test
    public void testExcludesNodeModulesPath() {
        assertTrue(AngularIndexScope.isExcludedPath("/project/node_modules/@angular/core/index.ts"));
        assertTrue(AngularIndexScope.isExcludedPath("/project/dist/app/main.js"));
        assertTrue(AngularIndexScope.isExcludedPath("/project/.angular/cache/foo"));
    }

    @Test
    public void testAllowsSourcePath() {
        assertFalse(AngularIndexScope.isExcludedPath("/project/src/app/app.ts"));
        assertFalse(AngularIndexScope.isExcludedPath("/project/src/index.html"));
    }
}
