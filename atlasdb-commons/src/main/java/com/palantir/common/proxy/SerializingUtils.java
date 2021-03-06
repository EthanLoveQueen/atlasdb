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
package com.palantir.common.proxy;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.palantir.util.ByteArrayIOStream;
import com.palantir.util.ObjectInputStreamFactory;

public class SerializingUtils {
    protected static final Logger log = LoggerFactory.getLogger(SerializingUtils.class);

    private SerializingUtils() { /* */ }

    public static <T> T copy(T orig) {
        return copy(orig, new ObjectInputStreamFactory() {
            @Override
            public ObjectInputStream create(InputStream is, String codebase) throws IOException {
                return new ObjectInputStream(is);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> T copy(T orig, ObjectInputStreamFactory factory) {
        T obj = null;

        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            // Write the object out to a byte array
            ByteArrayIOStream byteStream = new ByteArrayIOStream();
            out = new ObjectOutputStream(byteStream);
            out.writeObject(orig);
            out.close();

            // Make an input stream from the byte array and read
            // a copy of the object back in.
            in = factory.create(byteStream.getInputStream(), null);
            obj = (T)in.readObject();
        }
        catch(IOException e) {
            log.error("IO exception", e);
        }
        catch(ClassNotFoundException cnfe) {
            log.error("class not found exception", cnfe);
        }
        finally {
            closeQuietly(in);
            closeQuietly(out);
        }
        return obj;
    }

    private static void closeQuietly(Closeable closeable) {
       if (closeable == null) {
           return;
       }
       try {
           closeable.close();
       } catch (IOException e) {
           // Ignore
       }
    }

}
