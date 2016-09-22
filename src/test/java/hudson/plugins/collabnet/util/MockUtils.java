/*
 * Copyright 2013 CollabNet, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hudson.plugins.collabnet.util;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import org.easymock.EasyMock;
import org.easymock.IMockBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for mocks in tests.
 */
public class MockUtils {

    /** The mocks to keep track of. */
    private List<Object> mocks = new ArrayList<Object>();

    /**
     * Creates a new mock.
     *
     * @param name the friendly name of the mock to create. Used in error messages.
     * @param classType The type of mock to create
     * @param <T> The type of mock to create
     * @return the mock
     */
    public <T> T createMock(String name, Class<T> classType) {
        T mock = EasyMock.createMock(name, classType);
        mocks.add(mock);
        return mock;
    }

    /**
     * Creates a new mock.
     *
     * @param name the friendly name of the mock to create. Used in error messages.
     * @param mockBuilder The builder for the type of mock to create
     * @param <T> The type of mock to create
     * @return the mock
     */
    public <T> T createMock(String name, IMockBuilder<T> mockBuilder) {
        T mock = mockBuilder.createMock(name);
        mocks.add(mock);
        return mock;
    }

    /**
     * Creates a new 'nice' mock.
     *
     * @param name the friendly name of the mock to create. Used in error messages.
     * @param classType The type of mock to create
     * @param <T> The type of mock to create
     * @return the mock
     */
    public <T> T createNiceMock(String name, Class<T> classType) {
        T mock = EasyMock.createNiceMock(name, classType);
        mocks.add(mock);
        return mock;
    }

    /**
     * Sets all mocks to replay mode.
     */
    public void replayAll() {
        EasyMock.replay(mocks.toArray());
    }

    /**
     * Verifies all mocks.
     */
    public void verifyAll() {
        EasyMock.verify(mocks.toArray());
    }

}
