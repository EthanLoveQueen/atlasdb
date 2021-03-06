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
package com.palantir.example.profile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.palantir.atlasdb.ptobject.EncodingUtils;
import com.palantir.atlasdb.transaction.api.Transaction;
import com.palantir.atlasdb.transaction.api.TransactionManager;
import com.palantir.common.collect.IterableView;
import com.palantir.example.profile.protos.generated.ProfilePersistence.UserProfile;
import com.palantir.example.profile.schema.generated.ProfileTableFactory;
import com.palantir.example.profile.schema.generated.UserPhotosStreamStore;
import com.palantir.example.profile.schema.generated.UserProfileTable;
import com.palantir.example.profile.schema.generated.UserProfileTable.UserBirthdaysIdxTable;
import com.palantir.example.profile.schema.generated.UserProfileTable.UserBirthdaysIdxTable.UserBirthdaysIdxColumn;
import com.palantir.example.profile.schema.generated.UserProfileTable.UserBirthdaysIdxTable.UserBirthdaysIdxColumnValue;
import com.palantir.example.profile.schema.generated.UserProfileTable.UserBirthdaysIdxTable.UserBirthdaysIdxRow;
import com.palantir.example.profile.schema.generated.UserProfileTable.UserProfileRow;
import com.palantir.util.crypto.Sha256Hash;

public class ProfileStore {
    final TransactionManager txnMgr;
    final Transaction t;
    final ProfileTableFactory tables = ProfileTableFactory.of();

    public ProfileStore(TransactionManager txnMgr, Transaction t) {
        this.txnMgr = txnMgr;
        this.t = t;
    }

    private UUID getNewId() {
        return UUID.randomUUID();
    }

    public UUID storeNewUser(UserProfile data) {
        UUID userId = getNewId();
        UserProfileTable table = tables.getUserProfileTable(t);
        table.putMetadata(UserProfileRow.of(userId), data);
        return userId;
    }

    public UserProfile getUserData(UUID userId) {
        UserProfileTable table = tables.getUserProfileTable(t);
        Map<UserProfileRow, UserProfile> result = table.getMetadatas(ImmutableSet.of(UserProfileRow.of(userId)));
        if (result.isEmpty()) {
            return null;
        } else {
            return Iterables.getOnlyElement(result.values());
        }
    }

    private Long getPhotoStreamId(UUID userId) {
        UserProfileTable table = tables.getUserProfileTable(t);
        Map<UserProfileRow, Long> result = table.getPhotoStreamIds(ImmutableSet.of(UserProfileRow.of(userId)));
        if (result.isEmpty()) {
            return null;
        } else {
            return Iterables.getOnlyElement(result.values());
        }
    }

    public InputStream getImageForUser(UUID userId) {
        Long photoId = getPhotoStreamId(userId);
        if (photoId == null) {
            return null;
        }
        UserPhotosStreamStore streamStore = UserPhotosStreamStore.of(txnMgr, tables);
        return streamStore.loadStream(t, photoId);
    }

    public void updateImage(UUID userId, Sha256Hash hash, InputStream imageData) {
        UserProfile userData = getUserData(userId);
        Preconditions.checkNotNull(userData);

        UserPhotosStreamStore streamStore = UserPhotosStreamStore.of(txnMgr, tables);
        Long oldStreamId = getPhotoStreamId(userId);
        if (oldStreamId != null) {
            // Unmark old stream before we overwrite it.
            streamStore.unmarkStreamAsUsed(t, oldStreamId, EncodingUtils.encodeUUID(userId));
        }

        // This will either store a new stream and mark it as used or return an old stream that matches the hash and mark it as used.
        long streamId = streamStore.getByHashOrStoreStreamAndMarkAsUsed(t, hash, imageData, EncodingUtils.encodeUUID(userId));

        UserProfileTable table = tables.getUserProfileTable(t);
        table.putPhotoStreamId(UserProfileRow.of(userId), streamId);
    }

    public void deleteImage(UUID userId) {
        Long streamId = getPhotoStreamId(userId);
        if (streamId == null) {
            return;
        }
        UserProfileTable table = tables.getUserProfileTable(t);
        table.deletePhotoStreamId(UserProfileRow.of(userId));

        UserPhotosStreamStore streamStore = UserPhotosStreamStore.of(txnMgr, tables);
        streamStore.unmarkStreamAsUsed(t, streamId, EncodingUtils.encodeUUID(userId));
    }

    public Set<UUID> getUsersWithBirthday(long birthEpochDays) {
        UserProfileTable table = tables.getUserProfileTable(t);
        UserBirthdaysIdxTable idx = UserBirthdaysIdxTable.of(table);
        List<UserBirthdaysIdxColumnValue> columns = idx.getRowColumns(UserBirthdaysIdxRow.of(birthEpochDays));

        return IterableView.of(columns)
                .transform(UserBirthdaysIdxColumnValue.getColumnNameFun())
                .transform(UserBirthdaysIdxColumn.getIdFun())
                .immutableSetCopy();
    }

}
