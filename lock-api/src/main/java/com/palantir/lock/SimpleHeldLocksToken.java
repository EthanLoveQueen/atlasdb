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
package com.palantir.lock;

import java.io.Serializable;
import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

public class SimpleHeldLocksToken implements Serializable {
    private static final long serialVersionUID = 1L;
    private final BigInteger tokenId;
    private final long creationDateMs;

    public SimpleHeldLocksToken(@JsonProperty("tokenId") BigInteger tokenId,
                                @JsonProperty("creationDateMs") long creationDateMs) {
        this.tokenId = Preconditions.checkNotNull(tokenId);
        this.creationDateMs = creationDateMs;
    }

    public static SimpleHeldLocksToken fromHeldLocksToken(HeldLocksToken token) {
        return new SimpleHeldLocksToken(token.getTokenId(), token.getCreationDateMs());
    }

    public static SimpleHeldLocksToken fromLockRefreshToken(LockRefreshToken token) {
        return new SimpleHeldLocksToken(token.getTokenId(), 0L);
    }

    public LockRefreshToken asLockRefreshToken() {
        return new LockRefreshToken(tokenId, 0L);
    }

    public BigInteger getTokenId() {
        return tokenId;
    }

    public long getCreationDateMs() {
        return creationDateMs;
    }

    @Override
    public String toString() {
        return "SimpleHeldLocksToken [tokenId=" + tokenId + ", creationDateMs=" + creationDateMs
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tokenId == null) ? 0 : tokenId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SimpleHeldLocksToken other = (SimpleHeldLocksToken) obj;
        if (tokenId == null) {
            if (other.tokenId != null) {
                return false;
            }
        } else if (!tokenId.equals(other.tokenId)) {
            return false;
        }
        return true;
    }
}
