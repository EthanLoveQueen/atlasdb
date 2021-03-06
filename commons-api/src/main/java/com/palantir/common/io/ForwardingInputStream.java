/*
 * Copyright 2015 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.common.io;

import java.io.IOException;
import java.io.InputStream;

public abstract class ForwardingInputStream extends InputStream {

    @Override
    public int available() throws IOException {
        return delegate().available();
    }

    @Override
    public void close() throws IOException {
        delegate().close();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public void mark(int readlimit) {
        delegate().mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return delegate().markSupported();
    }

    @Override
    public int read() throws IOException {
        return delegate().read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate().read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate().read(b);
    }

    @Override
    public void reset() throws IOException {
        delegate().reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate().skip(n);
    }

    /**
     * Returns the backing delegate instance that methods are forwarded to.
     * Concrete subclasses override this method to supply the instance being
     * decorated.
     */
    protected abstract InputStream delegate();

    /**
     * Returns the string representation generated by the delegate's {@code
     * toString} method.
     */
    @Override
    public String toString() {
        return delegate().toString();
    }

    /* No equals or hashCode. See ForwardingObject comments for details. */
}
